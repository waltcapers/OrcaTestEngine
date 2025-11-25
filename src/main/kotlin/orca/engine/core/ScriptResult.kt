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

import java.util.Collections.emptyMap

/**
 * Represents the outcome of executing a script inside the Stress Engine.
 *
 * A [ScriptResult] is produced by all implementations of [ScriptHandler] and
 * passed back to the executing [StressEvent] for evaluation. It contains:
 *
 * - **exitCode** — the process exit status (0 = success; non-zero = failure)
 * - **stdout** — standard output text captured from the script
 * - **stderr** — standard error text captured from the script
 * - **metrics** — optional numeric values collected by the script, if any
 *
 * ### Metrics Use Case
 * Metrics allow scripts to push custom performance or diagnostic values
 * back into the engine—for example:
 *
 * ```json
 * { "cpuTimeMs": 42.7, "memoryKB": 1536 }
 * ```
 *
 * These metrics are aggregated globally by [OrcaEngine] and can be used as:
 * - pass/fail conditions in **conditional triggers**
 * - data for summary reports
 * - fine-grained performance monitoring
 *
 * ### Error Handling Behavior
 * If a script times out, is missing, or cannot be executed, the exit code
 * should be non-zero and `stderr` should describe the reason.
 *
 * @property exitCode   the exit status of the process (0 = success, non-zero = failure)
 * @property stdout     captured standard output text (may be empty)
 * @property stderr     captured standard error text (may be empty)
 * @property metrics    optional map of numeric metrics reported by the script;
 *                      defaults to an empty immutable map
 *
 * @see ScriptHandler
 * @see OrcaEngine
 */
data class ScriptResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val metrics: Map<String, Double> = emptyMap()
)
