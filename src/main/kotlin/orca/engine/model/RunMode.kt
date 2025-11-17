package orca.engine.model

/**
 * Defines the high-level execution mode for a stress test.
 *
 * The run mode influences how events are selected and executed by the
 * OrcaEngine. Although not all modes are currently used by the engine,
 * they are included for future extensibility and configuration clarity.
 *
 * ### RANDOM
 * The engine selects events based on weighted random choice. Only events whose
 * `mode == EventMode.RANDOM` participate in random selection. Weights may be
 * influenced by global weight, profileWeights, cooldowns, maxExecutions, etc.
 *
 * ### SEQUENTIAL
 * Events are executed strictly in the order they appear in the config's
 * `events` list. This mode is intended for deterministic, scenario-based
 * flows or scripted sequences where randomness is undesirable.
 *
 * ### MIXED
 * A hybrid mode reserved for future interpretation.
 * Potential uses include:
 * - random selection among groups of sequential blocks
 * - scenario-driven event chaining with random inserts
 * - state-machine-driven switching between modes
 *
 * Although the engine does not currently implement custom MIXED behavior,
 * the presence of this enum allows the JSON schema and configuration format
 * to remain forward-compatible.
 */
enum class RunMode {
    /** Weighted random selection of RANDOM-mode events. */
    RANDOM,

    /** Execute events in the order they are defined. */
    SEQUENTIAL,

    /** Reserved hybrid mode — interpretation is engine-defined. */
    MIXED
}
