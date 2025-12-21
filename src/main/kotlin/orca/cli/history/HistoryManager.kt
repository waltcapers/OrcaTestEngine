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
package orca.cli.history

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Standalone, reusable command history manager.
 *
 * Features:
 *  - add() → store commands
 *  - list() / print() → show numbered history
 *  - resolve():
 *        !!      → last command
 *        !n      → nth command
 *        !prefix → most recent command starting with prefix
 *
 *  - persistent history stored in ~/.orca/history
 *
 * Thread-safe, no external dependencies, no static state.
 */
class HistoryManager(
    private val maxHistory: Int = 500
) {

    private val history = mutableListOf<String>()

    // Location: ~/.orca/history
    private val historyDir: Path =
        Paths.get(System.getProperty("user.home"), ".orca")

    private val historyFile: Path =
        historyDir.resolve("history")

    init {
        loadPersistentHistory()
    }

    // ---------------------------------------------------------------------
    // Persistent History
    // ---------------------------------------------------------------------

    @Synchronized
    fun savePersistentHistory() {
        try {
            if (!Files.exists(historyDir)) {
                Files.createDirectories(historyDir)
            }
            Files.write(historyFile, history)
        } catch (e: Exception) {
            println("Warning: could not save history: ${e.message}")
        }
    }

    @Synchronized
    private fun loadPersistentHistory() {
        try {
            if (Files.exists(historyFile)) {
                val lines = Files.readAllLines(historyFile)
                history.clear()
                history.addAll(lines.takeLast(maxHistory))
            }
        } catch (e: Exception) {
            println("Warning: could not load history: ${e.message}")
        }
    }

    // ---------------------------------------------------------------------
    // Basic operations
    // ---------------------------------------------------------------------

    @Synchronized
    fun add(cmd: String) {
        if (cmd.isBlank()) return
        history += cmd
        if (history.size > maxHistory) {
            history.removeAt(0)
        }
        savePersistentHistory()
    }

    @Synchronized
    fun list(): List<String> = history.toList()

    @Synchronized
    fun print() {
        if (history.isEmpty()) {
            println("(no history)")
            return
        }
        history.forEachIndexed { idx, entry ->
            println("${idx + 1}  $entry")
        }
    }

    // ---------------------------------------------------------------------
    // History expansion (!!, !n, !prefix)
    // ---------------------------------------------------------------------

    /**
     * Resolve:
     *   !!       → last command
     *   !3       → third command
     *   !run     → last command starting with "run"
     *
     * Returns:
     *   expanded command string
     *   null → invalid or error already printed
     */
    @Synchronized
    fun resolve(input: String): String? {

        if (input == "!!") {
            if (history.isEmpty()) {
                println("No previous command.")
                return null
            }
            return history.last()
        }

        if (input.startsWith("!")) {
            val token = input.removePrefix("!")

            // !n  → numeric
            val number = token.toIntOrNull()
            if (number != null) {
                if (number < 1 || number > history.size) {
                    println("History index out of range: $input")
                    return null
                }
                return history[number - 1]
            }

            // !prefix → pattern search
            val prefix = token
            val match = history.lastOrNull { it.startsWith(prefix) }

            if (match == null) {
                println("No history entry starts with \"$prefix\"")
                return null
            }

            return match
        }

        return input
    }

    // ---------------------------------------------------------------------
    // Clear history
    // ---------------------------------------------------------------------

    @Synchronized
    fun clear() {
        history.clear()

        try {
            if (Files.exists(historyFile)) {
                Files.delete(historyFile)
            }
        } catch (e: Exception) {
            println("Warning: could not clear history: ${e.message}")
        }
    }
}
