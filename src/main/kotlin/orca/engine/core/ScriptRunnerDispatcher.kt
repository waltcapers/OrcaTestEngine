package orca.engine.core

import RubyScriptHandler
import orca.engine.model.ScriptLanguage
import orca.engine.model.StressEvent

/**
 * Default implementation of [ScriptRunner].
 *
 * This class acts as a *dispatcher*:
 *
 *  - OrcaEngine calls [run] with a full [StressEvent].
 *  - This runner:
 *      * Validates that the event has a script + language.
 *      * Chooses the correct [ScriptHandler] implementation
 *        based on [StressEvent.language].
 *      * Delegates execution to that handler.
 *
 * Handlers are language-specific and know how to:
 *  - Build the correct command line (sh, python, node, etc.)
 *  - Materialize inline scripts into temp files (when needed)
 *  - Call [ProcessUtils.runProcess] to actually execute the script.
 *
 * IMPORTANT:
 *  - This class does **not** know about retries, cooldowns,
 *    or reboot behavior. That is handled by [OrcaEngine].
 *  - This keeps responsibilities clean:
 *      OrcaEngine = "what event to run, when, and with what policy"
 *      ScriptRunner = "how to execute a SCRIPT-type event once"
 */
class ScriptRunnerDispatcher(

    /**
     * Handler for SHELL scripts.
     * Typically uses `/bin/sh -c` and runs on Unix-like environments.
     */
    private val shellHandler: ScriptHandler = ShellScriptHandler(),

    /**
     * Handler for Windows batch (`.bat` / `.cmd`) scripts.
     * Uses `cmd.exe /c`.
     */
    private val batchHandler: ScriptHandler = BatchScriptHandler(),

    /**
     * Handler for Python scripts.
     * Uses `python3` by default (you can swap this implementation later if needed).
     */
    private val pythonHandler: ScriptHandler = PythonScriptHandler(),

    /**
     * Handler for PowerShell scripts.
     * Uses `powershell -Command`.
     */
    private val powershellHandler: ScriptHandler = PowerShellScriptHandler(),

    /**
     * Handler for Ruby scripts.
     * Uses `ruby` CLI.
     */
    private val rubyHandler: ScriptHandler = RubyScriptHandler(),

    /**
     * Handler for Node.js scripts.
     * Uses `node` CLI.
     */
    private val nodeHandler: ScriptHandler = NodeScriptHandler(),

    /**
     * "Custom" handler, meant as an extension point.
     *
     * This can be used in several ways:
     *  - To run domain-specific test harnesses
     *  - To wrap existing tools without forcing a new ScriptLanguage
     *  - For experimentation (e.g., Java-based launchers, custom CLIs)
     */
    private val customHandler: ScriptHandler = CustomScriptHandler()
) : ScriptRunner {

    /**
     * Execute a single SCRIPT-type event and return a [ScriptResult].
     *
     * OrcaEngine guarantees that:
     *  - This is only called for events where `type == EventType.SCRIPT`
     *  - Retries, cooldowns, failure policies, and reboot logic are
     *    handled at the engine level.
     *
     * This function focuses solely on:
     *  1. Defensive validation (script + language present).
     *  2. Selecting the correct [ScriptHandler].
     *  3. Delegating execution and returning the result.
     */
    override fun run(event: StressEvent): ScriptResult {
        // --------------------------------------------------------------------
        // 1) Defensive checks for language + script
        // --------------------------------------------------------------------

        // If there is no language, we cannot choose a handler.
        val language = event.language
        if (language == null) {
            // We return a synthetic ScriptResult instead of throwing,
            // so that OrcaEngine can treat this as a normal failure
            // and apply the event's FailurePolicy.
            return ScriptResult(
                exitCode = -1,
                stdout = "",
                stderr = "No script language specified for event '${event.id}'."
            )
        }

        // The engine allows non-SCRIPT event types to exist with no script
        // (e.g., SEQUENCE, WAIT_FOR_DEVICE, NO_OP). By the time we reach
        // this runner, we expect a SCRIPT event and therefore a script.
        val script = event.script
        if (script == null) {
            return ScriptResult(
                exitCode = -1,
                stdout = "",
                stderr = "No script definition provided for event '${event.id}'."
            )
        }

        // --------------------------------------------------------------------
        // 2) Choose the correct ScriptHandler based on language
        // --------------------------------------------------------------------
        val handler: ScriptHandler = when (language) {
            ScriptLanguage.SHELL   -> shellHandler
            ScriptLanguage.BATCH   -> batchHandler
            ScriptLanguage.PYTHON  -> pythonHandler
            ScriptLanguage.POWERSHELL -> powershellHandler
            ScriptLanguage.RUBY    -> rubyHandler
            ScriptLanguage.NODE    -> nodeHandler
            ScriptLanguage.CUSTOM  -> customHandler
        }

        // --------------------------------------------------------------------
        // 3) Delegate to the handler
        // --------------------------------------------------------------------
        //
        // Handlers all share the same contract:
        //   fun execute(
        //       script: ScriptDefinition,
        //       args: List<String>,
        //       env: Map<String, String>
        //   ): ScriptResult
        //
        // They are responsible for:
        //   - Converting inline scripts to temp files (if needed)
        //   - Constructing the correct command-line invocation
        //   - Calling ProcessUtils.runProcess() and returning the result
        //
        // At this stage, OrcaEngine has *not* attached any extra metrics;
        // ScriptResult.metrics is purely what the handler or script chooses
        // to provide (if anything).
        return handler.execute(
            script = script,
            args = event.args,
            env = event.env
        )
    }
}
