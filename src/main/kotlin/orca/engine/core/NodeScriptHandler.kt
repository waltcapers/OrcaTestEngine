package orca.engine.core

import orca.engine.model.ScriptDefinition
import java.io.File

/**
 * Script handler responsible for executing JavaScript files using Node.js.
 *
 * This handler supports both:
 * - **Inline scripts**: JavaScript provided directly in the configuration as a list of strings.
 * - **File-based scripts**: A path to a `.js` file on disk.
 *
 * The handler constructs a `node` command invocation and delegates execution
 * to {@link ProcessUtils#runProcess}, which handles process spawning, IPC,
 * and capturing stdout/stderr.
 *
 * Usage examples:
 * ---------------
 * Inline:
 * ```
 * "script": {
 *    "inline": ["console.log('Hello');"]
 * },
 * "language": "NODE"
 * ```
 *
 * File-based:
 * ```
 * "script": {
 *    "file": "scripts/test.js"
 * },
 * "language": "NODE"
 * ```
 *
 * Responsibilities:
 * -----------------
 * - Writes inline JS to a temporary file.
 * - Builds a proper `node <file> <args>` command.
 * - Preserves environment variables passed from the engine.
 * - Returns a {@link ScriptResult} with exit code, stdout, stderr, and metrics.
 */
class NodeScriptHandler : ScriptHandler {

    /**
     * Executes a Node.js script according to the definition provided.
     *
     * @param script The script being executed. Can contain either inline JavaScript
     *               or a file reference to a `.js` file.
     * @param args   Command-line arguments to append to the script execution.
     * @param env    Environment variables to expose to the launched process.
     *
     * @return A {@link ScriptResult} containing the exit code, stdout, stderr,
     *         and any optional runtime metrics collected.
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        // Build base command
        val cmd = mutableListOf("node")

        // Inline script → write to temp JS file
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".js")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath

        } else {
            // File-based script
            cmd += script.file
                ?: error("NodeScriptHandler: script.file must not be null when inline is not provided.")
        }

        // Add additional arguments
        cmd += args

        // Execute and return results
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
