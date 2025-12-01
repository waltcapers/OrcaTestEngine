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

import orca.cli.util.Ansi
import kotlin.system.exitProcess

// ==========================================================================================
//  GLOBAL OPTION MODEL & CONTEXT
// ==========================================================================================

data class GlobalCliOptions(
    val deviceId: String? = null,
    val seedOverride: Long? = null,
    val logFile: String? = null,
    val debug: Boolean = false,
    val timeoutMs: Long? = null,
    val colorEnabled: Boolean = true,
    val artifactsDir: String? = null,
    val helpRequested: Boolean = false
)

/**
 * Commands (RunCommand, ProfileCommand, etc.) can read the parsed options from here.
 */
object OrcaCliContextHolder {
    @Volatile
    var globalOptions: GlobalCliOptions = GlobalCliOptions()
}

/** Internal struct: parse result */
private data class ParsedArgs(
    val options: GlobalCliOptions,
    val command: String,
    val commandArgs: List<String>
)

// ==========================================================================================
//  MAIN CLI ENTRYPOINT
// ==========================================================================================

object OrcaCLI {

    private const val ORCA_VERSION: String = "0.2.0-SNAPSHOT"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            return
        }

        val parsed = try {
            parseArgs(args)
        } catch (e: IllegalArgumentException) {
            val opts = OrcaCliContextHolder.globalOptions
            printError(e.message ?: "Invalid arguments.", opts)
            println()
            printUsage()
            exitProcess(1)
        }

        if (parsed.options.helpRequested && parsed.command.isEmpty()) {
            printUsage()
            return
        }

        if (parsed.command.isEmpty()) {
            val opts = parsed.options
            printError("Missing command.", opts)
            println()
            printUsage()
            exitProcess(1)
        }

        val finalOptions = prepareRunArtifactsDirIfNeeded(parsed.options, parsed.command)

        exitProcess(dispatch(parsed.command, parsed.commandArgs, finalOptions))
    }

    // ======================================================================================
    //  OPTION C FLAG PARSING (FLAGS ANYWHERE)
    // ======================================================================================

    private fun parseArgs(args: Array<String>): ParsedArgs {

        var deviceId: String? = null
        var seedOverride: Long? = null
        var logFile: String? = null
        var timeoutMs: Long? = null
        var debug = false
        var colorEnabled = true
        var artifactsDir: String? = null
        var helpRequested = false

        val tokens = args.toMutableList()

        val commandIndex = tokens.indexOfFirst { !it.startsWith("-") }
        if (commandIndex == -1) {
            throw IllegalArgumentException("No command found.")
        }

        val command = tokens[commandIndex]
        val commandArgs = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]

            if (i == commandIndex) {
                i++
                continue
            }

            when (t) {
                "--device" -> {
                    if (i + 1 >= tokens.size) throw IllegalArgumentException("Missing value for --device")
                    deviceId = tokens[i + 1]
                    i += 2
                    continue
                }

                "--seed" -> {
                    if (i + 1 >= tokens.size) throw IllegalArgumentException("Missing value for --seed")
                    seedOverride = tokens[i + 1].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid --seed value '${tokens[i + 1]}'")
                    i += 2
                    continue
                }

                "--log-file" -> {
                    if (i + 1 >= tokens.size) throw IllegalArgumentException("Missing value for --log-file")
                    logFile = tokens[i + 1]
                    i += 2
                    continue
                }

                "--timeout-ms" -> {
                    if (i + 1 >= tokens.size) throw IllegalArgumentException("Missing value for --timeout-ms")
                    timeoutMs = tokens[i + 1].toLongOrNull()
                        ?: throw IllegalArgumentException("Invalid --timeout-ms '${tokens[i + 1]}'")
                    i += 2
                    continue
                }

                "--artifacts-dir" -> {
                    if (i + 1 >= tokens.size) throw IllegalArgumentException("Missing value for --artifacts-dir")
                    artifactsDir = tokens[i + 1]
                    i += 2
                    continue
                }

                "--debug" -> {
                    debug = true
                    i++
                    continue
                }

                "--no-color" -> {
                    colorEnabled = false
                    i++
                    continue
                }

                "--help", "-h" -> {
                    helpRequested = true
                    i++
                    continue
                }

                else -> {
                    if (!t.startsWith("-") && i != commandIndex) {
                        commandArgs += t
                    }
                    i++
                }
            }
        }

        val artifactsRoot = artifactsDir ?: run {
            val home = System.getProperty("user.home")
            java.io.File(home, ".orca/artifacts").absolutePath
        }

        val opts = GlobalCliOptions(
            deviceId = deviceId,
            seedOverride = seedOverride,
            logFile = logFile,
            debug = debug,
            timeoutMs = timeoutMs,
            colorEnabled = colorEnabled,
            artifactsDir = artifactsRoot,
            helpRequested = helpRequested
        )

        OrcaCliContextHolder.globalOptions = opts

        return ParsedArgs(opts, command, commandArgs)
    }

    private fun prepareRunArtifactsDirIfNeeded(
        baseOptions: GlobalCliOptions,
        command: String
    ): GlobalCliOptions {

        val isRunLike = when (command) {
            "run", "dry-run", "profile", "run-event", "replay" -> true
            else -> false
        }

        if (!isRunLike || baseOptions.artifactsDir == null) return baseOptions

        val root = java.io.File(baseOptions.artifactsDir)
        if (!root.exists()) root.mkdirs()

        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        val runDir = java.io.File(root, "${command}_$timestamp")
        runDir.mkdirs()

        return baseOptions.copy(artifactsDir = runDir.absolutePath)
    }

    // ======================================================================================
    //  COMMAND DISPATCH + COLORIZED STATUS OUTPUT
    // ======================================================================================

    private fun dispatch(
        command: String,
        args: List<String>,
        opts: GlobalCliOptions
    ): Int {

        OrcaCliContextHolder.globalOptions = opts

        return try {
            when (command) {

                "run" -> {
                    if (args.isEmpty()) {
                        printError("Missing config file.", opts); printUsage(); 1
                    } else {
                        println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Running ${args[0]}…", opts.colorEnabled))
                        RunCommand.run(args[0])
                        println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} run complete.", opts.colorEnabled))
                        0
                    }
                }

                "validate" -> {
                    if (args.isEmpty()) {
                        printError("Missing config file.", opts); printUsage(); 1
                    } else {
                        println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Validating ${args[0]}…", opts.colorEnabled))
                        ValidateCommand.run(args[0])
                        println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} Validation passed.", opts.colorEnabled))
                        0
                    }
                }

                "replay" -> {
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Starting deterministic replay…", opts.colorEnabled))
                    ReplayCommand.run()
                    println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} Replay finished.", opts.colorEnabled))
                    0
                }

                "dry-run" -> {
                    if (args.isEmpty()) {
                        printError("Missing config file.", opts); printUsage(); 1
                    } else {
                        println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Dry-run for ${args[0]}…", opts.colorEnabled))
                        DryRunCommand.run(args[0])
                        println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} Dry-run complete.", opts.colorEnabled))
                        0
                    }
                }

                "list-events" -> {
                    if (args.isEmpty()) {
                        printError("Missing config file.", opts); printUsage(); 1
                    } else {
                        println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Listing events…", opts.colorEnabled))
                        ListEventsCommand.run(args[0])
                        0
                    }
                }

                "explain-event" -> {
                    if (args.size < 2) {
                        printError("Usage: orca explain-event <config> <eventId>", opts); return 1
                    }
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Explaining '${args[1]}'…", opts.colorEnabled))
                    ExplainEventCommand.run(args[0], args[1])
                    0
                }

                "detect-adb" -> {
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Detecting adb…", opts.colorEnabled))
                    DetectAdbCommand.run()
                    0
                }

                "run-event" -> {
                    if (args.size < 2) {
                        printError("Usage: orca run-event <config> <eventId> [n]", opts); return 1
                    }
                    val iters = args.getOrNull(2)?.toIntOrNull() ?: 1
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Running '${args[1]}' $iters times…", opts.colorEnabled))
                    RunEventCommand.run(args[0], args[1], iters)
                    println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} run-event complete.", opts.colorEnabled))
                    0
                }

                "profile" -> {
                    if (args.isEmpty()) {
                        printError("Usage: orca profile <config> [n]", opts); return 1
                    }
                    val iters = args.getOrNull(1)?.toIntOrNull() ?: 100
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Profiling ${args[0]} for $iters iterations…", opts.colorEnabled))
                    ProfileCommand.run(args[0], iters)
                    println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} Profile run complete.", opts.colorEnabled))
                    0
                }

                "dump-schema" -> {
                    val out = args.firstOrNull()
                    println(Ansi.blue("${Ansi.infoSymbol(opts.colorEnabled)} Dumping schema…", opts.colorEnabled))
                    DumpSchemaCommand.run(out)
                    println(Ansi.green("${Ansi.successSymbol(opts.colorEnabled)} Schema dump complete.", opts.colorEnabled))
                    0
                }

                "interactive" -> {
                    InteractiveShell.run(opts)
                    0
                }

                "version" -> {
                    println("OrcaTestEngine version $ORCA_VERSION")
                    0
                }

                "help", "--help", "-h" -> {
                    printUsage()
                    0
                }

                else -> {
                    printError("Unknown command: '$command'", opts)
                    printUsage()
                    1
                }
            }
        } catch (t: Throwable) {
            printError("Unhandled error: ${t.message}", opts)
            if (opts.debug) t.printStackTrace()
            1
        }
    }

    // ======================================================================================
    //  INTERACTIVE SHELL
    // ======================================================================================

    private object InteractiveShell {

        fun run(sessionOpts: GlobalCliOptions) {
            println(
                Ansi.cyan(
                    Ansi.bold("Entering Orca interactive mode. Type 'help' or 'exit'.", sessionOpts.colorEnabled),
                    sessionOpts.colorEnabled
                )
            )

            val reader = System.`in`.bufferedReader()

            while (true) {
                print(Ansi.green("orca${Ansi.promptSymbol(sessionOpts.colorEnabled)} ", sessionOpts.colorEnabled))
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.equals("exit", true) || trimmed.equals("quit", true)) break

                if (trimmed.equals("help", true)) {
                    printUsage()
                    continue
                }

                val tokens = tokenize(trimmed)
                try {
                    val parsed = parseArgs(tokens.toTypedArray())
                    try {
                        dispatch(parsed.command, parsed.commandArgs, parsed.options)
                    } catch (e: Exception) {
                        println(Ansi.red("Error: ${e.message}", sessionOpts.colorEnabled))
                    }
                } catch (e: IllegalArgumentException) {
                    printError(e.message ?: "Invalid command.", sessionOpts)
                }
            }

            println(Ansi.gray("Exiting interactive mode.", sessionOpts.colorEnabled))
        }

        private fun tokenize(s: String): List<String> {
            val result = mutableListOf<String>()
            val sb = StringBuilder()
            var inQuotes = false

            for (c in s) {
                when {
                    c == '"' -> inQuotes = !inQuotes
                    c.isWhitespace() && !inQuotes -> {
                        if (sb.isNotEmpty()) {
                            result += sb.toString()
                            sb.clear()
                        }
                    }
                    else -> sb.append(c)
                }
            }
            if (sb.isNotEmpty()) result += sb.toString()
            return result
        }
    }

    // ======================================================================================
    //  HELP & ERROR MESSAGES
    // ======================================================================================

    private fun printUsage() {
        val opts = OrcaCliContextHolder.globalOptions

        println(Ansi.cyan(Ansi.bold("\nOrcaTestEngine CLI"), opts.colorEnabled))
        println(Ansi.yellow("Usage:", opts.colorEnabled))
        println("  orca [flags] <command> [args]\n")

        println(Ansi.blue("Global Flags:", opts.colorEnabled))
        println("  --device <id>          Target Android device/emulator")
        println("  --seed <num>           Override RNG seed")
        println("  --log-file <path>      Output logs here")
        println("  --timeout-ms <num>     Optional max time")
        println("  --artifacts-dir <path> Artifact output root directory")
        println("  --debug                Verbose logging")
        println("  --no-color             Disable ANSI output")
        println("  --help, -h             Show help\n")

        println(Ansi.blue("Commands:", opts.colorEnabled))
        println("  run <cfg>              Execute full stress test")
        println("  validate <cfg>         Validate config schema")
        println("  replay                 Replay last failure deterministically")
        println("  dry-run <cfg>          Print events without running")
        println("  list-events <cfg>      List event definitions")
        println("  explain-event <c> <id> Explain a single event")
        println("  detect-adb             Check adb/device availability")
        println("  run-event <c> <id> [n] Execute one event")
        println("  profile <c> [n]        Weighted selection profile")
        println("  dump-schema [path]     Output JSON schema to file/console")
        println("  interactive            Enter interactive REPL")
        println("  version                Show version info")
        println("  help                   Show help\n")

        println(Ansi.gray("Examples:", opts.colorEnabled))
        println("  orca run config.json --device emulator-5554")
        println("  orca validate config.json")
        println("  orca run config.json --timeout-ms 30000")
        println()
    }

    private fun printError(msg: String, opts: GlobalCliOptions) {
        println(Ansi.red("${Ansi.errorSymbol(opts.colorEnabled)} $msg", opts.colorEnabled))
    }
}
