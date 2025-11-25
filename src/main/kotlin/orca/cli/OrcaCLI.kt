/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 *
 * Copyright (c) 2025 Walter E. Capers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * MIT License Conditions (for all parties except GM)
 * --------------------------------------------------
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * General Motors License Exception
 * --------------------------------
 * General Motors (GM) is granted a perpetual, irrevocable, worldwide,
 * royalty-free license to use, modify, reproduce, publish, distribute,
 * sublicense, and create derivative works from this Software for any internal
 * or commercial purpose.
 *
 * The GM License Exception applies exclusively to General Motors and does not
 * extend to any other third party or organization.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package orca.cli

import orca.engine.config.StressConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import orca.engine.system.DefaultSystemInspector
import java.io.File

/**
 * Top-level command-line entrypoint for the OrcaTestEngine.
 *
 * This object is intentionally very small: it just parses arguments and
 * delegates to specific command handlers (RunCommand, DryRunCommand, etc.).
 *
 * Suggested usage from the command line (once packaged as an app/JAR):
 *
 *   # Run a configuration
 *   orca run orca-config.json
 *
 *   # Validate configuration ONLY (no execution)
 *   orca validate orca-config.json
 *
 *   # Replay last failing run
 *   orca replay
 *
 *   # View events and configuration without executing anything
 *   orca dry-run orca-config.json
 *   orca list-events orca-config.json
 *   orca explain-event orca-config.json noop_1
 *
 *   # Check ADB connectivity and basic device state
 *   orca detect-adb
 *
 *   # Run a single event in isolation
 *   orca run-event orca-config.json noop_1 [iterations]
 *
 *   # Run and rely on OrcaEngine's summary as a “profile”
 *   orca profile orca-config.json [iterations]
 *
 *   # Print the JSON schema
 *   orca dump-schema [optional-output-path]
 */
object OrcaCLI {

    /**
     * Main entrypoint used when launching from a CLI context.
     *
     * You can either:
     *  - configure Gradle to use this as the application's main class, OR
     *  - call OrcaCLI.main(args) from your existing main.kt.
     */
    @JvmStatic
    fun main(args: Array<String>) {

        // If no arguments are provided, show usage and exit.
        if (args.isEmpty()) {
            printUsage()
            return
        }

        // First token is the subcommand (run, validate, replay, etc.)
        val cmd = args[0]

        when (cmd) {
            // -----------------------------------------------------------------
            // Existing “core” commands
            // -----------------------------------------------------------------

            "run" -> {
                if (args.size < 2) {
                    println("❌ Missing config file argument.")
                    printUsage()
                    return
                }
                RunCommand.run(args[1])
            }

            "validate" -> {
                if (args.size < 2) {
                    println("❌ Missing config file argument.")
                    printUsage()
                    return
                }
                ValidateCommand.run(args[1])
            }

            "replay" -> {
                // Simple replay: assumes replay_state.json in current directory.
                ReplayCommand.run()
            }

            // -----------------------------------------------------------------
            // NEW: Analysis / inspection / tooling commands
            // -----------------------------------------------------------------

            "dry-run" -> {
                if (args.size < 2) {
                    println("❌ Missing config file argument.")
                    printUsage()
                    return
                }
                DryRunCommand.run(args[1])
            }

            "list-events" -> {
                if (args.size < 2) {
                    println("❌ Missing config file argument.")
                    printUsage()
                    return
                }
                ListEventsCommand.run(args[1])
            }

            "explain-event" -> {
                if (args.size < 3) {
                    println("❌ Usage: orca explain-event <config.json> <eventId>")
                    return
                }
                ExplainEventCommand.run(configPath = args[1], eventId = args[2])
            }

            "detect-adb" -> {
                DetectAdbCommand.run()
            }

            "run-event" -> {
                if (args.size < 3) {
                    println("❌ Usage: orca run-event <config.json> <eventId> [iterations]")
                    return
                }
                val configPath = args[1]
                val eventId = args[2]
                val iterations =
                    if (args.size >= 4) args[3].toIntOrNull() ?: 1 else 1
                RunEventCommand.run(configPath, eventId, iterations)
            }

            "profile" -> {
                if (args.size < 2) {
                    println("❌ Usage: orca profile <config.json> [iterations]")
                    return
                }
                val configPath = args[1]
                val iterations =
                    if (args.size >= 3) args[2].toIntOrNull() ?: 100 else 100
                ProfileCommand.run(configPath, iterations)
            }

            "dump-schema" -> {
                val outputPath = if (args.size >= 2) args[1] else null
                DumpSchemaCommand.run(outputPath)
            }

            "help", "-h", "--help" -> printUsage()

            else -> {
                println("❌ Unknown command: $cmd\n")
                printUsage()
            }
        }
    }

    /**
     * Prints a concise usage guide for all supported CLI commands.
     */
    private fun printUsage() {
        println(
            """
            OrcaTestEngine CLI

            Usage:
              orca run <config.json>
              orca validate <config.json>
              orca replay

              orca dry-run <config.json>
              orca list-events <config.json>
              orca explain-event <config.json> <eventId>

              orca detect-adb
              orca run-event <config.json> <eventId> [iterations]
              orca profile <config.json> [iterations]
              orca dump-schema [output-path]

            Notes:
              - "run" executes the full test according to the config file.
              - "validate" checks the config against the JSON schema only.
              - "replay" replays the last failing run using replay_state.json.
              - "dry-run" prints what would execute, but does not run anything.
              - "list-events" shows all defined events in the configuration.
              - "explain-event" prints details about a single event.
              - "detect-adb" checks whether ADB is reachable and basic state.
              - "run-event" isolates a single event and executes it.
              - "profile" runs multiple iterations and relies on OrcaEngine's
                built-in summary to show selection distribution.
              - "dump-schema" prints or writes the JSON schema used for validation.
            """.trimIndent()
        )
    }
}
