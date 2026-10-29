package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import java.io.File
import kotlin.math.roundToInt

/**
 * Real device measurements for the Vitals widget.
 *
 * Everything here was verified readable by the app's own uid on a Galaxy S10
 * (Android 12): /proc/meminfo, the thermal zones and the per-core cpufreq nodes.
 * /proc/stat and /proc/loadavg are NOT readable by apps on modern Android, so "CPU"
 * is reported as average core frequency utilisation rather than a kernel load figure -
 * it tracks actual work the same way most system monitors on Android do.
 */
object DeviceTelemetry {

    private const val TAG = "PhonixTelemetry"
    private const val CPU_ROOT = "/sys/devices/system/cpu"
    private const val THERMAL_ROOT = "/sys/class/thermal"

    data class Snapshot(
        val cpuPercent: Int,
        val ramUsedMb: Int,
        val ramTotalMb: Int,
        val tempC: Float,
        val batteryLevel: Int,
        val isCharging: Boolean
    )

    fun read(context: Context): Snapshot {
        val (totalMb, usedMb) = readMemory()
        val battery = readBattery(context)
        return Snapshot(
            cpuPercent = readCpuPercent(),
            ramUsedMb = usedMb,
            ramTotalMb = totalMb,
            tempC = readTemperature() ?: battery.tempC,
            batteryLevel = battery.level,
            isCharging = battery.charging
        )
    }

    // --- memory -------------------------------------------------------------------

    private fun readMemory(): Pair<Int, Int> {
        var totalKb = 0L
        var availableKb = 0L
        try {
            File("/proc/meminfo").forEachLine { line ->
                when {
                    line.startsWith("MemTotal:") -> totalKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    line.startsWith("MemAvailable:") -> availableKb = line.filter { it.isDigit() }.toLongOrNull() ?: 0L
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "could not read /proc/meminfo", e)
        }
        val totalMb = (totalKb / 1024).toInt()
        val usedMb = ((totalKb - availableKb) / 1024).toInt().coerceAtLeast(0)
        return totalMb to usedMb
    }

    // --- cpu ----------------------------------------------------------------------

    /** Average of (current frequency / max frequency) across all online cores. */
    private fun readCpuPercent(): Int {
        return try {
            val cores = File(CPU_ROOT).listFiles { f ->
                f.isDirectory && f.name.matches(Regex("cpu\\d+"))
            } ?: return 0

            var sumCurrent = 0L
            var sumMax = 0L
            var counted = 0

            for (core in cores) {
                val cpufreq = File(core, "cpufreq")
                val current = readLong(File(cpufreq, "scaling_cur_freq")) ?: continue
                val max = readLong(File(cpufreq, "cpuinfo_max_freq"))
                    ?: readLong(File(cpufreq, "scaling_max_freq"))
                    ?: continue
                if (max <= 0L) continue
                sumCurrent += current
                sumMax += max
                counted++
            }

            if (counted == 0 || sumMax == 0L) 0
            else ((sumCurrent.toFloat() / sumMax.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } catch (e: Exception) {
            Log.w(TAG, "could not read cpufreq", e)
            0
        }
    }

    // --- temperature --------------------------------------------------------------

    /** Hottest plausible SoC/board sensor, in degrees Celsius. */
    private fun readTemperature(): Float? {
        return try {
            val zones = File(THERMAL_ROOT).listFiles { f -> f.name.startsWith("thermal_zone") }
                ?: return null
            var hottest: Float? = null
            for (zone in zones) {
                val raw = readLong(File(zone, "temp")) ?: continue
                val celsius = normaliseTemperature(raw)
                if (celsius in 5f..125f) {
                    if (hottest == null || celsius > hottest) hottest = celsius
                }
            }
            hottest
        } catch (e: Exception) {
            Log.w(TAG, "could not read thermal zones", e)
            null
        }
    }

    /**
     * Thermal zones are inconsistent across vendors: some report millidegrees (46900),
     * some tenths (469), some whole degrees (47).
     */
    private fun normaliseTemperature(raw: Long): Float = when {
        raw >= 1000L -> raw / 1000f
        raw >= 100L -> raw / 10f
        else -> raw.toFloat()
    }

    // --- battery ------------------------------------------------------------------

    private data class BatteryReading(val level: Int, val charging: Boolean, val tempC: Float)

    private fun readBattery(context: Context): BatteryReading {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                ?: Int.MIN_VALUE

            val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            // Some units (battery-less Mini PC builds in particular) report a bogus or
            // missing temperature; anything outside a sane range is discarded downstream.
            val tempC = tempTenths / 10f

            BatteryReading(percent, charging, tempC)
        } catch (e: Exception) {
            Log.w(TAG, "could not read battery", e)
            BatteryReading(0, false, 0f)
        }
    }

    private fun readLong(file: File): Long? = try {
        file.readText().trim().toLongOrNull()
    } catch (e: Exception) {
        null
    }
}
