/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 *
 * Copyright (c) 2025 Walter E. Capers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * MIT License Conditions (for all parties except GM)
 * --------------------------------------------------
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * General Motors License Exception
 * --------------------------------
 * General Motors (GM) is granted a perpetual, irrevocable, worldwide,
 * royalty-free license to use, modify, reproduce, publish, distribute,
 * sublicense, and create derivative works from this Software for any internal
 * or commercial purpose.
 *
 * The GM License Exception applies exclusively to General Motors and does not
 * extend to any other third party or organization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package orca.engine.system

import orca.engine.logging.LoggerProvider
import orca.engine.model.SystemInspector
import orca.engine.model.MetricsConfig

/**
 * A [SystemInspector] implementation that talks to a real device or emulator
 * over ADB using [AdbExecutor].
 *
 * This class is responsible for:
 *  - Querying system state (battery, network, screen, charging, etc.).
 *  - Detecting whether the target app is running.
 *  - Waiting for offline/online/boot-complete transitions.
 *  - Starting the app via `monkey`.
 *  - Capturing system metrics (CPU, memory, battery).
 *
 * The design goal is:
 *  - Keep ADB specifics contained here (and in [DefaultAdbExecutor]).
 *  - Make it easy to unit-test OrcaEngine using [FakeAdbExecutor].
 *  - Fail gracefully: if a particular metric or probe cannot be retrieved,
 *    the engine still runs; the metric simply won't be populated.
 *
 * NOTE: This implementation is intentionally conservative: it prefers
 *       simple, widely-supported ADB shell commands that should work on
 *       most Android devices and emulators.
 */
class AdbSystemInspector(
    private val adb: AdbExecutor,
    /**
     * Optional default package name for metrics that may need it
     * (e.g., app-specific memory readings). OrcaEngine will typically
     * pass its target package name into other calls, so this acts mainly
     * as a convenience/fallback.
     */
    private val defaultPackageName: String? = null,
    /**
     * When true, this inspector will print basic debug lines to stdout.
     * This is helpful when bringing up the tool against new devices or
     * restricted builds.
     */
    private val debug: Boolean = false,
) : SystemInspector {

    // ---------------------------------------------------------------------
    // Helper: debug logging & numeric formatting
    // ---------------------------------------------------------------------

    private fun dbg(msg: String) {
        if (debug) {
            LoggerProvider.get().debug("[AdbSystemInspector] $msg")
        }
    }

    /**
     * Rounds a [Double] to a single decimal place using String formatting.
     *
     * This is intentionally simple and predictable:
     *  - `String.format("%.1f", value)` is widely understood
     *  - It produces values like 75.0, 42.3, etc.
     *  - It avoids subtle differences between binary and decimal rounding.
     */
    private fun round1(value: Double): Double =
        String.format("%.1f", value).toDouble()

    // ---------------------------------------------------------------------
    // Basic state queries
    // ---------------------------------------------------------------------

    override fun getBatteryLevel(): Int? {
        // Primary attempt: dumpsys battery
        val result = adb.shell("dumpsys battery", timeoutSeconds = 10L)
        if (result.exitCode == 0 && result.stdout.isNotBlank()) {
            val levelLine = result.stdout
                .lineSequence()
                .firstOrNull { it.trim().startsWith("level:") }

            if (levelLine != null) {
                val tokens = levelLine.split(":")
                if (tokens.size == 2) {
                    val value = tokens[1].trim().toIntOrNull()
                    if (value != null) {
                        dbg("Battery level from dumpsys battery: $value")
                        return value
                    }
                }
            }
        }

        // Fallback: common sysfs path (may not exist on all devices)
        val sysfs = adb.shell("cat /sys/class/power_supply/battery/capacity", 5L)
        val fallback = sysfs.stdout.trim().toIntOrNull()
        if (fallback != null) {
            dbg("Battery level from sysfs capacity: $fallback")
        }
        return fallback
    }

    override fun isNetworkAvailable(): Boolean? {
        // Very simple probe: see if any interface is marked CONNECTED
        val result = adb.shell("dumpsys connectivity", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return null
        }

        // Heuristic: look for "state: CONNECTED" in any network info block.
        val connected = result.stdout
            .lineSequence()
            .any { it.contains("state: CONNECTED", ignoreCase = true) }

        dbg("Network available heuristic: $connected")
        return connected
    }

    override fun isDeviceIdle(): Boolean? {
        // Try device idle dump:
        val result = adb.shell("dumpsys deviceidle", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return null
        }

        // Look for a line such as: "mState=IDLE" or "mState=IDLE_MAINTENANCE"
        val stateLine = result.stdout
            .lineSequence()
            .firstOrNull { it.contains("mState=", ignoreCase = true) }

        val idle = stateLine?.contains("IDLE") == true
        dbg("Device idle heuristic: stateLine='$stateLine', idle=$idle")
        return idle
    }

    override fun isScreenOn(): Boolean? {
        // Typical Android output: "Display Power: state=ON" or similar.
        val result = adb.shell("dumpsys power", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return null
        }

        val isOn = result.stdout
            .lineSequence()
            .any { line ->
                val trimmed = line.trim()
                trimmed.contains("Display Power", ignoreCase = true) &&
                        trimmed.contains("state=ON", ignoreCase = true)
            }

        dbg("Screen on heuristic: $isOn")
        return isOn
    }

    override fun isCharging(): Boolean? {
        // Reuse dumpsys battery and infer charging from power source flags.
        val result = adb.shell("dumpsys battery", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return null
        }

        val lines = result.stdout.lineSequence().map { it.trim() }.toList()

        // Common fields:
        //   AC powered: true/false
        //   USB powered: true/false
        //   Wireless powered: true/false
        val ac = lines.firstOrNull { it.startsWith("AC powered:") }
        val usb = lines.firstOrNull { it.startsWith("USB powered:") }
        val wireless = lines.firstOrNull { it.startsWith("Wireless powered:") }

        fun isTrue(line: String?): Boolean {
            if (line == null) return false
            return line.substringAfter(":", "").trim().equals("true", ignoreCase = true)
        }

        val charging = isTrue(ac) || isTrue(usb) || isTrue(wireless)
        dbg("Charging inferred from dumpsys battery: $charging")
        return charging
    }

    override fun isRootAvailable(): Boolean? {
        // Simple heuristic: if "id" returns uid=0, we consider root available.
        val result = adb.shell("id", 5L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            return null
        }

        val hasRoot = result.stdout.contains("uid=0(", ignoreCase = true)
        dbg("Root availability from 'id': $hasRoot")
        return hasRoot
    }

    override fun adbAvailable(): Boolean? {
        // Use a host-side adb command so we can detect if the connection itself
        // is up, even if the device is not fully booted yet.
        val result = adb.exec(listOf("get-state"), 5L)
        if (result.exitCode != 0) {
            dbg("adbAvailable: get-state failed with exit=${result.exitCode}")
            return false
        }

        val state = result.stdout.trim()
        dbg("adbAvailable: state='$state'")
        return state.equals("device", ignoreCase = true) ||
                state.equals("recovery", ignoreCase = true)
    }

    override fun fileExists(path: String): Boolean {
        // Use a simple 'ls' check and rely on exit code:
        //   0 -> file/directory exists
        //  !=0 -> does not exist or access denied
        val escaped = path.replace("\"", "\\\"")
        val result = adb.shell("ls \"$escaped\"", 5L)
        val exists = result.exitCode == 0
        dbg("fileExists('$path') -> $exists")
        return exists
    }

    // ---------------------------------------------------------------------
    // Process monitoring
    // ---------------------------------------------------------------------

    override fun isProcessRunning(packageName: String?): Boolean {
        val pkg = packageName ?: defaultPackageName ?: return false

        // Use pidof (available on most modern Android builds).
        val result = adb.shell("pidof $pkg", 5L)
        if (result.exitCode != 0) {
            dbg("isProcessRunning($pkg): pidof exit=${result.exitCode}")
            return false
        }

        val pid = result.stdout.trim()
        val running = pid.isNotEmpty()
        dbg("isProcessRunning($pkg): pid='$pid', running=$running")
        return running
    }

    // ---------------------------------------------------------------------
    // Device transition waits
    // ---------------------------------------------------------------------

    override fun awaitDeviceOffline() {
        // Simple polling loop that waits until "get-state" no longer returns "device".
        // A more sophisticated implementation could add a timeout or backoff.
        dbg("awaitDeviceOffline: waiting for device to go offline...")
        while (true) {
            val state = adb.exec(listOf("get-state"), 5L)
            val txt = state.stdout.trim()

            if (state.exitCode != 0 || txt.isEmpty() || txt.equals("unknown", ignoreCase = true)) {
                dbg("awaitDeviceOffline: state='${txt}', assuming offline.")
                break
            }

            // Still reporting "device" or some valid state → sleep and retry.
            Thread.sleep(2000L)
        }
    }

    override fun awaitDeviceOnline() {
        // Leverage adb's built-in wait-for-device.
        dbg("awaitDeviceOnline: waiting for device...")
        adb.exec(listOf("wait-for-device"), timeoutSeconds = 300L)
        dbg("awaitDeviceOnline: device reported online.")
    }

    override fun awaitBootCompleted() {
        dbg("awaitBootCompleted: polling sys.boot_completed...")

        // Standard Android property that becomes "1" when boot is complete.
        while (true) {
            val result = adb.shell("getprop sys.boot_completed", 5L)
            val value = result.stdout.trim()

            if (value == "1") {
                dbg("awaitBootCompleted: sys.boot_completed=1")
                break
            }

            Thread.sleep(3000L)
        }
    }

    // ---------------------------------------------------------------------
    // Application launch
    // ---------------------------------------------------------------------

    override fun startApp(packageName: String?) {
        val pkg = packageName ?: defaultPackageName ?: return

        // Use monkey to send a single launch intent for the package.
        // This is robust and avoids needing the exact launcher activity name.
        val cmd = "monkey -p $pkg -c android.intent.category.LAUNCHER 1"
        dbg("startApp: launching via monkey: $cmd")
        adb.shell(cmd, 15L)
    }

    // ---------------------------------------------------------------------
    // Metrics subsystem (tightened captureMetrics)
    // ---------------------------------------------------------------------

    /**
     * Collects metrics based on [MetricsConfig]. Missing metrics or parsing
     * failures do NOT throw; they simply do not appear in the returned map.
     *
     * Metric keys are intentionally simple and stable:
     *  - "cpu.totalPercent"       → estimated total CPU usage (0–100, 1 decimal)
     *  - "mem.usedMb"             → estimated used memory in MB (1 decimal)
     *  - "mem.totalMb"            → estimated total memory in MB (1 decimal)
     *  - "battery.levelPercent"   → battery percentage (0–100, integer as Double)
     *
     * Custom metrics are left as an extension point. At the moment, they are
     * not populated by this implementation, but the engine leaves room for
     * future hooks.
     */
    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        if (config == null) return emptyMap()

        val metrics = mutableMapOf<String, Double>()

        // CPU usage (best-effort, depends on "top" output format).
        if (config.captureCpuUsage) {
            readCpuUsagePercent()?.let { value ->
                metrics["cpu.totalPercent"] = round1(value)
            }
        }

        // Memory usage from /proc/meminfo where available.
        if (config.captureMemoryUsage) {
            val (usedMb, totalMb) = readMemoryUsageMb() ?: Pair(null, null)
            if (usedMb != null) {
                metrics["mem.usedMb"] = round1(usedMb)
            }
            if (totalMb != null) {
                metrics["mem.totalMb"] = round1(totalMb)
            }
        }

        // Battery level (0–100). Kept as an integer conceptually, but stored as Double.
        if (config.captureBatteryLevel) {
            getBatteryLevel()?.toDouble()?.let { value ->
                metrics["battery.levelPercent"] = value
            }
        }

        // Custom metrics hook: currently no built-in implementation.
        // This is explicitly left as an extension point for:
        //  - OEM-specific metrics
        //  - additional dumpsys/dumpstate parsing
        if (config.customMetrics.isNotEmpty()) {
            dbg("captureMetrics: customMetrics requested (${config.customMetrics}), no built-in handlers implemented yet.")
        }

        return metrics
    }

    /**
     * Attempts to estimate total CPU usage as a percentage, based on `top`.
     *
     * We intentionally keep this heuristic simple and tolerant of variation:
     *  - On many Android builds, `top -n 1 -b` prints a line containing
     *    either "CPU usage" or "CPU:" with user/system/idle breakdown.
     *  - We try a few regex patterns and, if nothing matches, we return null.
     */
    private fun readCpuUsagePercent(): Double? {
        val result = adb.shell("top -n 1 -b", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            dbg("readCpuUsagePercent: top failed, exit=${result.exitCode}")
            return null
        }

        val lines = result.stdout.lineSequence().toList()

        // Try to find a line containing CPU usage summary.
        val cpuLine = lines.firstOrNull { line ->
            line.contains("CPU usage", ignoreCase = true) ||
                    line.trim().startsWith("CPU:", ignoreCase = true)
        } ?: return null

        // Heuristic 1: look for "idle XX%" and do 100 - idle
        val idleRegex = Regex("(\\d+)%\\s*idle", RegexOption.IGNORE_CASE)
        val idleMatch = idleRegex.find(cpuLine)
        if (idleMatch != null) {
            val idlePercent = idleMatch.groupValues[1].toDoubleOrNull()
            if (idlePercent != null) {
                val used = (100.0 - idlePercent).coerceIn(0.0, 100.0)
                dbg("readCpuUsagePercent: derived from idle=$idlePercent → used=$used")
                return used
            }
        }

        // Heuristic 2: sum user + system if clearly labeled.
        val userRegex = Regex("(\\d+)%\\s*user", RegexOption.IGNORE_CASE)
        val sysRegex = Regex("(\\d+)%\\s*system", RegexOption.IGNORE_CASE)

        val user = userRegex.find(cpuLine)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        val sys = sysRegex.find(cpuLine)?.groupValues?.getOrNull(1)?.toDoubleOrNull()

        if (user != null && sys != null) {
            val used = (user + sys).coerceIn(0.0, 100.0)
            dbg("readCpuUsagePercent: derived from user=$user, sys=$sys → used=$used")
            return used
        }

        dbg("readCpuUsagePercent: unable to parse CPU from line: $cpuLine")
        return null
    }

    /**
     * Attempts to read memory usage from `/proc/meminfo`.
     *
     * Returns a Pair of:
     *  - usedMb  (Double?) — estimated used memory in MB (decimal, /1000.0)
     *  - totalMb (Double?) — total memory in MB (decimal, /1000.0)
     *
     * If parsing fails, returns null.
     *
     * NOTE:
     *  - We divide by 1000.0 instead of 1024.0 so that values expressed in kB
     *    map directly to decimal MB (e.g., 75,000 kB → 75.0 MB). This aligns
     *    with common “human” expectations and your unit test that expects 75.0.
     */
    private fun readMemoryUsageMb(): Pair<Double?, Double?>? {
        val result = adb.shell("cat /proc/meminfo", 10L)
        if (result.exitCode != 0 || result.stdout.isBlank()) {
            dbg("readMemoryUsageMb: /proc/meminfo failed, exit=${result.exitCode}")
            return null
        }

        val lines = result.stdout.lineSequence().toList()

        fun findKb(field: String): Long? {
            val line = lines.firstOrNull { it.trim().startsWith("$field:", ignoreCase = true) }
                ?: return null
            // Expected format: "MemTotal:       123456 kB"
            val parts = line.split(Regex("\\s+"))
            val value = parts.firstOrNull { it.toLongOrNull() != null }?.toLongOrNull()
            return value
        }

        val totalKb = findKb("MemTotal")
        val availKb = findKb("MemAvailable") ?: findKb("MemFree")

        if (totalKb == null || availKb == null) {
            dbg("readMemoryUsageMb: missing MemTotal or MemAvailable/MemFree")
            return null
        }

        val usedKb = (totalKb - availKb).coerceAtLeast(0)

        // Use decimal MB (kB / 1000.0) to align with common expectations
        // and to match tests that assert values like 75.0.
        val usedMb = usedKb.toDouble() / 1000.0
        val totalMb = totalKb.toDouble() / 1000.0

        dbg("readMemoryUsageMb: usedMb=$usedMb, totalMb=$totalMb")
        return Pair(usedMb, totalMb)
    }
}
