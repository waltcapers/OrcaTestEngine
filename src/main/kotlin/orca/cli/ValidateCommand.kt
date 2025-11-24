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

import orca.engine.config.JsonSchemaValidator
import java.io.File

/**
 * Implements:
 *
 *   orca validate <config.json>
 *
 * Behavior:
 *   - Loads the configuration file as raw JSON.
 *   - Validates it against the JSON schema using JsonSchemaValidator.
 *   - Prints all validation errors (if any).
 *   - Does NOT run the test.
 */
object ValidateCommand {

    fun run(configPath: String) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        println("Validating config: ${file.absolutePath}")

        val violations = try {
            JsonSchemaValidator.validateConfig(file)
        } catch (ex: Exception) {
            println("❌ Failed to validate configuration: ${ex.message}")
            ex.printStackTrace()
            return
        }

        if (violations.isEmpty()) {
            println("✅ Configuration is VALID according to the JSON schema.")
        } else {
            println("❌ Configuration is INVALID. Found ${violations.size} issue(s):")
            violations.forEach { v ->
                println("  - ${v.message} (at path: ${v.path})")
            }
        }
    }
}
