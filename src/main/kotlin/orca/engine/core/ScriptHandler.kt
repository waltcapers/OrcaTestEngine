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

/**
 * Base interface for all script execution handlers used by the OrcaEngine.
 *
 * The framework includes multiple implementations of this interface—
 * such as Shell, Batch, Python, PowerShell, Ruby, Node.js, and Custom handlers—
 * each responsible for invoking scripts written in their respective languages.
 *
 * Script handlers are selected dynamically at runtime based on the
 * [ScriptLanguage] declared in a [StressEvent].
 *
 * ### Responsibilities of a ScriptHandler
 * - Receive the parsed [ScriptDefinition] from the event.
 * - Build an execution command appropriate for the script type.
 * - Execute the script via [ProcessUtils.runProcess].
 * - Return a [ScriptResult] containing:
 *   - exit code
 *   - stdout/stderr output
 *   - any collected execution metrics
 *
 * Implementations are expected to:
 * - support both inline script blocks and file-based scripts,
 * - apply event-specified environment variables,
 * - support argument forwarding.
 *
 * @see StressEvent
 * @see ScriptDefinition
 * @see ScriptResult
 * @see ProcessUtils.runProcess
 */
interface ScriptHandler {

    /**
     * Executes a script using the appropriate runtime interpreter or mechanism.
     *
     * @param script the script definition containing either an inline script block
     *               or a reference to a file on disk
     * @param args a list of command-line arguments to pass to the script
     * @param env a map of environment variables that should be injected into the process
     *
     * @return the result of script execution, including exit code, stdout, stderr,
     *         and any metrics captured by the underlying process runner
     */
    fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult
}
