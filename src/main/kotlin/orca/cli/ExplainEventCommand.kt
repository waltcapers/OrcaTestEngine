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

import orca.engine.config.OrcaConfigLoader
import orca.engine.model.StressEvent
import java.io.File

/**
 * Implements:
 *
 *   orca explain-event <config.json> <eventId>
 *
 * Behavior:
 *   - Loads config.
 *   - Locates the specified event by ID.
 *   - Prints a detailed explanation of that event's configuration:
 *       * Type, mode, weights
 *       * Preconditions
 *       * Retry policy and failure handling
 *       * Sequence, post events, conditional triggers
 *       * Metrics, logging flags, reboot flags
 */
object ExplainEventCommand {

    fun run(configPath: String, eventId: String) {
        val file = File(configPath)
        if (!file.exists()) {
            println("❌ Config file not found: ${file.absolutePath}")
            return
        }

        val config = try {
            OrcaConfigLoader.load(configPath)
        } catch (ex: Exception) {
            println("❌ Failed to load config: ${ex.message}")
            ex.printStackTrace()
            return
        }

        val event = config.events.find { it.id == eventId }
        if (event == null) {
            println("❌ Event not found: $eventId")
            return
        }

        printDetailed(event)
    }

    fun printDetailed(event: StressEvent) {
        println("=== Event Detail: ${event.id} ===")
        println("Description: ${event.description ?: "(none)"}")
        println("Type:        ${event.type}")
        println("Mode:        ${event.mode}")
        println("Weight:      ${event.weight}")
        println("Tags:        ${if (event.tags.isEmpty()) "(none)" else event.tags.joinToString()}")
        println("Safety:      ${event.safetyLevel}")
        println("Enabled:     ${event.enabled}")
        println()

        println("Timing / Limits:")
        println("  cooldownSeconds      = ${event.cooldownSeconds}")
        println("  maxExecutions        = ${event.maxExecutions}")
        println("  timeoutSeconds       = ${event.timeoutSeconds}")
        println("  durationSeconds      = ${event.durationSeconds}")
        println("  slowThresholdMillis  = ${event.slowThresholdMillis}")
        println()

        println("Failure handling:")
        println("  onFailure            = ${event.onFailure}")
        println("  retryPolicy          = ${event.retryPolicy}")
        println()

        println("State machine:")
        println("  requireState         = ${if (event.requireState.isEmpty()) "(none)" else event.requireState}")
        println("  setState             = ${if (event.setState.isEmpty()) "(none)" else event.setState}")
        println()

        println("Preconditions:")
        if (event.preconditions != null) {
            val p = event.preconditions
            println("  batteryAbove         = ${p.batteryAbove}")
            println("  batteryBelow         = ${p.batteryBelow}")
            println("  networkRequired      = ${p.networkRequired}")
            println("  deviceIdle           = ${p.deviceIdle}")
            println("  screenOn             = ${p.screenOn}")
            println("  chargingRequired     = ${p.chargingRequired}")
            println("  rootRequired         = ${p.rootRequired}")
            println("  adbAvailable         = ${p.adbAvailable}")
            println("  fileMustExist        = ${if (p.fileMustExist.isEmpty()) "(none)" else p.fileMustExist.joinToString()}")
        } else {
            println("  (none)")
        }
        println()

        println("Script / Sequence / Triggers:")
        println("  language             = ${event.language}")
        println("  script               = ${event.script}")
        println("  sequence             = ${if (event.sequence.isEmpty()) "(none)" else event.sequence.joinToString()}")
        println("  postEvents           = ${if (event.postEvents.isEmpty()) "(none)" else event.postEvents.joinToString()}")
        if (event.conditionalTriggers.isEmpty()) {
            println("  conditionalTriggers  = (none)")
        } else {
            println("  conditionalTriggers:")
            event.conditionalTriggers.forEach { ct ->
                println("    - triggerEventId      = ${ct.triggerEventId}")
                println("      ifOutputContains    = ${ct.ifOutputContains}")
                println("      ifExitCodeNotZero   = ${ct.ifExitCodeNotZero}")
                println("      ifExitCodeEquals    = ${ct.ifExitCodeEquals}")
                println("      ifMetricAbove       = ${ct.ifMetricAbove}")
                println("      ifMetricBelow       = ${ct.ifMetricBelow}")
            }
        }
        println()

        println("Metrics & Logging:")
        println("  metrics              = ${event.metrics}")
        println("  logOutput            = ${event.logOutput}")
        println("  logErrors            = ${event.logErrors}")
        println("  logFile              = ${event.logFile}")
        println()

        println("Reboot-related flags:")
        println("  causesReboot         = ${event.causesReboot}")
        println("  waitForBoot          = ${event.waitForBoot}")
        println("  restartAppAfterBoot  = ${event.restartAppAfterBoot}")
        println("  processDeathAllowed  = ${event.processDeathAllowed}")
    }
}
