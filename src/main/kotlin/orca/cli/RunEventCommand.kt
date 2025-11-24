package orca.cli

import orca.engine.config.StressConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.model.StressEvent
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Implements:
 *
 *   orca run-event <config.json> <eventId> [iterations]
 *
 * Behavior:
 *   - Loads config.
 *   - Finds a single event by ID.
 *   - Constructs a new OrcaTestConfig that includes ONLY that event.
 *   - Runs the engine for the specified number of iterations.
 *
 * Notes:
 *   - This is a “surgical” execution mode that isolates one event.
 *   - It is especially useful for debugging specific scripts or sequences.
 *   - For now we use DefaultSystemInspector, which is safe even without
 *     a real Android device connected. You can later swap in AdbSystemInspector.
 */
object RunEventCommand {

    fun run(configPath: String, eventId: String, iterations: Int) {
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

        val event = config.events.find { it.id == eventId }
        if (event == null) {
            println("❌ Event not found: $eventId")
            return
        }

        println("Running event '$eventId' in isolation for $iterations iteration(s).")

        // Create a narrow config with just this event.
        val singleEventConfig = config.copy(
            events = listOf<StressEvent>(event)
        )

        val logger = ConsoleEngineLogger()
        val inspector = DefaultSystemInspector(debug = true)
        val scriptRunner = ScriptRunnerDispatcher()

        val engine = OrcaEngine(
            config = singleEventConfig,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
        )

        engine.runForIterations(iterations.toLong())
    }
}
