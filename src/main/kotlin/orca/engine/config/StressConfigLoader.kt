package orca.engine.config

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.buffer
import okio.source
import orca.engine.model.OrcaTestConfig
import java.io.File

/**
 * Loads a {@link OrcaTestConfig} instance from a JSON configuration file.
 *
 * This object encapsulates all configuration parsing logic required by the
 * OrcaEngine. The underlying implementation uses Moshi with Kotlin reflection
 * support to deserialize the JSON into typed Kotlin data classes.
 *
 * Usage example:
 * ```
 * val config = StressConfigLoader.load("stress-config.json")
 * ```
 */
object StressConfigLoader {

    /**
     * A configured Moshi instance capable of parsing Kotlin data classes.
     *
     * Moshi is used as the JSON serialization/deserialization library.
     * The {@link KotlinJsonAdapterFactory} enables reflection-based adapters
     * for Kotlin classes.
     */
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /**
     * A JSON adapter used to convert configuration JSON into a
     * {@link OrcaTestConfig} object.
     */
    private val adapter = moshi.adapter(OrcaTestConfig::class.java)

    /**
     * Loads and parses a stress test configuration file from disk.
     *
     * @param path Absolute or relative path to the JSON configuration file.
     * @return A fully populated {@link OrcaTestConfig} instance.
     *
     * @throws IllegalArgumentException if the file does not exist.
     * @throws IllegalStateException if parsing fails or returns `null`.
     */
    fun load(path: String): orca.engine.model.OrcaTestConfig {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalArgumentException("Config file not found: $path")
        }

        file.source().buffer().use { bufferedSource ->
            return adapter.fromJson(bufferedSource)
                ?: error("Failed to parse config.")
        }
    }
}
