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
