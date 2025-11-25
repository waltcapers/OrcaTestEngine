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

package orca.engine.logging

import orca.engine.core.EngineLogger

/**
 * Simple stdout-based implementation of [EngineLogger].
 *
 * This logger is intended for local development, debugging, or console-based
 * execution of the OrcaEngine. All log messages are written directly to
 * standard output using `println()`.
 *
 * Log levels are formatted with a prefix for easy visual scanning:
 * - `[INFO]` for normal operational messages
 * - `[WARN]` for recoverable or unexpected conditions
 * - `[ERROR]` for failures, optionally including a stack trace
 *
 * This implementation is deliberately lightweight and has no external
 * dependencies. For production environments, CI systems, or structured
 * logging, another implementation (e.g., file-based or JSON-based) may be
 * provided by the user.
 */
class ConsoleEngineLogger : EngineLogger {

    /**
     * Prints an informational message to stdout.
     *
     * @param message the message to log.
     */
    override fun info(message: String) {
        println("[INFO] $message")
    }

    /**
     * Prints a warning message to stdout.
     *
     * @param message the message to log.
     */
    override fun warn(message: String) {
        println("[WARN] $message")
    }

    /**
     * Prints an error message to stdout. If a [Throwable] is provided,
     * its stack trace is converted to a string and appended.
     *
     * @param message the error message to log.
     * @param t optional exception associated with the error.
     */
    override fun error(message: String, t: Throwable?) {
        if (t != null) {
            println("[ERROR] $message\n${t.stackTraceToString()}")
        } else {
            println("[ERROR] $message")
        }
    }
}
