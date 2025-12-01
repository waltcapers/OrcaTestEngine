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

package orca.engine.core

import orca.engine.model.ScriptResult
import orca.engine.model.ScriptRunner
import orca.engine.model.StressEvent

/**
 * Fake [ScriptRunner] used only in unit tests.
 *
 * Capabilities:
 *  - Records execution order (executedEvents)
 *  - Counts per-event invocations
 *  - Allows per-event custom behavior lambdas
 *  - Stores the last ScriptResult for each event
 *  - Provides lastResult(eventId) for assertions
 */
class FakeScriptRunner : ScriptRunner {

    /** Execution order of events. */
    val executedEvents = mutableListOf<String>()

    /** Per-event invocation counters. */
    private val invocationCount = mutableMapOf<String, Int>()

    /** Per-event behavior handlers. */
    private val behavior = mutableMapOf<String, (Int) -> ScriptResult>()

    /** Stores the last result for each event ID. */
    private val lastResults = mutableMapOf<String, ScriptResult>()

    val lastDurations = mutableMapOf<String, Long>()


    /**
     * Assign custom behavior for an event.
     *
     * @param eventId ID of the event
     * @param fn A lambda taking the invocation index (1-based) and returning ScriptResult
     */
    fun setBehavior(eventId: String, fn: (Int) -> ScriptResult) {
        behavior[eventId] = fn
    }

    /**
     * Returns the number of times a given event has been executed.
     */
    fun getInvocationCount(eventId: String): Int =
        invocationCount[eventId] ?: 0

    /**
     * Returns the last ScriptResult produced for eventId.
     * Used heavily in metrics tests.
     */
    fun lastResult(eventId: String): ScriptResult? =
        lastResults[eventId]

    override fun run(event: StressEvent): ScriptResult {
        executedEvents += event.id

        val count = (invocationCount[event.id] ?: 0) + 1
        invocationCount[event.id] = count

        val fn = behavior[event.id]

        val start = System.currentTimeMillis()
        val result = fn?.invoke(count) ?: ScriptResult(
            exitCode = 0,
            stdout = "default success for ${event.id}",
            stderr = "",
            metrics = emptyMap()
        )
        val end = System.currentTimeMillis()

        // Store last execution duration for tests that inspect child timing
        lastDurations[event.id] = (end - start)

        lastResults[event.id] = result
        return result
    }


    /**
     * Default ScriptResult for events without custom behavior:
     *   exitCode = 0 (success)
     *   stdout = "default success for <id>"
     *   stderr = ""
     *   metrics = emptyMap()
     */
    private fun defaultSuccess(id: String): ScriptResult =
        ScriptResult(
            exitCode = 0,
            stdout = "default success for $id",
            stderr = "",
            metrics = emptyMap()
        )
}
