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

import orca.engine.core.ProcessUtils
import orca.engine.core.ScriptHandler
import orca.engine.core.ScriptResult
import orca.engine.model.ScriptDefinition
import java.io.File

/**
 * Script handler responsible for executing Ruby scripts as part of the
 * OrcaEngine's script-based event execution pipeline.
 *
 * This handler supports two types of Ruby script definitions:
 *
 * 1. **Inline Ruby scripts**
 *    - Provided directly inside the JSON configuration as a list of strings.
 *    - Converted into a temporary `.rb` file at runtime.
 *
 * 2. **File-based Ruby scripts**
 *    - A path referencing an existing `.rb` file on disk.
 *
 * The handler constructs a Ruby execution command and delegates to
 * [ProcessUtils.runProcess] to launch the external interpreter.
 *
 * ### Example Invocation
 * The generated command will look like:
 *
 * ```
 * ruby /path/to/script.rb arg1 arg2
 * ```
 *
 * Environment variables defined in the event configuration are injected
 * into the process environment before execution.
 *
 * @see ScriptHandler
 * @see ScriptDefinition
 * @see ProcessUtils.runProcess
 */
class RubyScriptHandler : ScriptHandler {

    /**
     * Executes a Ruby script using the system's `ruby` interpreter.
     *
     * @param script the script definition, either inline or file-based
     * @param args arguments passed from the StressEvent configuration
     * @param env environment variables applied to the Ruby process
     *
     * @return a [ScriptResult] containing exit code, stdout, stderr,
     *         and metrics collected during execution
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        val cmd = mutableListOf("ruby")

        // Inline script → write to a temporary .rb file
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".rb")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath

            // File-based script
        } else {
            cmd += script.file!!
        }

        // Append script arguments
        cmd += args

        // Execute via utility and return output
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
