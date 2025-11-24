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
