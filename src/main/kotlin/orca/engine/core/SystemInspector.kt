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

package orca.engine.core

import orca.engine.model.MetricsConfig

/**
 * Provides an abstraction layer for interacting with the device or emulator
 * on which the stress test is running.
 *
 * Implementations may execute ADB commands, query system state, read files,
 * or interface with OS-level services. The OrcaEngine uses this interface
 * to evaluate preconditions, detect failures, and perform reboot recovery.
 *
 * Responsibilities include:
 * - Querying device and environment state (battery, network, charging, etc.)
 * - Monitoring whether a target app/process is alive
 * - Waiting for device transitions (offline → online, boot completed, etc.)
 * - Starting an application after reboot or at run start
 * - Collecting performance metrics defined by the configuration
 */
interface SystemInspector {

    /**
     * Returns the current battery level, typically in the range 0–100.
     *
     * @return battery percentage or `null` if not available.
     */
    fun getBatteryLevel(): Int?

    /**
     * Indicates whether any network connectivity is available.
     *
     * @return true if network is available, false if offline, or null if unknown.
     */
    fun isNetworkAvailable(): Boolean?

    /**
     * Indicates whether the device is currently in an idle or doze state.
     *
     * @return true if idle, false if active, or null if unknown.
     */
    fun isDeviceIdle(): Boolean?

    /**
     * Returns whether the device screen is currently turned on.
     *
     * @return true if screen is on, false if off, or null if unknown.
     */
    fun isScreenOn(): Boolean?

    /**
     * Indicates whether the device is currently connected to a charger.
     *
     * @return true if plugged in, false if not, or null if unknown.
     */
    fun isCharging(): Boolean?

    /**
     * Returns whether the execution environment has root capabilities.
     * Used for scripts requiring elevated privileges.
     *
     * @return true if root is available, false if not, or null if unknown.
     */
    fun isRootAvailable(): Boolean?

    /**
     * Returns whether ADB functionality is available. Some tests may require
     * the ability to run shell commands or reboot the device.
     *
     * @return true if ADB is functional, false otherwise, or null if unknown.
     */
    fun adbAvailable(): Boolean?

    /**
     * Checks if a file exists at the given path on the device or emulator.
     *
     * @param path the absolute or relative path to check.
     * @return true if the file exists, false otherwise.
     */
    fun fileExists(path: String): Boolean

    /**
     * Collects performance or environment metrics defined by the provided
     * [MetricsConfig]. These values are merged into the engine’s global
     * metrics at the end of each event’s execution.
     *
     * @param config optional metrics request; null returns an empty map.
     * @return a map of metricName → value.
     */
    fun captureMetrics(config: MetricsConfig?): Map<String, Double>

    /**
     * Determines whether the target application process is currently running.
     * Used for crash detection and unexpected termination handling.
     *
     * @param packageName the package whose process should be checked.
     * @return true if running, false if not.
     */
    fun isProcessRunning(packageName: String?): Boolean

    /**
     * Blocks until the device transitions to an offline state. Typically used
     * during reboot recovery after an event that initiates a restart.
     */
    fun awaitDeviceOffline()

    /**
     * Blocks until the device is back online and responsive to ADB commands.
     * Usually follows [awaitDeviceOffline].
     */
    fun awaitDeviceOnline()

    /**
     * Blocks until the device has completed booting (BOOT_COMPLETED broadcast
     * or equivalent detection). Required for stable reboot recovery.
     */
    fun awaitBootCompleted()

    /**
     * Launches or relaunches the target application. Called at the beginning
     * of duration-based runs or after reboot recovery if configured.
     *
     * @param packageName the application package to start.
     */
    fun startApp(packageName: String?)
}
