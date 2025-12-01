/*
 * Dual License Notice
 * -------------------
 * (same header as your project…)
 */

package orca.engine.core

import orca.engine.model.*
import orca.engine.logging.LogcatManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests exclusively covering RANDOM-mode event selection rules.
 *
 * These tests DO NOT execute scripts — they validate that
 * selectNextEvent() chooses correct events under:
 *  - weights
 *  - maxExecutions
 *  - cooldownSeconds
 *  - disabled stats (SKIP_FUTURE)
 *  - profile-based weights
 */
class OrcaEngineEventSelectionTest {

    private lateinit var inspector: FakeSystemInspector
    private lateinit var runner: FakeScriptRunner
    private lateinit var logger: CollectingLogger
    private lateinit var logcat: LogcatManager

    @BeforeEach
    fun setup() {
        inspector = FakeSystemInspector()
        runner = FakeScriptRunner()
        logger = CollectingLogger()
        logcat = FakeLogcatManager()
    }

    /**
     * Creates a fresh engine with provided events.
     */
    private fun engineFor(vararg events: StressEvent): OrcaEngine {
        val config = OrcaTestConfig(
            randomSeed = 123L,
            events = events.toList()
        )
        return OrcaEngine(config, inspector, runner, logger, logcat)
    }

    // ---------------------------------------------------------------------------------------------
    // 1. WEIGHT DISTRIBUTION
    // ---------------------------------------------------------------------------------------------

    /**
     * Verifies that RANDOM selection favors higher weight.
     *
     * E1 weight=1
     * E2 weight=5
     *
     * Over 200 iterations, E2 should overwhelmingly dominate.
     */
    @Test
    fun `weighted random selection favors higher weights`() {
        val e1 = StressEvent(
            id = "low",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 1
        )
        val e2 = StressEvent(
            id = "high",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 5
        )

        val engine = engineFor(e1, e2)

        val counts = mutableMapOf("low" to 0, "high" to 0)

        repeat(200) {
            val ev = engine.runOnce() // just selecting, scripts succeed automatically
            // ScriptRunner records event id:
        }

        runner.executedEvents.forEach { id -> counts[id] = counts[id]!! + 1 }

        assertTrue(
            counts["high"]!! > counts["low"]!! * 3,
            "High-weight event should occur far more often. $counts"
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 2. MAX EXECUTIONS
    // ---------------------------------------------------------------------------------------------

    /**
     * Verifies that an event with maxExecutions stops being selected
     * after reaching the limit.
     */
    @Test
    fun `event with maxExecutions stops being selected`() {
        val limited = StressEvent(
            id = "limited",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            maxExecutions = 2,
            weight = 10 // ensure it is always chosen initially
        )
        val other = StressEvent(
            id = "other",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 1
        )

        val engine = engineFor(limited, other)

        // Run enough iterations that limited should hit max and stop
        repeat(10) {
            engine.runOnce()
        }

        val limitedCount = runner.executedEvents.count { it == "limited" }
        val otherCount = runner.executedEvents.count { it == "other" }

        assertEquals(2, limitedCount, "limited event should only execute up to maxExecutions=2")
        assertTrue(otherCount >= 1, "other should eventually run once limited is exhausted")
    }

    // ---------------------------------------------------------------------------------------------
    // 3. COOLDOWN SECONDS
    // ---------------------------------------------------------------------------------------------

    /**
     * cooldownSeconds prevents immediate reselection of an event.
     *
     * cooldownSeconds=2
     * runOnce() calls should:
     *   - run event first time
     *   - skip event on next run due to cooldown
     */
    @Test
    fun `cooldown prevents immediate reselection`() {
        val cooldown = StressEvent(
            id = "cd",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            cooldownSeconds = 2,
            weight = 10
        )

        val fallback = StressEvent(
            id = "fallback",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 1
        )

        val engine = engineFor(cooldown, fallback)

        // First run: should execute cd
        engine.runOnce()

        // Second run: cd is on cooldown → must choose fallback
        engine.runOnce()

        assertEquals(
            listOf("cd", "fallback"),
            runner.executedEvents.take(2),
            "Cooldown event must NOT be selected twice in a row."
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 4. DISABLED EVENTS (after SKIP_FUTURE)
    // ---------------------------------------------------------------------------------------------

    /**
     * Once an event is disabled through SKIP_FUTURE, it must never be selected.
     */
    @Test
    fun `disabled event is never selected`() {
        val bad = StressEvent(
            id = "bad",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            onFailure = FailurePolicy.SKIP_FUTURE
        )
        val ok = StressEvent(
            id = "ok",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM
        )

        val engine = engineFor(bad, ok)

        // Make "bad" fail so it disables itself.
        runner.setBehavior("bad") { _ ->
            ScriptResult(exitCode = 1, stdout = "", stderr = "fail", metrics = emptyMap())
        }

        engine.runOnce() // runs bad, disables it
        runner.executedEvents.clear()

        // All further runs should pick ok only
        repeat(5) {
            engine.runOnce()
        }

        assertTrue(
            runner.executedEvents.all { it == "ok" },
            "Disabled event 'bad' should never be selected again."
        )
    }

    // ---------------------------------------------------------------------------------------------
    // 5. PROFILE-BASED WEIGHTS
    // ---------------------------------------------------------------------------------------------

    /**
     * Verifies that profileWeights override base weight when a profile is supplied.
     */
    @Test
    fun `profile weights override default weight`() {
        val e1 = StressEvent(
            id = "one",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 1,
            profileWeights = mapOf("stress" to 10)
        )
        val e2 = StressEvent(
            id = "two",
            type = EventType.SCRIPT,
            mode = EventMode.RANDOM,
            weight = 10,
            profileWeights = mapOf("stress" to 1)
        )

        val engine = engineFor(e1, e2)

        val counts = mutableMapOf("one" to 0, "two" to 0)

        repeat(200) {
            engine.runOnce(profile = "stress")
        }

        runner.executedEvents.forEach { id -> counts[id] = counts[id]!! + 1 }

        assertTrue(
            counts["one"]!! > counts["two"]!! * 3,
            "Profile 'stress' should favor event 'one' due to profileWeights override"
        )
    }
}
