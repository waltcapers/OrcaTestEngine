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

import orca.engine.core.EngineLogger

/**
 * Simple stdout-based implementation of [EngineLogger].
 *
 * This logger is intended for local development, debugging, or console-based
 * execution of the OrcaEngine. All log messages are written directly to
 * standard output using `println()`.
 *
 * Log levels are formatted with a prefix for easy visual scanning:
 * - `[INFO]` for normal operational messages
 * - `[WARN]` for recoverable or unexpected conditions
 * - `[ERROR]` for failures, optionally including a stack trace
 *
 * This implementation is deliberately lightweight and has no external
 * dependencies. For production environments, CI systems, or structured
 * logging, another implementation (e.g., file-based or JSON-based) may be
 * provided by the user.
 */
class ConsoleEngineLogger : EngineLogger {

    /**
     * Prints an informational message to stdout.
     *
     * @param message the message to log.
     */
    override fun info(message: String) {
        println("[INFO] $message")
    }

    /**
     * Prints a warning message to stdout.
     *
     * @param message the message to log.
     */
    override fun warn(message: String) {
        println("[WARN] $message")
    }

    /**
     * Prints an error message to stdout. If a [Throwable] is provided,
     * its stack trace is converted to a string and appended.
     *
     * @param message the error message to log.
     * @param t optional exception associated with the error.
     */
    override fun error(message: String, t: Throwable?) {
        if (t != null) {
            println("[ERROR] $message\n${t.stackTraceToString()}")
        } else {
            println("[ERROR] $message")
        }
    }
}
