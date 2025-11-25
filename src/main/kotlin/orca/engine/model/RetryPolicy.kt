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
