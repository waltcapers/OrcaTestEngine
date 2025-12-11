/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 *
 * Copyright (c) 2025 Walter E. Capers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * MIT License Conditions (for all parties except GM)
 * --------------------------------------------------
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * General Motors License Exception
 * --------------------------------
 * General Motors (GM) is granted a perpetual, irrevocable, worldwide,
 * royalty-free license to use, modify, reproduce, publish, distribute,
 * sublicense, and create derivative works from this Software for any internal
 * or commercial purpose.
 *
 * The GM License Exception applies exclusively to General Motors and does not
 * extend to any other third party or organization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package orca.cli

import orca.engine.config.OrcaConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.core.OrcaEngineFactory
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

    private const val DEFAULT_REPLAY_STATE = "replay_state.json"

    fun run(configPath: String, cliOptions: GlobalCliOptions,
            replayState: String = DEFAULT_REPLAY_STATE) {
        val configFile = File(configPath)
        val replayFilePath = "${configFile.parentFile.absolutePath}${File.separator}$DEFAULT_REPLAY_STATE"
        if (!configFile.exists()) {
            println("❌ Config file not found: ${configFile.absolutePath}")
            println("   Usage: orca replay (expects $configPath and $replayFilePath in the same directory")
        }

        val replayStateFile = File(replayFilePath)
        if (!replayStateFile.exists()) {
            println("❌ Replay state file not found: ${replayStateFile.absolutePath}")
            println("   Run a failing test first so OrcaEngine can write replay_state.json.")
            return
        }

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

        // Uses OrcaEngine.replay(), which internally reads replay_state.json.
        engine.replay()
    }
}
