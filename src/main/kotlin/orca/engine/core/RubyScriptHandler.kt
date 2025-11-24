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
import orca.engine.core.ProcessUtils
import orca.engine.core.ScriptHandler
import orca.engine.core.ScriptResult
import orca.engine.model.ScriptDefinition
import java.io.File

/**
 * Script handler responsible for executing Ruby scripts as part of the
 * OrcaEngine's script-based event execution pipeline.
 *
 * This handler supports two types of Ruby script definitions:
 *
 * 1. **Inline Ruby scripts**
 *    - Provided directly inside the JSON configuration as a list of strings.
 *    - Converted into a temporary `.rb` file at runtime.
 *
 * 2. **File-based Ruby scripts**
 *    - A path referencing an existing `.rb` file on disk.
 *
 * The handler constructs a Ruby execution command and delegates to
 * [ProcessUtils.runProcess] to launch the external interpreter.
 *
 * ### Example Invocation
 * The generated command will look like:
 *
 * ```
 * ruby /path/to/script.rb arg1 arg2
 * ```
 *
 * Environment variables defined in the event configuration are injected
 * into the process environment before execution.
 *
 * @see ScriptHandler
 * @see ScriptDefinition
 * @see ProcessUtils.runProcess
 */
class RubyScriptHandler : ScriptHandler {

    /**
     * Executes a Ruby script using the system's `ruby` interpreter.
     *
     * @param script the script definition, either inline or file-based
     * @param args arguments passed from the StressEvent configuration
     * @param env environment variables applied to the Ruby process
     *
     * @return a [ScriptResult] containing exit code, stdout, stderr,
     *         and metrics collected during execution
     */
    override fun execute(
        script: ScriptDefinition,
        args: List<String>,
        env: Map<String, String>
    ): ScriptResult {

        val cmd = mutableListOf("ruby")

        // Inline script → write to a temporary .rb file
        if (script.inline != null) {
            val temp = File.createTempFile("inline", ".rb")
            temp.writeText(script.inline.joinToString("\n"))
            cmd += temp.absolutePath

            // File-based script
        } else {
            cmd += script.file!!
        }

        // Append script arguments
        cmd += args

        // Execute via utility and return output
        return ProcessUtils.runProcess(cmd, env = env)
    }
}
