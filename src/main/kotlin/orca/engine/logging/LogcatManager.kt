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

/**
 * Defines the contract for managing logcat capture during a stress-test run.
 *
 * Implementations of this interface handle:
 * - Starting a persistent `adb logcat` process and routing its output somewhere (usually a file).
 * - Stopping the active capture process gracefully.
 * - Rotating logs by restarting capture under a new file or context.
 *
 * This interface is intentionally minimal so that multiple logging backends
 * (file capture, streaming, filtering, remote upload, etc.) can be plugged in
 * without modifying the engine.
 */
interface LogcatManager {

    /**
     * Starts capturing logcat output.
     *
     * @param tag
     *   Optional log tag or substring to filter output. Implementations may
     *   perform filtering via shell pipelines (e.g., `adb logcat | grep`) or
     *   through other mechanisms.
     *
     * If a capture session is already running, implementations should stop
     * the existing capture before starting a new one.
     */
    fun startCapture(tag: String? = null)

    /**
     * Stops the currently active logcat capture, if any.
     *
     * Implementations should ensure the underlying process is terminated and
     * resources such as file handles are released.
     */
    fun stopCapture()

    /**
     * Rotates the log output by stopping the current capture and starting a new one.
     *
     * @param tag
     *   Optional tag to apply to the new capture session.
     *
     * This is typically used when:
     * - Reboots occur
     * - Log segments need to be separated by phase
     * - Continuous runs require periodic log rollover
     */
    fun rotate(tag: String? = null)
}
