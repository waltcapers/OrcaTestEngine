package orca.engine.core

import RubyScriptHandler
import orca.engine.model.*

/**
 * Default implementation of the {@link ScriptRunner} interface.
 *
 * This class acts as a dispatcher that routes script execution requests to the
 * appropriate {@link ScriptHandler} based on the script language defined in
 * {@link StressEvent#language}.
 *
 * Handlers supported:
 * -------------------
 * - Shell (sh / bash)
 * - Batch (cmd.exe)
 * - Python
 * - PowerShell
 * - Ruby
 * - Node.js
 * - Custom (arbitrary script paths or inline content)
 *
 * Runtime behavior:
 * -----------------
 *  1. The event’s script definition is validated.
 *  2. The correct handler is selected from the constructor-injected list.
 *  3. The selected handler executes the script with event args + env variables.
 *  4. A {@link ScriptResult} is returned containing exitCode, stdout, stderr, and metrics.
 *
 * Handlers are dependency-injectable for unit testing or customization.
 */
class DefaultScriptRunner(
    private val shellHandler: ScriptHandler = ShellScriptHandler(),
    private val batchHandler: ScriptHandler = BatchScriptHandler(),
    private val pythonHandler: ScriptHandler = PythonScriptHandler(),
    private val powershellHandler: ScriptHandler = PowerShellScriptHandler(),
    private val rubyHandler: ScriptHandler = RubyScriptHandler(),
    private val nodeHandler: ScriptHandler = NodeScriptHandler(),
    private val customHandler: ScriptHandler = CustomScriptHandler()
) : ScriptRunner {

    /**
     * Executes the script defined by a {@link StressEvent}.
     *
     * @param event The event whose script should be executed.
     * @return A {@link ScriptResult} capturing exit code, stdout, stderr, and optional metrics.
     *
     * Execution flow:
     *  1. Validate the script definition.
     *  2. Select handler based on {@link ScriptLanguage}.
     *  3. Pass script + args + env to handler.
     *
     * Error cases:
     *  - Missing script definition → exitCode = -1
     *  - Missing script language → exitCode = -1
     */
    override fun run(event: StressEvent): ScriptResult {

        // Validate script definition
        val script = event.script ?: return ScriptResult(
            exitCode = -1,
            stdout = "",
            stderr = "Missing script definition"
        )

        // Select appropriate handler based on the event's language
        val handler = when (event.language) {
            ScriptLanguage.SHELL -> shellHandler
            ScriptLanguage.BATCH -> batchHandler
            ScriptLanguage.PYTHON -> pythonHandler
            ScriptLanguage.POWERSHELL -> powershellHandler
            ScriptLanguage.RUBY -> rubyHandler
            ScriptLanguage.NODE -> nodeHandler
            ScriptLanguage.CUSTOM -> customHandler

            // No language specified → fail fast
            null -> return ScriptResult(
                exitCode = -1,
                stdout = "",
                stderr = "No script language specified"
            )
        }

        // Execute script via the selected handler
        return handler.execute(
            script = script,
            args = event.args,
            env = event.env
        )

        // Note: Duration tracking is handled at OrcaEngine level,
        // not in this class, to keep runners simple.
    }
}
