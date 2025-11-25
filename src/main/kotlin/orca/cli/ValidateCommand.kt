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
