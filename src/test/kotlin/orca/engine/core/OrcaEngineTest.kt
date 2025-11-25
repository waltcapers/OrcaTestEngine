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

import orca.engine.logging.LogcatManager
import orca.engine.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [OrcaEngine].
 *
 * These tests deliberately avoid real ADB / OS interaction by providing
 * fake implementations of:
 *
 *  - [SystemInspector]   → [FakeSystemInspector]
 *  - [ScriptRunner]      → [FakeScriptRunner]
 *  - [LogcatManager]     → [FakeLogcatManager]
 *  - [EngineLogger]      → [CollectingLogger]
 *
 * The goal is to validate the **business logic** of OrcaEngine:
 *  - event selection (basic RANDOM path)
 *  - preconditions evaluation
 *  - retry / failure policies
 *  - sequence execution
 *  - postEvents chaining
 *  - conditional triggers (including metrics-based triggers)
 *  - reboot recovery behavior
 *
 * This is intentionally JVM-only and should run under Gradle/IntelliJ
 * without any attached device or emulator.
 */
class OrcaEngineTest {

    /** Fake system inspector whose behavior we control per-test. */
    private lateinit var systemInspector: FakeSystemInspector

    /** Fake script runner that records which events were run and how. */
    private lateinit var scriptRunner: FakeScriptRunner

    /** Logger that collects log lines for assertions. */
    private lateinit var logger: CollectingLogger

    /** No-op logcat manager to avoid spawning real adb logcat processes. */
    private lateinit var logcatManager: FakeLogcatManager

    @BeforeEach
    fun setUp() {
        systemInspector = FakeSystemInspector()
        scriptRunner = FakeScriptRunner()
        logger = CollectingLogger()
        logcatManager = FakeLogcatManager()
    }

    // -------------------------------------------------------------------------
    // 1. BASIC SINGLE-EVENT EXECUTION
    // -------------------------------------------------------------------------

    /**
     * Verifies that a single RANDOM + SCRIPT event is executed by runOnce()
     * and delegated to [ScriptRunner.run].
     */
    @Test
    fun `runOnce executes simple script event`() {
        val event = StressEvent(
            id = "simple",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            language = ScriptLanguage.SHELL,
            enabled = true
        )

        // Script succeeds by default in FakeScriptRunner (exitCode=0)
        val config = OrcaTestConfig(
            randomSeed = 123L,
            events = listOf(event)
        )

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "Single script event should succeed")
        assertEquals(
            listOf("simple"),
            scriptRunner.executedEvents,
            "ScriptRunner should have been invoked exactly once for event 'simple'"
        )
    }

    // -------------------------------------------------------------------------
    // 2. PRECONDITIONS
    // -------------------------------------------------------------------------

    /**
     * Validates that preconditions are checked via [SystemInspector] and
     * that the engine **skips** events whose preconditions fail, without
     * invoking the [ScriptRunner].
     */
    @Test
    fun `event is skipped when preconditions fail`() {
        // Event requires batteryAbove=50, but we'll report 40.
        val event = StressEvent(
            id = "needs_battery",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            preconditions = Preconditions(
                batteryAbove = 50
            )
        )

        val config = OrcaTestConfig(
            randomSeed = 42L,
            events = listOf(event)
        )

        // Fake system state: low battery
        systemInspector.batteryLevel = 40

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertFalse(success, "runOnce should return false when no event can run (preconditions fail)")
        assertTrue(
            scriptRunner.executedEvents.isEmpty(),
            "ScriptRunner must NOT be called when preconditions fail"
        )
    }

    // -------------------------------------------------------------------------
    // 3. RETRY POLICY
    // -------------------------------------------------------------------------

    /**
     * Tests that an event with onFailure=RETRY and a RetryPolicy is executed
     * multiple times until success.
     *
     * We configure the FakeScriptRunner so that:
     *  - First two attempts → exitCode != 0 (failure)
     *  - Third attempt      → exitCode = 0 (success)
     */
    @Test
    fun `retry policy retries until success`() {
        val event = StressEvent(
            id = "flaky",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            onFailure = FailurePolicy.RETRY,
            retryPolicy = RetryPolicy(
                maxAttempts = 3,
                backoffSeconds = 0,                // avoid sleeps in tests
                strategy = RetryStrategy.LINEAR
            )
        )

        val config = OrcaTestConfig(
            randomSeed = 99L,
            events = listOf(event)
        )

        // Configure FakeScriptRunner to fail twice then succeed.
        scriptRunner.setBehavior("flaky") { invocation ->
            val exit = if (invocation < 3) 1 else 0
            ScriptResult(
                exitCode = exit,
                stdout = "attempt=$invocation",
                stderr = if (exit != 0) "error attempt $invocation" else "",
                metrics = emptyMap()
            )
        }

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "Event should eventually succeed within retry policy")
        assertEquals(
            3,
            scriptRunner.getInvocationCount("flaky"),
            "ScriptRunner should have been called three times (2 failures + 1 success)"
        )
    }

    // -------------------------------------------------------------------------
    // 4. SKIP_FUTURE POLICY
    // -------------------------------------------------------------------------

    /**
     * Verifies that an event with FailurePolicy.SKIP_FUTURE is disabled after
     * the first failure and is **not** executed on subsequent runOnce() calls.
     */
    @Test
    fun `skip future disables event after failure`() {
        val event = StressEvent(
            id = "bad_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            onFailure = FailurePolicy.SKIP_FUTURE
        )

        val config = OrcaTestConfig(
            randomSeed = 7L,
            events = listOf(event)
        )

        // This event always fails (exitCode=1).
        scriptRunner.setBehavior("bad_event") { _ ->
            ScriptResult(
                exitCode = 1,
                stdout = "",
                stderr = "boom",
                metrics = emptyMap()
            )
        }

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        // First run → event executes once and fails.
        val first = engine.runOnce()
        // Second run → event should be disabled and therefore never executed again.
        val second = engine.runOnce()

        assertFalse(first, "First execution should fail.")
        assertFalse(second, "Second runOnce should find no eligible events (event disabled).")

        assertEquals(
            1,
            scriptRunner.getInvocationCount("bad_event"),
            "Event with SKIP_FUTURE must only run once across two runOnce() calls."
        )
    }

    // -------------------------------------------------------------------------
    // 5. SEQUENCE EVENTS
    // -------------------------------------------------------------------------

    /**
     * Verifies that a SEQUENCE event executes its child events in order and
     * delegates each child to ScriptRunner via executeEventWithPolicy.
     *
     * The sequence parent itself is a non-script event; only children run scripts.
     */
    @Test
    fun `sequence event executes children in order`() {
        val child1 = StressEvent(
            id = "child_1",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )
        val child2 = StressEvent(
            id = "child_2",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )
        val parent = StressEvent(
            id = "sequence_parent",
            type = EventType.SEQUENCE,
            mode = EventMode.RANDOM,
            enabled = true,
            sequence = listOf("child_1", "child_2")
        )

        val config = OrcaTestConfig(
            randomSeed = 101L,
            events = listOf(parent, child1, child2)
        )

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "Sequence event should succeed when both children succeed.")

        // ScriptRunner should have processed child_1 then child_2, not the parent.
        assertEquals(
            listOf("child_1", "child_2"),
            scriptRunner.executedEvents,
            "Sequence should execute children in declared order."
        )
    }

    // -------------------------------------------------------------------------
    // 6. POST EVENTS
    // -------------------------------------------------------------------------

    /**
     * Validates that postEvents are executed after the main event succeeds.
     *
     * Flow:
     *   root_event (RANDOM, SCRIPT) succeeds → postEvents=["post_event"]
     *   → engine invokes executeEventWithPolicy() for "post_event".
     */
    @Test
    fun `post events are triggered after success`() {
        val root = StressEvent(
            id = "root_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            postEvents = listOf("post_event")
        )
        val post = StressEvent(
            id = "post_event",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )

        val config = OrcaTestConfig(
            randomSeed = 202L,
            events = listOf(root, post)
        )

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "Root event should succeed and trigger post-event chain.")

        // We expect the root to run first, then the post event.
        assertEquals(
            listOf("root_event", "post_event"),
            scriptRunner.executedEvents,
            "Post event should be executed immediately after the root event succeeds."
        )
    }

    // -------------------------------------------------------------------------
    // 7. CONDITIONAL TRIGGERS (OUTPUT + EXIT CODE)
    // -------------------------------------------------------------------------

    /**
     * Verifies that a conditional trigger fires when script output and exit code
     * satisfy the trigger conditions.
     *
     * Condition:
     *   - ifOutputContains = "magic"
     *   - ifExitCodeEquals 0
     *
     * Behavior:
     *   - main_event executes and returns stdout containing "magic", exitCode=0
     *   - conditional trigger fires and executes child_event
     */
    @Test
    fun `conditional trigger fires based on output and exit code`() {
        val child = StressEvent(
            id = "child_event",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )

        val trigger = ConditionalTrigger(
            triggerEventId = "child_event",
            ifOutputContains = "magic",
            ifExitCodeEquals = 0
        )

        val main = StressEvent(
            id = "main_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            conditionalTriggers = listOf(trigger)
        )

        val config = OrcaTestConfig(
            randomSeed = 303L,
            events = listOf(main, child)
        )

        // Configure ScriptRunner:
        //  - main_event → success with stdout containing "magic"
        //  - child_event → success default
        scriptRunner.setBehavior("main_event") { _ ->
            ScriptResult(
                exitCode = 0,
                stdout = "this contains magic text",
                stderr = "",
                metrics = emptyMap()
            )
        }

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "main_event should succeed.")
        assertEquals(
            listOf("main_event", "child_event"),
            scriptRunner.executedEvents,
            "Conditional trigger should execute child_event after main_event."
        )
    }

    // -------------------------------------------------------------------------
    // 8. CONDITIONAL TRIGGER VIA METRICS (metrics + SystemInspector.captureMetrics)
    // -------------------------------------------------------------------------

    /**
     * Tests metrics-based conditional triggers.
     *
     * Scenario:
     *   - main_event has MetricsConfig(captureCpuUsage = true)
     *   - FakeSystemInspector returns:
     *       preMetrics["cpu.totalPercent"] = 10.0
     *       postMetrics["cpu.totalPercent"] = 30.0
     *     → OrcaEngine computes delta = 20.0
     *
     *   - ConditionalTrigger requires:
     *       ifMetricAbove["cpu.totalPercent"] = 15.0
     *
     *   Expected:
     *     - metrics delta (20.0) satisfies the condition → triggers child_event.
     */
    @Test
    fun `conditional trigger fires based on metrics`() {
        val child = StressEvent(
            id = "metrics_child",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )

        val trigger = ConditionalTrigger(
            triggerEventId = "metrics_child",
            ifMetricAbove = mapOf("cpu.totalPercent" to 15.0)
        )

        val main = StressEvent(
            id = "metrics_main",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            metrics = MetricsConfig(
                captureCpuUsage = true
            ),
            conditionalTriggers = listOf(trigger)
        )

        val config = OrcaTestConfig(
            randomSeed = 404L,
            events = listOf(main, child)
        )

        // Script result for main_event: success with no inherent metrics.
        scriptRunner.setBehavior("metrics_main") { _ ->
            ScriptResult(
                exitCode = 0,
                stdout = "test",
                stderr = "",
                metrics = emptyMap()
            )
        }

        // Configure FakeSystemInspector to return:
        //   first captureMetrics() call → preMetrics
        //   second captureMetrics() call → postMetrics
        systemInspector.metricsSequence = listOf(
            mapOf("cpu.totalPercent" to 10.0),   // pre
            mapOf("cpu.totalPercent" to 30.0)    // post
        )

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "metrics_main should succeed.")
        assertEquals(
            listOf("metrics_main", "metrics_child"),
            scriptRunner.executedEvents,
            "Metrics-based conditional trigger should fire and execute metrics_child."
        )
    }

    // -------------------------------------------------------------------------
    // 9. REBOOT RECOVERY FLOW
    // -------------------------------------------------------------------------

    /**
     * Validates that reboot recovery logic invokes SystemInspector's:
     *  - awaitDeviceOffline
     *  - awaitDeviceOnline
     *  - awaitBootCompleted   (when waitForBoot=true)
     *  - startApp             (when restartAppAfterBoot=true)
     *
     * This test configures a SCRIPT event that:
     *  - succeeds
     *  - sets causesReboot=true
     *  - waitForBoot=true
     *  - restartAppAfterBoot=true
     */
    @Test
    fun `reboot recovery invokes offline online boot and app restart`() {
        val rebootEvent = StressEvent(
            id = "reboot_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            causesReboot = true,
            waitForBoot = true,
            restartAppAfterBoot = true
        )

        val config = OrcaTestConfig(
            randomSeed = 505L,
            events = listOf(rebootEvent),
            targetPackage = "com.example.app",
            logcat = LogcatConfig(
                enabled = true,
                rotateOnReboot = true,
                tag = "TAG"
            )
        )

        // The main script succeeds; we only care about reboot flow.
        scriptRunner.setBehavior("reboot_event") { _ ->
            ScriptResult(
                exitCode = 0,
                stdout = "trigger reboot",
                stderr = "",
                metrics = emptyMap()
            )
        }

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val success = engine.runOnce()

        assertTrue(success, "Reboot event should succeed, including recovery.")

        // Verify that reboot-related hooks were invoked.
        assertTrue(systemInspector.awaitOfflineCalled, "awaitDeviceOffline must be called.")
        assertTrue(systemInspector.awaitOnlineCalled, "awaitDeviceOnline must be called.")
        assertTrue(systemInspector.awaitBootCalled, "awaitBootCompleted must be called.")
        assertEquals(
            listOf("com.example.app"),
            systemInspector.startedApps,
            "startApp should be called once for target package."
        )

        // LogcatManager should have been rotated (stopped then started).
        assertTrue(
            logcatManager.stopCalled,
            "LogcatManager.stopCapture should be called for reboot rotation."
        )
        assertTrue(
            logcatManager.startCalled,
            "LogcatManager.startCapture should be called after reboot."
        )
    }
}

/* ================================================================================================
 * TEST DOUBLE IMPLEMENTATIONS
 * ================================================================================================
 *
 * The following classes live **only in the test source set** and provide
 * controlled behavior for OrcaEngine:
 *
 *  - FakeSystemInspector
 *  - FakeScriptRunner
 *  - FakeLogcatManager
 *  - CollectingLogger
 *
 * They are heavily commented because they serve as documentation for how
 * OrcaEngine collaborates with its dependencies.
 */

/**
 * Minimal [SystemInspector] implementation for unit tests.
 *
 * All fields are mutable so that each test can configure the "device"
 * behavior it needs without touching production code.
 */
class FakeSystemInspector : SystemInspector {

    // -----------------------------
    // Configurable system state
    // -----------------------------

    var batteryLevel: Int? = 100
    var networkAvailable: Boolean? = true
    var deviceIdle: Boolean? = true
    var screenOn: Boolean? = true
    var charging: Boolean? = true
    var rootAvailable: Boolean? = false
    var adbUp: Boolean? = true

    /** List of paths that exist from the engine's point of view. */
    val existingFiles: MutableSet<String> = mutableSetOf()

    /** Packages considered "running" by isProcessRunning(). */
    val runningPackages: MutableSet<String> = mutableSetOf()

    /** Sequence of metrics snapshots returned on consecutive captureMetrics() calls. */
    var metricsSequence: List<Map<String, Double>> = emptyList()
    private var metricsIndex = 0

    // -----------------------------
    // Reboot / wait tracking flags
    // -----------------------------

    var awaitOfflineCalled: Boolean = false
    var awaitOnlineCalled: Boolean = false
    var awaitBootCalled: Boolean = false

    /** Records which packages were "started" by startApp(). */
    val startedApps: MutableList<String> = mutableListOf()

    // -----------------------------
    // SystemInspector implementation
    // -----------------------------

    override fun getBatteryLevel(): Int? = batteryLevel

    override fun isNetworkAvailable(): Boolean? = networkAvailable

    override fun isDeviceIdle(): Boolean? = deviceIdle

    override fun isScreenOn(): Boolean? = screenOn

    override fun isCharging(): Boolean? = charging

    override fun isRootAvailable(): Boolean? = rootAvailable

    override fun adbAvailable(): Boolean? = adbUp

    override fun fileExists(path: String): Boolean = existingFiles.contains(path)

    override fun isProcessRunning(packageName: String?): Boolean {
        if (packageName == null) return false
        return runningPackages.contains(packageName)
    }

    override fun awaitDeviceOffline() {
        awaitOfflineCalled = true
    }

    override fun awaitDeviceOnline() {
        awaitOnlineCalled = true
    }

    override fun awaitBootCompleted() {
        awaitBootCalled = true
    }

    override fun startApp(packageName: String?) {
        if (packageName != null) {
            startedApps += packageName
        }
    }

    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        if (config == null || metricsSequence.isEmpty()) {
            return emptyMap()
        }
        // Return metrics in sequence; if out of range, repeat last snapshot.
        val idx = metricsIndex.coerceAtMost(metricsSequence.size - 1)
        val snapshot = metricsSequence[idx]
        metricsIndex++
        return snapshot
    }
}

/**
 * Fake [ScriptRunner] that:
 *  - Records each event ID it was asked to run.
 *  - Allows per-event custom behavior to simulate success/failure/metrics.
 */
class FakeScriptRunner : ScriptRunner {

    /**
     * Records the order in which events were executed.
     */
    val executedEvents: MutableList<String> = mutableListOf()

    /**
     * Per-event behavior map.
     *
     * The lambda receives the current invocation index (1-based) for that event
     * and returns a [ScriptResult] representing the outcome of that invocation.
     */
    private val behavior: MutableMap<String, (Int) -> ScriptResult> = mutableMapOf()

    /** Tracks how many times each event has been invoked. */
    private val invocations: MutableMap<String, Int> = mutableMapOf()

    /**
     * Configure behavior for a given event ID.
     */
    fun setBehavior(eventId: String, fn: (invocation: Int) -> ScriptResult) {
        behavior[eventId] = fn
    }

    /**
     * Returns how many times the given event ID has been executed.
     */
    fun getInvocationCount(eventId: String): Int = invocations[eventId] ?: 0

    override fun run(event: StressEvent): ScriptResult {
        executedEvents += event.id

        val currentCount = (invocations[event.id] ?: 0) + 1
        invocations[event.id] = currentCount

        // Use configured behavior if present; otherwise return a generic success.
        val fn = behavior[event.id]
        return fn?.invoke(currentCount)
            ?: ScriptResult(
                exitCode = 0,
                stdout = "default success for ${event.id}",
                stderr = "",
                metrics = emptyMap()
            )
    }
}

/**
 * No-op [LogcatManager] implementation for unit tests.
 *
 * We only track whether methods were called; no actual processes are spawned.
 */
class FakeLogcatManager : LogcatManager {

    var startCalled: Boolean = false
    var stopCalled: Boolean = false

    override fun startCapture(tag: String?) {
        startCalled = true
    }

    override fun stopCapture() {
        stopCalled = true
    }

    override fun rotate(tag: String?) {
        // For tests, just mark stop+start.
        stopCalled = true
        startCalled = true
    }
}

/**
 * Simple [EngineLogger] implementation that collects log lines in memory.
 *
 * This is useful for optional assertions on log content. For now, we do not
 * assert logs in the tests above, but this logger makes it easy to add such
 * checks later.
 */
class CollectingLogger : EngineLogger {

    val infoLines: MutableList<String> = mutableListOf()
    val warnLines: MutableList<String> = mutableListOf()
    val errorLines: MutableList<String> = mutableListOf()

    override fun info(message: String) {
        infoLines += message
    }

    override fun warn(message: String) {
        warnLines += message
    }

    override fun error(message: String, t: Throwable?) {
        val full = if (t != null) "$message: ${t.message}" else message
        errorLines += full
    }
}
