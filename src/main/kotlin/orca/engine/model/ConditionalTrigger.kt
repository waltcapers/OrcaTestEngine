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
 * Represents a conditional rule that can trigger another event based on the
 * result of executing a SCRIPT event.
 *
 * Conditional triggers allow reactive behavior during a stress test. For example:
 * - Restart an app if output contains an error keyword.
 * - Trigger cleanup actions if CPU usage spikes above a threshold.
 * - Trigger a recovery event when a script exits with an error code.
 *
 * Each conditional field is optional. During evaluation, the engine checks
 * only the fields that are non-null.
 *
 * All enabled conditions are evaluated logically ANDed together.
 * If all conditions that are present evaluate to true, the corresponding
 * `triggerEventId` is fired.
 *
 * @property triggerEventId
 *   ID of the event that should be triggered when the conditions match.
 *
 * @property ifOutputContains
 *   If non-null, the trigger fires only when the script's STDOUT or STDERR
 *   contains this substring.
 *
 * @property ifExitCodeNotZero
 *   If true, the trigger fires only when the script returns a non-zero exit code.
 *
 * @property ifExitCodeEquals
 *   Exact exit code required for the trigger to fire.
 *
 * @property ifMetricAbove
 *   Map of metric names to numeric thresholds.
 *   All referenced metrics must be **strictly greater** than their threshold
 *   for the trigger to activate.
 *   Example: `{"cpuUsage": 0.85}` meaning fire only if CPU usage exceeds 85%.
 *
 * @property ifMetricBelow
 *   Map of metric names to numeric thresholds.
 *   All referenced metrics must be **strictly less** than their threshold
 *   for the trigger to activate.
 */
data class ConditionalTrigger(
    val triggerEventId: String,
    val ifOutputContains: String? = null,
    val ifExitCodeNotZero: Boolean? = null,
    val ifExitCodeEquals: Int? = null,
    val ifMetricAbove: Map<String, Double>? = null,
    val ifMetricBelow: Map<String, Double>? = null
)
