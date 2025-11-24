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
 * Defines how a [StressEvent] should retry after a failed execution attempt.
 *
 * Retry behavior is implemented inside the engine’s `executeEventWithPolicy()`
 * method. The policy controls:
 *
 *  - How many times the engine attempts to run the event
 *  - How long the engine waits between attempts
 *  - Whether backoff delay progression is linear or exponential
 *
 * Only events whose `onFailure` is set to [FailurePolicy.RETRY] will use retry
 * logic. All other failure policies ignore this configuration.
 *
 * @property maxAttempts
 *     Total number of attempts allowed for the event, including the first one.
 *     For example:
 *     - `maxAttempts = 1` → No retries (default)
 *     - `maxAttempts = 3` → Run, then retry twice if needed
 *
 * @property backoffSeconds
 *     Base delay (in seconds) inserted between retry attempts.
 *
 *     Interpretation depends on [strategy]:
 *     - **LINEAR:** delay stays constant
 *       e.g., backoffSeconds=5 → wait 5 seconds before each retry.
 *
 *     - **EXPONENTIAL:** delay grows exponentially per attempt
 *       e.g., backoffSeconds=5 → attempt delays: 5s, 10s, 20s, ...
 *
 * @property strategy
 *     Retry backoff strategy used to compute the delay for each retry.
 *
 *     See [RetryStrategy] for supported modes:
 *     - LINEAR
 *     - EXPONENTIAL
 */
data class RetryPolicy(
    val maxAttempts: Int = 1,
    val backoffSeconds: Int = 0,
    val strategy: RetryStrategy = RetryStrategy.LINEAR
)
