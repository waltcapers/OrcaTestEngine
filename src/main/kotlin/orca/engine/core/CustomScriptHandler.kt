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
import orca.engine.model.ScriptDefinition
import orca.engine.model.ScriptResult
import java.io.File

/**
 * A flexible script handler used for executing arbitrary/custom script types.
 *
 * Unlike language-specific handlers (Shell, Batch, Python, etc.), this handler:
 *
 *  - Accepts *any* script content.
 *  - Stores inline scripts in a temporary file before execution.
 *  - Executes file-based scripts directly.
 *  - Delegates execution to {@link ProcessUtils#runProcess}.
 *
 *
 * Execution behavior:
 * -------------------
 * If the script is INLINE:
 *   1. A temporary file is created.
 *   2. The inline text is written to the file.
 *   3. The file path becomes the command to execute.
 *
 * If the script is FILE-BASED:
 *   1. The script's file path is passed directly into the command list.
 *
 * In both cases:
 *   - Script arguments are appended.
 *   - The provided environment variables are applied to the process.
 */
class CustomScriptHandler : ScriptHandler {

    /**
     * Executes a custom script from either an inline definition or an external file.
     *
     * @param script The script definition, containing inline content or a file path.
     * @param args Command-line arguments to append to the script invocation.
     * @param env A map of environment variables passed to the subprocess.
     *
     * @return A {@link ScriptResult} containing exit code, stdout, stderr, and metrics.
     *
     * @throws IllegalStateException if both `inline` and `file` are missing.
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        val cmd = mutableListOf<String>()

        // Case 1: Inline script — write to a temporary file first
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".txt")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath
        }
        // Case 2: External script file — execute directly
        else {
            cmd += requireNotNull(script.file) {
                "CustomScriptHandler requires either 'inline' or 'file' to be defined"
            }
        }

        // Append arguments
        cmd += args

        // Execute via shared process utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
