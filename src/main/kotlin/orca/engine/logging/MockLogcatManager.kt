/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 *
 * Copyright (c) 2025 Walter E. Capers
 */

package orca.engine.logging

/**
 * Mock implementation of [LogcatManager] that never calls adb.
 *
 * It simply logs when capture is started/stopped/rotated.
 * This is ideal for mock/dry-run mode.
 */
class MockLogcatManager(
    private val debug: Boolean = true
) : LogcatManager {

    private fun dbg(msg: String) {
        if (debug) println("[MockLogcatManager] $msg")
    }

    override fun startCapture(tag: String?) {
        dbg("startCapture(tag=$tag) → NO-OP in mock")
    }

    override fun stopCapture() {
        dbg("stopCapture() → NO-OP in mock")
    }

    override fun rotate(tag: String?) {
        dbg("rotate(tag=$tag) → NO-OP in mock")
    }
}
