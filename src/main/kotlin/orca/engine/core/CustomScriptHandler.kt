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
import orca.engine.model.ScriptDefinition
import java.io.File

/**
 * A flexible script handler used for executing arbitrary/custom script types.
 *
 * Unlike language-specific handlers (Shell, Batch, Python, etc.), this handler:
 *
 *  - Accepts *any* script content.
 *  - Stores inline scripts in a temporary file before execution.
 *  - Executes file-based scripts directly.
 *  - Delegates execution to {@link ProcessUtils#runProcess}.
 *
 *
 * Execution behavior:
 * -------------------
 * If the script is INLINE:
 *   1. A temporary file is created.
 *   2. The inline text is written to the file.
 *   3. The file path becomes the command to execute.
 *
 * If the script is FILE-BASED:
 *   1. The script's file path is passed directly into the command list.
 *
 * In both cases:
 *   - Script arguments are appended.
 *   - The provided environment variables are applied to the process.
 */
class CustomScriptHandler : ScriptHandler {

    /**
     * Executes a custom script from either an inline definition or an external file.
     *
     * @param script The script definition, containing inline content or a file path.
     * @param args Command-line arguments to append to the script invocation.
     * @param env A map of environment variables passed to the subprocess.
     *
     * @return A {@link ScriptResult} containing exit code, stdout, stderr, and metrics.
     *
     * @throws IllegalStateException if both `inline` and `file` are missing.
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        val cmd = mutableListOf<String>()

        // Case 1: Inline script — write to a temporary file first
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".txt")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath
        }
        // Case 2: External script file — execute directly
        else {
            cmd += requireNotNull(script.file) {
                "CustomScriptHandler requires either 'inline' or 'file' to be defined"
            }
        }

        // Append arguments
        cmd += args

        // Execute via shared process utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
