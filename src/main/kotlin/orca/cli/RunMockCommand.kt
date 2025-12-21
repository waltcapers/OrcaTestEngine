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
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Implements:
 *
 *   orca run-mock <config.json>
 *
 * Behavior:
 *   - Loads config.
 *   - Uses DefaultSystemInspector (no ADB calls).
 *   - Runs the engine either normally or in step mode (--step).
 */
object RunMockCommand {

    fun run(configPath: String, cliOptions: GlobalCliOptions) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        val config = try {
            OrcaConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        if( cliOptions.seedOverride != null){
            config.randomSeed = cliOptions.seedOverride
        }
        val engine = OrcaEngineFactory.newEngine(
            targetPackage = config.targetPackage,
            mockMode = true,
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

        // --------------------------------------------------------------------
        //  NORMAL MOCK BEHAVIOR (no logcat, no adb)
        // --------------------------------------------------------------------
        if (config.maxTestDurationSeconds != null) {
            engine.runForDuration(config.maxTestDurationSeconds.toLong())
        } else {
            engine.runLoop()
        }
    }
}
