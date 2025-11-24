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
