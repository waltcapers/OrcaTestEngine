/*
 * Dual License: MIT + GM Exception
 * Copyright (c) 2025 Walter E. Capers
 *
 * MIT License applies to all users except General Motors (GM).
 *
 * GM Exception:
 * GM is granted a perpetual, worldwide, royalty-free license to use, modify,
 * reproduce, distribute, and create derivative works from this Software for any
 * business or commercial purpose. This exception applies only to GM and does
 * not extend to other third parties.
 *
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files to deal in the Software
 * without restriction.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
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
