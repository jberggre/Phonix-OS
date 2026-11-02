package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.engine.LinuxRuntime
import com.example.engine.ServiceSupervisor
import com.example.model.ServiceObservation
import com.example.model.ServiceProbe
import com.example.model.ServiceSpec
import com.example.model.SupervisionPhase
import com.example.model.SupervisionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicLong

/**
 * Proves the supervisor on the bench unit, against the real PRoot guest.
 *
 * A unit test with a fake runner shows the loop; this shows that `kill -0` on a pid written by one
 * PRoot invocation is visible to the next one — the assumption the whole design rests on, and the
 * same mechanism the AI server's pid file already uses.
 *
 * Run with:
 *   gradle :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.ServiceSupervisorInstrumentedTest
 */
@RunWith(AndroidJUnit4::class)
class ServiceSupervisorInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = AtomicLong(0L)

    @Before
    fun requireGuest() {
        val container = LinuxRuntime.container(context)
        assumeTrue("no guest rootfs installed; nothing to supervise", container.isInstalled)
    }

    private fun exec(command: String, timeoutSeconds: Long = 60): String {
        val buffer = StringBuilder()
        LinuxRuntime.container(context).execOnce(command, timeoutSeconds) { line ->
            buffer.append(line).append('\n')
        }
        return buffer.toString()
    }

    private fun loopSpec(id: String) = ServiceSpec(
        id = id,
        name = "Supervisor probe loop",
        launchCommand = "rm -f /tmp/$id.pid; nohup sh -c 'while :; do sleep 1; done' > /tmp/$id.log 2>&1 & " +
            "echo \$! > /tmp/$id.pid; sleep 1; " +
            "kill -0 \$(cat /tmp/$id.pid) 2>/dev/null && echo ph-launched || echo ph-missing",
        stopCommand = "kill \$(cat /tmp/$id.pid) 2>/dev/null; rm -f /tmp/$id.pid; true",
        probe = ServiceProbe.Process("pidof sh"),
        processCommand = "kill -0 \$(cat /tmp/$id.pid) 2>/dev/null && echo ph-alive || echo ph-dead"
    )

    private suspend fun awaitPhase(
        supervisor: ServiceSupervisor,
        id: String,
        phase: SupervisionPhase,
        timeoutMs: Long
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (supervisor.state(id)?.phase == phase) return
            delay(250)
        }
        val last = supervisor.state(id)
        throw AssertionError("expected $phase, last state was ${last?.phase} (${last?.detail})")
    }

    @Test
    fun aKilledProcessComesBackUnderThePolicyAndReportsHealthy(): Unit = runBlocking {
        val id = "svprobe"
        exec("rm -f /tmp/$id.pid")
        val supervisor = ServiceSupervisor(
            scope = this,
            runner = com.example.engine.GuestServiceRunner(context),
            policy = SupervisionPolicy(
                maxAttempts = 4,
                baseDelayMs = 500,
                maxDelayMs = 4_000,
                stableAfterMs = 600_000,
                probeIntervalMs = 1_000,
                startTimeoutMs = 15_000
            ),
            clock = { System.currentTimeMillis() }
        )
        val spec = loopSpec(id)
        try {
            supervisor.start(spec)
            awaitPhase(supervisor, id, SupervisionPhase.RUNNING, 40_000)
            val firstPid = exec("cat /tmp/$id.pid").trim()
            assertTrue("the first run should have a pid", firstPid.isNotEmpty())

            // Kill the supervised process the way a crash would: from outside, with no notice.
            exec("kill -9 \$(cat /tmp/$id.pid) 2>/dev/null; true")

            awaitPhase(supervisor, id, SupervisionPhase.BACKOFF, 30_000)
            awaitPhase(supervisor, id, SupervisionPhase.RUNNING, 40_000)

            val secondPid = exec("cat /tmp/$id.pid").trim()
            assertTrue("the restart should have a pid", secondPid.isNotEmpty())
            assertNotEquals("the supervisor must launch a new process, not adopt the dead one", firstPid, secondPid)
            assertEquals(0, supervisor.state(id)!!.unhealthyTicks)
            assertNotNull(supervisor.state(id)!!.healthySinceMs)
            android.util.Log.i(
                "PhonixSupervisor",
                "restarted $id: $firstPid -> $secondPid, phase ${supervisor.state(id)!!.phase}"
            )
        } finally {
            supervisor.stopSupervising(id)
            exec(spec.stopCommand)
        }
    }

    @Test
    fun aProcessThatDiesImmediatelyEndsInGaveUp(): Unit = runBlocking {
        val id = "svgiveup"
        val supervisor = ServiceSupervisor(
            scope = this,
            runner = com.example.engine.GuestServiceRunner(context),
            policy = SupervisionPolicy(
                maxAttempts = 3,
                baseDelayMs = 300,
                maxDelayMs = 1_000,
                stableAfterMs = 600_000,
                probeIntervalMs = 400,
                startTimeoutMs = 5_000
            )
        )
        // Launch "succeeds" but leaves nothing behind, which is exactly the case that used to look
        // like a running service.
        val spec = ServiceSpec(
            id = id,
            name = "Dies immediately",
            launchCommand = "true; echo ph-launched",
            stopCommand = "true",
            probe = ServiceProbe.Process("true"),
            processCommand = "echo ph-dead"
        )
        supervisor.start(spec)
        awaitPhase(supervisor, id, SupervisionPhase.GAVE_UP, 60_000)
        assertEquals(3, supervisor.state(id)!!.failures)
        android.util.Log.i("PhonixSupervisor", "gave up as expected: ${supervisor.state(id)!!.detail}")
    }
    /**
     * Measures what this guest does with a backgrounded child, because the launch path assumes it can
     * hand a pid back. If PRoot waits for its tracee, the launch call blocks instead of returning,
     * which is a fact worth logging rather than guessing at.
     */
    @Test
    fun diagnosesBackgroundLaunch(): Unit = runBlocking {
        val startedAt = System.currentTimeMillis()
        val launch = runCatching {
            exec(
                "rm -f /tmp/diag.pid; nohup sh -c 'sleep 30' > /tmp/diag.log 2>&1 & " +
                    "echo \$! > /tmp/diag.pid; echo ph-launched",
                15
            )
        }
        android.util.Log.i(
            "PhonixSupervisor",
            "diagnostic launch: ${System.currentTimeMillis() - startedAt}ms, " +
                "out='${launch.getOrNull()?.trim()}', err='${launch.exceptionOrNull()?.message}'"
        )
        val pid = runCatching { exec("cat /tmp/diag.pid", 10).trim() }.getOrDefault("")
        android.util.Log.i("PhonixSupervisor", "diagnostic pid file reads '$pid'")
        val alive = runCatching {
            exec("kill -0 \$(cat /tmp/diag.pid) 2>/dev/null && echo ph-alive || echo ph-dead", 10).trim()
        }.getOrDefault("")
        android.util.Log.i("PhonixSupervisor", "diagnostic kill -0 says '$alive'")
        runCatching { exec("kill -9 \$(cat /tmp/diag.pid) 2>/dev/null; true", 10) }
        assertTrue("the diagnostic only has to run", true)
    }

}
