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

package orca.cli

import orca.engine.logging.ConsoleEngineLogger
import orca.engine.logging.LoggerProvider
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor

/**
 * Implements:
 *
 *   orca detect-adb
 *
 * Behavior:
 *   - Creates a DefaultAdbExecutor + AdbSystemInspector.
 *   - Probes:
 *       * adbAvailable()
 *       * getBatteryLevel()
 *       * isScreenOn()
 *       * isDeviceIdle()
 *       * isCharging()
 *   - Prints the results to help diagnose connectivity and restrictions.
 */
object DetectAdbCommand {

    fun run() {

        println("Detecting ADB connectivity and basic device state...")

        val adbExecutor = DefaultAdbExecutor(
            adbPath = "adb",
            deviceSerial = null,
        )

        val inspector = AdbSystemInspector(
            adb = adbExecutor,
            defaultPackageName = null,
            debug = true,
        )

        val adbOk = inspector.adbAvailable()
        LoggerProvider.get().info("ADB available: $adbOk")

        val battery = inspector.getBatteryLevel()
        LoggerProvider.get().info("Battery level: ${battery ?: "(unknown)"}")

        val screenOn = inspector.isScreenOn()
        LoggerProvider.get().info("Screen on:     ${screenOn ?: "(unknown)"}")

        val idle = inspector.isDeviceIdle()
        LoggerProvider.get().info("Device idle:   ${idle ?: "(unknown)"}")

        val charging = inspector.isCharging()
        LoggerProvider.get().info("Charging:      ${charging ?: "(unknown)"}")

        LoggerProvider.get().info("Note: Some fields may be null/unknown if the device restricts dumpsys output or ADB shell usage.")
    }
}
