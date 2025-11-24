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
package orca.engine.system

/**
 * Abstraction for executing ADB commands.
 *
 * This allows the engine to:
 *  - use real ADB (DefaultAdbExecutor)
 *  - or fake ADB for testing (FakeAdbExecutor)
 *
 * Provides two entry points:
 *  - exec()  → host-side commands (e.g., `adb get-state`)
 *  - shell() → device-side commands (`adb shell cmd`)
 */
interface AdbExecutor {

    /**
     * Executes a HOST-side ADB command:
     *   adb <args...>
     */
    fun exec(args: List<String>, timeoutSeconds: Long): AdbResult

    /**
     * Executes a DEVICE-side shell command:
     *   adb shell "<command>"
     */
    fun shell(command: String, timeoutSeconds: Long): AdbResult
}
