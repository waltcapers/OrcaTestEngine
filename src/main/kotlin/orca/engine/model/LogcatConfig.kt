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

package orca.engine.model

/**
 * Configuration options for logcat capture during a stress test run.
 *
 * This controls whether logcat recording is active, how it behaves across
 * device reboot cycles, and which tag (if any) should be filtered.
 *
 * The OrcaEngine uses these values to manage logcat lifecycle:
 * - When a run begins, logcat is started if `enabled` is true.
 * - During reboot recovery, logcat may be stopped and restarted if
 *   `rotateOnReboot` is enabled.
 * - If a `tag` is provided, logcat is filtered to only show output that
 *   matches the specified tag; otherwise, full logcat output is captured.
 *
 * @property enabled        True to enable logcat capture during stress runs.
 * @property rotateOnReboot If true, logcat capture is stopped before reboot
 *                          and restarted after the device comes back online.
 * @property tag            Optional tag used for filtering (e.g., package name);
 *                          if null, full logcat output is captured.
 */
data class LogcatConfig(
    val enabled: Boolean = true,
    val rotateOnReboot: Boolean = true,
    val tag: String? = null
)
