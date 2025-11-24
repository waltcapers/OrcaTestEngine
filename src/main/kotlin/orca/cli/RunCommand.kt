package orca.cli

import orca.engine.config.StressConfigLoader
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.core.OrcaEngine
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import java.io.File

/**
 * Implements the `orca run <config.json>` CLI command.
 *
 * Responsibilities:
 *  - Load and validate configuration
 *  - Construct the appropriate inspector (ADB or default)
 *  - Build the engine
 *  - Execute for duration or runLoop
 */
object RunCommand {

    fun run(configPath: String) {
        val logger = ConsoleEngineLogger()
        val configFile = File(configPath)

        if (!configFile.exists()) {
            println("❌ Config file not found: $configPath")
            return
        }

        println("ℹ️  Loading config: $configPath")

        val config = StressConfigLoader.load(configPath)

        // ---- Create ADB inspector (default mode) ----
        val inspector = AdbSystemInspector(
            adb = DefaultAdbExecutor(
                adbPath = "adb",
                deviceSerial = null,
                logger = logger
            ),
            defaultPackageName = config.targetPackage,
            debug = config.debug
        )

        val scriptRunner = ScriptRunnerDispatcher()

        val engine = OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
        )

        // Shutdown hook for CTRL+C
        Runtime.getRuntime().addShutdownHook(Thread {
            println("\nStopping engine...")
            engine.stop()
        })

        if (config.maxTestDurationSeconds != null) {
            engine.runForDuration(config.maxTestDurationSeconds.toLong())
            engine.saveReplayState()
        } else {
            engine.runLoop()
        }
    }
}
