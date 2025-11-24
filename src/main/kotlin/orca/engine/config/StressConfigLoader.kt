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
