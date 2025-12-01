package orca.engine.model

/**
 * Interface defining the execution engine for SCRIPT-type [StressEvent] operations.
 *
 * A [ScriptRunner] is responsible for selecting the appropriate [orca.engine.core.ScriptHandler]
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
 * The [orca.engine.core.OrcaEngine] delegates all SCRIPT-type event execution to this interface.
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
 * @see orca.engine.core.ScriptHandler
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