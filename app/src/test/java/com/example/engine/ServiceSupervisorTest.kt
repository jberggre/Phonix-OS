package com.example.engine

import com.example.model.ServiceObservation
import com.example.model.ServiceProbe
import com.example.model.ServiceSpec
import com.example.model.SupervisionPhase
import com.example.model.SupervisionPolicy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The loop around the state machine: does it actually relaunch after a death, stop after the budget,
 * and stay quiet once the user stops supervising? Timings are virtual, so these run in milliseconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceSupervisorTest {

    private val policy = SupervisionPolicy(
        maxAttempts = 3,
        baseDelayMs = 100,
        maxDelayMs = 400,
        stableAfterMs = 60_000,
        probeIntervalMs = 50,
        startTimeoutMs = 2_000
    )

    private val spec = ServiceSpec(
        id = "svc",
        name = "Test service",
        launchCommand = "launch",
        stopCommand = "stop",
        probe = ServiceProbe.Process("pidof test"),
        processCommand = "pidof test"
    )

    private class FakeRunner(var launchSucceeds: Boolean = true) : ServiceSupervisor.Runner {
        val launches = mutableListOf<Long>()
        val stops = mutableListOf<Long>()
        var alive = false
        var clock: () -> Long = { 0L }

        override suspend fun launch(spec: ServiceSpec): Result<Unit> {
            launches += clock()
            if (!launchSucceeds) return Result.failure(IllegalStateException("guest refused"))
            alive = true
            return Result.success(Unit)
        }

        override suspend fun probe(spec: ServiceSpec) = ServiceObservation(processAlive = alive)

        override suspend fun stop(spec: ServiceSpec) {
            stops += clock()
            alive = false
        }
    }

    @Test
    fun aDeadProcessIsRelaunchedAndHealthyAgain() = runTest {
        val runner = FakeRunner().also { it.clock = { testScheduler.currentTime } }
        val supervisor = ServiceSupervisor(backgroundScope, runner, policy) { testScheduler.currentTime }
        supervisor.start(spec)
        advanceTimeBy(100)
        runCurrent()

        assertEquals(1, runner.launches.size)
        assertEquals(SupervisionPhase.RUNNING, supervisor.state("svc")!!.phase)
        assertTrue(SupervisionMachineDescribe.isHealthy(supervisor.state("svc")!!.phase))

        // The process dies while the supervisor is watching it. Poll instead of assuming an exact
        // instant: the probe interval and the backoff delay decide when each transition lands, and
        // asserting a precise millisecond is how this test first failed against its own timeline.
        runner.alive = false
        var guard = 0
        while (supervisor.state("svc")!!.phase != SupervisionPhase.BACKOFF && guard++ < 40) {
            advanceTimeBy(25)
            runCurrent()
        }
        assertEquals(SupervisionPhase.BACKOFF, supervisor.state("svc")!!.phase)
        assertEquals(100L, supervisor.state("svc")!!.retryInMs)
        val launchesBeforeRestart = runner.launches.size

        guard = 0
        while (supervisor.state("svc")!!.phase != SupervisionPhase.RUNNING && guard++ < 40) {
            advanceTimeBy(25)
            runCurrent()
        }
        assertEquals(SupervisionPhase.RUNNING, supervisor.state("svc")!!.phase)
        assertEquals(launchesBeforeRestart + 1, runner.launches.size)
        assertEquals(0, supervisor.state("svc")!!.unhealthyTicks)

        supervisor.stopSupervising("svc")
    }

    @Test
    fun aServiceThatNeverStartsEndsInGaveUpWithTheFullBudgetUsed() = runTest {
        val runner = FakeRunner(launchSucceeds = false).also { it.clock = { testScheduler.currentTime } }
        val supervisor = ServiceSupervisor(backgroundScope, runner, policy) { testScheduler.currentTime }
        supervisor.start(spec)

        // Budget: failures 1 and 2 back off (100 + 200 ms), the third gives up.
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(3, runner.launches.size)
        assertEquals(SupervisionPhase.GAVE_UP, supervisor.state("svc")!!.phase)
        assertEquals(3, supervisor.state("svc")!!.failures)
        assertTrue(supervisor.state("svc")!!.detail.contains("guest refused"))

        // Gave up means gave up: nothing launches afterwards.
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(3, runner.launches.size)
    }

    @Test
    fun stoppingSupervisionStopsTheLaunchesAndLeavesTheProcessAlone() = runTest {
        val runner = FakeRunner().also { it.clock = { testScheduler.currentTime } }
        val supervisor = ServiceSupervisor(backgroundScope, runner, policy) { testScheduler.currentTime }
        supervisor.start(spec)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(1, runner.launches.size)

        supervisor.stopSupervising("svc")
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(1, runner.launches.size)
        assertTrue("stopping must not kill the guest process", runner.stops.isEmpty())
        assertEquals(SupervisionPhase.STOPPED, supervisor.state("svc")!!.phase)
    }

    @Test
    fun anUnhealthyButAliveServiceIsStoppedAndReplaced() = runTest {
        // Launch succeeds, but the process is gone on the very next probe: the supervisor must notice
        // rather than trust the launch status.
        val runner = object : ServiceSupervisor.Runner {
            var launches = 0
            override suspend fun launch(spec: ServiceSpec): Result<Unit> {
                launches++
                return Result.success(Unit)
            }

            override suspend fun probe(spec: ServiceSpec) = ServiceObservation(processAlive = false)

            override suspend fun stop(spec: ServiceSpec) = Unit
        }
        val supervisor = ServiceSupervisor(backgroundScope, runner, policy) { testScheduler.currentTime }
        supervisor.start(spec)
        advanceTimeBy(1_000)
        runCurrent()

        assertTrue("it must retry a launch that produced nothing", runner.launches >= 2)
        assertEquals(SupervisionPhase.GAVE_UP, supervisor.state("svc")!!.phase)
    }
}

/** Tiny helper so the assertions read as intent rather than as enum comparisons. */
private object SupervisionMachineDescribe {
    fun isHealthy(phase: SupervisionPhase): Boolean = phase == SupervisionPhase.RUNNING
}
