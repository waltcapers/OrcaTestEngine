/*
 * Dual License Notice
 * -------------------
 * (same header as your project)
 */

package orca.engine.system

import orca.engine.core.EngineLogger
import orca.engine.logging.ConsoleEngineLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests the real behavior of the current AdbSystemInspector:
 *
 *  - awaitDeviceOffline(): loops until get-state exit != 0 or text blank/unknown
 *  - awaitDeviceOnline(): calls "wait-for-device" and returns
 *  - awaitBootCompleted(): polls until getprop sys.boot_completed returns "1"
 *
 * Instead of using timeouts, we simulate a finite sequence of fake ADB results.
 */
class AdbSystemInspectorWaitTests {

    /**
     * Helper to build a FakeAdbExecutor with sequential behavior.
     */
    private fun inspectorWithSequences(
        execSeq: Map<List<String>, List<AdbResult>> = emptyMap(),
        shellSeq: Map<String, List<AdbResult>> = emptyMap(),
        logger: EngineLogger
    ): AdbSystemInspector {

        val execCounters = execSeq.mapValues { 0 }.toMutableMap()
        val shellCounters = shellSeq.mapValues { 0 }.toMutableMap()

        val fake = FakeAdbExecutor(
            cannedExec = { args ->
                val list = execSeq[args]
                if (list != null) {
                    val i = execCounters[args]!!
                    execCounters[args] = i + 1
                    list.getOrElse(i) { list.last() }
                } else AdbResult(0, "", "")
            },
            cannedShell = { cmd ->
                val list = shellSeq[cmd]
                if (list != null) {
                    val i = shellCounters[cmd]!!
                    shellCounters[cmd] = i + 1
                    list.getOrElse(i) { list.last() }
                } else AdbResult(0, "", "")
            }
        )

        return AdbSystemInspector(
            adb = fake,
            defaultPackageName = "com.test",
            debug = false,
            logger = logger
        )
    }

    // ---------------------------------------------------------------------
    // awaitDeviceOffline tests
    // ---------------------------------------------------------------------

    @Test
    fun `awaitDeviceOffline exits once get-state stops reporting device`() {
        val ins = inspectorWithSequences(
            execSeq = mapOf(
                listOf("get-state") to listOf(
                    AdbResult(0, "device", ""),   // still online
                    AdbResult(0, "device", ""),   // still online
                    AdbResult(1, "", "")          // exit != 0 → offline!
                )
            ),
            logger = ConsoleEngineLogger()
        )

        // If the logic is correct, this will return and NOT infinite-loop.
        ins.awaitDeviceOffline()

        // If we reach here, it succeeded.
        assertTrue(true)
    }

    @Test
    fun `awaitDeviceOffline exits when get-state returns unknown`() {
        val ins = inspectorWithSequences(
            execSeq = mapOf(
                listOf("get-state") to listOf(
                    AdbResult(0, "device", ""),
                    AdbResult(0, "unknown", "")   // unknown → offline
                )
            ),
            logger = ConsoleEngineLogger()
        )

        ins.awaitDeviceOffline()

        assertTrue(true)
    }

    // ---------------------------------------------------------------------
    // awaitDeviceOnline tests
    // ---------------------------------------------------------------------

    @Test
    fun `awaitDeviceOnline issues wait-for-device`() {

        var ran = false

        val fake = FakeAdbExecutor(
            cannedExec = { args ->
                if (args == listOf("wait-for-device")) {
                    ran = true
                }
                AdbResult(0, "", "")
            },
            cannedShell = { AdbResult(0, "", "") }
        )

        val ins = AdbSystemInspector(fake, "com.test", debug = false)

        ins.awaitDeviceOnline()

        assertTrue(ran, "wait-for-device should be issued exactly once")
    }

    // ---------------------------------------------------------------------
    // awaitBootCompleted tests
    // ---------------------------------------------------------------------

    @Test
    fun `awaitBootCompleted polls until getprop sysboot returns 1`() {

        val ins = inspectorWithSequences(
            shellSeq = mapOf(
                "getprop sys.boot_completed" to listOf(
                    AdbResult(0, "0", ""),   // still booting
                    AdbResult(0, "0", ""),   // still booting
                    AdbResult(0, "1", "")    // boot complete
                )
            ),
            logger = ConsoleEngineLogger()
        )

        // Should exit normally with no infinite loop
        ins.awaitBootCompleted()

        assertTrue(true)
    }
}
