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
