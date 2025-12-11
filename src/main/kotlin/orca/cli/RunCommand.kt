package orca.cli

import orca.engine.config.OrcaConfigLoader
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.core.OrcaEngine
import orca.engine.core.OrcaEngineFactory
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import java.io.File

/**
 * Implements the `orca run <config.json>` CLI command.
 *
 * Responsibilities:
 *  - Load and validate configuration
 *  - Construct the appropriate inspector (ADB)
 *  - Build the engine
 *  - Execute for duration or runLoop
 *  - Optionally run in single-step mode (--step)
 */
object RunCommand {

    fun run(configPath: String, cliOptions: GlobalCliOptions) {
        val configFile = File(configPath)

        if (!configFile.exists()) {
            println("❌ Config file not found: $configPath")
            return
        }

        println("ℹ️  Loading config: $configPath")

        val config = try {
            OrcaConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        val engine = OrcaEngineFactory.newEngine(
            targetPackage = config.targetPackage,
            mockMode = cliOptions.mockMode,
            configAttrib =  config,
            logger = ConsoleEngineLogger(),
        )
        if (engine == null) {
            println("❌ Failed to create Orca Engine")
            return
        }

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
