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

import orca.engine.model.StressEvent

/**
 * Interface defining the execution engine for SCRIPT-type [StressEvent] operations.
 *
 * A [ScriptRunner] is responsible for selecting the appropriate [ScriptHandler]
 * (Shell, Python, Batch, PowerShell, Ruby, Node, or Custom) and invoking it
 * with the event’s provided script, arguments, and environment variables.
 *
 * ### Responsibilities
 * - Determine the correct scripting backend based on `event.language`
 * - Handle inline script text or external script files
 * - Pass arguments and environment variables to the script
 * - Return a complete [ScriptResult] containing output, errors, metrics, and exit status
 *
 * ### When It's Used
 * The [OrcaEngine] delegates all SCRIPT-type event execution to this interface.
 * Non-script events (NO_OP, WAIT_FOR_DEVICE, SEQUENCE) bypass the runner.
 *
 * ### Failure Behavior
 * The returned [ScriptResult] determines whether the engine treats the event as:
 * - Successful (`exitCode == 0`)
 * - Failed (`exitCode != 0`), triggering:
 *   - retry
 *   - stop test
 *   - skip future executions
 *   - or log-only behavior
 * depending on the event’s [FailurePolicy]
 *
 * @see StressEvent
 * @see ScriptHandler
 * @see ScriptResult
 */
interface ScriptRunner {

    /**
     * Executes the script associated with the given [StressEvent] and returns
     * a detailed [ScriptResult] describing the outcome.
     *
     * @param event the event whose script should be executed
     * @return the result of script execution, including exit code, stdout, stderr, and metrics
     */
    fun run(event: StressEvent): ScriptResult
}
