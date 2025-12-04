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

import orca.engine.core.EngineLogger
import orca.engine.model.SystemInspector
import orca.engine.model.MetricsConfig

/**
 * A non-ADB fallback SystemInspector used when running the engine
 * on a regular desktop JVM with **no Android device connected**.
 *
 * This inspector:
 *  - Never calls adb
 *  - Always returns safe, reasonable defaults
 *  - Allows all events to run
 *  - Prints debug output so you can observe business logic flow
 *
 * This is extremely helpful while developing the engine itself,
 * before integrating with a real Android device.
 */
class DefaultSystemInspector(
    private val debug: Boolean = true,
    private val logger: EngineLogger
) : SystemInspector {

    private fun dbg(msg: String) {
        if (debug) logger.debug ("[DefaultSystemInspector] $msg")
    }

    override fun getBatteryLevel(): Int? {
        dbg("getBatteryLevel() → returning 80%")
        return 80
    }

    override fun isNetworkAvailable(): Boolean? {
        dbg("isNetworkAvailable() → true")
        return true
    }

    override fun isDeviceIdle(): Boolean? {
        dbg("isDeviceIdle() → false")
        return false
    }

    override fun isScreenOn(): Boolean? {
        dbg("isScreenOn() → true")
        return true
    }

    override fun isCharging(): Boolean? {
        dbg("isCharging() → true")
        return true
    }

    override fun isRootAvailable(): Boolean? {
        dbg("isRootAvailable() → false")
        return false
    }

    override fun adbAvailable(): Boolean? {
        dbg("adbAvailable() → false (offline mode)")
        return false
    }

    override fun fileExists(path: String): Boolean {
        dbg("fileExists('$path') → returning false (simulated)")
        return false
    }

    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        if (config == null) return emptyMap()

        dbg("captureMetrics() → returning default dummy metrics")

        val map = mutableMapOf<String, Double>()

        if (config.captureCpuUsage) map["cpu.totalPercent"] = 12.5
        if (config.captureMemoryUsage) {
            map["mem.usedMb"] = 512.0
            map["mem.totalMb"] = 4096.0
        }
        if (config.captureBatteryLevel) map["battery.levelPercent"] = 80.0

        return map
    }

    override fun isProcessRunning(packageName: String?): Boolean {
        dbg("isProcessRunning($packageName) → returning true")
        return true
    }

    override fun awaitDeviceOffline() {
        dbg("awaitDeviceOffline() → skipped (offline mode)")
    }

    override fun awaitDeviceOnline() {
        dbg("awaitDeviceOnline() → skipped (offline mode)")
    }

    override fun awaitBootCompleted() {
        dbg("awaitBootCompleted() → skipped (offline mode)")
    }

    override fun startApp(packageName: String?) {
        dbg("startApp($packageName) → simulated launch")
    }
}
