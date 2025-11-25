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
