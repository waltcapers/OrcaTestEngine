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

import java.io.File

/**
 * Implements:
 *
 *   orca dump-schema [output-path]
 *
 * Behavior:
 *   - Loads the JSON schema resource used for validation:
 *         schema/stress-test-config.schema.json
 *   - If output-path is provided:
 *       * Writes the schema to that file.
 *     Else:
 *       * Prints the schema to stdout.
 *
 * This is mainly useful for:
 *   - IDE autocompletion (e.g., VS Code JSON schema support).
 *   - Documentation or sharing the expected configuration shape.
 */
object DumpSchemaCommand {

    private const val RESOURCE_PATH = "schema/stress-test-config.schema.json"

    fun run(outputPath: String? = null) {
        val stream = javaClass.classLoader
            .getResourceAsStream(RESOURCE_PATH)

        if (stream == null) {
            println("❌ Could not find schema resource at: $RESOURCE_PATH")
            println("   Ensure the file exists under src/main/resources/schema/")
            return
        }

        val text = stream.bufferedReader().use { it.readText() }

        if (outputPath == null) {
            println(text)
        } else {
            val file = File(outputPath)
            file.writeText(text)
            println("✅ Schema written to: ${file.absolutePath}")
        }
    }
}
