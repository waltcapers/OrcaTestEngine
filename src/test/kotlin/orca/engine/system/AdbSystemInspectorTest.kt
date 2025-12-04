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
 * in the Software without restriction...
 * (license continued)
 */

package orca.engine.system

import orca.engine.core.EngineLogger
import orca.engine.model.MetricsConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AdbSystemInspector] using [FakeAdbExecutor].
 *
 * These tests verify:
 *  - Parsing logic (battery level, CPU %, memory info)
 *  - Boolean heuristics (charging, network, idle, screen on)
 *  - File existence checks
 *  - Process running detection
 *
 * NO real ADB is required. All adb.shell() and adb.exec() calls
 * are intercepted by FakeAdbExecutor.
 */
class AdbSystemInspectorTest {

    /**
     * Helper to build an inspector with canned shell + exec behavior.
     *
     * @param shellMap Map<String, AdbResult>
     *     Keys = EXACT shell command strings.
     *     Values = results returned by FakeAdbExecutor.
     *
     * @param execMap Map<List<String>, AdbResult>
     *     Keys = EXACT exec argument lists.
     */
    private fun inspector(
        shellMap: Map<String, AdbResult>,
        execMap: Map<List<String>, AdbResult> = emptyMap(),
        logger: EngineLogger
    ): AdbSystemInspector {

        val fake = FakeAdbExecutor(
            cannedExec = { args ->
                execMap[args] ?: AdbResult(
                    exitCode = 0,
                    stdout = "",
                    stderr = ""
                )
            },
            cannedShell = { cmd ->
                shellMap[cmd] ?: AdbResult(
                    exitCode = 0,
                    stdout = "",
                    stderr = ""
                )
            }
        )

        return AdbSystemInspector(
            adb = fake,
            defaultPackageName = "com.example.test",
            debug = false,
            logger = logger
        )
    }

    // ---------------------------------------------------------------------
    // BATTERY LEVEL TEST
    // ---------------------------------------------------------------------

    @Test
    fun `getBatteryLevel parses from dumpsys battery`() {
        val shellMap = mapOf(
            "dumpsys battery" to AdbResult(
                0,
                """
                    level: 87
                    AC powered: false
                """.trimIndent(),
                ""
            )
        )

        val ins = inspector(shellMap)

        assertEquals(87, ins.getBatteryLevel())
    }

    @Test
    fun `getBatteryLevel falls back to sysfs value`() {
        val shellMap = mapOf(
            "dumpsys battery" to AdbResult(0, "", ""), // missing
            "cat /sys/class/power_supply/battery/capacity" to AdbResult(
                0, "65", ""
            )
        )

        val ins = inspector(shellMap)

        assertEquals(65, ins.getBatteryLevel())
    }

    // ---------------------------------------------------------------------
    // NETWORK CONNECTIVITY
    // ---------------------------------------------------------------------

    @Test
    fun `isNetworkAvailable detects CONNECTED state`() {
        val shellMap = mapOf(
            "dumpsys connectivity" to AdbResult(
                0,
                """
                    NetworkInfo:
                      state: CONNECTED
                """.trimIndent(),
                ""
            )
        )

        val ins = inspector(shellMap)
        assertTrue(ins.isNetworkAvailable()!!)
    }

    @Test
    fun `isNetworkAvailable returns null on error`() {
        val shellMap = mapOf(
            "dumpsys connectivity" to AdbResult(1, "", "")
        )

        val ins = inspector(shellMap)
        assertNull(ins.isNetworkAvailable())
    }

    // ---------------------------------------------------------------------
    // SCREEN STATE
    // ---------------------------------------------------------------------

    @Test
    fun `isScreenOn detects ON state`() {
        val shellMap = mapOf(
            "dumpsys power" to AdbResult(
                0,
                "Display Power: state=ON",
                ""
            )
        )

        val ins = inspector(shellMap)
        assertTrue(ins.isScreenOn()!!)
    }

    @Test
    fun `isScreenOn detects OFF state`() {
        val shellMap = mapOf(
            "dumpsys power" to AdbResult(
                0,
                "Display Power: state=OFF",
                ""
            )
        )

        val ins = inspector(shellMap)
        assertFalse(ins.isScreenOn()!!)
    }

    // ---------------------------------------------------------------------
    // CHARGING STATE
    // ---------------------------------------------------------------------

    @Test
    fun `isCharging detects AC or USB true`() {
        val shellMap = mapOf(
            "dumpsys battery" to AdbResult(
                0,
                """
                    AC powered: true
                    USB powered: false
                """.trimIndent(),
                ""
            )
        )

        val ins = inspector(shellMap)
        assertTrue(ins.isCharging()!!)
    }

    // ---------------------------------------------------------------------
    // PROCESS RUNNING
    // ---------------------------------------------------------------------

    @Test
    fun `isProcessRunning true when pidof returns pid`() {
        val shellMap = mapOf(
            "pidof com.example.test" to AdbResult(0, "1234", "")
        )

        val ins = inspector(shellMap)
        assertTrue(ins.isProcessRunning("com.example.test"))
    }

    @Test
    fun `isProcessRunning false on empty pid`() {
        val shellMap = mapOf(
            "pidof com.example.test" to AdbResult(0, "", "")
        )

        val ins = inspector(shellMap)
        assertFalse(ins.isProcessRunning("com.example.test"))
    }

    // ---------------------------------------------------------------------
    // FILE CHECKS
    // ---------------------------------------------------------------------

    @Test
    fun `fileExists true when ls returns exitCode 0`() {
        val shellMap = mapOf(
            "ls \"test.txt\"" to AdbResult(0, "", "")
        )

        val ins = inspector(shellMap)

        assertTrue(ins.fileExists("test.txt"))
    }

    @Test
    fun `fileExists false on error`() {
        val shellMap = mapOf(
            "ls \"missing.txt\"" to AdbResult(1, "", "")
        )

        val ins = inspector(shellMap)
        assertFalse(ins.fileExists("missing.txt"))
    }

    // ---------------------------------------------------------------------
    // METRICS — CPU, MEMORY, BATTERY
    // ---------------------------------------------------------------------

    @Test
    fun `captureMetrics collects CPU percent`() {
        val shellMap = mapOf(
            "top -n 1 -b" to AdbResult(
                0,
                "CPU usage from 23% user, 5% system, 72% idle",
                ""
            )
        )

        val ins = inspector(shellMap)

        val result = ins.captureMetrics(
            MetricsConfig(captureCpuUsage = true)
        )

        assertEquals(28.0, result["cpu.totalPercent"])
    }

    @Test
    fun `captureMetrics collects memory usage`() {
        val shellMap = mapOf(
            "cat /proc/meminfo" to AdbResult(
                0,
                """
                    MemTotal: 100000 kB
                    MemAvailable: 25000 kB
                """.trimIndent(),
                ""
            )
        )

        val ins = inspector(shellMap)

        val result = ins.captureMetrics(
            MetricsConfig(captureMemoryUsage = true)
        )

        assertEquals(75.0, result["mem.usedMb"]!!, 0.1)
        assertEquals(100.0, result["mem.totalMb"]!!, 0.1)
    }

    @Test
    fun `captureMetrics collects battery level`() {
        val shellMap = mapOf(
            "dumpsys battery" to AdbResult(
                0,
                "level: 51",
                ""
            )
        )

        val ins = inspector(shellMap)

        val result = ins.captureMetrics(
            MetricsConfig(captureBatteryLevel = true)
        )

        assertEquals(51.0, result["battery.levelPercent"])
    }
}
