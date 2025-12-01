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
package orca.cli.util

/**
 * Global ANSI color + symbol utility.
 *
 * - Fully cross-platform (Windows 11, macOS, Linux)
 * - Provides safe fallbacks when color is disabled
 * - All CLI and Engine modules can use this consistently
 */
object Ansi {

    // ============================================================
    // ANSI COLOR CODES
    // ============================================================

    const val RESET = "\u001B[0m"
    const val BOLD = "\u001B[1m"

    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val MAGENTA = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val GRAY = "\u001B[90m"

    // ============================================================
    // COLOR HELPERS
    // Each takes (msg, enabled) so callers can toggle ANSI easily.
    // ============================================================

    fun red(msg: String, enabled: Boolean = true) =
        if (enabled) "$RED$msg$RESET" else msg

    fun green(msg: String, enabled: Boolean = true) =
        if (enabled) "$GREEN$msg$RESET" else msg

    fun yellow(msg: String, enabled: Boolean = true) =
        if (enabled) "$YELLOW$msg$RESET" else msg

    fun blue(msg: String, enabled: Boolean = true) =
        if (enabled) "$BLUE$msg$RESET" else msg

    fun magenta(msg: String, enabled: Boolean = true) =
        if (enabled) "$MAGENTA$msg$RESET" else msg

    fun cyan(msg: String, enabled: Boolean = true) =
        if (enabled) "$CYAN$msg$RESET" else msg

    fun gray(msg: String, enabled: Boolean = true) =
        if (enabled) "$GRAY$msg$RESET" else msg

    fun bold(msg: String, enabled: Boolean = true) =
        if (enabled) "$BOLD$msg$RESET" else msg

    // ============================================================
    // SYMBOL HELPERS
    // Provide Unicode when enabled, ASCII fallbacks otherwise.
    // ============================================================

    fun successSymbol(enabled: Boolean = true): String =
        if (enabled) "✓" else "OK"

    fun errorSymbol(enabled: Boolean = true): String =
        if (enabled) "✗" else "ERR"

    fun warnSymbol(enabled: Boolean = true): String =
        if (enabled) "⚠" else "!!"

    fun infoSymbol(enabled: Boolean = true): String =
        if (enabled) "ℹ" else "i"

    fun promptSymbol(enabled: Boolean = true): String =
        if (enabled) "❯" else ">"
}

