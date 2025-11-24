package orca.cli

import java.io.File

object CreateSampleCommand {

    fun run() {
        val dir = File("samples")
        if (!dir.exists()) dir.mkdirs()

        val out = File(dir, "orca-sample.json")

        if (out.exists()) {
            println("⚠️ Sample file already exists at: ${out.absolutePath}")
            println("   (Delete it to regenerate.)")
            return
        }

        out.writeText(
            """
            {
              "name": "Sample Test",
              "description": "Auto-generated sample config.",
              "randomSeed": 42,
              "runMode": "RANDOM",
              "debug": true,
            
              "targetPackage": "com.example.placeholder",
              "defaultSlowThresholdMillis": 1500,
            
              "events": [
                {
                  "id": "noop_fast",
                  "description": "A fast NO-OP event",
                  "type": "NO_OP",
                  "mode": "RANDOM",
                  "weight": 5,
                  "enabled": true
                },
                {
                  "id": "noop_slow",
                  "description": "A slow NO-OP event for visibility",
                  "type": "NO_OP",
                  "mode": "RANDOM",
                  "enabled": true,
                  "weight": 1,
                  "slowThresholdMillis": 1000
                }
              ]
            }
            """.trimIndent()
        )

        println("🎉 Sample created: ${out.absolutePath}\n")
    }
}
