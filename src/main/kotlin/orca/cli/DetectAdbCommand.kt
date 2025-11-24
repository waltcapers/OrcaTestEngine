/*
 * Dual License: MIT + GM Exception
 * Copyright (c) 2025 Walter E. Capers
 *
 * MIT License applies to all users except General Motors (GM).
 *
 * GM Exception:
 * GM is granted a perpetual, worldwide, royalty-free license to use, modify,
 * reproduce, distribute, and create derivative works from this Software for any
 * business or commercial purpose. This exception applies only to GM and does
 * not extend to other third parties.
 *
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files to deal in the Software
 * without restriction.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package orca.cli

import orca.engine.logging.ConsoleEngineLogger
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
        val logger = ConsoleEngineLogger()

        println("Detecting ADB connectivity and basic device state...")

        val adbExecutor = DefaultAdbExecutor(
            adbPath = "adb",
            deviceSerial = null,
            logger = logger
        )

        val inspector = AdbSystemInspector(
            adb = adbExecutor,
            defaultPackageName = null,
            debug = true
        )

        val adbOk = inspector.adbAvailable()
        println("ADB available: $adbOk")

        val battery = inspector.getBatteryLevel()
        println("Battery level: ${battery ?: "(unknown)"}")

        val screenOn = inspector.isScreenOn()
        println("Screen on:     ${screenOn ?: "(unknown)"}")

        val idle = inspector.isDeviceIdle()
        println("Device idle:   ${idle ?: "(unknown)"}")

        val charging = inspector.isCharging()
        println("Charging:      ${charging ?: "(unknown)"}")

        println("Note: Some fields may be null/unknown if the device restricts dumpsys output or ADB shell usage.")
    }
}
