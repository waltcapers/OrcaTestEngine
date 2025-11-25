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
import java.util.Collections.emptyMap
import java.util.concurrent.TimeUnit

/**
 * Utility object that provides a standardized way to run external processes
 * for the Stress Engine. All script handlers (shell, batch, PowerShell,
 * Python, Ruby, Node, etc.) depend on this helper.
 *
 * This class wraps Java's {@link ProcessBuilder} to:
 *
 * - Launch external processes.
 * - Pass environment variables to them.
 * - Optionally set a working directory.
 * - Capture and return both `stdout` and `stderr`.
 * - Enforce a timeout and forcibly terminate processes that exceed it.
 *
 * The result of every execution is returned as a {@link ScriptResult}, which
 * includes the exit code and captured output streams.
 *
 * ## Example Usage
 * ```
 * val result = ProcessUtils.runProcess(
 *     command = listOf("python3", "script.py", "--flag"),
 *     env = mapOf("DEBUG" to "1"),
 *     timeoutSeconds = 30
 * )
 *
 * if (result.exitCode == 0) {
 *     println(result.stdout)
 * } else {
 *     println("Error: ${result.stderr}")
 * }
 * ```
 */
object ProcessUtils {

    /**
     * Executes an external process and returns its output wrapped in a
     * {@link ScriptResult}.
     *
     * @param command A list representing the command to execute where the
     *                first element is the executable and the remainder are
     *                arguments.
     * @param workingDir Optional working directory for the process. If null,
     *                   the current JVM working directory is used.
     * @param env A map of environment variables to apply to the process.
     *           These override inherited environment values.
     * @param timeoutSeconds Maximum time to allow the process to run before
     *                       forcibly terminating it.
     *
     * @return A {@link ScriptResult} containing:
     *         - exitCode — the process exit code, or –1 on timeout
     *         - stdout — text captured from standard output
     *         - stderr — text captured from standard error
     *
     * @see ScriptResult
     */
    fun runProcess(
        command: List<String>,
        workingDir: File? = null,
        env: Map<String, String> = emptyMap(),
        timeoutSeconds: Long = 60L
    ): ScriptResult {

        val builder = ProcessBuilder(command)

        // Optional working directory
        if (workingDir != null) {
            builder.directory(workingDir)
        }

        // Apply environment variables
        val processEnv = builder.environment()
        for ((k, v) in env) {
            processEnv[k] = v
        }

        // Do not merge stdout/stderr; we want separate streams
        builder.redirectErrorStream(false)

        val process = builder.start()

        // Wait for completion with timeout enforcement
        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

        if (!finished) {
            // If timeout hit → kill process and return an error result
            process.destroyForcibly()
            return ScriptResult(
                exitCode = -1,
                stdout = "",
                stderr = "Process timeout after ${timeoutSeconds}s"
            )
        }

        // Capture output streams
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()

        return ScriptResult(
            exitCode = process.exitValue(),
            stdout = stdout,
            stderr = stderr
        )
    }
}
