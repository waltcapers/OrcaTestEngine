package orca.engine.core

import java.util.Collections.emptyMap

/**
 * Represents the outcome of executing a script inside the Stress Engine.
 *
 * A [ScriptResult] is produced by all implementations of [ScriptHandler] and
 * passed back to the executing [StressEvent] for evaluation. It contains:
 *
 * - **exitCode** — the process exit status (0 = success; non-zero = failure)
 * - **stdout** — standard output text captured from the script
 * - **stderr** — standard error text captured from the script
 * - **metrics** — optional numeric values collected by the script, if any
 *
 * ### Metrics Use Case
 * Metrics allow scripts to push custom performance or diagnostic values
 * back into the engine—for example:
 *
 * ```json
 * { "cpuTimeMs": 42.7, "memoryKB": 1536 }
 * ```
 *
 * These metrics are aggregated globally by [OrcaEngine] and can be used as:
 * - pass/fail conditions in **conditional triggers**
 * - data for summary reports
 * - fine-grained performance monitoring
 *
 * ### Error Handling Behavior
 * If a script times out, is missing, or cannot be executed, the exit code
 * should be non-zero and `stderr` should describe the reason.
 *
 * @property exitCode   the exit status of the process (0 = success, non-zero = failure)
 * @property stdout     captured standard output text (may be empty)
 * @property stderr     captured standard error text (may be empty)
 * @property metrics    optional map of numeric metrics reported by the script;
 *                      defaults to an empty immutable map
 *
 * @see ScriptHandler
 * @see OrcaEngine
 */
data class ScriptResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val metrics: Map<String, Double> = emptyMap()
)
