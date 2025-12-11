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
 *   orca dry-run <config.json>
 *
 * Behavior:
 *   - Loads the config.
 *   - Does NOT execute any events.
 *   - Prints a human-readable summary of what the engine would do:
 *       * Event IDs, types, modes, weights, safety level.
 *       * Basic state preconditions.
 *       * Flags like causesReboot, processDeathAllowed, etc.
 *
 * This is useful for:
 *   - Verifying that JSON was parsed as expected.
 *   - Quickly inspecting test structure without connecting to a device.
 */
object DryRunCommand {

    fun run(configPath: String) {
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

        println("=== DRY RUN: ${config.name ?: "Unnamed Test"} ===")
        println("Description: ${config.description ?: "(none)"}")
        println("Random seed: ${config.randomSeed}")
        println("Run mode:   ${config.runMode}")
        println("Target app: ${config.targetPackage ?: "(none)"}")
        println("Events:     ${config.events.size}")
        println()

        config.events.forEach { event ->
            printEventSummary(event)
        }
    }

    /**
     * Prints a compact, but detailed, summary of a single StressEvent.
     */
    private fun printEventSummary(event: StressEvent) {
        println("------------------------------------------------------------")
        println("ID:          ${event.id}")
        println("Description: ${event.description ?: "(none)"}")
        println("Type/Mode:   ${event.type} / ${event.mode}")
        println("Weight:      ${event.weight}")
        println("Tags:        ${if (event.tags.isEmpty()) "(none)" else event.tags.joinToString()}")
        println("Safety:      ${event.safetyLevel}")
        println("Enabled:     ${event.enabled}")

        if (event.causesReboot || event.waitForBoot || event.restartAppAfterBoot) {
            println("Reboot flags:")
            println("  causesReboot        = ${event.causesReboot}")
            println("  waitForBoot         = ${event.waitForBoot}")
            println("  restartAppAfterBoot = ${event.restartAppAfterBoot}")
        }

        if (event.preconditions != null) {
            val p = event.preconditions
            println("Preconditions:")
            println("  batteryAbove     = ${p.batteryAbove}")
            println("  batteryBelow     = ${p.batteryBelow}")
            println("  networkRequired  = ${p.networkRequired}")
            println("  deviceIdle       = ${p.deviceIdle}")
            println("  screenOn         = ${p.screenOn}")
            println("  chargingRequired = ${p.chargingRequired}")
            println("  rootRequired     = ${p.rootRequired}")
            println("  adbAvailable     = ${p.adbAvailable}")
            println("  fileMustExist    = ${if (p.fileMustExist.isEmpty()) "(none)" else p.fileMustExist.joinToString()}")
        }

        if (event.requireState.isNotEmpty()) {
            println("Require state: ${event.requireState}")
        }
        if (event.setState.isNotEmpty()) {
            println("Set state:     ${event.setState}")
        }

        if (event.sequence.isNotEmpty()) {
            println("Sequence:      ${event.sequence.joinToString()}")
        }

        if (event.postEvents.isNotEmpty()) {
            println("Post events:   ${event.postEvents.joinToString()}")
        }

        if (event.conditionalTriggers.isNotEmpty()) {
            println("Conditional triggers:")
            event.conditionalTriggers.forEach { ct ->
                println("  → ${ct.triggerEventId} (ifOutputContains=${ct.ifOutputContains}, ifExitCodeNotZero=${ct.ifExitCodeNotZero}, ifExitCodeEquals=${ct.ifExitCodeEquals})")
            }
        }

        println()
    }
}
