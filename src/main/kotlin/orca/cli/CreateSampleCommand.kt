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
