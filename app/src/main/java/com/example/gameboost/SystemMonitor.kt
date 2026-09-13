package com.example.gameboost

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.RandomAccessFile

/**
 * Reads system stats that a normal (non-root) Android app is actually allowed to read.
 *
 * Notes on realism:
 * - CPU % is computed from /proc/stat deltas. This file is readable without root on
 *   most stock Android builds, but some OEMs (Samsung, some MIUI builds) restrict it.
 *   If it's unreadable, cpuUsagePercent() returns -1 and the caller should show "N/A".
 * - GPU temperature has no public, non-root Android API. We do not fake a number.
 * - "RAM cleanup" is intentionally not implemented as a real action: since Android 5+,
 *   apps cannot kill other apps' processes (Activity.killBackgroundProcesses only
 *   affects the caller's own cached processes and is not a real performance boost).
 */
object SystemMonitor {

    private var lastIdle = 0L
    private var lastTotal = 0L

    fun cpuUsagePercent(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" ").filter { it.isNotBlank() }
            // toks[0] = "cpu", 1..7 = user, nice, system, idle, iowait, irq, softirq
            val user = toks[1].toLong()
            val nice = toks[2].toLong()
            val system = toks[3].toLong()
            val idle = toks[4].toLong()
            val ioWait = toks[5].toLong()
            val irq = toks[6].toLong()
            val softIrq = toks[7].toLong()

            val total = user + nice + system + idle + ioWait + irq + softIrq
            val idleTotal = idle + ioWait

            val diffTotal = total - lastTotal
            val diffIdle = idleTotal - lastIdle

            lastTotal = total
            lastIdle = idleTotal

            if (diffTotal <= 0) return -1
            (100 * (diffTotal - diffIdle) / diffTotal).toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            // /proc/stat blocked on this device (common on some OEM skins) — be honest, don't fake it
            -1
        }
    }

    fun batteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun isCharging(context: Context): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = context.registerReceiver(null, filter)
        val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS
    }

    /** Free RAM in MB, as reported by ActivityManager (system-wide figure, no root needed). */
    fun freeRamMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
    }
}
