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

import orca.engine.core.EngineLogger

/**
 * Mock implementation of [AdbExecutor] for dry-run / demo scenarios.
 *
 * - Never calls the real `adb` binary.
 * - Logs every "command" that would have been executed.
 * - Returns a synthetic [AdbResult] with exitCode = 0 by default.
 *
 * This is useful for:
 *   - Unit tests
 *   - Classroom / demo environments without adb installed
 *   - Deterministic "no device required" walkthroughs
 */
class MockAdbExecutor(
    private val logger: EngineLogger? = null,
    /**
     * If true, stdout will contain a short echo of the simulated command,
     * which can be helpful in demos or when asserting in tests.
     */
    private val echoCommandsToStdout: Boolean = true
) : AdbExecutor {

    /**
     * Captured history of mock calls, useful for tests.
     */
    val calls: MutableList<MockCall> = mutableListOf()

    data class MockCall(
        val kind: Kind,
        val argsOrCommand: List<String>,
        val timeoutSeconds: Long
    ) {
        enum class Kind { EXEC, SHELL }
    }

    override fun exec(args: List<String>, timeoutSeconds: Long): AdbResult {
        val human = "adb ${args.joinToString(" ")} (timeout=${timeoutSeconds}s)"
        log("[MOCK-ADB] exec: $human")

        calls += MockCall(
            kind = MockCall.Kind.EXEC,
            argsOrCommand = args.toList(),
            timeoutSeconds = timeoutSeconds
        )

        val stdout = if (echoCommandsToStdout) "MOCK_EXEC: $human" else ""
        return AdbResult(
            exitCode = 0,
            stdout = stdout,
            stderr = ""
        )
    }

    override fun shell(command: String, timeoutSeconds: Long): AdbResult {
        val human = "adb shell $command (timeout=${timeoutSeconds}s)"
        log("[MOCK-ADB] shell: $human")

        calls += MockCall(
            kind = MockCall.Kind.SHELL,
            argsOrCommand = listOf(command),
            timeoutSeconds = timeoutSeconds
        )

        val stdout = if (echoCommandsToStdout) "MOCK_SHELL: $human" else ""
        return AdbResult(
            exitCode = 0,
            stdout = stdout,
            stderr = ""
        )
    }

    private fun log(msg: String) {
        logger?.info(msg) ?: println(msg)
    }
}
