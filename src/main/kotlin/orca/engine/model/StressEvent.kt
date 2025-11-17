package orca.engine.model

/**
 * Represents a single executable unit within a stress test.
 *
 * A `StressEvent` defines:
 *  - what action should occur (SCRIPT, SEQUENCE, NO_OP, WAIT_FOR_DEVICE)
 *  - how often it is eligible for selection (weight, cooldown, maxExecutions)
 *  - how it behaves under failures (retryPolicy, onFailure)
 *  - when it is allowed to run (preconditions, requireState)
 *  - how it affects engine state (setState)
 *  - reboot behavior (causesReboot, waitForBoot, restartAppAfterBoot)
 *  - observability (metrics config, stdout/stderr logging)
 *
 * Stress events are consumed by `OrcaEngine`, which evaluates all
 * fields to decide when and how the event should run.
 *
 * The model is designed to be JSON-serializable for easy external configuration.
 */
data class StressEvent(

    /** Unique logical ID for this event. Must be unique across all events in the config. */
    val id: String,

    /** Human-readable description of the purpose of this event. */
    val description: String? = null,

    /** Defines the high-level behavior: SCRIPT, NO_OP, SEQUENCE, WAIT_FOR_DEVICE, etc. */
    val type: EventType = EventType.SCRIPT,

    /** Determines how/when the event is selected (RANDOM, SEQUENTIAL, CONDITIONAL). */
    val mode: EventMode = EventMode.RANDOM,

    /** Optional threshold for logging warnings when execution exceeds this duration in milliseconds. */
    val slowThresholdMillis: Long? = null,

    /**
     * If false, the engine will consider unexpected process death
     * (after running this event) as a failure and trigger replay logging.
     */
    val processDeathAllowed: Boolean = false,

    /** Arbitrary classification tags (e.g., “network”, “power”, “reboot”). */
    val tags: List<String> = emptyList(),

    /** If true, this event triggers a device reboot and initiates reboot recovery logic. */
    val causesReboot: Boolean = false,

    /**
     * If true, the engine waits for BOOT_COMPLETED after the reboot
     * triggered by this event.
     */
    val waitForBoot: Boolean = false,

    /**
     * If true, the engine will restart the target application after
     * reboot recovery.
     */
    val restartAppAfterBoot: Boolean = false,

    // -------------------------------------------------------------------------
    // Selection / frequency control
    // -------------------------------------------------------------------------

    /**
     * Base weight used for RANDOM mode selection.
     * Higher values mean more frequent selection relative to other events.
     */
    val weight: Int = 1,

    /**
     * Profile-specific weight overrides.
     * Example:
     *    { "aggressive": 5, "light": 1 }
     *
     * Used when run profile is specified.
     */
    val profileWeights: Map<String, Int>? = null,

    /**
     * Minimum number of seconds that must elapse between executions.
     * Cooldown is based on lastExecutionTimeMillis.
     */
    val cooldownSeconds: Int? = null,

    /**
     * Maximum number of times this event may run during a test.
     * Once reached, the event is skipped.
     */
    val maxExecutions: Int? = null,

    // -------------------------------------------------------------------------
    // Timing constraints
    // -------------------------------------------------------------------------

    /**
     * Optional timeout (seconds) for the script execution.
     * If exceeded, the script runner returns a timeout result.
     */
    val timeoutSeconds: Int? = null,

    /** Optional target duration (seconds) for long-running or ramp tests. */
    val durationSeconds: Int? = null,

    // -------------------------------------------------------------------------
    // Safety & enablement
    // -------------------------------------------------------------------------

    /** Indicates potential risk level of this event (for UI + validation). */
    val safetyLevel: SafetyLevel = SafetyLevel.LOW,

    /** If false, the event is disabled and will never run. */
    val enabled: Boolean = true,

    // -------------------------------------------------------------------------
    // Script execution info
    // -------------------------------------------------------------------------

    /**
     * Script language handler (SHELL, BATCH, PYTHON, NODE, etc.).
     * Only meaningful when type == SCRIPT or type == NO_OP.
     */
    val language: ScriptLanguage? = null,

    /**
     * The script definition itself—either inline text lines or a file path.
     */
    val script: ScriptDefinition? = null,

    /** List of arguments passed to the script handler. */
    val args: List<String> = emptyList(),

    /** Environment variables passed to the script execution. */
    val env: Map<String, String> = emptyMap(),

    /** If true, the script requires root access (systemInspector.rootAvailable must be true). */
    val requiresRoot: Boolean = false,

    // -------------------------------------------------------------------------
    // Preconditions / state machine
    // -------------------------------------------------------------------------

    /**
     * System conditions that must be satisfied before execution:
     * battery, network, root, screenOn, adb availability, file existence.
     */
    val preconditions: Preconditions? = null,

    /**
     * Key/value pairs that must already exist in the engine's internal state
     * before this event is eligible to run.
     */
    val requireState: Map<String, String> = emptyMap(),

    /**
     * Key/value pairs applied to engine state after the event succeeds.
     * Enables finite-state-machine-like flows.
     */
    val setState: Map<String, String> = emptyMap(),

    // -------------------------------------------------------------------------
    // Flow control & conditional logic
    // -------------------------------------------------------------------------

    /**
     * Specifies behavior when the script fails:
     * STOP_TEST, RETRY, SKIP_FUTURE, LOG_ONLY.
     */
    val onFailure: FailurePolicy = FailurePolicy.LOG_ONLY,

    /** Optional retry policy overriding the global defaultRetry in OrcaTestConfig. */
    val retryPolicy: RetryPolicy? = null,

    /** List of event IDs to automatically trigger after successful execution. */
    val postEvents: List<String> = emptyList(),

    /**
     * Conditional triggers that fire based on output, exit codes,
     * or custom metrics.
     */
    val conditionalTriggers: List<ConditionalTrigger> = emptyList(),

    // -------------------------------------------------------------------------
    // Sequence events
    // -------------------------------------------------------------------------

    /**
     * For SEQUENCE-type events, this list defines the ordered list
     * of child event IDs to execute.
     */
    val sequence: List<String> = emptyList(),

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    /**
     * Optional metrics sampling configuration (CPU, memory, battery, etc.).
     */
    val metrics: MetricsConfig? = null,

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    /** If true, stdout is logged when the event succeeds. */
    val logOutput: Boolean = true,

    /** If true, stderr is logged when the event fails. */
    val logErrors: Boolean = true,

    /**
     * Optional file path to write event-specific logs to.
     * EngineLogger still receives stdout/stderr unless configured otherwise.
     */
    val logFile: String? = null
)
