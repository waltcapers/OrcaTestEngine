package orca.engine.model

/**
 * Defines how a `StressEvent` participates in execution scheduling.
 *
 * Event mode determines when and how the engine is allowed to select
 * the event for execution. The mode affects random selection, sequence
 * handling, and conditional branching.
 *
 * Modes:
 *
 * - **RANDOM**
 *   Included in the weighted event pool and eligible for `selectNextEvent()`.
 *   Weight, profileWeights, cooldowns, and maxExecutions apply.
 *
 * - **SEQUENTIAL**
 *   Not chosen randomly. Executed *only* when referenced in a SEQUENCE-type
 *   parent event (i.e., scenarios defined in `sequence: []`).
 *   Order is strictly defined by the sequence list.
 *
 * - **CONDITIONAL**
 *   Not selected randomly. Executed only when triggered via
 *   `ConditionalTrigger` rules based on metrics, script output, or exit codes.
 */
enum class EventMode {
    /** Eligible for weighted random selection. */
    RANDOM,

    /** Executed only as part of an explicitly defined event SEQUENCE. */
    SEQUENTIAL,

    /** Executed only when one of its conditional triggers is satisfied. */
    CONDITIONAL
}
