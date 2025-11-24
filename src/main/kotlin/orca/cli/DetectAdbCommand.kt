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
