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

package orca.engine.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Utility object responsible for serializing and deserializing
 * [ReplayState] to and from JSON on disk.
 *
 * This serializer enables deterministic replay functionality in the
 * OrcaEngine by persisting the random seed and the number of RNG
 * calls consumed during a real test run. When a replay is triggered,
 * the saved `ReplayState` is loaded to reproduce the exact execution
 * path that led to a failure.
 *
 * ## Format
 * The state is stored as a simple JSON file, usually named
 * `replay_state.json`, containing:
 *
 * ```json
 * {
 *   "seed": 123456789,
 *   "rngCalls": 42
 * }
 * ```
 *
 * ## Usage
 * - The OrcaEngine calls [saveReplayState] automatically when a
 *   run encounters a failure, enabling developers to replay that
 *   scenario later.
 * - The replay logic calls [loadReplayState] to restore the previous
 *   engine RNG position and seed.
 *
 * ## Thread Safety
 * This object is stateless aside from its internal `Gson` instance
 * and is safe to use across threads.
 *
 * @see ReplayState
 * @see orca.engine.core.OrcaEngine#replay
 */
object ReplayStateSerializer {

    /** Internal Gson instance used for (de)serialization. */
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    /**
     * Serializes a [ReplayState] instance to a JSON file.
     *
     * @param state the deterministic replay state to persist
     * @param path optional output file path (defaults to `replay_state.json`)
     *
     * @throws java.io.IOException if the file cannot be written
     */
    fun saveReplayState(state: ReplayState, path: String = "replay_state.json") {
        val json = gson.toJson(state)
        File(path).writeText(json)
    }

    /**
     * Loads a previously saved [ReplayState] from disk.
     *
     * @param path the file path to load from (defaults to `replay_state.json`)
     * @return the parsed [ReplayState] object
     *
     * @throws IllegalStateException if the replay file does not exist
     * @throws com.google.gson.JsonSyntaxException if file contents are invalid JSON
     */
    fun loadReplayState(path: String = "replay_state.json"): ReplayState {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("Replay file not found: $path")
        }

        return gson.fromJson(file.readText(), ReplayState::class.java)
    }
}
