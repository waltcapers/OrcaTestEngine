package orca.cli.util

object Banner {

    fun printStartupBanner(color: Boolean) {
        // ASCII Art (two versions: color and plain fallback)
        val ascii = if (color) {
            Ansi.cyan("""
               ___  ____   ____    _    
              / _ \|  _ \ / ___|  / \   
             | | | | |_) | |     / _ \  
             | |_| |  _ <| |___ / ___ \ 
              \___/|_| \_\\____/_/   \_\
            """.trimIndent(), true)
        } else {
            """
               ___  ____   ____    _    
              / _ \|  _ \ / ___|  / \   
             | | | | |_) | |     / _ \  
             | |_| |  _ <| |___ / ___ \ 
              \___/|_| \_\\____/_/   \_\
            """.trimIndent()
        }
        println()
        println()
        println(ascii)
        println()

        // Title line
        println(
            Ansi.bold(
                "🐋 OrcaTestEngine  —  Stress Testing Framework for Android",
                color
            )
        )

        // Legal line
        println(
            Ansi.gray(
                "© 2025 Walter E. Capers. Licensed under MIT + GM License Exception.",
                color
            )
        )

        // Separator
        println(
            if (color) Ansi.blue("──────────────────────────────────────────────────────", true)
            else       "------------------------------------------------------"
        )

        println()
    }
}
