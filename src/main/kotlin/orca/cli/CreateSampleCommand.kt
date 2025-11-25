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
