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
 * Represents a conditional rule that can trigger another event based on the
 * result of executing a SCRIPT event.
 *
 * Conditional triggers allow reactive behavior during a stress test. For example:
 * - Restart an app if output contains an error keyword.
 * - Trigger cleanup actions if CPU usage spikes above a threshold.
 * - Trigger a recovery event when a script exits with an error code.
 *
 * Each conditional field is optional. During evaluation, the engine checks
 * only the fields that are non-null.
 *
 * All enabled conditions are evaluated logically ANDed together.
 * If all conditions that are present evaluate to true, the corresponding
 * `triggerEventId` is fired.
 *
 * @property triggerEventId
 *   ID of the event that should be triggered when the conditions match.
 *
 * @property ifOutputContains
 *   If non-null, the trigger fires only when the script's STDOUT or STDERR
 *   contains this substring.
 *
 * @property ifExitCodeNotZero
 *   If true, the trigger fires only when the script returns a non-zero exit code.
 *
 * @property ifExitCodeEquals
 *   Exact exit code required for the trigger to fire.
 *
 * @property ifMetricAbove
 *   Map of metric names to numeric thresholds.
 *   All referenced metrics must be **strictly greater** than their threshold
 *   for the trigger to activate.
 *   Example: `{"cpuUsage": 0.85}` meaning fire only if CPU usage exceeds 85%.
 *
 * @property ifMetricBelow
 *   Map of metric names to numeric thresholds.
 *   All referenced metrics must be **strictly less** than their threshold
 *   for the trigger to activate.
 */
data class ConditionalTrigger(
    val triggerEventId: String,
    val ifOutputContains: String? = null,
    val ifExitCodeNotZero: Boolean? = null,
    val ifExitCodeEquals: Int? = null,
    val ifMetricAbove: Map<String, Double>? = null,
    val ifMetricBelow: Map<String, Double>? = null
)
