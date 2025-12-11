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

package orca.engine.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import java.io.File

/**
 * Performs JSON Schema validation using the NetworkNT validator.
 *
 * Supports:
 *   - validateConfig(File)
 *   - validate(jsonString)
 *
 * IMPORTANT:
 *   - The JSON schema must live under:
 *         src/main/resources/schema/stress-test-config.schema.json
 *   - Loaded from classpath via ClassLoader.
 */
object JsonSchemaValidator {

    private val mapper = ObjectMapper()

    /**
     * ---------------------------------------------
     * Internal helper:
     * Loads the schema JSON from CLASSPATH.
     * ---------------------------------------------
     */
    private fun loadSchema() =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
            .getSchema(
                javaClass.classLoader
                    .getResourceAsStream("schema/stress-test-config.schema.json")
                    ?: error(
                        "Schema file not found on classpath: schema/stress-test-config.schema.json\n" +
                                "Ensure it exists under src/main/resources/schema/"
                    )
            )

    /**
     * ------------------------------------------------------------
     * VALIDATE FROM FILE
     * This is used by OrcaConfigLoader.load(path)
     * ------------------------------------------------------------
     */
    fun validateConfig(configFile: File): Set<ValidationMessage> {
        if (!configFile.exists()) {
            error("Config file not found: ${configFile.absolutePath}")
        }

        val schema = loadSchema()
        val node = mapper.readTree(configFile)

        val results = schema.validate(node)

        // Print diagnostics with general guidance
        results.forEach { vm ->
            println(
                "❌ Schema violation at ${vm.path}: ${vm.message}\n" +
                        "   Check the value type, spelling, and structure in your JSON."
            )
        }

        return results
    }



    /**
     * ------------------------------------------------------------
     * VALIDATE FROM RAW JSON STRING
     *
     * This is the function you attempted to call:
     *      JsonSchemaValidator.validate(rawJson)
     *
     * Now it exists and works exactly as intended.
     * ------------------------------------------------------------
     */
    fun validate(json: String): Set<ValidationMessage> {
        val schema = loadSchema()
        val node: JsonNode = mapper.readTree(json)
        return schema.validate(node)
    }
}
