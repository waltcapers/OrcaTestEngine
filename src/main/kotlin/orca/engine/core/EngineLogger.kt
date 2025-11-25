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

/**
 * Logging abstraction used throughout the OrcaEngine.
 *
 * This interface provides a minimal logging contract that allows the engine
 * to emit informational messages, warnings, and errors without depending on a
 * specific logging framework (e.g., Log4j, SLF4J, Android Logcat).
 *
 * Rationale:
 * ----------
 *  - Keeps the core engine independent from underlying logging technology.
 *  - Allows different front-ends (console, logcat, file logging, UI, etc.)
 *    to provide their own implementations.
 *  - Simplifies testing by allowing test doubles or no-op logger implementations.
 *
 * Typical usage:
 *  --------------
 *  val logger: EngineLogger = ConsoleLogger()
 *  logger.info("Test started")
 *  logger.warn("Slow operation detected")
 *  logger.error("Script failed", exception)
 */
interface EngineLogger {

    /**
     * Logs an informational message.
     *
     * Use this method for:
     *  - Normal runtime flow messages
     *  - Script execution output (stdout)
     *  - Status updates or progress tracking
     *
     * @param message The message to log.
     */
    fun info(message: String)

    /**
     * Logs a warning message indicating a non-critical issue.
     *
     * Use this method for:
     *  - Non-fatal script failures
     *  - Slow event detection
     *  - Deprecated or unexpected behavior that does not stop execution
     *
     * @param message The message describing the warning condition.
     */
    fun warn(message: String)

    /**
     * Logs an error message with an optional exception.
     *
     * Use this method for:
     *  - Critical failures
     *  - Unexpected exceptions
     *  - Script errors that require attention
     *
     * @param message A description of the error.
     * @param t Optional throwable representing the underlying cause.
     */
    fun error(message: String, t: Throwable? = null)
}
