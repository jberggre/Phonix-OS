package com.example.model

import java.util.Locale

/**
 * How a supervised service proves it is alive.
 *
 * A port that answers is the strongest signal, so anything that listens gets an HTTP probe. A service
 * with no HTTP surface falls back to the process: alive is alive, and the supervisor says which
 * signal it used instead of pretending both are equal.
 */
sealed interface ServiceProbe {
    val describe: String

    data class Process(val aliveCommand: String) : ServiceProbe {
        override val describe: String get() = "process"
    }

    data class Http(val port: Int, val path: String = "/health") : ServiceProbe {
        override val describe: String get() = "http://127.0.0.1:$port$path"
    }
}

/**
 * The restart budget. Every number here is a decision, not a default that happened:
 *
 *  - **Backoff grows by doubling from 2 s** so a service that dies instantly does not spin the CPU,
 *    while one that dies after a long run is back within seconds. The cap matters more than the base:
 *    unbounded doubling at 30 s intervals reaches hours, which reads as "never comes back".
 *  - **The attempt cap is 5.** Five launches is 2+4+8+16 s of waiting — about 30 s of trying — after
 *    which the state becomes GAVE_UP rather than looping forever. A supervisor that retries infinitely
 *    hides the failure; this one reports it and stops.
 *  - **A run that stayed healthy for 60 s resets the budget.** Without that, a service that ran for a
 *    week and then died would be one failure away from GAVE_UP. With it, only flapping exhausts the
 *    budget, which is the case the cap exists for.
 *  - **Three consecutive unhealthy probes while the process is alive** means unhealthy-but-alive is a
 *    real failure, not a slow start: the engine stops it and restarts it under the same budget.
 */
data class SupervisionPolicy(
    val maxAttempts: Int = 5,
    val baseDelayMs: Long = 2_000L,
    val maxDelayMs: Long = 60_000L,
    val stableAfterMs: Long = 60_000L,
    val probeIntervalMs: Long = 5_000L,
    val startTimeoutMs: Long = 30_000L,
    val unhealthyTolerance: Int = 3
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must allow at least one launch" }
        require(baseDelayMs >= 1L) { "a zero delay would spin" }
        require(maxDelayMs >= baseDelayMs) { "the cap cannot be below the base" }
        require(unhealthyTolerance >= 1) { "one unhealthy probe must at least be noticed" }
    }

    /** Wait before the next launch, after [failures] consecutive failed runs (1-based). */
    fun delayAfterFailure(failures: Int): Long {
        require(failures >= 1) { "failures counts from 1" }
        var delay = baseDelayMs
        var doublings = failures - 1
        while (doublings > 0 && delay < maxDelayMs) {
            delay = minOf(delay * 2, maxDelayMs)
            doublings--
        }
        return delay
    }

    /** True once this failure count has used the whole budget. */
    fun gaveUp(failures: Int): Boolean = failures >= maxAttempts

    /** A run that stayed healthy this long is not flapping, so its failures do not accumulate. */
    fun isStable(upForMs: Long): Boolean = upForMs >= stableAfterMs
}

enum class SupervisionPhase { IDLE, STARTING, RUNNING, BACKOFF, GAVE_UP, STOPPED }

/** HEALTHY is an observation, never an assumption: UNKNOWN means nothing has answered yet. */
enum class ServiceHealth { UNKNOWN, HEALTHY, UNHEALTHY }

/** What one probe saw. [httpStatus] is null when the probe had no HTTP surface to ask. */
data class ServiceObservation(
    val processAlive: Boolean,
    val httpStatus: Int? = null
)

data class SupervisionState(
    val serviceId: String,
    val phase: SupervisionPhase = SupervisionPhase.IDLE,
    val health: ServiceHealth = ServiceHealth.UNKNOWN,
    /** Consecutive failed runs, reset by a run that stayed healthy past the stability window. */
    val failures: Int = 0,
    val unhealthyTicks: Int = 0,
    /** Set while BACKOFF, so the UI can say when the next launch is. */
    val retryInMs: Long? = null,
    /** When this run first reported HEALTHY, used to decide whether it was stable. */
    val healthySinceMs: Long? = null,
    val detail: String = ""
)

sealed interface SupervisionEvent {
    data object Launched : SupervisionEvent
    data class Probed(val health: ServiceHealth, val atMs: Long) : SupervisionEvent
    data class Exited(val atMs: Long, val detail: String = "") : SupervisionEvent
    data object Stopped : SupervisionEvent
}

sealed interface SupervisionAction {
    data object None : SupervisionAction
    data object Launch : SupervisionAction
    data class RelaunchAfter(val delayMs: Long) : SupervisionAction
    /** Stop the process, then treat the stop as an exit and re-enter the budget. */
    data object StopProcess : SupervisionAction
    data object GiveUp : SupervisionAction
}

data class SupervisionTransition(val state: SupervisionState, val action: SupervisionAction)

/**
 * The decision table, separated from the coroutine that runs it so every band is unit-testable.
 */
object SupervisionMachine {

    fun evaluate(probe: ServiceProbe, observation: ServiceObservation): ServiceHealth = when (probe) {
        is ServiceProbe.Process -> if (observation.processAlive) ServiceHealth.HEALTHY else ServiceHealth.UNHEALTHY
        is ServiceProbe.Http -> when {
            // No process means no listener, whatever the last status was.
            !observation.processAlive -> ServiceHealth.UNHEALTHY
            // Alive but silent is not healthy: this is the state the old code reported as success.
            observation.httpStatus == null -> ServiceHealth.UNHEALTHY
            observation.httpStatus in 200..299 -> ServiceHealth.HEALTHY
            else -> ServiceHealth.UNHEALTHY
        }
    }

    fun reduce(
        state: SupervisionState,
        event: SupervisionEvent,
        policy: SupervisionPolicy
    ): SupervisionTransition = when (event) {
        is SupervisionEvent.Launched -> SupervisionTransition(
            state.copy(
                phase = SupervisionPhase.STARTING,
                health = ServiceHealth.UNKNOWN,
                unhealthyTicks = 0,
                retryInMs = null,
                healthySinceMs = null,
                detail = ""
            ),
            SupervisionAction.None
        )

        is SupervisionEvent.Probed -> {
            val healthy = event.health == ServiceHealth.HEALTHY
            val healthySince = when {
                healthy && state.healthySinceMs == null -> event.atMs
                healthy -> state.healthySinceMs
                else -> null
            }
            val ticks = if (healthy) 0 else state.unhealthyTicks + 1
            val nextPhase = if (healthy && state.phase == SupervisionPhase.STARTING) {
                SupervisionPhase.RUNNING
            } else {
                state.phase
            }
            // An unhealthy process that keeps failing probes is restarted, but only after the
            // tolerance: a slow boot must not be mistaken for a crash.
            val action = if (!healthy && ticks >= policy.unhealthyTolerance) {
                SupervisionAction.StopProcess
            } else {
                SupervisionAction.None
            }
            SupervisionTransition(
                state.copy(
                    phase = nextPhase,
                    health = event.health,
                    unhealthyTicks = ticks,
                    healthySinceMs = healthySince
                ),
                action
            )
        }

        is SupervisionEvent.Exited -> {
            val upFor = state.healthySinceMs?.let { event.atMs - it } ?: 0L
            val previous = if (policy.isStable(upFor)) 0 else state.failures
            val failures = previous + 1
            if (policy.gaveUp(failures)) {
                SupervisionTransition(
                    state.copy(
                        phase = SupervisionPhase.GAVE_UP,
                        failures = failures,
                        health = ServiceHealth.UNHEALTHY,
                        unhealthyTicks = 0,
                        retryInMs = null,
                        healthySinceMs = null,
                        detail = event.detail.ifBlank { "Stopped after $failures launches." }
                    ),
                    SupervisionAction.GiveUp
                )
            } else {
                val delay = policy.delayAfterFailure(failures)
                SupervisionTransition(
                    state.copy(
                        phase = SupervisionPhase.BACKOFF,
                        failures = failures,
                        health = ServiceHealth.UNHEALTHY,
                        unhealthyTicks = 0,
                        retryInMs = delay,
                        healthySinceMs = null,
                        detail = event.detail
                    ),
                    SupervisionAction.RelaunchAfter(delay)
                )
            }
        }

        is SupervisionEvent.Stopped -> SupervisionTransition(
            state.copy(
                phase = SupervisionPhase.STOPPED,
                health = ServiceHealth.UNKNOWN,
                retryInMs = null,
                unhealthyTicks = 0,
                healthySinceMs = null,
                detail = "Stopped by the user."
            ),
            SupervisionAction.None
        )
    }

    /** One line for the UI: never claims health it has not observed. */
    fun describe(state: SupervisionState): String = when (state.phase) {
        SupervisionPhase.IDLE -> "Not supervised"
        SupervisionPhase.STARTING -> "Starting; health not observed yet"
        SupervisionPhase.RUNNING -> when (state.health) {
            ServiceHealth.HEALTHY -> "Running and healthy"
            ServiceHealth.UNHEALTHY -> "Running but not answering (${state.unhealthyTicks} failed probes)"
            ServiceHealth.UNKNOWN -> "Running; health unknown"
        }
        SupervisionPhase.BACKOFF -> "Restarting in " +
            String.format(Locale.ROOT, "%.1fs", (state.retryInMs ?: 0L) / 1000.0) +
            " (failure ${state.failures})"
        SupervisionPhase.GAVE_UP -> "Gave up after ${state.failures} launches; press Start to try again"
        SupervisionPhase.STOPPED -> "Stopped"
    }
}

/** What the supervisor needs to run one service. Commands are guest shell, not host shell. */
data class ServiceSpec(
    val id: String,
    val name: String,
    val launchCommand: String,
    val stopCommand: String,
    val probe: ServiceProbe,
    /** Prints ph-alive when the process exists; the HTTP probe is layered on top of this. */
    val processCommand: String
)
