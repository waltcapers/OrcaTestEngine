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
package orca.cli

import java.io.File

object DumpSummaryCommand {

    private val summaryFile = File("logs/last-summary.txt")

    fun run() {
        if (!summaryFile.exists()) {
            println("❌ No summary file found at ${summaryFile.absolutePath}")
            println("Run a test first or configure summary saving.")
            return
        }

        println("\n=== LAST SUMMARY ===\n")
        println(summaryFile.readText())
        println("\n====================\n")
    }
}
