package orca.engine.system

import orca.engine.core.SystemInspector
import orca.engine.model.MetricsConfig
import java.io.File

/**
 * Default (stub) implementation of [SystemInspector].
 *
 * This implementation is intended for:
 *  - Local desktop/debug runs
 *  - Dry-run/warm-up testing of the OrcaEngine
 *  - Environments with no device or emulator attached
 *
 * All methods return static or placeholder values. On a real device, these
 * methods should be replaced with ADB or platform-specific logic located in a
 * subclass (e.g., `AdbSystemInspector`).
 */
class DefaultSystemInspector : SystemInspector {

    /**
     * Returns a fixed battery level (100%).
     *
     * Real implementation should execute:
     *  `adb shell dumpsys battery`
     */
    override fun getBatteryLevel(): Int? = 100

    /** Always returns true. */
    override fun isNetworkAvailable(): Boolean = true

    /** Always returns false (device is never considered idle). */
    override fun isDeviceIdle(): Boolean = false

    /** Always returns true (screen assumed on). */
    override fun isScreenOn(): Boolean = true

    /** Always returns true (device assumed charging). */
    override fun isCharging(): Boolean = true

    /** Always returns true (placeholder — no actual root check). */
    override fun isRootAvailable(): Boolean = true

    /** Always returns true (placeholder — no ADB validation). */
    override fun adbAvailable(): Boolean = true

    /** Returns true if the file exists on the *host* filesystem. */
    override fun fileExists(path: String): Boolean =
        File(path).exists()

    /**
     * Placeholder for metric collection.
     *
     * A real version may gather CPU, memory, or custom values via:
     *  - `adb shell top`
     *  - `dumpsys meminfo`
     *  - `/proc` polling
     */
    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        return emptyMap() // Not implemented yet
    }

    /**
     * Always returns true.
     *
     * Real implementation should check:
     *  `adb shell pidof <package>`
     */
    override fun isProcessRunning(packageName: String?): Boolean = true

    /**
     * Simulates a device shutdown by sleeping.
     *
     * Replace with polling:
     *  `adb get-state` → "offline"
     */
    override fun awaitDeviceOffline() {
        Thread.sleep(5000)
    }

    /**
     * Simulates a device boot-up by sleeping.
     *
     * Replace with polling until:
     *  `adb get-state` → "device"
     */
    override fun awaitDeviceOnline() {
        Thread.sleep(15000)
    }

    /**
     * Simulates boot completion by sleeping.
     *
     * Replace with polling:
     *  `adb shell getprop sys.boot_completed` → "1"
     */
    override fun awaitBootCompleted() {
        Thread.sleep(20000)
    }

    /**
     * Simulates starting an app.
     *
     * Real implementation should run:
     *  `adb shell monkey -p <pkg> -c android.intent.category.LAUNCHER 1`
     */
    override fun startApp(packageName: String?) {
        println("Starting app: $packageName (placeholder)")
    }
}
