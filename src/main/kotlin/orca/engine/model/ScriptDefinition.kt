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
 * Defines the source of a script associated with a SCRIPT-type StressEvent.
 *
 * A script may be supplied in one of two forms:
 *
 * 1. **Inline script (`inline`)**
 *    - A list of text lines embedded directly inside the configuration file.
 *    - Useful for small helper scripts, commands, or portable multi-line logic.
 *
 * 2. **External script file (`file`)**
 *    - A filesystem path to a script stored outside the JSON configuration.
 *    - Ideal for larger scripts, shared utilities, or scripts written in
 *      languages requiring specific formatting or indentation.
 *
 * Exactly one of `inline` or `file` must be provided. Supplying neither is
 * considered a configuration error and will result in an exception at load time.
 *
 * The engine uses this definition to provide the script source to the appropriate
 * `ScriptHandler` implementation (SHELL, PYTHON, POWERSHELL, etc.).
 *
 * Example (inline):
 * ```
 * {
 *   "type": "SCRIPT",
 *   "language": "SHELL",
 *   "script": {
 *     "inline": [
 *       "echo Starting test",
 *       "adb shell input keyevent 3"
 *     ]
 *   }
 * }
 * ```
 *
 * Example (file):
 * ```
 * {
 *   "type": "SCRIPT",
 *   "language": "PYTHON",
 *   "script": {
 *     "file": "scripts/test_cpu.py"
 *   }
 * }
 * ```
 *
 * @property inline Optional list of inline script lines.
 * @property file Optional filesystem path to a script file.
 *
 * @throws IllegalArgumentException if both `inline` and `file` are null.
 */
data class ScriptDefinition(
    val inline: List<String>? = null,
    val file: String? = null
) {
    init {
        require(!(inline == null && file == null)) {
            "ScriptDefinition requires either 'inline' or 'file'"
        }
    }
}
