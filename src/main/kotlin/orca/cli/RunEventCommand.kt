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

import orca.engine.config.StressConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.model.StressEvent
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Implements:
 *
 *   orca run-event <config.json> <eventId> [iterations]
 *
 * Behavior:
 *   - Loads config.
 *   - Finds a single event by ID.
 *   - Constructs a new OrcaTestConfig that includes ONLY that event.
 *   - Runs the engine for the specified number of iterations.
 *
 * Notes:
 *   - This is a “surgical” execution mode that isolates one event.
 *   - It is especially useful for debugging specific scripts or sequences.
 *   - For now we use DefaultSystemInspector, which is safe even without
 *     a real Android device connected. You can later swap in AdbSystemInspector.
 */
object RunEventCommand {

    fun run(configPath: String, eventId: String, iterations: Int) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        val config = try {
            StressConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        val event = config.events.find { it.id == eventId }
        if (event == null) {
            println("❌ Event not found: $eventId")
            return
        }

        println("Running event '$eventId' in isolation for $iterations iteration(s).")

        // Create a narrow config with just this event.
        val singleEventConfig = config.copy(
            events = listOf<StressEvent>(event)
        )

        val logger = ConsoleEngineLogger()
        val inspector = DefaultSystemInspector(debug = true, logger)
        val scriptRunner = ScriptRunnerDispatcher()

        val engine = OrcaEngine(
            config = singleEventConfig,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
        )

        engine.runForIterations(iterations.toLong())
    }
}
