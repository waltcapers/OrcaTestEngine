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
package orca.engine.logging

/**
 * Defines the contract for managing logcat capture during a stress-test run.
 *
 * Implementations of this interface handle:
 * - Starting a persistent `adb logcat` process and routing its output somewhere (usually a file).
 * - Stopping the active capture process gracefully.
 * - Rotating logs by restarting capture under a new file or context.
 *
 * This interface is intentionally minimal so that multiple logging backends
 * (file capture, streaming, filtering, remote upload, etc.) can be plugged in
 * without modifying the engine.
 */
interface LogcatManager {

    /**
     * Starts capturing logcat output.
     *
     * @param tag
     *   Optional log tag or substring to filter output. Implementations may
     *   perform filtering via shell pipelines (e.g., `adb logcat | grep`) or
     *   through other mechanisms.
     *
     * If a capture session is already running, implementations should stop
     * the existing capture before starting a new one.
     */
    fun startCapture(tag: String? = null)

    /**
     * Stops the currently active logcat capture, if any.
     *
     * Implementations should ensure the underlying process is terminated and
     * resources such as file handles are released.
     */
    fun stopCapture()

    /**
     * Rotates the log output by stopping the current capture and starting a new one.
     *
     * @param tag
     *   Optional tag to apply to the new capture session.
     *
     * This is typically used when:
     * - Reboots occur
     * - Log segments need to be separated by phase
     * - Continuous runs require periodic log rollover
     */
    fun rotate(tag: String? = null)
}
