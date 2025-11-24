package orca.cli

import orca.engine.config.StressConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Implements:
 *
 *   orca profile <config.json> [iterations]
 *
 * Behavior:
 *   - Loads config.
 *   - Runs the engine for N iterations using runForIterations().
 *   - Relies on OrcaEngine.printSummary() to show:
 *       * how many times each event executed
 *       * failures
 *       * metrics
 *
 * This is essentially a “quick distribution sampler” useful for:
 *   - Seeing how weights affect selection frequency.
 *   - Confirming that cooldown and maxExecutions behave as expected.
 *
 * NOTE:
 *   - We use DefaultSystemInspector so this is safe without Android attached.
 *   - For configs that truly depend on ADB, you can later build a variant
 *     that uses AdbSystemInspector instead.
 */
object ProfileCommand {

    fun run(configPath: String, iterations: Int) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        val config = try {
            StressConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        val logger = ConsoleEngineLogger()
        val inspector = DefaultSystemInspector(debug = false)
        val scriptRunner = ScriptRunnerDispatcher()

        val engine = OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
        )

        println("Profiling selection behavior for $iterations iteration(s)...")
        engine.runForIterations(iterations.toLong())
        // OrcaEngine.runForIterations() already calls printSummary() at the end.
    }
}
