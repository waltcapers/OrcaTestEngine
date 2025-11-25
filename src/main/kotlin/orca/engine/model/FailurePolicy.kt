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
 * Specifies how the OrcaEngine should respond when an event fails
 * (i.e., when its script or action returns a non-zero exit code).
 *
 * Each policy controls whether to stop the run, retry, or continue execution.
 */
enum class FailurePolicy {

    /**
     * Immediately stops the entire stress test.
     *
     * When triggered:
     * - The engine halts all further execution.
     * - Replay state is saved so the failure can be deterministically reproduced.
     *
     * Use this for critical operations where failure must abort the run.
     */
    STOP_TEST,

    /**
     * Only logs the failure and continues running.
     *
     * No retry is attempted unless explicitly defined by the event's retryPolicy.
     *
     * Use this policy when failures are expected or non-critical.
     */
    LOG_ONLY,

    /**
     * Retries the event according to the event’s `retryPolicy` or the
     * engine-level `defaultRetry` configuration.
     *
     * After all retry attempts fail, only the failure is recorded.
     *
     * Use this when transient or flaky failures are expected.
     */
    RETRY,

    /**
     * Marks the event as permanently disabled after failure.
     *
     * The event will not be selected in future iterations (unless reset),
     * but the rest of the stress test continues normally.
     *
     * Use this to isolate problematic events while allowing the test to proceed.
     */
    SKIP_FUTURE
}
