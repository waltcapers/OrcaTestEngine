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
 * Script handler for executing PowerShell scripts within the Stress Engine.
 *
 * This handler supports both:
 *
 * - **Inline scripts** — PowerShell code provided directly in the JSON configuration.
 * - **File-based scripts** — A reference to a `.ps1` file on disk.
 *
 * The handler constructs a PowerShell command using:
 *
 * ```
 * powershell -Command "<script> <args>"
 * ```
 *
 * It then delegates process execution to {@link ProcessUtils#runProcess}, which handles
 * launching the external process, collecting stdout/stderr, and returning a {@link ScriptResult}.
 *
 * ## Inline Script Example
 * ```
 * "script": {
 *    "inline": [
 *       "Write-Host 'Hello PowerShell!'",
 *       "Write-Host 'Args:' $args"
 *    ]
 * },
 * "language": "POWERSHELL"
 * ```
 *
 * ## File-based Example
 * ```
 * "script": {
 *    "file": "scripts/test.ps1"
 * },
 * "language": "POWERSHELL"
 * ```
 *
 * ## Responsibilities
 * -------------------
 * - Builds the PowerShell execution command.
 * - Reads inline or file-based script content.
 * - Appends event arguments (`args`) to the PowerShell command.
 * - Passes environmental variables to the executed process.
 * - Returns a strongly typed {@link ScriptResult}.
 */
class PowerShellScriptHandler : ScriptHandler {

    /**
     * Executes a PowerShell script using either inline text or a file-based script.
     *
     * @param script The script definition containing inline PowerShell code
     *               or a file reference.
     * @param args   A list of command-line arguments to append to the script.
     * @param env    A map of environment variables exposed to the PowerShell process.
     *
     * @return A {@link ScriptResult} containing:
     *         - exit code
     *         - standard output
     *         - standard error
     *         - optional metrics
     *
     * @throws IllegalStateException if the script definition does not contain
     *         inline code or a valid file reference.
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        // Construct baseline PowerShell command.
        val cmd = mutableListOf("powershell", "-Command")

        // Resolve script content (inline first, fallback to file)
        val scriptText = script.inline?.joinToString("\n")
            ?: File(script.file
                ?: error("PowerShellScriptHandler: script.file must not be null when inline script is not provided.")
            ).readText()

        // Append arguments after script code
        cmd += scriptText + " " + args.joinToString(" ")

        // Execute and return process result
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
