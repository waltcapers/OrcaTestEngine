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

package orca.engine.model

/**
 * Holds runtime execution statistics for a single `StressEvent`.
 *
 * These values are updated by the engine each time the event executes,
 * and they are used for:
 * - cooldown handling (`lastExecutionTimeMillis`)
 * - performance analysis (duration fields)
 * - event disabling after failures (`disabled`)
 * - summary reporting at the end of the run
 *
 * All fields are mutable because stats evolve throughout the test.
 */
data class EventStats(

    /**
     * Timestamp (in millis, from `System.currentTimeMillis()`) of the most recent
     * successful execution. Used for evaluating `cooldownSeconds`.
     */
    var lastExecutionTimeMillis: Long? = null,

    /**
     * Duration (in milliseconds) of the most recent execution.
     */
    var lastDurationMs: Long? = null,

    /**
     * Total number of times the event has successfully executed.
     */
    var executions: Int = 0,

    /**
     * number of successful runs
     */
    var successes: Int = 0,

    /**
     * number of failed runs
     */
    var failures:Int = 0,

    /**
     * When true, the engine will skip this event entirely.
     * Primarily used by `FailurePolicy.SKIP_FUTURE`.
     */
    var disabled: Boolean = false,

    /**
     * Sum of all recorded durations. Used to compute average execution time.
     */
    var totalDuration: Long = 0,

    /**
     * Minimum observed duration across all executions.
     * Initialized to Long.MAX_VALUE to ensure proper comparison on first update.
     */
    var minDuration: Long = Long.MAX_VALUE,

    /**
     * Maximum observed duration across all executions.
     * Initialized to Long.MIN_VALUE to ensure proper comparison on first update.
     */
    var maxDuration: Long = Long.MIN_VALUE
) {

    /**
     * Average duration (in ms) across all successful executions.
     * Returns 0 if the event has never executed.
     */
    val averageDuration: Long
        get() = if (executions > 0) totalDuration / executions else 0
}
