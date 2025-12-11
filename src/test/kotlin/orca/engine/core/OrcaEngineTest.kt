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
        systemInspector.fakeBatteryLevel = 40

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

        // make child execution take measurable time
        scriptRunner.setBehavior("child_1") { _ ->
            Thread.sleep(10)
            ScriptResult(0, "", "", emptyMap())
        }
        scriptRunner.setBehavior("child_2") { _ ->
            Thread.sleep(15)
            ScriptResult(0, "", "", emptyMap())
        }

        val engine = OrcaEngine(
            config = config,
            systemInspector = systemInspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )

        val ok = engine.runOnce()
        assertTrue(ok)

        // children executed in order
        assertEquals(listOf("child_1", "child_2"), scriptRunner.executedEvents)

        // now validate parent stats
        val stats = engine.javaClass
            .getDeclaredField("eventStats")
            .apply { isAccessible = true }
            .get(engine) as MutableMap<String, EventStats>

        val parentStats = stats["sequence_parent"]!!
        assertEquals(1, parentStats.executions, "Parent should count as one execution")

        val duration = parentStats.lastDurationMs
        assertNotNull(duration, "Parent sequence must record a duration")
        assertTrue(
            duration!! >= 25,
            "Parent duration should include the sum of all child durations"
        )
        println("Parent sequence duration = $duration ms")
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
    // -------------------------------------------------------------------------
    // 10. RUN LOOP (runLoop / runForDuration / runForIterations)
    // -------------------------------------------------------------------------

    /**
     * Verifies that runForIterations() executes runOnce() the correct number
     * of times and stops early if a failure occurs.
     */
    @Test
    fun `runForIterations stops on first failure`() {
        val e1 = StressEvent(
            id = "ok_1",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true
        )
        val e2 = StressEvent(
            id = "fail_2",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true
        )

        val config = OrcaTestConfig(
            randomSeed = 111L,
            events = listOf(e1, e2)
        )

        // ok_1 succeeds always
        // fail_2 fails immediately when executed
        scriptRunner.setBehavior("fail_2") { _ ->
            ScriptResult(exitCode = 1, stdout = "", stderr = "err", metrics = emptyMap())
        }

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        engine.runForIterations(10)

        // Should have executed only until failure — never reaching 10.
        assertTrue(
            scriptRunner.executedEvents.size < 10,
            "runForIterations should stop on first failure."
        )
    }

    /**
     * runForDuration should loop while time remains and then print a summary.
     * We cannot assert time precisely, but we can assert:
     *  - at least one event ran
     *  - logcat start/stop called
     */
    @Test
    fun `runForDuration executes events and manages logcat`() {
        val e = StressEvent(
            id = "loop_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true
        )

        val config = OrcaTestConfig(
            randomSeed = 999L,
            events = listOf(e),
            targetPackage = "com.example.app",
            logcat = LogcatConfig(enabled = true, tag = "TAG")
        )

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        engine.runForDuration(maxSeconds = 0)   // effectively a quick single-loop

        assertTrue(scriptRunner.executedEvents.isNotEmpty(), "At least one event must run.")
        assertTrue(logcatManager.startCalled, "Logcat should start in runForDuration.")
        assertTrue(logcatManager.stopCalled, "Logcat should stop in runForDuration.")
    }

    // -------------------------------------------------------------------------
    // 11. RNG REPLAY BEHAVIOR
    // -------------------------------------------------------------------------

    /**
     * Verifies that replay():
     *  - loads a replay file
     *  - fast-forwards RNG to recorded state
     *  - executes one additional event
     *
     * We simulate the replay file via ReplayStateSerializer.
     */
    @Test
    fun `replay restores RNG state and executes final event`() {
        val event = StressEvent(
            id = "replay_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true
        )

        val config = OrcaTestConfig(
            randomSeed = 1234L,
            events = listOf(event)
        )

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        // Create fake replay state on disk
        ReplayStateSerializer.saveReplayState(
            ReplayState(seed = 1234L, rngCalls = 5L),
            path = "replay_state.json"
        )

        engine.replay()

        assertEquals(
            6,
            scriptRunner.getInvocationCount("replay_event"),
            "Replay should fast-forward RNG and execute 1 final event."
        )
    }

    // -------------------------------------------------------------------------
    // 12. METRICS — additional tests
    // -------------------------------------------------------------------------

    /**
     * Ensure that script-provided metrics override system metrics where
     * duplicate keys exist.
     */
    @Test
    fun `script metrics override system metrics`() {
        val event = StressEvent(
            id = "metrics_override",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            metrics = MetricsConfig(captureCpuUsage = true)
        )

        val config = OrcaTestConfig(
            randomSeed = 222L,
            events = listOf(event)
        )

        // Script provides its own metric
        scriptRunner.setBehavior("metrics_override") { _ ->
            ScriptResult(
                exitCode = 0,
                stdout = "",
                stderr = "",
                metrics = mapOf("cpu.totalPercent" to 999.0)
            )
        }

        // System metrics return CPU usage 10 → 20
        systemInspector.metricsSequence = listOf(
            mapOf("cpu.totalPercent" to 10.0),
            mapOf("cpu.totalPercent" to 20.0)
        )

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        val success = engine.runOnce()
        assertTrue(success)

        // Script metrics (999.0) should NOT be overwritten by delta (10.0→20.0)
        val final = scriptRunner.lastResult("metrics_override")
        assertNotNull(final, "Expected lastResult for metrics_override to be recorded")
        assertEquals(999.0, final!!.metrics["cpu.totalPercent"])
    }

    // -------------------------------------------------------------------------
    // 13. SCRIPT BEHAVIOR — stderr handling, success/failure logging
    // -------------------------------------------------------------------------

    /**
     * If logErrors=true and script fails with stderr, logger.error should be invoked.
     */
    @Test
    fun `script failure logs stderr when logErrors enabled`() {
        val event = StressEvent(
            id = "error_event",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            logErrors = true
        )

        val config = OrcaTestConfig(randomSeed = 777L, events = listOf(event))

        scriptRunner.setBehavior("error_event") { _ ->
            ScriptResult(exitCode = 1, stdout = "", stderr = "boom", metrics = emptyMap())
        }

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        engine.runOnce()

        assertTrue(
            logger.errors.any { it.contains("boom") },
            "stderr should appear in logger.error"
        )
    }

    // -------------------------------------------------------------------------
    // 14. PRECONDITIONS (expanded)
    // -------------------------------------------------------------------------

    /**
     * Event should fail preconditions when fileMustExist contains missing files.
     */
    @Test
    fun `preconditions fail when required file is missing`() {
        val event = StressEvent(
            id = "file_check",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            preconditions = Preconditions(fileMustExist = listOf("/missing.txt"))
        )

        val config = OrcaTestConfig(randomSeed = 12L, events = listOf(event))

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        val ok = engine.runOnce()
        assertFalse(ok)
        assertTrue(scriptRunner.executedEvents.isEmpty())
    }

    // -------------------------------------------------------------------------
    // 15. TRIGGER TESTS — additional edge cases
    // -------------------------------------------------------------------------

    /**
     * Trigger should not fire when exit code does not match
     */
    @Test
    fun `conditional trigger does not fire when exit code mismatch`() {
        val child = StressEvent(
            id = "child_fail",
            type = EventType.SCRIPT,
            mode = EventMode.SEQUENTIAL,
            enabled = true
        )

        val trigger = ConditionalTrigger(
            triggerEventId = "child_fail",
            ifExitCodeEquals = 0
        )

        val main = StressEvent(
            id = "main_fail",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            conditionalTriggers = listOf(trigger)
        )

        val config = OrcaTestConfig(randomSeed = 554L, events = listOf(main, child))

        scriptRunner.setBehavior("main_fail") { _ ->
            ScriptResult(
                exitCode = 1,
                stdout = "nope",
                stderr = "",
                metrics = emptyMap()
            )
        }

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        engine.runOnce()

        assertEquals(
            1,
            scriptRunner.getInvocationCount("main_fail"),
            "Only main event should run."
        )
    }

    // -------------------------------------------------------------------------
    // 16. REBOOT RECOVERY — does NOT wait for boot when waitForBoot=false
    // -------------------------------------------------------------------------

    @Test
    fun `reboot recovery skips boot wait when waitForBoot is false`() {
        val rebootEvent = StressEvent(
            id = "reboot_no_boot_wait",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            enabled = true,
            causesReboot = true,
            waitForBoot = false
        )

        val config = OrcaTestConfig(
            randomSeed = 909L,
            targetPackage = "com.test",
            events = listOf(rebootEvent),
            logcat = LogcatConfig(enabled = true)
        )

        scriptRunner.setBehavior("reboot_no_boot_wait") { _ ->
            ScriptResult(exitCode = 0, stdout = "", stderr = "", metrics = emptyMap())
        }

        val engine = OrcaEngine(config, systemInspector, scriptRunner, logcatManager, logger)

        engine.runOnce()

        assertFalse(
            systemInspector.awaitBootCalled,
            "Boot wait should NOT be invoked when waitForBoot=false."
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

    var fakeBatteryLevel: Int? = 100
    var fakeNetworkAvailable: Boolean? = true
    var fakeDeviceIdle: Boolean? = true
    var fakeScreenOn: Boolean? = true
    var fakeCharging: Boolean? = true
    var fakeRootAvailable: Boolean? = false
    var fakeAdbUp: Boolean? = true

    /** List of paths that exist from the engine's point of view. */
    val existingFiles: MutableSet<String> = mutableSetOf()

    /** Packages considered "running" by isProcessRunning(). */
    val runningPackages: MutableSet<String> = mutableSetOf()

    /** Sequence of metrics snapshots returned on consecutive captureMetrics() calls. */
    var metricsSequence: List<Map<String, Double>> = emptyList()
    private var metricsIndex = 0

    // -----------------------------
    // Reboot/wait tracking flags
    // -----------------------------

    var awaitOfflineCalled = false
    var awaitOnlineCalled = false
    var awaitBootCalled = false

    val startedApps = mutableListOf<String>()

    // -----------------------------
    // SystemInspector implementation
    // -----------------------------

    override fun getBatteryLevel(): Int? = fakeBatteryLevel
    override fun isNetworkAvailable(): Boolean? = fakeNetworkAvailable
    override fun isDeviceIdle(): Boolean? = fakeDeviceIdle
    override fun isScreenOn(): Boolean? = fakeScreenOn
    override fun isCharging(): Boolean? = fakeCharging
    override fun isRootAvailable(): Boolean? = fakeRootAvailable
    override fun adbAvailable(): Boolean? = fakeAdbUp

    override fun fileExists(path: String): Boolean = existingFiles.contains(path)

    override fun isProcessRunning(packageName: String?): Boolean {
       /* if (packageName == null) return false
        return runningPackages.contains(packageName) */
        return true
    }

    override fun awaitDeviceOffline() { awaitOfflineCalled = true }
    override fun awaitDeviceOnline() { awaitOnlineCalled = true }
    override fun awaitBootCompleted() { awaitBootCalled = true }

    override fun startApp(packageName: String?) {
        if (packageName != null) startedApps += packageName
    }

    override fun captureMetrics(config: MetricsConfig?): Map<String, Double> {
        if (config == null || metricsSequence.isEmpty()) return emptyMap()

        val idx = metricsIndex.coerceAtMost(metricsSequence.lastIndex)
        val snapshot = metricsSequence[idx]
        metricsIndex++
        return snapshot
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
    val infos = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()
    val debug = mutableListOf<String>()

    override fun info(message: String) { infos += message }
    override fun warn(message: String) { warnings += message }
    override fun debug(message:String) { debug += message}
    override fun error(message: String, t: Throwable?) {
        errors += if (t != null) "$message\n${t.message}" else message
    }
}


