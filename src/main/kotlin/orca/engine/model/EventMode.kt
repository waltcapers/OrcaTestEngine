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
 * Defines how a `StressEvent` participates in execution scheduling.
 *
 * Event mode determines when and how the engine is allowed to select
 * the event for execution. The mode affects random selection, sequence
 * handling, and conditional branching.
 *
 * Modes:
 *
 * - **RANDOM**
 *   Included in the weighted event pool and eligible for `selectNextEvent()`.
 *   Weight, profileWeights, cooldowns, and maxExecutions apply.
 *
 * - **SEQUENTIAL**
 *   Not chosen randomly. Executed *only* when referenced in a SEQUENCE-type
 *   parent event (i.e., scenarios defined in `sequence: []`).
 *   Order is strictly defined by the sequence list.
 *
 * - **CONDITIONAL**
 *   Not selected randomly. Executed only when triggered via
 *   `ConditionalTrigger` rules based on metrics, script output, or exit codes.
 */
enum class EventMode {
    /** Eligible for weighted random selection. */
    RANDOM,

    /** Executed only as part of an explicitly defined event SEQUENCE. */
    SEQUENTIAL,

    /** Executed only when one of its conditional triggers is satisfied. */
    CONDITIONAL
}
