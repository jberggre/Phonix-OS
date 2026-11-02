package com.example.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The guard is only useful if it refuses loudly and stays quiet otherwise: a policy that reports
 * every phone as hot is as bad as no policy, and one that silently allows everything is worse.
 */
class ThermalPolicyTest {

    @Test
    fun noSensorMeansNoOpinionRatherThanZeroDegrees() {
        val absent = ThermalReading(hottestC = null, hasReading = false)
        assertEquals(ThermalVerdict.UNKNOWN, ThermalPolicy.verdict(absent))
        // 0 C is not a plausible phone temperature; a stale zero must not read as cool and unlock.
        assertEquals(ThermalVerdict.UNKNOWN, ThermalPolicy.verdict(ThermalReading(0f, hasReading = false)))
        assertTrue(ThermalPolicy.mayStartSustainedWork(absent))
        assertNull(ThermalPolicy.refusal(absent, "Local AI"))
    }

    @Test
    fun coolPhoneStartsWithoutComment() {
        val cool = ThermalReading(41f, isCharging = false, hasReading = true)
        assertEquals(ThermalVerdict.OK, ThermalPolicy.verdict(cool))
        assertTrue(ThermalPolicy.mayStartSustainedWork(cool))
        assertNull(ThermalPolicy.refusal(cool, "Local AI"))
        assertNull(ThermalPolicy.caution(cool))
    }

    @Test
    fun warmPhoneStartsButSaysSo() {
        val warm = ThermalReading(58f, isCharging = false, hasReading = true)
        assertEquals(ThermalVerdict.WARM, ThermalPolicy.verdict(warm))
        assertTrue(ThermalPolicy.mayStartSustainedWork(warm))
        assertNull(ThermalPolicy.refusal(warm, "Local AI"))
        assertNotNull(ThermalPolicy.caution(warm))
        assertTrue(ThermalPolicy.caution(warm)!!.contains("58"))
    }

    @Test
    fun chargingLowersTheWarmThreshold() {
        // Below the charging band, charging is still fine: 45 C is under 48 C.
        assertEquals(ThermalVerdict.OK, ThermalPolicy.verdict(ThermalReading(45f, isCharging = false, hasReading = true)))
        assertEquals(ThermalVerdict.OK, ThermalPolicy.verdict(ThermalReading(45f, isCharging = true, hasReading = true)))
        // At the charging threshold and above, charging is what makes it warm.
        assertEquals(ThermalVerdict.OK, ThermalPolicy.verdict(ThermalReading(48f, isCharging = false, hasReading = true)))
        assertEquals(ThermalVerdict.WARM, ThermalPolicy.verdict(ThermalReading(48f, isCharging = true, hasReading = true)))
        val caution = ThermalPolicy.caution(ThermalReading(50f, isCharging = true, hasReading = true))!!
        assertTrue(caution.contains("charging"))
        // The tail must survive the charging branch; it used to be dropped there.
        assertTrue(caution.contains("throttle"))
    }

    @Test
    fun hotPhoneIsRefusedWithTheNumberAndTheLimit() {
        val hot = ThermalReading(74f, isCharging = true, hasReading = true)
        assertEquals(ThermalVerdict.HOT, ThermalPolicy.verdict(hot))
        assertFalse(ThermalPolicy.mayStartSustainedWork(hot))
        val refusal = ThermalPolicy.refusal(hot, "Local AI")!!
        assertTrue(refusal.contains("74"))
        assertTrue(refusal.contains("70"))
        assertTrue(refusal.contains("Local AI"))
    }
}
