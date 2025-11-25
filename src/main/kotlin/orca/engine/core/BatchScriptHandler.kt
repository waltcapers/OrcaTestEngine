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

import java.io.File

/**
 * Executes Windows Batch scripts (`.bat` or inline commands) for SCRIPT-type events.
 *
 * This handler is responsible for running scripts using `cmd.exe` on Windows systems.
 * It supports two modes of execution:
 *
 * 1. **Inline script** – uses the `inline` list in {@link ScriptDefinition}
 * 2. **File-based script** – loads content from the path in {@link ScriptDefinition.file}
 *
 * The script is executed using:
 * ```
 * cmd.exe /c <script> <args...>
 * ```
 *
 * Environment variables provided by the OrcaEngine are passed to the subprocess.
 */
class BatchScriptHandler : ScriptHandler {

    /**
     * Executes a batch script through `cmd.exe`.
     *
     * @param script The script definition, containing either inline commands or a file path.
     * @param args A list of argument strings appended to the script invocation.
     * @param env A mapping of environment variables to apply to the executed process.
     *
     * @return A {@link ScriptResult} containing exitCode, stdout, stderr, and metrics.
     *
     * @throws IllegalStateException if neither `inline` nor `file` is provided.
     */
    override fun execute(
        script: orca.engine.model.ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): orca.engine.core.ScriptResult {

        // Load script text from inline definition or external file
        val scriptText =
            script.inline?.joinToString("\n")
                ?: File(requireNotNull(script.file) { "Batch script must define 'inline' or 'file'" })
                    .readText()

        // Build Windows command line
        val cmd = listOf(
            "cmd.exe",
            "/c",
            scriptText + " " + args.joinToString(" ")
        )

        // Execute with process utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}

