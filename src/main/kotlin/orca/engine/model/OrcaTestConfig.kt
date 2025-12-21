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

package orca.engine.model

/**
 * Top-level configuration object for the deterministic stress testing engine.
 *
 * A `OrcaTestConfig` instance defines the entire behavior of a test run:
 *
 *  - Test metadata (`name`, `description`)
 *  - Global engine behavior (`runMode`, `randomSeed`, `debug`)
 *  - Defaults for event execution (`defaultTimeoutSeconds`, `defaultRetry`,
 *    `defaultSlowThresholdMillis`)
 *  - The complete list of [`StressEvent`][StressEvent] definitions
 *  - App/process monitoring (`targetPackage`)
 *  - Test duration control (`maxTestDurationSeconds`)
 *  - Logcat capture configuration (`logcat`)
 *  - Optional script hook for post-reboot actions (`postRebootScript`)
 *
 * This object is typically loaded from JSON via `OrcaConfigLoader` and
 * is consumed by `OrcaEngine` at runtime.
 */
data class OrcaTestConfig(

    /**
     * Optional human-friendly test name.
     * Useful when aggregating results across multiple configurations.
     */
    val name: String? = null,

    /**
     * Optional detailed description of the test scenario.
     */
    val description: String? = null,

    // -------------------------------------------------------------------------
    // Execution defaults
    // -------------------------------------------------------------------------

    /**
     * Default threshold (in milliseconds) for detecting "slow" events.
     * If an event does not specify `slowThresholdMillis`, this global value is used.
     *
     * A value of `null` disables slow-event warnings globally.
     */
    val defaultSlowThresholdMillis: Long? = 2000,

    /**
     * Seed for deterministic RNG used by the engine.
     *
     * All RANDOM-mode event selection and weighted choice processing
     * depends on this seed. Reusing the same seed guarantees repeatable
     * event ordering unless event definitions change.
     */
    var randomSeed: Long,

    /**
     * Global run mode for the engine.
     *
     * Currently, only `RunMode.RANDOM` is used internally, but the field
     * is kept to allow future expansion (e.g., SEQUENTIAL, MIXED, scripted flows).
     */
    val runMode: RunMode = RunMode.RANDOM,

    /**
     * Default timeout (in seconds) for event script execution.
     * Individual events may override this using `timeoutSeconds`.
     */
    val defaultTimeoutSeconds: Int? = null,

    /**
     * Default retry policy applied when events do not specify their own.
     *
     * If null, events with no retryPolicy default to maxAttempts=1.
     */
    val defaultRetry: RetryPolicy? = null,

    // -------------------------------------------------------------------------
    // Event set
    // -------------------------------------------------------------------------

    /**
     * Mandatory list of all stress events that the engine may execute.
     *
     * This array defines the entire test plan and includes:
     *  - SCRIPT events
     *  - WAIT_FOR_DEVICE events
     *  - NO_OP instrumentation events
     *  - SEQUENCE events for composite flows
     *
     * At least one event is required.
     */
    val events: List<StressEvent>,

    // -------------------------------------------------------------------------
    // Process monitoring
    // -------------------------------------------------------------------------

    /**
     * The Android package name of the app under test.
     *
     * Used by:
     *  - `SystemInspector.isProcessRunning()` for crash detection
     *  - automatic reboot recovery (event.waitForBoot + restartAppAfterBoot)
     *  - automatic logcat tagging if no explicit tag is provided
     */
    val targetPackage: String? = null,

    // -------------------------------------------------------------------------
    // Debugging / Logging
    // -------------------------------------------------------------------------

    /**
     * Enables additional debug logging inside the engine.
     *
     * When true, internal selection details, cooldown checks, and trigger
     * evaluations are logged via EngineLogger.
     */
    var debug: Boolean = false,

    /**
     * Optional debug step triggers
     * (default to false)
     */
    val debugBreakBeforeEvent: Boolean? = null,
    val debugBreakAfterEvent: Boolean? = null,
    val debugBreakOnError: Boolean? = null,
    val debugBreakOnRetry: Boolean? = null,
    val debugBreakOnReboot: Boolean? = null,
    val debugBreakWhenPreconditionsFail: Boolean? = null,

    // -------------------------------------------------------------------------
    // Duration control
    // -------------------------------------------------------------------------

    /**
     * Global cap on test duration (in seconds) when invoking runForDuration().
     *
     * If unspecified, runForDuration() must explicitly define the limit.
     * Does not affect runLoop() or runForIterations().
     */
    val maxTestDurationSeconds: Int? = null,

    // -------------------------------------------------------------------------
    // Logcat integration
    // -------------------------------------------------------------------------

    /**
     * Configuration for automatic logcat capture during test execution.
     *
     * When enabled, OrcaEngine:
     *  - starts logcat at test start (runLoop, runForDuration)
     *  - stops it when the run completes
     *  - *optionally* restarts logcat after reboot recovery
     *
     * If null, logcat support is disabled entirely.
     */
    val logcat: LogcatConfig? = null,

    // -------------------------------------------------------------------------
    // Optional post-reboot hook
    // -------------------------------------------------------------------------

    /**
     * Optional path to a script executed after reboot recovery.
     *
     * This can be used to:
     *  - reinitialize external systems
     *  - reapply adb settings
     *  - re-authenticate shell sessions
     *
     * This hook is invoked only when an event declares `causesReboot = true`
     * and recovery completes successfully.
     */
    val postRebootScript: String? = null

)
