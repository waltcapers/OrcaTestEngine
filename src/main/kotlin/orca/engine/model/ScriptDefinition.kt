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
 * Defines the source of a script associated with a SCRIPT-type StressEvent.
 *
 * A script may be supplied in one of two forms:
 *
 * 1. **Inline script (`inline`)**
 *    - A list of text lines embedded directly inside the configuration file.
 *    - Useful for small helper scripts, commands, or portable multi-line logic.
 *
 * 2. **External script file (`file`)**
 *    - A filesystem path to a script stored outside the JSON configuration.
 *    - Ideal for larger scripts, shared utilities, or scripts written in
 *      languages requiring specific formatting or indentation.
 *
 * Exactly one of `inline` or `file` must be provided. Supplying neither is
 * considered a configuration error and will result in an exception at load time.
 *
 * The engine uses this definition to provide the script source to the appropriate
 * `ScriptHandler` implementation (SHELL, PYTHON, POWERSHELL, etc.).
 *
 * Example (inline):
 * ```
 * {
 *   "type": "SCRIPT",
 *   "language": "SHELL",
 *   "script": {
 *     "inline": [
 *       "echo Starting test",
 *       "adb shell input keyevent 3"
 *     ]
 *   }
 * }
 * ```
 *
 * Example (file):
 * ```
 * {
 *   "type": "SCRIPT",
 *   "language": "PYTHON",
 *   "script": {
 *     "file": "scripts/test_cpu.py"
 *   }
 * }
 * ```
 *
 * @property inline Optional list of inline script lines.
 * @property file Optional filesystem path to a script file.
 *
 * @throws IllegalArgumentException if both `inline` and `file` are null.
 */
data class ScriptDefinition(
    val inline: List<String>? = null,
    val file: String? = null
) {
    init {
        require(!(inline == null && file == null)) {
            "ScriptDefinition requires either 'inline' or 'file'"
        }
    }
}
