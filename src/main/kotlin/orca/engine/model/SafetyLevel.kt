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
 * Represents the relative safety classification of an event.
 *
 * Safety levels allow the engine or the test author to indicate how risky
 * a given event may be to the stability of the device, application, or
 * test environment. Although the core OrcaEngine does not limit execution
 * based on safety level, this field is designed for:
 *
 * - external tooling or dashboards that want to filter or highlight risky events
 * - human review before running potentially destructive tests
 * - future engine policies (e.g., restricting CRITICAL operations without a flag)
 * - scenario grouping or prioritization
 *
 * Descriptions of each level:
 *
 * ### LOW
 * Events considered minimally risky. Typically harmless operations such as
 * querying metrics, small UI interactions, or benign system checks.
 *
 * ### MODERATE
 * Events that could cause temporary disruption but are unlikely to destabilize
 * the system. Examples include clearing app cache or triggering normal lifecycle
 * transitions.
 *
 * ### HIGH
 * Events that may cause app restarts, significant state changes, or temporary
 * system instability. Requires caution and awareness of test-side effects.
 *
 * ### CRITICAL
 * Events capable of causing reboots, force-stops, network suspension, or other
 * major disruptions. Should be used sparingly and with explicit intent.
 */
enum class SafetyLevel {
    /** Minimal risk to system or application stability. */
    LOW,

    /** Some risk; may introduce temporary inconsistency or behavioral changes. */
    MODERATE,

    /** Significant risk; may impact app or system stability. */
    HIGH,

    /** Very high risk; may cause reboots, force-stops, or major disruption. */
    CRITICAL
}
