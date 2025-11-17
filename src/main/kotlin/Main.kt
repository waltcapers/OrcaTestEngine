import orca.engine.config.StressConfigLoader
import orca.engine.core.DefaultScriptRunner
import orca.engine.core.OrcaEngine
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.DefaultSystemInspector
import java.io.File

fun main() {
    val config = StressConfigLoader.load("stress-config.json")
    val inspector = DefaultSystemInspector()
    val scriptRunner = DefaultScriptRunner()
    val logger = ConsoleEngineLogger()

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
    if(replayFile.exists()) {
        engine.replay()
    } else {

        // Start the loop
        val maxDurationSec = config.maxTestDurationSeconds

        if (maxDurationSec != null) {
            engine.runForDuration(maxDurationSec.toLong())
            engine.saveReplayState()
        } else {
            // fallback behavior if no duration provided
            engine.runLoop()
        }
    }

}
