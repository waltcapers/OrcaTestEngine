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
