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
 * Defines the core categories of events that the OrcaEngine can execute.
 *
 * Each event type determines the execution flow:
 * - how the engine processes the event,
 * - whether scripts run,
 * - whether child events are invoked,
 * - or whether the engine waits for a system state change.
 */
enum class EventType {

    /**
     * A script-based event.
     *
     * The engine uses a `ScriptRunner` to execute the associated script
     * using the language specified by `StressEvent.language`.
     *
     * Supports:
     * - inline scripts
     * - external script files
     * - metrics collection
     * - conditional triggers
     */
    SCRIPT,

    /**
     * A no-operation event.
     *
     * Used when testing engine flow or verifying sequencing behavior
     * without performing any actual action.
     *
     * The engine logs the event description and marks it successful
     * with a duration of 0ms.
     */
    NO_OP,

    /**
     * Executes a predefined sequence of other event IDs.
     *
     * The engine resolves each child ID and executes it using full
     * retry / failure / conditional handling logic.
     *
     * The parent SEQUENCE event succeeds only if all children succeed.
     */
    SEQUENCE,

    /**
     * An event that blocks until the Android device is reachable.
     *
     * The engine:
     * - waits for the device to appear over ADB,
     * - optionally waits for BOOT_COMPLETED (if enabled on the event),
     * - then resumes execution.
     *
     * Used for reboot recovery or when scripts intentionally disconnect ADB.
     */
    WAIT_FOR_DEVICE
}
