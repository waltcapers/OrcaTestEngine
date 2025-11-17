package orca.engine.model

/**
 * Captures a single failure that occurred during the execution of a StressEvent.
 *
 * This record is produced whenever an event's script or action returns a
 * non-zero exit code. The OrcaEngine logs a list of these records and
 * displays them in the summary at the end of a run.
 *
 * @property eventId  The ID of the event that failed.
 * @property exitCode The exit code returned by the script or command.
 * @property stderr   The standard error output produced by the event.
 */
data class FailureRecord(
    val eventId: String,
    val exitCode: Int,
    val stderr: String
)
