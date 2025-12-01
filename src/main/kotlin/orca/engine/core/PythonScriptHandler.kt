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
 * ScriptHandler implementation for executing Python scripts using the `python3`
 * interpreter. This handler supports both:
 *
 *  - **Inline scripts**: Provided directly inside the JSON configuration.
 *  - **File-based scripts**: Referencing an existing `.py` file on disk.
 *
 * Inline scripts are written to a temporary file before execution, ensuring
 * consistent and isolated behavior. Arguments and environment variables are
 * passed through to the Python interpreter.
 *
 * This class delegates process execution to {@link ProcessUtils}, which handles
 * timeout behavior, output capture, and process termination.
 *
 * ## Example JSON Usage
 * ```json
 * {
 *   "id": "py_test",
 *   "type": "SCRIPT",
 *   "language": "PYTHON",
 *   "script": {
 *     "inline": [
 *       "import sys",
 *       "print('Hello from Python')"
 *     ]
 *   }
 * }
 * ```
 *
 * ## Execution Model
 * The executed command structure is:
 * ```
 * python3 <scriptfile> <args...>
 * ```
 *
 * @see ScriptHandler
 * @see ScriptDefinition
 * @see ProcessUtils
 */
class PythonScriptHandler : ScriptHandler {

    /**
     * Executes a Python script using the system `python3` interpreter.
     *
     * @param script The script definition, either inline or referencing an external file.
     * @param args   A list of arguments to pass to the Python script.
     * @param env    A map of environment variables to expose to the interpreter.
     *
     * @return A {@link ScriptResult} containing exit code, stdout, and stderr output.
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        val cmd = mutableListOf("python3")

        // Inline script support → write to temporary .py file
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".py")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath
        } else {
            // Script references an external file path
            cmd += script.file!!
        }

        // Append additional script arguments
        cmd += args

        // Execute using shared process utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
