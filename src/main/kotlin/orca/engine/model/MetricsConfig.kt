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
 * Configuration for collecting runtime system metrics during the execution
 * of stress events.
 *
 * This configuration determines which metrics the engine should request from
 * the [SystemInspector] during event execution. Collected metrics are attached
 * to each event’s [ScriptResult] and later aggregated into global summaries.
 *
 * Built-in metrics supported by the engine include:
 *  - CPU usage
 *  - Memory usage
 *  - Battery level
 *
 * In addition, callers may specify arbitrary metric names using
 * `customMetrics`. These may correspond to custom hooks implemented by the
 * [SystemInspector] for advanced monitoring.
 *
 * @property captureCpuUsage       Whether CPU usage should be measured and
 *                                 included in per-event metrics.
 * @property captureMemoryUsage    Whether memory usage should be measured and
 *                                 included in per-event metrics.
 * @property captureBatteryLevel   Whether battery level should be captured as
 *                                 part of per-event metrics.
 * @property customMetrics         A list of custom metric identifiers that
 *                                 the engine should request from the
 *                                 [SystemInspector]. These hooks are optional
 *                                 and user-defined.
 */
data class MetricsConfig(
    val captureCpuUsage: Boolean = false,
    val captureMemoryUsage: Boolean = false,
    val captureBatteryLevel: Boolean = false,
    val customMetrics: List<String> = emptyList()
)
