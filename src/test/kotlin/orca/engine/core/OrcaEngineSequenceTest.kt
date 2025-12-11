/*
 * Dual License Notice
 * -------------------
 * (same header)
 */

package orca.engine.core

import orca.engine.logging.LogcatManager
import orca.engine.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests covering SEQUENCE event execution behavior.
 *
 * Verifies:
 *  - child events are executed in order
 *  - sequence stops on first child failure
 *  - unknown child IDs are skipped (warning only)
 *  - nested sequences work correctly
 *  - sequence setState merges properly
 *  - postEvents and conditional triggers inside children are honored
 *  - sequence success updates stats correctly
 */
class OrcaEngineSequenceTest {

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

    private fun engineFor(vararg events: StressEvent): OrcaEngine {
        val cfg = OrcaTestConfig(
            randomSeed = 1,
            events = events.toList(),
            debug = true
        )
        return OrcaEngine(cfg, inspector, runner, logcat, logger)
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 1. BASIC SEQUENCE EXECUTION ORDER
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `sequence executes children in declared order`() {
        val e1 = StressEvent(id = "A", type = EventType.SCRIPT)
        val e2 = StressEvent(id = "B", type = EventType.SCRIPT)
        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("A", "B")
        )

        val engine = engineFor(seq, e1, e2)

        val success = engine.runOnce()
        assertTrue(success)

        assertEquals(listOf("A", "B"), runner.executedEvents,
            "Sequence should execute A then B in order")
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 2. SEQUENCE HALTS ON FIRST FAILURE
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `sequence stops on first failing child`() {
        val ok = StressEvent(id = "OK", type = EventType.SCRIPT)
        val bad = StressEvent(id = "BAD", type = EventType.SCRIPT)

        runner.setBehavior("BAD") {
            ScriptResult(exitCode = 1, stdout = "", stderr = "fail", metrics = emptyMap())
        }

        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("OK", "BAD", "NEVER")
        )

        val engine = engineFor(seq, ok, bad)

        val result = engine.runOnce()
        assertFalse(result, "Sequence should return false if any child fails")

        assertEquals(listOf("OK", "BAD"), runner.executedEvents,
            "Sequence must stop after BAD (NEVER should not execute)")
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 3. UNKNOWN CHILD IDS ARE SKIPPED
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `sequence skips unknown child IDs but continues`() {
        val ok = StressEvent(id = "OK", type = EventType.SCRIPT)
        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("MISSING", "OK")
        )

        val engine = engineFor(seq, ok)

        val success = engine.runOnce()
        assertTrue(success)

        assertEquals(listOf("OK"), runner.executedEvents,
            "Only known children should run")

        assertTrue(
            logger.warnings.any { it.contains("MISSING") },
            "Missing sequence ID should generate warning"
        )
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 4. NESTED SEQUENCES
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `nested sequences execute correctly`() {
        val c1 = StressEvent(id = "C1", type = EventType.SCRIPT)
        val c2 = StressEvent(id = "C2", type = EventType.SCRIPT)

        val inner = StressEvent(
            id = "INNER",
            type = EventType.SEQUENCE,
            sequence = listOf("C1", "C2")
        )

        val outer = StressEvent(
            id = "OUTER",
            type = EventType.SEQUENCE,
            sequence = listOf("INNER")
        )

        val engine = engineFor(outer, inner, c1, c2)

        val success = engine.runOnce()
        assertTrue(success)

        assertEquals(
            listOf("C1", "C2"),
            runner.executedEvents,
            "Nested sequence should flatten to sequential evaluation"
        )
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 5. setState WORKS INSIDE SEQUENCES
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `child setState values propagate through sequence`() {
        val c1 = StressEvent(
            id = "C1",
            type = EventType.SCRIPT,
            setState = mapOf("x" to "1")
        )
        val c2 = StressEvent(
            id = "C2",
            type = EventType.SCRIPT,
            setState = mapOf("y" to "2")
        )

        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("C1", "C2")
        )

        val engine = engineFor(seq, c1, c2)

        engine.runOnce()

        val finalState = engine
            .javaClass
            .getDeclaredField("state")
            .apply { isAccessible = true }
            .get(engine) as Map<*, *>

        assertEquals("1", finalState["x"])
        assertEquals("2", finalState["y"])
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 6. postEvents INSIDE A CHILD INSIDE SEQUENCE
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `postEvents inside sequence child are executed`() {
        val post = StressEvent(id = "POST", type = EventType.SCRIPT)

        val child = StressEvent(
            id = "C1",
            type = EventType.SCRIPT,
            postEvents = listOf("POST")
        )

        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("C1")
        )

        val engine = engineFor(seq, child, post)

        engine.runOnce()

        assertEquals(
            listOf("C1", "POST"),
            runner.executedEvents,
            "post event should execute immediately after child"
        )
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 7. conditionalTriggers INSIDE A CHILD INSIDE A SEQUENCE
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `conditional trigger inside sequence child fires correctly`() {
        val triggered = StressEvent(id = "TRIG", type = EventType.SCRIPT)

        val child = StressEvent(
            id = "C1",
            type = EventType.SCRIPT,
            conditionalTriggers = listOf(
                ConditionalTrigger(
                    triggerEventId = "TRIG",
                    ifExitCodeEquals = 0
                )
            )
        )

        val seq = StressEvent(
            id = "SEQ",
            type = EventType.SEQUENCE,
            sequence = listOf("C1")
        )

        val engine = engineFor(seq, child, triggered)

        engine.runOnce()

        assertEquals(
            listOf("C1", "TRIG"),
            runner.executedEvents,
            "conditional trigger should fire from inside sequence child"
        )
    }

    // ------------------------------------------------------------------------------------------------------------------
    // 8. sequence stats increment
    // ------------------------------------------------------------------------------------------------------------------

    @Test
    fun `sequence event stats increment on success`() {
        val c1 = StressEvent(id = "C1", type = EventType.SCRIPT)
        val seq = StressEvent(id = "SEQ", type = EventType.SEQUENCE, sequence = listOf("C1"))

        val engine = engineFor(seq, c1)

        engine.runOnce()

        val statsMap =
            engine.javaClass.getDeclaredField("eventStats")
                .apply { isAccessible = true }
                .get(engine) as Map<*, *>

        val seqStats = statsMap["SEQ"] as EventStats

        assertEquals(1, seqStats.executions)
    }
}
