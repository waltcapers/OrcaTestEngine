import orca.cli.OrcaCLI
import orca.engine.config.StressConfigLoader
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.core.OrcaEngine
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import orca.engine.system.DefaultSystemInspector
import java.io.File

fun main(args: Array<String>) {
    OrcaCLI.main(args)
}

fun mainTest() {
    val logger = ConsoleEngineLogger()
    val useAdb = false

    val config = try {
        StressConfigLoader.load("orca-config.json")
    } catch (t: Throwable) {
        println("\n❌ CONFIG VALIDATION FAILED\n${t.message}")
        return
    }
    // ADB-backed inspector
    val inspector = if (useAdb) {
        AdbSystemInspector(
            adb = DefaultAdbExecutor(
                adbPath = "adb",
                deviceSerial = null,
                logger = logger
            ),
            defaultPackageName = config.targetPackage,
            debug = false
        )
    } else {
        DefaultSystemInspector( debug = true)
    }

    val scriptRunner = ScriptRunnerDispatcher()

    val engine = OrcaEngine(
        config = config,
        systemInspector = inspector,
        scriptRunner = scriptRunner,
        logger = logger
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nStopping stress engine from shutdown hook...")
        engine.stop()
    })

    val replayFile = File("replay_state.json")
    if (replayFile.exists()) {
        engine.replay()
    } else {
        val maxDurationSec = config.maxTestDurationSeconds

        if (maxDurationSec != null) {
            engine.runForDuration(maxDurationSec.toLong())
            engine.saveReplayState()
        } else {
            engine.runLoop()
        }
    }
}
