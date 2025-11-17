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
