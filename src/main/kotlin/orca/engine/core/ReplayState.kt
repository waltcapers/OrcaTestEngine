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

/**
 * Represents the deterministic replay state used by the OrcaEngine.
 *
 * The replay mechanism allows the engine to reproduce the exact same
 * sequence of randomly selected events by restoring:
 *
 * 1. The **original random seed** used during the test.
 * 2. The **number of RNG calls** consumed up to the failure point.
 *
 * During a real test run, the engine periodically saves this object to
 * `replay_state.json`. In deterministic replay mode, the engine loads it,
 * resets its RNG to the same seed, fast-forwards the RNG call count,
 * and then reproduces the run exactly.
 *
 * ## Purpose
 * Deterministic replay is essential for debugging nondeterministic failures
 * caused by randomized stress testing. By capturing the seed and RNG position,
 * engineers can reproduce failures consistently across machines and test runs.
 *
 * ## Example Saved JSON
 * ```json
 * {
 *   "seed": 123456789,
 *   "rngCalls": 42
 * }
 * ```
 *
 * @property seed
 *   The initial random seed used when the OrcaEngine was started.
 *
 * @property rngCalls
 *   The total number of calls made to the RNG before the failure occurred.
 *
 * @see orca.engine.core.OrcaEngine
 * @see orca.engine.core.ReplayStateSerializer
 */
data class ReplayState(
    val seed: Long,
    val rngCalls: Long
)
