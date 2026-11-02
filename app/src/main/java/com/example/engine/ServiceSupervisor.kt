package com.example.engine

import android.content.Context
import com.example.model.ManifestService
import com.example.model.ServiceHealth
import com.example.model.ServiceObservation
import com.example.model.ServiceProbe
import com.example.model.ServiceSpec
import com.example.model.SupervisionAction
import com.example.model.SupervisionEvent
import com.example.model.SupervisionMachine
import com.example.model.SupervisionPolicy
import com.example.model.SupervisionState
import com.example.model.ServerServiceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Keeps long-running guest services alive, because PRoot has no init to do it.
 *
 * The loop is deliberately thin: every decision lives in [SupervisionMachine], which is pure and
 * tested. This class only launches, probes, waits and publishes, so a bug in the restart rules is
 * reproducible in a unit test instead of only on a phone.
 *
 * It never claims health it has not seen. A launch command returning 0 proves nothing, so the state
 * only reaches HEALTHY after a probe answers, and a service whose health cannot be read ends in
 * GAVE_UP with a reason rather than sitting in a reassuring green state.
 */
class ServiceSupervisor(
    private val scope: CoroutineScope,
    private val runner: Runner,
    private val policy: SupervisionPolicy = SupervisionPolicy(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    /** The guest-facing operations, injectable so the loop can be tested without a phone. */
    interface Runner {
        suspend fun launch(spec: ServiceSpec): Result<Unit>
        suspend fun probe(spec: ServiceSpec): ServiceObservation
        suspend fun stop(spec: ServiceSpec)
    }

    private val _states = MutableStateFlow<Map<String, SupervisionState>>(emptyMap())
    val states: StateFlow<Map<String, SupervisionState>> = _states.asStateFlow()

    private val jobs = ConcurrentHashMap<String, Job>()

    fun state(serviceId: String): SupervisionState? = _states.value[serviceId]

    /** Starts supervising [spec]; an earlier supervision of the same id is replaced. */
    fun start(spec: ServiceSpec) {
        jobs.remove(spec.id)?.cancel()
        publish(SupervisionState(spec.id))
        val job = scope.launch { supervise(spec) }
        jobs[spec.id] = job
        job.invokeOnCompletion { jobs.remove(spec.id, job) }
    }

    /**
     * Stops supervising without killing the guest process.
     *
     * The process keeps running, which is what the existing terminal-driven workflow does; killing
     * a user's server because they closed a panel would be a behaviour change nobody asked for.
     */
    fun stopSupervising(serviceId: String) {
        jobs.remove(serviceId)?.cancel()
        val current = _states.value[serviceId] ?: SupervisionState(serviceId)
        publish(SupervisionMachine.reduce(current, SupervisionEvent.Stopped, policy).state)
    }

    fun stopAll() {
        jobs.keys.toList().forEach { stopSupervising(it) }
    }

    private fun publish(state: SupervisionState) {
        _states.update { it + (state.serviceId to state) }
        // Wrapped: android.util.Log is a stub on the JVM, and a unit test must not die on a log line.
        runCatching {
            android.util.Log.i(
                "PhonixSupervisor",
                state.serviceId + ": " + state.phase + " health=" + state.health +
                    " failures=" + state.failures +
                    if (state.detail.isBlank()) "" else " (" + state.detail + ")"
            )
        }
    }

    private suspend fun supervise(spec: ServiceSpec) {
        var state = _states.value[spec.id] ?: SupervisionState(spec.id)

        fun apply(event: SupervisionEvent): SupervisionAction {
            val transition = SupervisionMachine.reduce(state, event, policy)
            state = transition.state
            publish(state)
            return transition.action
        }

        while (currentCoroutineContext().isActive) {
            apply(SupervisionEvent.Launched)
            val launched = runCatching { runner.launch(spec) }
                .getOrElse { error -> Result.failure(error) }
            val action = if (launched.isFailure) {
                val reason = launched.exceptionOrNull()?.message ?: "the guest did not start it"
                state = state.copy(detail = reason)
                apply(SupervisionEvent.Exited(clock(), reason))
            } else {
                watch(spec, ::apply)
            }

            when (action) {
                is SupervisionAction.RelaunchAfter -> delay(action.delayMs)
                SupervisionAction.GiveUp -> return
                else -> return
            }
        }
    }

    /**
     * Watches one run: waits for the first healthy probe inside [SupervisionPolicy.startTimeoutMs],
     * then polls until the process dies or the machine says to stop it.
     */
    private suspend fun watch(
        spec: ServiceSpec,
        apply: (SupervisionEvent) -> SupervisionAction
    ): SupervisionAction {
        val startedAt = clock()
        while (currentCoroutineContext().isActive) {
            val observation = runCatching { runner.probe(spec) }
            if (observation.isFailure) {
                return apply(
                    SupervisionEvent.Exited(
                        clock(),
                        "probe failed: " + (observation.exceptionOrNull()?.message ?: "the guest did not answer")
                    )
                )
            }
            val seen = observation.getOrThrow()
            val health = SupervisionMachine.evaluate(spec.probe, seen)
            val action = apply(SupervisionEvent.Probed(health, clock()))
            if (action is SupervisionAction.StopProcess) {
                runCatching { runner.stop(spec) }
                return apply(SupervisionEvent.Exited(clock(), "stopped: not answering while alive"))
            }
            if (!seen.processAlive) {
                return apply(SupervisionEvent.Exited(clock(), "the process exited"))
            }
            if (health == ServiceHealth.HEALTHY) break
            if (clock() - startedAt >= policy.startTimeoutMs) {
                // Alive but never answered: stop it rather than leaving a half-started service.
                runCatching { runner.stop(spec) }
                return apply(
                    SupervisionEvent.Exited(
                        clock(),
                        "no healthy probe within ${policy.startTimeoutMs / 1000}s"
                    )
                )
            }
            delay(STARTUP_POLL_MS)
        }

        while (currentCoroutineContext().isActive) {
            delay(policy.probeIntervalMs)
            val observation = runCatching { runner.probe(spec) }
            if (observation.isFailure) {
                return apply(
                    SupervisionEvent.Exited(
                        clock(),
                        "probe failed: " + (observation.exceptionOrNull()?.message ?: "the guest did not answer")
                    )
                )
            }
            val seen = observation.getOrThrow()
            val health = SupervisionMachine.evaluate(spec.probe, seen)
            val action = apply(SupervisionEvent.Probed(health, clock()))
            if (action is SupervisionAction.StopProcess) {
                runCatching { runner.stop(spec) }
                return apply(SupervisionEvent.Exited(clock(), "unhealthy while the process stayed alive"))
            }
            if (!seen.processAlive) {
                return apply(SupervisionEvent.Exited(clock(), "the process exited"))
            }
        }
        return SupervisionAction.None
    }

    companion object {
        /** First healthy answer is often a second or two after launch; this is a poll, not a timeout. */
        private const val STARTUP_POLL_MS = 500L


        /**
         * Turns a catalog entry into something the supervisor can run: a launch that leaves a
         * process behind, a stop that matches how it was started, and the strongest probe available.
         */
        fun specFor(item: ServerServiceItem): ServiceSpec {
            val log = "/tmp/phonix-${item.id}.log"
            val launch = "cd /root 2>/dev/null || cd /; " +
                "nohup sh -c '${item.startHint.replace("'", "'\\''")}' > $log 2>&1 & " +
                "sleep 2; pidof ${item.processName} >/dev/null 2>&1 && echo ph-launched || echo ph-missing"
            val stop = "for p in \$(pidof ${item.processName}); do kill \$p 2>/dev/null; done; sleep 1; true"
            val probe = if (item.protocol == "HTTP" || item.protocol == "HTTPS") {
                ServiceProbe.Http(port = item.port)
            } else {
                ServiceProbe.Process("pidof ${item.processName}")
            }
            return ServiceSpec(
                id = item.id,
                name = item.name,
                launchCommand = launch,
                stopCommand = stop,
                probe = probe,
                processCommand = "pidof ${item.processName} >/dev/null 2>&1 && echo ph-alive || echo ph-dead"
            )
        }

        /**
         * The same contract as the catalog [specFor], built from a manifest declaration: launch, stop
         * and probe come from the file, and the identity comes from the Hub item. The launch shape is
         * deliberately identical so a manifest service behaves like a catalog one on this PRoot.
         */
        fun specFor(item: ServerServiceItem, service: ManifestService): ServiceSpec {
            val log = "/tmp/phonix-${service.id}.log"
            val launch = "cd /root 2>/dev/null || cd /; " +
                "nohup sh -c '${service.launchCommand.replace("'", "'\\''")}' > $log 2>&1 & " +
                "sleep 2; pidof ${service.processName} >/dev/null 2>&1 && echo ph-launched || echo ph-missing"
            return ServiceSpec(
                id = service.id,
                name = service.name,
                launchCommand = launch,
                stopCommand = service.stopCommand,
                probe = service.probe,
                processCommand = "pidof ${service.processName} >/dev/null 2>&1 && echo ph-alive || echo ph-dead"
            )
        }
    }
}

/**
 * Runs supervisor operations inside the active desktop guest.
 *
 * `execOnce` is blocking and each call is a fresh PRoot launch, so everything here hops to IO. The
 * HTTP half talks to the guest over loopback exactly like the AI's `/health` gate does.
 */
class GuestServiceRunner(private val context: Context) : ServiceSupervisor.Runner {

    private companion object {
        /** How long a launch may take to answer its liveness probe before it counts as failed. */
        const val LAUNCH_CONFIRM_MS = 20_000L

        /** A supervised service may legitimately run for hours; its launch thread waits that long. */
        const val LAUNCH_LIFETIME_SECONDS = 12L * 60L * 60L
    }


    private val http = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.SECONDS)
        .build()

    /**
     * Starts the service and reports success from the process, not from the command returning.
     *
     * Measured on the bench unit: PRoot does not detach a backgrounded child, so a launch command that
     * leaves a server running blocks until that server exits - the diagnostic returned "Guest command
     * timed out after 15s" while the pid file was already written and `kill -0` answered alive. The
     * blocking exec therefore runs on its own thread for the service's lifetime, and this returns as
     * soon as the process answers a liveness probe. Stopping the service releases that thread.
     */
    override suspend fun launch(spec: ServiceSpec): Result<Unit> = withContext(Dispatchers.IO) {
        val runner = Thread({
            runCatching { exec(spec.launchCommand, LAUNCH_LIFETIME_SECONDS) }
        }, "phonix-service-" + spec.id).apply {
            isDaemon = true
            start()
        }

        val deadline = System.currentTimeMillis() + LAUNCH_CONFIRM_MS
        while (System.currentTimeMillis() < deadline) {
            delay(500)
            val alive = runCatching { exec(spec.processCommand, 15) }
                .getOrDefault("")
                .contains("ph-alive")
            if (alive) return@withContext Result.success(Unit)
            if (!runner.isAlive) break
        }
        Result.failure(
            IllegalStateException(
                "The guest did not keep " + spec.name + " running; nothing answered the liveness probe."
            )
        )
    }

    override suspend fun probe(spec: ServiceSpec): ServiceObservation = withContext(Dispatchers.IO) {
        val alive = runCatching { exec(spec.processCommand, 15) }.getOrDefault("").contains("ph-alive")
        val status = if (alive && spec.probe is ServiceProbe.Http) httpStatus(spec.probe) else null
        ServiceObservation(processAlive = alive, httpStatus = status)
    }

    override suspend fun stop(spec: ServiceSpec) = withContext(Dispatchers.IO) {
        runCatching { exec(spec.stopCommand, 20) }
        Unit
    }

    private fun httpStatus(probe: ServiceProbe.Http): Int? = try {
        val url = "http://127.0.0.1:${probe.port}${if (probe.path.startsWith("/")) probe.path else "/${probe.path}"}"
        http.newCall(Request.Builder().url(url).build()).execute().use { it.code }
    } catch (_: Throwable) {
        null
    }

    private fun exec(command: String, timeoutSeconds: Long): String {
        val buffer = StringBuilder()
        LinuxRuntime.container(context).execOnce(command, timeoutSeconds) { line ->
            buffer.append(line).append('\n')
        }
        return buffer.toString()
    }
}
