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
