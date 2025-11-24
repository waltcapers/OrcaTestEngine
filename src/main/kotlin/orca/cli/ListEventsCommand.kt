package orca.cli

import orca.engine.config.StressConfigLoader
import java.io.File

/**
 * Implements:
 *
 *   orca list-events <config.json>
 *
 * Behavior:
 *   - Loads config.
 *   - Prints a concise table: ID, Type, Mode, Weight, Enabled.
 *
 * This command is intentionally small and fast, ideal for quickly scanning
 * what events are available without the more verbose output of dry-run.
 */
object ListEventsCommand {

    fun run(configPath: String) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        val config = try {
            StressConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        println("Events in config: ${file.name}")
        println(
            String.format(
                "%-30s %-10s %-12s %-8s %-8s",
                "ID", "Type", "Mode", "Weight", "Enabled"
            )
        )
        println("--------------------------------------------------------------------------")

        config.events.forEach { e ->
            println(
                String.format(
                    "%-30s %-10s %-12s %-8d %-8b",
                    e.id,
                    e.type,
                    e.mode,
                    e.weight,
                    e.enabled
                )
            )
        }
    }
}
