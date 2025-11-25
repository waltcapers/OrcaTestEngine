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

import orca.engine.model.StressEvent

/**
 * Interface defining the execution engine for SCRIPT-type [StressEvent] operations.
 *
 * A [ScriptRunner] is responsible for selecting the appropriate [ScriptHandler]
 * (Shell, Python, Batch, PowerShell, Ruby, Node, or Custom) and invoking it
 * with the event’s provided script, arguments, and environment variables.
 *
 * ### Responsibilities
 * - Determine the correct scripting backend based on `event.language`
 * - Handle inline script text or external script files
 * - Pass arguments and environment variables to the script
 * - Return a complete [ScriptResult] containing output, errors, metrics, and exit status
 *
 * ### When It's Used
 * The [OrcaEngine] delegates all SCRIPT-type event execution to this interface.
 * Non-script events (NO_OP, WAIT_FOR_DEVICE, SEQUENCE) bypass the runner.
 *
 * ### Failure Behavior
 * The returned [ScriptResult] determines whether the engine treats the event as:
 * - Successful (`exitCode == 0`)
 * - Failed (`exitCode != 0`), triggering:
 *   - retry
 *   - stop test
 *   - skip future executions
 *   - or log-only behavior
 * depending on the event’s [FailurePolicy]
 *
 * @see StressEvent
 * @see ScriptHandler
 * @see ScriptResult
 */
interface ScriptRunner {

    /**
     * Executes the script associated with the given [StressEvent] and returns
     * a detailed [ScriptResult] describing the outcome.
     *
     * @param event the event whose script should be executed
     * @return the result of script execution, including exit code, stdout, stderr, and metrics
     */
    fun run(event: StressEvent): ScriptResult
}
