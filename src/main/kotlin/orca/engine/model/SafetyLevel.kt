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
