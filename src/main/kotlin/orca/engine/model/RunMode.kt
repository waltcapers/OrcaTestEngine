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
