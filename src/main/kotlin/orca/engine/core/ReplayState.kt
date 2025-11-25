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
