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
package orca.engine.core

import java.io.File

/**
 * Executes Windows Batch scripts (`.bat` or inline commands) for SCRIPT-type events.
 *
 * This handler is responsible for running scripts using `cmd.exe` on Windows systems.
 * It supports two modes of execution:
 *
 * 1. **Inline script** – uses the `inline` list in {@link ScriptDefinition}
 * 2. **File-based script** – loads content from the path in {@link ScriptDefinition.file}
 *
 * The script is executed using:
 * ```
 * cmd.exe /c <script> <args...>
 * ```
 *
 * Environment variables provided by the OrcaEngine are passed to the subprocess.
 */
class BatchScriptHandler : ScriptHandler {

    /**
     * Executes a batch script through `cmd.exe`.
     *
     * @param script The script definition, containing either inline commands or a file path.
     * @param args A list of argument strings appended to the script invocation.
     * @param env A mapping of environment variables to apply to the executed process.
     *
     * @return A {@link ScriptResult} containing exitCode, stdout, stderr, and metrics.
     *
     * @throws IllegalStateException if neither `inline` nor `file` is provided.
     */
    override fun execute(
        script: orca.engine.model.ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): orca.engine.core.ScriptResult {

        // Load script text from inline definition or external file
        val scriptText =
            script.inline?.joinToString("\n")
                ?: File(requireNotNull(script.file) { "Batch script must define 'inline' or 'file'" })
                    .readText()

        // Build Windows command line
        val cmd = listOf(
            "cmd.exe",
            "/c",
            scriptText + " " + args.joinToString(" ")
        )

        // Execute with process utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}

