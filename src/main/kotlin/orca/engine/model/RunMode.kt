/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 *
 * Copyright (c) 2025 Walter E. Capers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * MIT License Conditions (for all parties except GM)
 * --------------------------------------------------
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * General Motors License Exception
 * --------------------------------
 * General Motors (GM) is granted a perpetual, irrevocable, worldwide,
 * royalty-free license to use, modify, reproduce, publish, distribute,
 * sublicense, and create derivative works from this Software for any internal
 * or commercial purpose.
 *
 * The GM License Exception applies exclusively to General Motors and does not
 * extend to any other third party or organization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
