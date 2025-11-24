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
 * Holds runtime execution statistics for a single `StressEvent`.
 *
 * These values are updated by the engine each time the event executes,
 * and they are used for:
 * - cooldown handling (`lastExecutionTimeMillis`)
 * - performance analysis (duration fields)
 * - event disabling after failures (`disabled`)
 * - summary reporting at the end of the run
 *
 * All fields are mutable because stats evolve throughout the test.
 */
data class EventStats(

    /**
     * Timestamp (in millis, from `System.currentTimeMillis()`) of the most recent
     * successful execution. Used for evaluating `cooldownSeconds`.
     */
    var lastExecutionTimeMillis: Long? = null,

    /**
     * Duration (in milliseconds) of the most recent execution.
     */
    var lastDurationMs: Long? = null,

    /**
     * Total number of times the event has successfully executed.
     */
    var executions: Int = 0,

    /**
     * When true, the engine will skip this event entirely.
     * Primarily used by `FailurePolicy.SKIP_FUTURE`.
     */
    var disabled: Boolean = false,

    /**
     * Sum of all recorded durations. Used to compute average execution time.
     */
    var totalDuration: Long = 0,

    /**
     * Minimum observed duration across all executions.
     * Initialized to Long.MAX_VALUE to ensure proper comparison on first update.
     */
    var minDuration: Long = Long.MAX_VALUE,

    /**
     * Maximum observed duration across all executions.
     * Initialized to Long.MIN_VALUE to ensure proper comparison on first update.
     */
    var maxDuration: Long = Long.MIN_VALUE
) {

    /**
     * Average duration (in ms) across all successful executions.
     * Returns 0 if the event has never executed.
     */
    val averageDuration: Long
        get() = if (executions > 0) totalDuration / executions else 0
}
