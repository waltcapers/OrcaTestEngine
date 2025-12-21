package orca.cli

import MockScriptRunner
import orca.cli.history.HistoryManager
import orca.cli.util.Ansi
import orca.engine.config.JsonSchemaValidator
import orca.engine.config.OrcaConfigLoader
import orca.engine.core.OrcaEngine
import orca.engine.core.OrcaEngineFactory
import orca.engine.core.ScriptRunnerDispatcher
import orca.engine.logging.ConsoleEngineLogger
import orca.engine.system.DefaultSystemInspector
import orca.engine.model.OrcaTestConfig
import orca.engine.model.ScriptRunner
import orca.engine.model.SystemInspector
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import orca.engine.system.MockSystemInspector
import java.io.File

/**
 * Orca Interactive Debug Shell
 *
 * Features:
 *   load <file>     Load configuration JSON
 *   validate        Validate JSON schema
 *   events          List all events
 *   event <id>      Run one event by ID
 *   run             Run full engine loop
 *   step            Run exactly one iteration of the engine
 *   verbose on|off    Toggle debug logging in config
 *   summary         Print engine summary
 *   state           Print engine key-value state
 *   help            Show commands
 *   exit            Quit shell
 */
class OrcaShellDebugger {

    private var engine: OrcaEngine? = null
    private var config: OrcaTestConfig? = null
    private var configPath: String? = null
    private var historyManager = HistoryManager()

    fun start() {
        println("🐋 Orca Debug Shell — type 'help' for commands.")

        println()

        while (true) {
            print("orca-debug> ")
            var line = readLine()?.trim() ?: break
            if(line == "history") {
                historyManager.print()
                continue
            }

            // HISTORY SHORTCUTS (!!, !n)
            val expanded = historyManager.resolve(line)
            if (expanded == null) continue  // invalid or error already printed

            line = expanded
            if (line.isEmpty()) continue
            historyManager.add(line)
            if (!handleCommand(line)) break
        }
    }

    // ---------------------------------------------------------
    // Command Dispatcher
    // ---------------------------------------------------------
    private fun handleCommand(line: String): Boolean {
        val parts = line.split(" ")
        val cmd = parts[0].lowercase()
        val args = parts.drop(1)

        return when (cmd) {
            "load"     -> { cmdLoad(args); true }
            "validate" -> { cmdValidate(); true }
            "events"   -> { cmdEvents(); true }
            "event"    -> { cmdEvent(args); true }
            "run"      -> { cmdRun(args); true }
            "step"     -> { cmdStep(); true }
            "verbose"  -> { cmdDebug(args); true }
            "summary"  -> { cmdSummary(); true }
            "state"    -> { cmdState(); true }
            "explain"  -> { cmdExplain(args); true}
            "help"     -> { cmdHelp(); true }
            "exit", "quit" -> false
            else -> {
                println("Unknown command '$cmd'. Try 'help'.")
                true
            }
        }
    }

    // ---------------------------------------------------------
    // load <file>
    // ---------------------------------------------------------
    private fun cmdLoad(args: List<String>) {
        if (args.isEmpty()) {
            println("Usage: load <file>")
            return
        }

        val path = args[0]
        val f = File(path)

        if (!f.exists()) {
            println("✗ File not found: $path")
            return
        }

        val loadedConfig = try {
            OrcaConfigLoader.load(path)
        } catch (t: Throwable) {
            println("❌ Failed to load config: ${t.message}")
            return
        }

        try {
            engine = OrcaEngineFactory.newEngine(
            targetPackage = loadedConfig.targetPackage,
            configAttrib = loadedConfig,
            mockMode = args.contains("--mock"),
            ConsoleEngineLogger())

            val logger = ConsoleEngineLogger()
            val inspector = DefaultSystemInspector(debug = false)
            val runner = ScriptRunnerDispatcher()

            this.config = loadedConfig
            this.configPath = path


            println("✓ Loaded configuration '${f.name}'")
            println("✓ Events: ${loadedConfig.events.size}")
            println("✓ Target package: ${loadedConfig.targetPackage}")

        } catch (e: Exception) {
            println("✗ Engine initialization failed: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // validate
    // ---------------------------------------------------------
    private fun cmdValidate() {
        val cfgPath = configPath ?: return println("No configuration loaded.")
        val result = try {
            JsonSchemaValidator.validateConfig(File(cfgPath))
        } catch (e: Exception) {
            println("❌ Validation failed: ${e.message}")
            return
        }

        if (result.isEmpty()) {
            println("✓ Configuration is VALID.")
        } else {
            println("❌ Invalid configuration:")
            result.forEach { println(" - ${it.message} (at ${it.path})") }
        }
    }

    // ---------------------------------------------------------
    // events
    // ---------------------------------------------------------
    private fun cmdEvents() {
        val cfg = config ?: return println("No configuration loaded.")
        println("Events (${cfg.events.size}):")
        cfg.events.forEach { e ->
            println(" - ${e.id} [${e.type}] mode=${e.mode}")
        }
    }

    // ---------------------------------------------------------
    // event <id>
    // ---------------------------------------------------------
    private fun cmdEvent(args: List<String>) {
        val e = engine ?: return println("No configuration loaded.")
        val cfg = config ?: return println("No configuration loaded.")

        if (args.isEmpty()) {
            println("Usage: event <id>")
            return
        }

        val id = args[0]
        if (cfg.events.none { it.id == id }) {
            println("✗ Event '$id' not found.")
            return
        }

        println("→ Running event '$id'...")
        val result = e.runEventById(id)
        println("Result: ${if (result) "✓ success" else "✗ failure"}")
    }

    // ---------------------------------------------------------
    // run
    // ---------------------------------------------------------
    private fun cmdRun(args: List<String>) {
        val cfg = config ?: return println("No configuration loaded.")
        if(engine == null) return println("EventEngine was not created.")
        engine!!.runLoop()
    }


    // ---------------------------------------------------------
    // step
    // ---------------------------------------------------------
    private fun cmdStep() {
        val e = engine ?: return println("No configuration loaded.")
        val ok = e.runOnce()
        println("Step result: ${if (ok) "✓ success" else "✗ failure"}")
    }
    private fun cmdExplain(args: List<String>){
        if (args.isEmpty()) {
            println("Usage: explain <id>")
            return
        }
        val cfg = config ?: return println("No configuration loaded.")

        val event = cfg.events.find { it.id == args[0] }
            if (event == null) {
                println("❌ Event not found: $args[0]")
                return
            }

        ExplainEventCommand.printDetailed(event)
    }
    // ---------------------------------------------------------
    // debug on/off
    // ---------------------------------------------------------
    private fun cmdDebug(args: List<String>) {
        val cfg = config ?: return println("No configuration loaded.")
        if (args.isEmpty()) {
            println("Usage: debug on|off")
            return
        }
        val enable = args[0].lowercase() == "on"
        cfg.debug = enable
        println("Debug logging: ${if (enable) "ENABLED" else "DISABLED"}")
    }

    // ---------------------------------------------------------
    // summary
    // ---------------------------------------------------------
    private fun cmdSummary() {
        val e = engine ?: return println("No configuration loaded.")
        e.printSummary()
    }

    // ---------------------------------------------------------
    // state
    // ---------------------------------------------------------
    private fun cmdState() {
        val e = engine ?: return println("No configuration loaded.")
        val map = e.getStateSnapshot()
        if (map.isEmpty()) {
            println("(State is empty)")
            return
        }
        println("Engine State:")
        map.forEach { (k, v) -> println("  $k = $v") }
    }

    // ---------------------------------------------------------
    // help
    // ---------------------------------------------------------
    private fun cmdHelp() {


        println(
            """
Available Commands:

  load <file> [--mock] Load a config file
  validate             Validate JSON schema
  events               List all events
  event <id>           Run a single event by ID
  run                  Run the engine loop
  step                 Run exactly one engine iteration
  verbose on|off       Toggle config.debug
  state                Show engine state variables
  explain <id>         Explain an event
  summary              Show final summary
  help                 Show this list
  exit                 Quit debug-shell
""".trimIndent()
        )
    }
}
