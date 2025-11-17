package orca.engine.model

/**
 * Defines how retry backoff delays are computed for a [RetryPolicy].
 *
 * Retry strategies control how long the engine waits between consecutive retry
 * attempts after a failed event execution. The strategy is applied inside the
 * OrcaEngine’s retry loop.
 *
 * The engine interprets strategies as follows:
 *
 * ### LINEAR
 *   - The delay remains constant for every retry.
 *   - Example with `backoffSeconds = 5`:
 *       Attempt 1 → fail → wait 5s
 *       Attempt 2 → fail → wait 5s
 *       Attempt 3 → fail → stop (max attempts reached)
 *
 * ### EXPONENTIAL
 *   - The delay increases exponentially per attempt:
 *       delay = backoffSeconds × 2^(attemptIndex)
 *
 *   - Example with `backoffSeconds = 5`:
 *       Attempt 1 → fail → wait 5s
 *       Attempt 2 → fail → wait 10s
 *       Attempt 3 → fail → wait 20s
 *
 * Both strategies are subject to the maximum number of attempts defined in
 * the associated [RetryPolicy].
 */
enum class RetryStrategy {
    /** Constant delay before each retry attempt. */
    LINEAR,

    /** Backoff delay doubles for each attempt. */
    EXPONENTIAL
}
