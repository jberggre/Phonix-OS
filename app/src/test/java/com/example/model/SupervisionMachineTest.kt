package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every band and boundary of the restart rules, pinned. The bug class this guards against is a test
 * that disagrees with its own threshold, so the numbers below are written as literals and checked
 * against the policy constants only where the constant itself is the subject.
 */
class SupervisionMachineTest {

    private val policy = SupervisionPolicy()

    private fun state(
        phase: SupervisionPhase = SupervisionPhase.STARTING,
        failures: Int = 0,
        ticks: Int = 0,
        healthySince: Long? = null
    ) = SupervisionState(
        serviceId = "svc",
        phase = phase,
        failures = failures,
        unhealthyTicks = ticks,
        healthySinceMs = healthySince
    )

    // ---- backoff arithmetic ------------------------------------------------------------------

    @Test
    fun backoffDoublesFromTwoSeconds() {
        assertEquals(2_000L, policy.delayAfterFailure(1))
        assertEquals(4_000L, policy.delayAfterFailure(2))
        assertEquals(8_000L, policy.delayAfterFailure(3))
        assertEquals(16_000L, policy.delayAfterFailure(4))
        assertEquals(32_000L, policy.delayAfterFailure(5))
    }

    @Test
    fun backoffIsCappedRatherThanGrowingWithoutBound() {
        // 2000 * 2^5 = 64000, which the 60 s cap must flatten.
        assertEquals(60_000L, policy.delayAfterFailure(6))
        assertEquals(60_000L, policy.delayAfterFailure(20))
        val tight = SupervisionPolicy(baseDelayMs = 1_000, maxDelayMs = 5_000)
        assertEquals(1_000L, tight.delayAfterFailure(1))
        assertEquals(2_000L, tight.delayAfterFailure(2))
        assertEquals(4_000L, tight.delayAfterFailure(3))
        assertEquals(5_000L, tight.delayAfterFailure(4))
        assertEquals(5_000L, tight.delayAfterFailure(9))
    }

    @Test
    fun theAttemptCapIsInclusiveOfTheLastLaunch() {
        assertFalse(policy.gaveUp(4))
        assertTrue(policy.gaveUp(5))
        assertTrue(policy.gaveUp(6))
        assertFalse(SupervisionPolicy(maxAttempts = 1).gaveUp(0))
        assertTrue(SupervisionPolicy(maxAttempts = 1).gaveUp(1))
    }

    // ---- the loop --------------------------------------------------------------------------

    @Test
    fun launchMovesToStartingWithoutClaimingHealth() {
        val transition = SupervisionMachine.reduce(state(phase = SupervisionPhase.IDLE), SupervisionEvent.Launched, policy)
        assertEquals(SupervisionPhase.STARTING, transition.state.phase)
        assertEquals(ServiceHealth.UNKNOWN, transition.state.health)
        assertEquals(SupervisionAction.None, transition.action)
    }

    @Test
    fun theFirstHealthyProbeIsWhatMarksItRunning() {
        val probed = SupervisionMachine.reduce(
            state(),
            SupervisionEvent.Probed(ServiceHealth.HEALTHY, atMs = 5_000),
            policy
        )
        assertEquals(SupervisionPhase.RUNNING, probed.state.phase)
        assertEquals(ServiceHealth.HEALTHY, probed.state.health)
        assertEquals(5_000L, probed.state.healthySinceMs)
    }

    @Test
    fun aFirstExitBacksOffForTwoSeconds() {
        val exit = SupervisionMachine.reduce(state(), SupervisionEvent.Exited(atMs = 1_000), policy)
        assertEquals(SupervisionPhase.BACKOFF, exit.state.phase)
        assertEquals(1, exit.state.failures)
        assertEquals(2_000L, exit.state.retryInMs)
        assertEquals(SupervisionAction.RelaunchAfter(2_000), exit.action)
    }

    @Test
    fun theFifthFailureGivesUpInsteadOfBackingOffAgain() {
        val exit = SupervisionMachine.reduce(
            state(phase = SupervisionPhase.RUNNING, failures = 4, healthySince = null),
            SupervisionEvent.Exited(atMs = 2_000),
            policy
        )
        assertEquals(SupervisionPhase.GAVE_UP, exit.state.phase)
        assertEquals(5, exit.state.failures)
        assertEquals(SupervisionAction.GiveUp, exit.action)
        assertEquals(null, exit.state.retryInMs)
        assertTrue(SupervisionMachine.describe(exit.state).contains("Gave up after 5"))
    }

    @Test
    fun aRunThatStayedHealthyResetsTheBudget() {
        // Healthy from 0, died at 61 s: past the 60 s stability window, so this counts as a first
        // failure again rather than the fifth.
        val exit = SupervisionMachine.reduce(
            state(phase = SupervisionPhase.RUNNING, failures = 4, healthySince = 0L),
            SupervisionEvent.Exited(atMs = 61_000),
            policy
        )
        assertEquals(1, exit.state.failures)
        assertEquals(SupervisionPhase.BACKOFF, exit.state.phase)
        assertEquals(SupervisionAction.RelaunchAfter(2_000), exit.action)
    }

    @Test
    fun aRunJustShortOfTheWindowDoesNotReset() {
        val exit = SupervisionMachine.reduce(
            state(phase = SupervisionPhase.RUNNING, failures = 2, healthySince = 0L),
            SupervisionEvent.Exited(atMs = 59_999),
            policy
        )
        assertEquals(3, exit.state.failures)
        assertEquals(SupervisionAction.RelaunchAfter(8_000), exit.action)
    }

    @Test
    fun unhealthyButAliveIsStoppedOnlyAfterTheTolerance() {
        var current = state(phase = SupervisionPhase.RUNNING, healthySince = 0L)
        val first = SupervisionMachine.reduce(current, SupervisionEvent.Probed(ServiceHealth.UNHEALTHY, 1_000), policy)
        assertEquals(1, first.state.unhealthyTicks)
        assertEquals(SupervisionAction.None, first.action)
        current = first.state
        val second = SupervisionMachine.reduce(current, SupervisionEvent.Probed(ServiceHealth.UNHEALTHY, 2_000), policy)
        assertEquals(2, second.state.unhealthyTicks)
        assertEquals(SupervisionAction.None, second.action)
        val third = SupervisionMachine.reduce(second.state, SupervisionEvent.Probed(ServiceHealth.UNHEALTHY, 3_000), policy)
        assertEquals(3, third.state.unhealthyTicks)
        assertEquals(SupervisionAction.StopProcess, third.action)
    }

    @Test
    fun aHealthyProbeClearsTheUnhealthyStreak() {
        val recovered = SupervisionMachine.reduce(
            state(phase = SupervisionPhase.RUNNING, ticks = 2),
            SupervisionEvent.Probed(ServiceHealth.HEALTHY, 4_000),
            policy
        )
        assertEquals(0, recovered.state.unhealthyTicks)
        assertEquals(ServiceHealth.HEALTHY, recovered.state.health)
    }

    @Test
    fun stoppingByTheUserIsNotAFailure() {
        val stopped = SupervisionMachine.reduce(
            state(phase = SupervisionPhase.RUNNING, failures = 3, healthySince = 0L),
            SupervisionEvent.Stopped,
            policy
        )
        assertEquals(SupervisionPhase.STOPPED, stopped.state.phase)
        assertEquals(ServiceHealth.UNKNOWN, stopped.state.health)
        assertEquals(SupervisionAction.None, stopped.action)
        // The budget is untouched, so a later start does not begin one launch from GAVE_UP.
        assertEquals(3, stopped.state.failures)
    }

    // ---- health evaluation -----------------------------------------------------------------

    @Test
    fun aliveButSilentIsNotHealthy() {
        assertEquals(
            ServiceHealth.UNHEALTHY,
            SupervisionMachine.evaluate(ServiceProbe.Http(8080), ServiceObservation(processAlive = true, httpStatus = null))
        )
        assertEquals(
            ServiceHealth.HEALTHY,
            SupervisionMachine.evaluate(ServiceProbe.Http(8080), ServiceObservation(processAlive = true, httpStatus = 200))
        )
        assertEquals(
            ServiceHealth.UNHEALTHY,
            SupervisionMachine.evaluate(ServiceProbe.Http(8080), ServiceObservation(processAlive = true, httpStatus = 503))
        )
        assertEquals(
            ServiceHealth.UNHEALTHY,
            SupervisionMachine.evaluate(ServiceProbe.Http(8080), ServiceObservation(processAlive = false, httpStatus = 200))
        )
    }

    @Test
    fun processProbeIgnoresHttpEntirely() {
        val probe = ServiceProbe.Process("pidof nginx")
        assertEquals(ServiceHealth.HEALTHY, SupervisionMachine.evaluate(probe, ServiceObservation(processAlive = true)))
        assertEquals(ServiceHealth.UNHEALTHY, SupervisionMachine.evaluate(probe, ServiceObservation(processAlive = false, httpStatus = 200)))
        assertEquals("process", probe.describe)
        assertEquals("http://127.0.0.1:8080/health", ServiceProbe.Http(8080).describe)
    }

    @Test
    fun aServiceThatCannotBeProbedIsReportedNotAssumedHealthy() {
        val state = SupervisionState("svc", phase = SupervisionPhase.STARTING)
        val describe = SupervisionMachine.describe(state)
        assertTrue(describe.contains("health not observed"))
        assertFalse(describe.contains("healthy"))
    }
}
