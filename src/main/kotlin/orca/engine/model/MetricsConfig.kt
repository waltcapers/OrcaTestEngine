package orca.engine.model

/**
 * Configuration for collecting runtime system metrics during the execution
 * of stress events.
 *
 * This configuration determines which metrics the engine should request from
 * the [SystemInspector] during event execution. Collected metrics are attached
 * to each event’s [ScriptResult] and later aggregated into global summaries.
 *
 * Built-in metrics supported by the engine include:
 *  - CPU usage
 *  - Memory usage
 *  - Battery level
 *
 * In addition, callers may specify arbitrary metric names using
 * `customMetrics`. These may correspond to custom hooks implemented by the
 * [SystemInspector] for advanced monitoring.
 *
 * @property captureCpuUsage       Whether CPU usage should be measured and
 *                                 included in per-event metrics.
 * @property captureMemoryUsage    Whether memory usage should be measured and
 *                                 included in per-event metrics.
 * @property captureBatteryLevel   Whether battery level should be captured as
 *                                 part of per-event metrics.
 * @property customMetrics         A list of custom metric identifiers that
 *                                 the engine should request from the
 *                                 [SystemInspector]. These hooks are optional
 *                                 and user-defined.
 */
data class MetricsConfig(
    val captureCpuUsage: Boolean = false,
    val captureMemoryUsage: Boolean = false,
    val captureBatteryLevel: Boolean = false,
    val customMetrics: List<String> = emptyList()
)
