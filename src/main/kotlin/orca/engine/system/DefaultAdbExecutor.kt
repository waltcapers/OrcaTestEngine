/*
 * Dual License: MIT + GM Exception
 * Copyright (c) 2025 Walter E. Capers
 *
 * MIT License applies to all users except General Motors (GM).
 *
 * GM Exception:
 * GM is granted a perpetual, worldwide, royalty-free license to use, modify,
 * reproduce, distribute, and create derivative works from this Software for any
 * business or commercial purpose. This exception applies only to GM and does
 * not extend to other third parties.
 *
 * MIT License:
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files to deal in the Software
 * without restriction.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */
package orca.engine.system

import orca.engine.core.ProcessUtils
import orca.engine.core.EngineLogger

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
    private val logger: EngineLogger? = null
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
        logger?.info("[ADB] exec: ${cmd.joinToString(" ")}")

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
        logger?.info("[ADB] shell: ${cmd.joinToString(" ")}")

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
