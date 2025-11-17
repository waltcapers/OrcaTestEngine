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
