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
 * Supported scripting languages for SCRIPT-type events.
 *
 * A StressEvent marked with `EventType.SCRIPT` must specify a language that
 * determines which {@link orca.engine.core.ScriptHandler} implementation the
 * engine will use. Each enum value maps directly to a handler in
 * `DefaultScriptRunner`.
 *
 * The scripting language affects:
 * - How the script is executed (shell, interpreter, OS command processor, etc.)
 * - How inline scripts are written
 * - Which runtime must be installed on the host machine
 *
 * ### Language descriptions
 *
 * - **SHELL**
 *   Runs the script using `/bin/sh -c` on Unix-like systems.
 *   Suitable for Bash-like commands, adb invocations, and general automation.
 *
 * - **BATCH**
 *   Executes the script using Windows `cmd.exe /c`.
 *   Used on Windows hosts for `.bat` or inline batch commands.
 *
 * - **PYTHON**
 *   Runs the script using `python3`.
 *   Inline scripts are written to a temporary `.py` file.
 *
 * - **POWERSHELL**
 *   Executes via `powershell -Command`.
 *   Ideal for modern Windows CLI automation.
 *
 * - **RUBY**
 *   Executes Ruby scripts using `ruby`.
 *   Mainly for developers preferring Ruby-based tooling.
 *
 * - **NODE**
 *   Runs the script using the Node.js interpreter (`node`).
 *   Supports JavaScript automation or Node-based test utilities.
 *
 * - **CUSTOM**
 *   Generic extension point intended for user-defined interpreters.
 *   Mapped to `CustomScriptHandler` by default, allowing arbitrary command execution.
 */
enum class ScriptLanguage {
    SHELL,
    BATCH,
    PYTHON,
    POWERSHELL,
    RUBY,
    NODE,
    CUSTOM        // Extension point for user-defined runners
}
