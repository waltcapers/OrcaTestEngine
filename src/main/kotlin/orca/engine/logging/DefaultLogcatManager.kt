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

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Default implementation of [LogcatManager] that captures logcat output
 * to timestamped files in a local `logs/` directory.
 *
 * ### Responsibilities:
 * - Starts and stops a persistent background `adb logcat` process.
 * - Optionally filters log output using a tag (via grep).
 * - Rotates log files by stopping and restarting capture.
 *
 * ### Notes:
 * - Log output is written to files named `logcat_<timestamp>.txt`.
 * - All captured logs are stored under a `logs/` directory created automatically.
 * - Pipes (`|`) and other shell features cannot be used directly with
 *   [ProcessBuilder], so tag filtering is executed through a shell wrapper.
 *
 * ### Lifecycle:
 * - Only one capture process is active at a time. Calling [startCapture]
 *   automatically stops any previous capture.
 * - [rotate] simply calls stop → start, creating a fresh log file.
 */
class DefaultLogcatManager : LogcatManager {

    /** The active logcat process, if one is currently running. */
    private var process: Process? = null

    /** The file currently being written to, if capture is active. */
    private var currentFile: File? = null

    /**
     * Starts capturing `adb logcat` output to a timestamped file.
     *
     * If a tag is given, the capture is filtered using:
     * ```
     * adb logcat | grep <tag>
     * ```
     * executed through a shell (`sh -c`) because ProcessBuilder does not
     * interpret pipes or redirects natively.
     *
     * @param tag optional logcat tag or substring to filter output.
     */
    override fun startCapture(tag: String?) {
        // Stop any previous capture.
        stopCapture()

        // Create directory and timestamped output file.
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val logsDir = File("logs")
        logsDir.mkdirs()

        currentFile = File(logsDir, "logcat_$timestamp.txt")

        // Construct command through a shell so grep/pipes work as intended.
        val cmd = if (tag != null) {
            listOf("sh", "-c", "adb logcat | grep '$tag'")
        } else {
            listOf("sh", "-c", "adb logcat")
        }

        // Start the process and redirect output to file.
        process = ProcessBuilder(cmd)
            .redirectOutput(currentFile)
            .redirectErrorStream(true)
            .start()
    }

    /**
     * Stops the currently running logcat capture, if any.
     *
     * This destroys the underlying process and clears the reference.
     */
    override fun stopCapture() {
        process?.destroy()
        process = null
    }

    /**
     * Performs a log rotation:
     * - Stops the current capture
     * - Immediately starts a new capture
     *
     * @param tag optional tag to apply to the new capture.
     */
    override fun rotate(tag: String?) {
        stopCapture()
        startCapture(tag)
    }
}
