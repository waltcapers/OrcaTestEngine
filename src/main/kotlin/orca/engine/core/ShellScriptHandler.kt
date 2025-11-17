package orca.engine.core

import orca.engine.model.ScriptDefinition
import java.io.File

/**
 * Executes shell scripts (sh-based) for SCRIPT-type events.
 *
 * This handler is responsible for running Unix shell scripts using:
 *
 * ```
 * /bin/sh -c "<script> <args>"
 * ```
 *
 * It supports both:
 * - **Inline scripts** embedded directly in the configuration JSON
 * - **External script files** referenced through `script.file`
 *
 * ### Behavior
 *
 * 1. If `script.inline` is present, the lines are joined and executed directly.
 * 2. Otherwise, the file path in `script.file` is read into memory.
 * 3. The script content is passed to `/bin/sh -c` along with arguments.
 * 4. The handler delegates process execution to [ProcessUtils.runProcess].
 *
 * ### Use Cases
 * This handler is ideal for:
 * - Unix-based command execution
 * - ADB command automation (`adb shell ...`)
 * - System manipulations inside Linux or macOS environments
 *
 * On Windows platforms, this handler is typically unused unless WSL or Git Bash
 * is installed and available from the system PATH.
 *
 * @see ScriptDefinition
 * @see ScriptHandler
 * @see ProcessUtils
 * @see ScriptResult
 */
class ShellScriptHandler : ScriptHandler {

    /**
     * Executes a shell script using `/bin/sh -c`.
     *
     * @param script the script definition containing inline or file-based content
     * @param args the argument list passed to the script
     * @param env the environment variables exposed to the script's process
     * @return a [ScriptResult] representing exit status, stdout, stderr, and metrics
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        // Prepare the base shell command
        val cmd = mutableListOf("/bin/sh", "-c")

        // Determine script content: inline text or file contents
        val scriptText =
            script.inline?.joinToString("\n") ?: File(script.file!!).readText()

        // Append arguments to the script invocation
        cmd += scriptText + " " + args.joinToString(" ")

        // Delegate to unified process execution utility
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
