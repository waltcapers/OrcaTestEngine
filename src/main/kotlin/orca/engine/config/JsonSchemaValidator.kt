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
     * This is used by StressConfigLoader.load(path)
     * ------------------------------------------------------------
     */
    fun validateConfig(configFile: File): Set<ValidationMessage> {
        if (!configFile.exists()) {
            error("Config file not found: ${configFile.absolutePath}")
        }

        val schema = loadSchema()
        val configNode: JsonNode = mapper.readTree(configFile)

        return schema.validate(configNode)
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
