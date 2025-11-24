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
 * Defines conditions that must be satisfied for a [StressEvent] to be eligible
 * for execution. If any configured precondition fails, the engine will skip the
 * event without treating it as a failure.
 *
 * Preconditions are evaluated inside the engine’s `checkPreconditions()` method,
 * using data provided by the [SystemInspector] implementation.
 *
 * Supported conditions include:
 *  - Battery thresholds
 *  - Network connectivity
 *  - Device idleness
 *  - Screen state
 *  - Charging state
 *  - Root availability
 *  - Required files
 *  - ADB connectivity
 *
 * Any field that is `null` is considered unspecified and will not be checked.
 *
 * @property batteryAbove
 *     Event is only eligible if the device battery percentage is *greater than
 *     or equal to* this value. Example: require battery ≥ 40%.
 *
 * @property batteryBelow
 *     Event is only eligible if the device battery percentage is *less than or
 *     equal to* this value. Example: prohibit running when battery > 80%.
 *
 * @property networkRequired
 *     If `true`, the device must report that a network connection is available.
 *
 * @property deviceIdle
 *     If `true`, the device must be in an idle state as defined by the
 *     [SystemInspector] implementation.
 *
 * @property screenOn
 *     If `true`, the device screen must currently be on.
 *
 * @property chargingRequired
 *     If `true`, the device must be plugged in and charging.
 *
 * @property rootRequired
 *     If `true`, event execution requires that the device supports root commands.
 *
 * @property fileMustExist
 *     A list of filesystem paths that must exist for the event to run.
 *     If any listed file does not exist, the event is skipped.
 *
 * @property adbAvailable
 *     If `true`, the event requires a functioning ADB connection.
 *     If `false`, it requires *no* ADB connection.
 *     If `null`, ADB state is not checked.
 */
data class Preconditions(
    val batteryAbove: Int? = null,
    val batteryBelow: Int? = null,
    val networkRequired: Boolean? = null,
    val deviceIdle: Boolean? = null,
    val screenOn: Boolean? = null,
    val chargingRequired: Boolean? = null,
    val rootRequired: Boolean? = null,
    val fileMustExist: List<String> = emptyList(),
    val adbAvailable: Boolean? = null
)

