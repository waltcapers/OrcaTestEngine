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
 * Supported scripting languages for SCRIPT-type events.
 *
 * A StressEvent marked with `EventType.SCRIPT` must specify a language that
 * determines which {@link orca.engine.core.ScriptHandler} implementation the
 * engine will use. Each enum value maps directly to a handler in
 * `DefaultScriptRunner`.
 *
 * The scripting language affects:
 * - How the script is executed (shell, interpreter, OS command processor, etc.)
 * - How inline scripts are written
 * - Which runtime must be installed on the host machine
 *
 * ### Language descriptions
 *
 * - **SHELL**
 *   Runs the script using `/bin/sh -c` on Unix-like systems.
 *   Suitable for Bash-like commands, adb invocations, and general automation.
 *
 * - **BATCH**
 *   Executes the script using Windows `cmd.exe /c`.
 *   Used on Windows hosts for `.bat` or inline batch commands.
 *
 * - **PYTHON**
 *   Runs the script using `python3`.
 *   Inline scripts are written to a temporary `.py` file.
 *
 * - **POWERSHELL**
 *   Executes via `powershell -Command`.
 *   Ideal for modern Windows CLI automation.
 *
 * - **RUBY**
 *   Executes Ruby scripts using `ruby`.
 *   Mainly for developers preferring Ruby-based tooling.
 *
 * - **NODE**
 *   Runs the script using the Node.js interpreter (`node`).
 *   Supports JavaScript automation or Node-based test utilities.
 *
 * - **CUSTOM**
 *   Generic extension point intended for user-defined interpreters.
 *   Mapped to `CustomScriptHandler` by default, allowing arbitrary command execution.
 */
enum class ScriptLanguage {
    SHELL,
    BATCH,
    PYTHON,
    POWERSHELL,
    RUBY,
    NODE,
    CUSTOM        // Extension point for user-defined runners
}
