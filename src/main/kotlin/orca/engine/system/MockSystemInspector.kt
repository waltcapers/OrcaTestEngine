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

import orca.engine.model.MetricsConfig
import orca.engine.model.SystemInspector

/**
 * Deterministic, no-ADB mock implementation of [SystemInspector].
 *
 * Designed for:
 *   - Dry-run configs
 *   - Demo / classroom usage without a device
 *   - Unit tests that need to control "system" conditions
 *
 * All values are in-memory and controllable via setters.
 */
class MockSystemInspector(
    private val debug: Boolean = true,
    private var batteryLevel: Int = 80,
    private var networkAvailable: Boolean = true,
    private var deviceIdle: Boolean = false,
    private var screenOn: Boolean = true,
    private var charging: Boolean = true,
    private var rootAvailable: Boolean = false,
    private var adbAvailableFlag: Boolean = false,
    private var appRunning: Boolean = true
) : SystemInspector {

    private fun dbg(msg: String) {
        if (debug) println("[MockSystemInspector] $msg")
    }

    // ---------------------------------------------------------------------
    // Basic device state
    // ---------------------------------------------------------------------

    override fun getBatteryLevel(): Int? {
        dbg("getBatteryLevel() → $batteryLevel")
        return batteryLevel
    }

    override fun isNetworkAvailable(): Boolean? {
        dbg("isNetworkAvailable() → $networkAvailable")
        return networkAvailable
    }

    override fun isDeviceIdle(): Boolean? {
        dbg("isDeviceIdle() → $deviceIdle")
        return deviceIdle
    }

    override fun isScreenOn(): Boolean? {
        dbg("isScreenOn() → $screenOn")
        return screenOn
    }

    override fun isCharging(): Boolean? {
        dbg("isCharging() → $charging")
        return charging
    }

    override fun isRootAvailable(): Boolean? {
        dbg("isRootAvailable() → $rootAvailable")
        return rootAvailable
    }

    override fun adbAvailable(): Boolean? {
        dbg("adbAvailable() → $adbAvailableFlag (mock mode)")
        return adbAvailableFlag
    }

    override fun fileExists(path: String): Boolean {
        // For now, assume nothing exists unless you want to make this smarter.
        dbg("fileExists('$path') → false (mock)")
        return false
    }

    // ---------------------------------------------------------------------
    // Metrics
    // ---------------------------------------------------------------------

    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        if (config == null) {
            dbg("captureMetrics(config = null) → {}")
            return emptyMap()
        }

        val out = mutableMapOf<String, Double>()

        if (config.captureCpuUsage) {
            out["cpu.totalPercent"] = 25.0
        }
        if (config.captureMemoryUsage) {
            out["mem.usedMb"] = 1024.0
            out["mem.totalMb"] = 4096.0
        }
        if (config.captureBatteryLevel) {
            out["battery.levelPercent"] = batteryLevel.toDouble()
        }

        config.customMetrics.forEach { name ->
            // deterministic-but-fake values for custom metrics
            out[name] = 1.0
        }

        dbg("captureMetrics(...) → $out")
        return out
    }

    // ---------------------------------------------------------------------
    // Process / reboot behavior
    // ---------------------------------------------------------------------

    override fun isProcessRunning(packageName: String?): Boolean {
        dbg("isProcessRunning($packageName) → $appRunning")
        return appRunning
    }

    override fun awaitDeviceOffline() {
        dbg("awaitDeviceOffline() → NO-OP in mock")
    }

    override fun awaitDeviceOnline() {
        dbg("awaitDeviceOnline() → NO-OP in mock")
    }

    override fun awaitBootCompleted() {
        dbg("awaitBootCompleted() → NO-OP in mock")
    }

    override fun startApp(packageName: String?) {
        dbg("startApp($packageName) → setting appRunning = true (mock)")
        appRunning = true
    }

    // ---------------------------------------------------------------------
    // Mutators for tests / demos
    // ---------------------------------------------------------------------

    fun setBatteryLevel(value: Int) {
        batteryLevel = value
        dbg("setBatteryLevel($value)")
    }

    fun setNetworkAvailable(value: Boolean) {
        networkAvailable = value
        dbg("setNetworkAvailable($value)")
    }

    fun setDeviceIdle(value: Boolean) {
        deviceIdle = value
        dbg("setDeviceIdle($value)")
    }

    fun setScreenOn(value: Boolean) {
        screenOn = value
        dbg("setScreenOn($value)")
    }

    fun setCharging(value: Boolean) {
        charging = value
        dbg("setCharging($value)")
    }

    fun setRootAvailable(value: Boolean) {
        rootAvailable = value
        dbg("setRootAvailable($value)")
    }

    fun setAdbAvailable(value: Boolean) {
        adbAvailableFlag = value
        dbg("setAdbAvailable($value)")
    }

    fun setAppRunning(value: Boolean) {
        appRunning = value
        dbg("setAppRunning($value)")
    }
}
