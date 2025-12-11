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

import orca.cli.OrcaCLI
import orca.engine.config.OrcaConfigLoader
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
        OrcaConfigLoader.load("orca-config.json")
    } catch (t: Throwable) {
        println("\n❌ CONFIG VALIDATION FAILED\n${t.message}")
        return
    }
    // ADB-backed inspector
    val inspector = if (useAdb) {
        AdbSystemInspector(
            adb = DefaultAdbExecutor(
                adbPath = "adb",
                deviceSerial = null
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
