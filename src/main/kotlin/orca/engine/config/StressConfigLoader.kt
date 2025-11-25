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

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import orca.engine.model.OrcaTestConfig
import java.io.File
import orca.engine.config.JsonSchemaValidator

/**
 * Loads a validated OrcaTestConfig from disk.
 *
 * This class now performs **two-phase loading**:
 *
 *  PHASE 1 — Raw JSON Schema validation (NetworkNT):
 *      - Ensures structure is correct before deserializing
 *      - Gives detailed, field-specific error messages
 *
 *  PHASE 2 — Moshi object parsing:
 *      - Converts validated JSON into strongly typed Kotlin objects
 */
object StressConfigLoader {

    /** Moshi instance for Kotlin data-class parsing */
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /** Adapter for mapping JSON to OrcaTestConfig */
    private val adapter = moshi.adapter(OrcaTestConfig::class.java)

    /**
     * Loads, validates, and parses a stress-test configuration JSON file.
     */
    fun load(path: String): OrcaTestConfig {
        val file = File(path)

        if (!file.exists()) {
            throw IllegalArgumentException("Config file not found: $path")
        }

        // ---- PHASE 1: Raw JSON string read ----
        val rawJson: String = file.readText()

        // ---- NEW: Validate JSON using schema BEFORE parsing ----
        JsonSchemaValidator.validate(rawJson)

        // ---- PHASE 2: Deserialize into OrcaTestConfig ----
        file.source().buffer().use { buffered ->
            return adapter.fromJson(buffered)
                ?: error("Failed to parse validated config JSON into OrcaTestConfig.")
        }
    }
}
