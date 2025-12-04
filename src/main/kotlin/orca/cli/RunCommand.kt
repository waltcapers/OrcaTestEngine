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
 *  - Construct the appropriate inspector (ADB)
 *  - Build the engine
 *  - Execute for duration or runLoop
 *  - Optionally run in single-step mode (--step)
 */
object RunCommand {

    fun run(configPath: String, cliOptions: GlobalCliOptions) {
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
                deviceSerial = cliOptions.deviceId,
                logger = logger
            ),
            defaultPackageName = config.targetPackage,
            debug = config.debug,
            logger = logger
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

        // --------------------------------------------------------------------
        //  STEP MODE: run one event at a time when --step is provided
        // --------------------------------------------------------------------
        if (cliOptions.stepMode) {
            println()
            println("▶ Step mode enabled. Press <Enter> to run the next event, or type 'q' to quit.")
            val reader = System.`in`.bufferedReader()

            while (true) {
                print("step> ")
                val line = reader.readLine() ?: break
                val trimmed = line.trim()

                if (trimmed.equals("q", ignoreCase = true) ||
                    trimmed.equals("quit", ignoreCase = true) ||
                    trimmed.equals("exit", ignoreCase = true)
                ) {
                    println("Exiting step mode.")
                    break
                }

                val success = engine.runOnce()
                if (!success) {
                    println("Last event failed (see logs for details).")
                }
            }

            engine.printSummary()
            return
        }

        // --------------------------------------------------------------------
        //  NORMAL BEHAVIOR (unchanged)
        // --------------------------------------------------------------------
        if (config.maxTestDurationSeconds != null) {
            engine.runForDuration(config.maxTestDurationSeconds.toLong())
            engine.saveReplayState()
        } else {
            engine.runLoop()
        }
    }
}
