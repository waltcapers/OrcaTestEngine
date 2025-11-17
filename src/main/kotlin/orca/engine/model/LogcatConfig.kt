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
