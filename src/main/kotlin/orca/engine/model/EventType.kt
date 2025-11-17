package orca.engine.model

/**
 * Defines the core categories of events that the OrcaEngine can execute.
 *
 * Each event type determines the execution flow:
 * - how the engine processes the event,
 * - whether scripts run,
 * - whether child events are invoked,
 * - or whether the engine waits for a system state change.
 */
enum class EventType {

    /**
     * A script-based event.
     *
     * The engine uses a `ScriptRunner` to execute the associated script
     * using the language specified by `StressEvent.language`.
     *
     * Supports:
     * - inline scripts
     * - external script files
     * - metrics collection
     * - conditional triggers
     */
    SCRIPT,

    /**
     * A no-operation event.
     *
     * Used when testing engine flow or verifying sequencing behavior
     * without performing any actual action.
     *
     * The engine logs the event description and marks it successful
     * with a duration of 0ms.
     */
    NO_OP,

    /**
     * Executes a predefined sequence of other event IDs.
     *
     * The engine resolves each child ID and executes it using full
     * retry / failure / conditional handling logic.
     *
     * The parent SEQUENCE event succeeds only if all children succeed.
     */
    SEQUENCE,

    /**
     * An event that blocks until the Android device is reachable.
     *
     * The engine:
     * - waits for the device to appear over ADB,
     * - optionally waits for BOOT_COMPLETED (if enabled on the event),
     * - then resumes execution.
     *
     * Used for reboot recovery or when scripts intentionally disconnect ADB.
     */
    WAIT_FOR_DEVICE
}
