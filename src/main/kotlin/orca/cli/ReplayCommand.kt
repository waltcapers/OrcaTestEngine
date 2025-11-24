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
package orca.cli

import orca.engine.config.StressConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Implements:
 *
 *   orca replay
 *
 * Behavior:
 *   - Loads the default config file (orca-config.json in current directory).
 *     (You can extend this later to accept a config path argument.)
 *   - Creates an OrcaEngine with a DefaultSystemInspector.
 *   - Calls engine.replay() which uses replay_state.json.
 *
 * NOTE:
 *   The replay mechanism does not depend on ADB; it is purely driven by the
 *   recorded RNG seed and RNG call count.
 */
object ReplayCommand {

    private const val DEFAULT_CONFIG = "orca-config.json"
    private const val DEFAULT_REPLAY_STATE = "replay_state.json"

    fun run() {
        val configFile = File(DEFAULT_CONFIG)
        if (!configFile.exists()) {
            println("❌ Default config file not found: ${configFile.absolutePath}")
            println("   Usage: orca replay (expects $DEFAULT_CONFIG and $DEFAULT_REPLAY_STATE in the current directory)")
            return
        }

        val replayStateFile = File(DEFAULT_REPLAY_STATE)
        if (!replayStateFile.exists()) {
            println("❌ Replay state file not found: ${replayStateFile.absolutePath}")
            println("   Run a failing test first so OrcaEngine can write replay_state.json.")
            return
        }

        val logger = ConsoleEngineLogger()

        val config = try {
            StressConfigLoader.load(DEFAULT_CONFIG)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        val inspector = DefaultSystemInspector(debug = false)
        val scriptRunner = orca.engine.core.ScriptRunnerDispatcher()

        val engine = OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
        )

        // Uses OrcaEngine.replay(), which internally reads replay_state.json.
        engine.replay()
    }
}
