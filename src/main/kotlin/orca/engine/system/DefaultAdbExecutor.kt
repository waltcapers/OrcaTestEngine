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

package orca.engine.system

import orca.engine.core.ProcessUtils
import orca.engine.core.EngineLogger
import orca.engine.logging.LoggerProvider

/**
 * Real implementation of [AdbExecutor] that executes actual adb commands.
 *
 * @property adbPath       path to the adb binary (default = "adb")
 * @property deviceSerial  optional device serial; if set, all commands include "-s <serial>"
 * @property logger        engine logger so that ADB traffic appears in logs
 */
class DefaultAdbExecutor(
    private val adbPath: String = "adb",
    private val deviceSerial: String? = null,
) : AdbExecutor {

    /**
     * Builds a base ADB command including:
     *  adb [-s SERIAL]
     */
    private fun base(): MutableList<String> {
        val cmd = mutableListOf(adbPath)
        if (!deviceSerial.isNullOrBlank()) {
            cmd += listOf("-s", deviceSerial)
        }
        return cmd
    }

    /**
     * Executes:
     *   adb <args...>
     */
    override fun exec(args: List<String>, timeoutSeconds: Long): AdbResult {
        val cmd = base() + args
        LoggerProvider.get().info("[ADB] exec: ${cmd.joinToString(" ")}")

        val result = ProcessUtils.runProcess(
            command = cmd,
            timeoutSeconds = timeoutSeconds
        )

        return AdbResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr
        )
    }

    /**
     * Executes:
     *   adb shell "<command>"
     *
     * This uses:
     *   adb shell <command split by whitespace>
     * because Android requires args separated.
     */
    override fun shell(command: String, timeoutSeconds: Long): AdbResult {
        val cmd = base() + listOf("shell") + command.split(" ")
        LoggerProvider.get().info("[ADB] shell: ${cmd.joinToString(" ")}")

        val result = ProcessUtils.runProcess(
            command = cmd,
            timeoutSeconds = timeoutSeconds
        )

        return AdbResult(
            exitCode = result.exitCode,
            stdout = result.stdout,
            stderr = result.stderr
        )
    }
}
