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
