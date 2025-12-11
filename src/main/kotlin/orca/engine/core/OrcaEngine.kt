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

package orca.engine.core

import orca.cli.util.Ansi
import orca.engine.logging.DefaultLogcatManager
import orca.engine.logging.LogcatManager
import orca.engine.logging.LoggerProvider
import orca.engine.model.*
import orca.engine.util.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Core deterministic stress engine.
 *
 * Responsibilities:
 * - Selects and executes events based on weights, state, and retry policy.
 * - Tracks basic metrics and per-event execution stats.
 * - Monitors the target process and triggers replay-state saving on failure.
 * - Manages logcat capture lifecycle during runs and across reboot recovery.
 *
 * Design goals:
 * - Deterministic by default (seed + RNG call count).
 * - Easy to replay failures in a follow-up run.
 * - "Boring by design": failures are logged, not silently ignored.
 */
class OrcaEngine(
    private val config: OrcaTestConfig,
    private val systemInspector: SystemInspector,
    private val scriptRunner: ScriptRunner,
    private val logcat: LogcatManager = DefaultLogcatManager(),
    private val logger: EngineLogger,
    private val sleepProvider: SleepProvider = DefaultSleepProvider
) {

    init {
        LoggerProvider.set(logger)
    }
    /** Deterministic RNG seeded from config.randomSeed. */
    private var rng = Random(config.randomSeed)

    /** Convenience index of events by ID for sequences / triggers. */
    private val eventsById = config.events.associateBy { it.id }.toMutableMap()

    /** Per-event execution statistics (counts, durations, disabled flag). */
    private val eventStats = mutableMapOf<String, EventStats>()

    /** Engine-level state used for requireState/setState conditions. */
    private val state = mutableMapOf<String, String>()

    /** Per-event failure counts. */
    private val eventFailureCount = mutableMapOf<String, Int>()

    /** Log of failures (for summary printout). */
    private val failureLog = mutableListOf<FailureRecord>()

    /** Global metrics aggregated across events. */
    private val globalMetrics = mutableMapOf<String, MutableList<Double>>()

    /** Shared reader for interactive debug breakpoints. */
    private val debugReader by lazy { System.`in`.bufferedReader() }

    @Volatile
    private var running = true

    /** Whether we are currently breaking before each event (config-driven). */
    private var debugBreakActive: Boolean = config.debugBreakBeforeEvent == true

    // -------------------------------------------------------------------------
    // RNG tracking for deterministic replay
    // -------------------------------------------------------------------------

    /** Total number of RNG calls performed through nextRandomInt(). */
    private var rngCalls: Long = 0

    /** Flag indicating the engine is running in replay mode. */
    private var replayMode = false

    /** Skip re-running the last event once after a debug breakpoint */
    private var skipNextRetryForEvent: String? = null

    /**
     * Wrapper for RNG access that increments [rngCalls].
     *
     * Always use this method instead of rng.nextInt() directly to keep
     * deterministic replay accurate.
     */
    private fun nextRandomInt(bound: Int): Int {
        rngCalls++
        return rng.nextInt(bound)
    }

    /**
     * Load replay state (seed + RNG call count) from JSON file.
     *
     * @param path path to replay_state.json, defaults to `"replay_state.json"`.
     */
    fun loadReplayState(path: String = "replay_state.json"): ReplayState {
        return ReplayStateSerializer.loadReplayState(path)
    }

    /**
     * Save replay state (seed + RNG call count) to JSON file.
     *
     * @param path path to replay_state.json, defaults to `"replay_state.json"`.
     */
    fun saveReplayState(path: String = "replay_state.json") {
        ReplayStateSerializer.saveReplayState(
            ReplayState(
                seed = config.randomSeed,
                rngCalls = getRngCallCount()
            ),
            path
        )
    }

    /** @return total RNG calls consumed so far. */
    fun getRngCallCount(): Long = rngCalls

    /**
     * Fast-forwards the RNG by consuming the given number of random values.
     * Also clears internal engine state.
     *
     * This is mainly a low-level hook for advanced use or testing;
     * in normal flows, [replay] should be preferred.
     *
     * @param calls number of RNG calls to consume.
     */
    fun fastForwardRng(calls: Long) {
        repeat(calls.toInt()) { rng.nextInt() }
        rngCalls += calls
        resetReplayState()
    }

    /** Requests the engine to stop after the current loop iteration. */
    fun stop() {
        running = false
    }

    /**
     * Exposes a safe read-only copy of the engine's internal state map.
     * Used by the OrcaShellDebugger.
     */
    fun getStateSnapshot(): Map<String, String> {
        return state.toMap()
    }

    /**
     * runs an event by its id
     */
    fun runEventById(id: String): Boolean {
        val event = eventsById[id]
        if (event == null) {
            logger.error("runEventById: No event with ID '$id'.")
            return false
        }
        return executeEventWithPolicy(event)
    }

    /**
     * Executes a single event:
     * - Selects the next event based on weights / mode / cooldown.
     * - Applies policies, retries, and preconditions.
     *
     * @param profile optional profile key used for profileWeights.
     * @return true if the event ultimately succeeded, false otherwise.
     */
    fun runOnce(profile: String? = null): Boolean {
        val event = selectNextEvent(profile) ?: run {
            logger.warn("No eligible events to run.")
            return false
        }

        // All breakpoint logic is handled inside executeEventWithPolicy().
        return executeEventWithPolicy(event)
    }



    // -------------------------------------------------------------------------
    // Event selection
    // -------------------------------------------------------------------------

    /**
     * Selects the next RANDOM-mode event based on:
     * - enabled flag
     * - disabled flag in [EventStats]
     * - maxExecutions
     * - cooldownSeconds
     * - weight / profileWeights
     *
     * Events that are not in [EventMode.RANDOM] do not participate in this
     * selection; they are typically triggered explicitly or used in sequences.
     */
    private fun selectNextEvent(profile: String?): StressEvent? {
        val now = System.currentTimeMillis()

        var candidates = config.events.filter { event ->
            if (!event.enabled) return@filter false
            val stats = eventStats[event.id]

            // Skip if disabled
            if (stats?.disabled == true) return@filter false

            // Respect maxExecutions
            if (event.maxExecutions != null && (stats?.executions ?: 0) >= event.maxExecutions) {
                return@filter false
            }

            // Respect cooldown
            if (event.cooldownSeconds != null && stats?.lastExecutionTimeMillis != null) {
                val elapsedSec = (now - stats.lastExecutionTimeMillis!!) / 1000
                if (elapsedSec < event.cooldownSeconds) return@filter false
            }

            // Only RANDOM-mode events participate here
            event.mode == EventMode.RANDOM
        }

        if (candidates.isEmpty()) return null

        // --------------------------------------------------------
        // ⭐ NEW: Skip the LAST event if we stepped past a failure
        // --------------------------------------------------------
        if (skipNextRetryForEvent != null) {
            val lastEventId = skipNextRetryForEvent!!
            candidates = candidates.filter { it.id != lastEventId }
            debug("Step-mode: Excluding $lastEventId from next selection cycle.")
            skipNextRetryForEvent = null    // consume the skip
        }

        debug("Candidate events after filtering: ${candidates.map { it.id }}")

        val weighted = candidates.flatMap { event ->
            val weight = profile
                ?.let { event.profileWeights?.get(it) }
                ?: event.weight
            List(weight) { event }
        }

        if (weighted.isEmpty()) return null

        val selected = weighted[nextRandomInt(weighted.size)]
        debug("Selected event: ${selected.id}")
        LoggerProvider.get().event(selected,getStateSnapshot())
        return selected
    }


    /**
     * Clears all engine runtime state used for replay or for a fresh run.
     *
     * This does **not** change the RNG seed; see [setSeed] if you also need
     * to reset the randomness.
     */
    private fun resetReplayState() {
        state.clear()
        eventStats.clear()
        eventFailureCount.clear()
        failureLog.clear()
        globalMetrics.clear()
    }

    // -------------------------------------------------------------------------
    // Execution with policies
    // -------------------------------------------------------------------------

    /**
     * Executes an event respecting retry policy and onFailure behavior.
     *
     * Breakpoint semantics:
     *  - config.debugBreakBeforeEvent == true OR event.debugBreak == true
     *        → pause before first attempt.
     *  - config.debugBreakWhenPreconditionsFail == true
     *        → pause when preconditions/requireState fail.
     *  - config.debugBreakOnRetry == true
     *        → pause before each retry attempt.
     *  - config.debugBreakOnError == true
     *        → pause inside executeSingleAttempt() when an attempt fails.
     *
     * State semantics:
     *  - setState is applied in markSuccess()
     *  - clearState is applied here after the event ultimately succeeds.
     */
    private fun executeEventWithPolicy(event: StressEvent): Boolean {

        // -----------------------------------------------------------------
        // Global + per-event "break before execute"
        // -----------------------------------------------------------------
        if (config.debugBreakBeforeEvent == true || event.debugBreak == true) {
            debugBreakpoint("Before executing event ${event.id}", event)
            if (!running) return false
        }

        // -----------------------------------------------------------------
        // Preconditions
        // -----------------------------------------------------------------
        var preconditionReason: String? = null
        val preconditionsOk = checkPreconditions(event) { reason ->
            preconditionReason = reason
        }

        if (!preconditionsOk) {
            logger.info(
                "Preconditions not met for ${event.id}, skipping." +
                        (preconditionReason?.let { " ($it)" } ?: "")
            )

            if (config.debugBreakWhenPreconditionsFail == true) {
                debugBreakpoint(
                    "Preconditions failed for event ${event.id}: ${preconditionReason ?: "no details"}",
                    event
                )
                if (!running) return false
            }

            return false
        }

        // -----------------------------------------------------------------
        // Retry policy
        // -----------------------------------------------------------------
        val retry = event.retryPolicy ?: config.defaultRetry ?: RetryPolicy(maxAttempts = 1)
        var attempt = 0
        var success = false

        while (attempt < retry.maxAttempts && running) {
            attempt++

            val thisAttemptSuccess = executeSingleAttempt(event)
            success = thisAttemptSuccess

            if (thisAttemptSuccess) {
                // success: break out of retry loop
                break
            }

            // No retry configured → bail
            if (event.onFailure != FailurePolicy.RETRY) {
                break
            }

            // If we still have attempts left, handle backoff + optional breakpoint
            if (attempt < retry.maxAttempts) {
                val delaySec = when (retry.strategy) {
                    RetryStrategy.LINEAR -> retry.backoffSeconds
                    RetryStrategy.EXPONENTIAL -> retry.backoffSeconds * (1 shl (attempt - 1))
                }

                if (config.debugBreakOnRetry == true) {
                    debugBreakpoint(
                        "Retrying ${event.id} (attempt $attempt/${retry.maxAttempts})",
                        event
                    )
                    if (!running) return false
                }

                if (delaySec > 0) {
                    logger.info(
                        "Retrying ${event.id} in $delaySec seconds " +
                                "(attempt $attempt/${retry.maxAttempts})"
                    )
                    sleepProvider.sleep(delaySec * 1000L)
                }
            }
        }

        // -----------------------------------------------------------------
        // If we ultimately succeeded:
        //  - clearState (if any)
        //  - optional "after event" breakpoint
        // -----------------------------------------------------------------
        if (success) {
            // Apply clearState BEFORE we proceed to next event.
            // setState is applied in markSuccess(), which is called from executeSingleAttempt().
            if (event.clearState.isNotEmpty()) {
                for (key in event.clearState) {
                    state.remove(key)
                    debug("State cleared: $key")
                }
            }

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After executing event ${event.id}", event)
                if (!running) return false
            }

            return true
        }

        // -----------------------------------------------------------------
        // Failure handling (after exhausting attempts or not retrying)
        // -----------------------------------------------------------------

        when (event.onFailure) {
            FailurePolicy.STOP_TEST -> {
                logger.error("FailurePolicy.STOP_TEST triggered by ${event.id}")
                saveReplayState()
            }
            FailurePolicy.SKIP_FUTURE -> {
                logger.warn("Disabling event ${event.id} after failure.")
                eventStats.getOrPut(event.id) { EventStats() }.disabled = true
            }
            FailurePolicy.LOG_ONLY,
            FailurePolicy.RETRY -> {
                // No extra action here
            }
        }

        // stopOnFailure kills the run immediately
        if (event.stopOnFailure) {
            logger.error("stopOnFailure=true → stopping engine immediately due to failure of ${event.id}.")
            LoggerProvider.get().event(event,getStateSnapshot())
            saveReplayState()
            stop()
            return false
        }

        return false
    }




    /**
     * Executes a single attempt of an event (no retries).
     * Handles:
     * - SEQUENCE
     * - NO_OP
     * - WAIT_FOR_DEVICE
     * - SCRIPT execution via [ScriptRunner]
     * - reboot recovery and process monitoring
     *
     * 🔍 Metrics integration
     *
     * If the event defines a [MetricsConfig] (event.metrics != null), the engine:
     *  - captures a pre-execution metrics snapshot from [SystemInspector]
     *  - runs the script
     *  - captures a post-execution metrics snapshot
     *  - computes deltas (post - pre) where possible
     *  - merges the resulting metrics into [ScriptResult.metrics]
     *  - contributes them to [globalMetrics] for summary reporting
     */
    private fun executeSingleAttempt(event: StressEvent): Boolean {
        val stats = eventStats.getOrPut(event.id) { EventStats() }

        // ---------------------------------------------------------------------
        // 1. Handle SEQUENCE events (no script execution here)
        // ---------------------------------------------------------------------
        if (event.type == EventType.SEQUENCE) {
            val ok = executeSequence(event)

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After SEQUENCE event ${event.id} (success=$ok)", event)
                if (!running) return ok
            }

            return ok
        }


        // ---------------------------------------------------------------------
        // 2. Handle NO_OP events (instant success, no external interaction)
        // ---------------------------------------------------------------------
        if (event.type == EventType.NO_OP) {
            logger.info("NO-OP event ${event.id}: ${event.description}")

            stats.lastDurationMs = 0
            stats.totalDuration += 0
            stats.minDuration = minOf(stats.minDuration, 0)
            stats.maxDuration = maxOf(stats.maxDuration, 0)

            markSuccess(event, stats, emptyMap())
            debug("NO_OP event executed: ${event.id}")

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After NO_OP event ${event.id}", event)
                if (!running) return true
            }

            return true
        }



        // ---------------------------------------------------------------------
        // 3. Handle WAIT_FOR_DEVICE events (reboot recovery glue)
        // ---------------------------------------------------------------------
        if (event.type == EventType.WAIT_FOR_DEVICE) {
            systemInspector.awaitDeviceOnline()
            if (event.waitForBoot) systemInspector.awaitBootCompleted()

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After WAIT_FOR_DEVICE event ${event.id}", event)
                if (!running) return true
            }

            return true
        }


        // ---------------------------------------------------------------------
        // 4. SCRIPT events — metrics + execution timing live here
        // ---------------------------------------------------------------------
        logger.info("Executing event ${event.id} (${event.language})")

        // We will build up the ScriptResult in stages:
        //  - First by executing the script via ScriptRunner
        //  - Then by augmenting it with metrics captured by the SystemInspector
        lateinit var result: ScriptResult

        val metricsConfig = event.metrics

        // 4a. Pre-execution metrics snapshot (optional)
        val preMetrics: Map<String, Double> = if (metricsConfig != null) {
            debug("Capturing pre-execution metrics for event ${event.id}")
            systemInspector.captureMetrics(metricsConfig)
        } else {
            emptyMap()
        }

        // 4b. Measure the script execution duration
        val durationMillis = measureTimeMillis {
            result = scriptRunner.run(event)
        }

        stats.lastDurationMs = durationMillis
        stats.totalDuration += durationMillis
        if (durationMillis < stats.minDuration) stats.minDuration = durationMillis
        if (durationMillis > stats.maxDuration) stats.maxDuration = durationMillis

        logger.info("Event ${event.id} completed in ${durationMillis}ms (avg=${stats.averageDuration}ms)")

        // Warn if the event exceeded its slow threshold.
        val slowThreshold = event.slowThresholdMillis ?: config.defaultSlowThresholdMillis
        if (slowThreshold != null && durationMillis >= slowThreshold) {
            logger.warn(
                "⚠️ Event ${event.id} took ${durationMillis}ms which exceeds the slow threshold of ${slowThreshold}ms"
            )
        }

        // 4c. Post-execution metrics snapshot + delta computation (optional)
        if (metricsConfig != null) {
            debug("Capturing post-execution metrics for event ${event.id}")
            val postMetrics = systemInspector.captureMetrics(metricsConfig)

            val deltaMetrics = mutableMapOf<String, Double>()
            val allKeys = preMetrics.keys union postMetrics.keys

            for (key in allKeys) {
                val before = preMetrics[key]
                val after = postMetrics[key]

                val value = when {
                    before != null && after != null -> after - before
                    after != null -> after          // only post snapshot available
                    else -> before!!                // only pre snapshot available
                }

                deltaMetrics[key] = value
            }

            if (deltaMetrics.isNotEmpty()) {
                debug("Merging ${deltaMetrics.size} metric(s) into ScriptResult for event ${event.id}")

                val mergedMetrics = result.metrics.toMutableMap()
                for ((k, v) in deltaMetrics) {
                    if (!mergedMetrics.containsKey(k)) {
                        mergedMetrics[k] = v
                    }
                }

                result = result.copy(metrics = mergedMetrics)
            }
        }

        val success = result.exitCode == 0
        val metrics = result.metrics

        // 4d. Failure handling and logging
        if (!success) {
            stats.failures++
            eventFailureCount[event.id] = stats.failures

            failureLog += FailureRecord(event.id, result.exitCode, result.stderr)

            logger.warn("Event ${event.id} failed with exitCode=${result.exitCode}")
            if (event.logErrors && result.stderr.isNotBlank()) {
                logger.error("stderr for ${event.id}:\n${result.stderr}")
            }
        }
         else {
            if (event.logOutput && result.stdout.isNotBlank()) {
                logger.info("stdout for ${event.id}:\n${result.stdout}")
            }
        }

        if (!success && config.debugBreakOnError == true) {
            debugBreakpoint("Event ${event.id} failed with exitCode=${result.exitCode}", event)
            if (!running) return false
        }

        if (success) {
            markSuccess(event, stats, metrics)
            handlePostEvents(event)
            handleConditionalTriggers(event, result, metrics)
        }

        // 4e. Merge metrics into global summary
        metrics.forEach { (k, v) ->
            globalMetrics.getOrPut(k) { mutableListOf() }.add(v)
        }

        if (event.causesReboot) {
            logger.warn("Event ${event.id} initiated a reboot. Entering reboot recovery mode...")

            if (config.debugBreakOnReboot == true) {
                debugBreakpoint("Event ${event.id} is causing a reboot; about to enter reboot recovery.", event)
                if (!running) return false
            }

            handleRebootRecovery(event)

            // After recovery, consider the event successful if no fatal error.
            markSuccess(event, stats, metrics)

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After reboot recovery for event ${event.id}", event)
                if (!running) return true
            }

            if (config.debugBreakAfterEvent == true) {
                debugBreakpoint("After event ${event.id} (success=$success)", event)
                if (!running) return success
            }
            return success
        }

        // 6. Process monitoring — detect unexpected app death
        if (!event.processDeathAllowed && !event.causesReboot) {
            val target = config.targetPackage

            if (!systemInspector.isProcessRunning(target)) {
                logger.error("❌ Target process $target is NOT running after event ${event.id}")

                // Automatically capture replay state for deterministic reproduction.
                saveReplayState()

                return when (event.onFailure) {
                    FailurePolicy.STOP_TEST -> false
                    FailurePolicy.SKIP_FUTURE -> {
                        eventStats[event.id]?.disabled = true
                        false
                    }
                    FailurePolicy.RETRY -> false
                    FailurePolicy.LOG_ONLY -> false
                }
            }
        }

        return success
    }

    /** Helper for debug logging that respects config.debug. */
    private fun debug(msg: String) {
        if (config.debug) {
            logger.debug("$msg")
        }
    }

    /**
     * Handles reboot recovery:
     * - Optionally stops logcat before reboot (to avoid stale handles).
     * - Waits for device offline → online → boot completed (if requested).
     * - Optionally restarts target app.
     * - Optionally restarts logcat after reboot if rotateOnReboot is true.
     */
    private fun handleRebootRecovery(event: StressEvent) {

        val logcatEnabled = config.logcat?.enabled != false
        val rotateOnReboot = config.logcat?.rotateOnReboot != false

        // Stop logcat before we expect the device to go away, if rotation is enabled.
        if (logcatEnabled && rotateOnReboot) {
            logger.info("Stopping logcat capture before reboot...")
            logcat.stopCapture()
        }

        logger.info("Waiting for device to go offline...")
        systemInspector.awaitDeviceOffline()

        logger.info("Waiting for device to come back online...")
        systemInspector.awaitDeviceOnline()

        if (event.waitForBoot) {
            logger.info("Waiting for BOOT_COMPLETED...")
            systemInspector.awaitBootCompleted()
        }

        if (event.restartAppAfterBoot) {
            logger.info("Restarting target app ${config.targetPackage}...")
            systemInspector.startApp(config.targetPackage)
        }

        // Restart logcat after reboot, if configured to rotate
        if (logcatEnabled && rotateOnReboot) {
            val tagOrPackage = config.logcat?.tag ?: config.targetPackage
            logger.info("Restarting logcat capture after reboot for: $tagOrPackage")
            logcat.startCapture(tagOrPackage)
        }

        logger.info("Reboot recovery complete.")
    }

    /**
     * Executes a SEQUENCE event by resolving child IDs and running them
     * with normal policy handling. The parent event counts as one execution and
     * its [EventStats.lastDurationMs] equals the SUM of all child durations.
     *
     * Each child is executed through [executeEventWithPolicy], so:
     *  - retries and onFailure policies apply per child
     *  - metrics and triggers still function as usual
     */
    private fun executeSequence(event: StressEvent): Boolean {
        logger.info("Executing SEQUENCE ${event.id}: ${event.sequence}")

        var totalDuration: Long = 0
        var allSuccessful = true

        for (childId in event.sequence) {
            val child = eventsById[childId]
            if (child == null) {
                logger.warn("Sequence ${event.id} references unknown event $childId")
                continue
            }

            debug("Sequence ${event.id}: executing child event $childId")

            val start = System.currentTimeMillis()
            val success = executeEventWithPolicy(child)
            val end = System.currentTimeMillis()

            if (!success) allSuccessful = false

            // accumulate child duration (measured here rather than child stats)
            totalDuration += (end - start)
        }

        // Update parent stats
        val stats = eventStats.getOrPut(event.id) { EventStats() }
        stats.lastDurationMs = totalDuration
        stats.totalDuration += totalDuration
        stats.minDuration = minOf(stats.minDuration, totalDuration)
        stats.maxDuration = maxOf(stats.maxDuration, totalDuration)

        // parent event counts as an execution
        stats.executions++
        stats.lastExecutionTimeMillis = System.currentTimeMillis()

        return allSuccessful
    }

    /**
     * Marks a successful event:
     * - updates [EventStats.lastExecutionTimeMillis]
     * - increments [EventStats.executions]
     * - applies [StressEvent.setState] transitions
     */
    private fun markSuccess(event: StressEvent, stats: EventStats, metrics: Map<String, Double>) {
        stats.lastExecutionTimeMillis = System.currentTimeMillis()
        stats.executions++
        stats.successes++

        event.setState.forEach { (k, v) -> state[k] = v }

        // metrics already merged globally in executeSingleAttempt
    }

    // -------------------------------------------------------------------------
    // Preconditions & triggers
    // -------------------------------------------------------------------------

    /**
     * Evaluates state-based and system preconditions for the event.
     *
     * If any condition fails, the event is skipped and returns false from
     * [executeEventWithPolicy], but no failure is recorded against the event.
     */
    private fun checkPreconditions(event:StressEvent,
                                   onFailure: ((String) -> Unit)? = null
    ): Boolean {
        // State-based requirements
        for ((key, value) in event.requireState) {
            if (state[key] != value) {
                onFailure?.invoke("requireState[$key]=$value but actual=${state[key]}")
                logger.info("Event ${event.id} requires state[$key]=$value but got ${state[key]}")
                return false
            }
        }

        val p = event.preconditions ?: return true

        val battery = systemInspector.getBatteryLevel()
        if (p.batteryAbove != null && battery != null && battery < p.batteryAbove) {
            onFailure?.invoke("battery=$battery% < batteryAbove=${p.batteryAbove}%")
            return false
        }
        if (p.batteryBelow != null && battery != null && battery > p.batteryBelow) {
            onFailure?.invoke("battery=$battery% > batteryBelow=${p.batteryBelow}%")
            return false
        }

        if (p.networkRequired == true && systemInspector.isNetworkAvailable() == false) {
            onFailure?.invoke("networkRequired=true but networkAvailable=false")
            return false
        }
        if (p.deviceIdle == true && systemInspector.isDeviceIdle() == false) {
            onFailure?.invoke("deviceIdle=true but deviceIdle=false")
            return false
        }
        if (p.screenOn == true && systemInspector.isScreenOn() == false) {
            onFailure?.invoke("screenOn=true but screenOn=false")
            return false
        }
        if (p.chargingRequired == true && systemInspector.isCharging() == false) {
            onFailure?.invoke("chargingRequired=true but charging=false")
            return false
        }
        if (p.rootRequired == true && systemInspector.isRootAvailable() == false) {
            onFailure?.invoke("rootRequired=true but rootAvailable=false")
            return false
        }
        if (p.adbAvailable == true && systemInspector.adbAvailable() == false) {
            onFailure?.invoke("adbAvailable=true but adbAvailable=false")
            return false
        }

        for (path in p.fileMustExist) {
            if (!systemInspector.fileExists(path)) {
                onFailure?.invoke("file must exist but missing: $path")
                return false
            }
        }

        return true

    }

    /**
     * Executes [StressEvent.postEvents] after a successful event.
     *
     * Post events are executed with their own policies and preconditions and
     * can chain further post events if configured.
     */
    private fun handlePostEvents(event: StressEvent) {
        for (id in event.postEvents) {
            val e = eventsById[id]
            if (e == null) {
                logger.warn("postEvents: ${event.id} references unknown event $id")
                continue
            }
            debug("Post-event chain: ${event.id} → $id")
            logger.info("Triggering post event $id from ${event.id}")
            executeEventWithPolicy(e)
        }
    }

    /**
     * Evaluates and executes conditional triggers based on [ScriptResult] +
     * merged metrics.
     */
    private fun handleConditionalTriggers(
        event: StressEvent,
        result: ScriptResult,
        metrics: Map<String, Double>
    ) {
        debug("Evaluating conditional triggers for ${event.id}")
        for (ct in event.conditionalTriggers) {
            if (!shouldTrigger(ct, result, metrics)) continue

            val e = eventsById[ct.triggerEventId]
            if (e == null) {
                logger.warn("conditionalTriggers: ${event.id} references unknown event ${ct.triggerEventId}")
                continue
            }
            logger.info("Conditionally triggering ${ct.triggerEventId} from ${event.id}")
            executeEventWithPolicy(e)
        }
    }

    /**
     * Returns true if the given conditional trigger should fire based on output,
     * exitCode, or metric thresholds.
     */
    private fun shouldTrigger(
        ct: ConditionalTrigger,
        result: ScriptResult,
        metrics: Map<String, Double>
    ): Boolean {
        ct.ifOutputContains?.let {
            if (!result.stdout.contains(it) && !result.stderr.contains(it)) return false
        }
        ct.ifExitCodeNotZero?.let {
            if (it && result.exitCode == 0) return false
        }
        ct.ifExitCodeEquals?.let {
            if (result.exitCode != it) return false
        }
        ct.ifMetricAbove?.forEach { (name, threshold) ->
            val value = metrics[name] ?: return false
            if (value <= threshold) return false
        }
        ct.ifMetricBelow?.forEach { (name, threshold) ->
            val value = metrics[name] ?: return false
            if (value >= threshold) return false
        }
        debug("Conditional trigger matched: ${ct.triggerEventId}")
        return true
    }

    // -------------------------------------------------------------------------
    // Run modes
    // -------------------------------------------------------------------------

    /**
     * Continuous run loop (until [stop] is called).
     *
     * If logcat is enabled, capture is started before the loop and stopped
     * when the loop exits.
     */
    fun runLoop(delayMs: Long = 500, profile: String? = null) {
        running = true

        val logcatEnabled = config.logcat?.enabled != false
        val tagOrPackage = config.logcat?.tag ?: config.targetPackage

        if (logcatEnabled) {
            logger.info("Starting logcat capture for: $tagOrPackage")
            logcat.startCapture(tagOrPackage)
        }

        try {
            while (running) {
                runOnce(profile)
                sleepProvider.sleep(delayMs)
            }

            printSummary()
        } finally {
            if (logcatEnabled) {
                logger.info("Stopping logcat capture after runLoop.")
                logcat.stopCapture()
            }
        }
    }

    /**
     * Runs the engine for a fixed duration in seconds.
     *
     * Behavior:
     * - Starts logcat capture (if enabled) before the run.
     * - Starts the target app once at the beginning via [SystemInspector.startApp].
     * - Calls [runOnce] repeatedly until the time window expires.
     * - Logs but does not abort on individual event failures.
     * - Prints the summary at the end.
     * - Stops logcat capture in a finally block.
     *
     * **Safety net:**
     * If the time window elapses without any event reporting success
     * (i.e., [runOnce] never returned `true`), the engine will perform a
     * *single* best-effort execution of the first enabled SCRIPT event.
     *
     * This:
     * - Ensures tests and callers can rely on "at least one attempt" during
     *   very short runs or misconfigured weights.
     * - Still respects preconditions and failure policies during that
     *   fallback execution.
     */
    fun runForDuration(maxSeconds: Long, profile: String? = null) {
        val stopTime = System.currentTimeMillis() + maxSeconds * 1000L

        val logcatEnabled = config.logcat?.enabled != false
        val tagOrPackage = config.logcat?.tag ?: config.targetPackage

        if (logcatEnabled) {
            logger.info("Starting logcat capture for: $tagOrPackage")
            logcat.startCapture(tagOrPackage)
        }

        try {
            systemInspector.startApp(config.targetPackage)

            var executedAnySuccess = false

            while (System.currentTimeMillis() < stopTime && running) {
                val success = runOnce(profile)

                if (success) {
                    executedAnySuccess = true
                }

                if (!success) {
                    logger.warn("Last event failed.")
                }
            }

            if (!executedAnySuccess) {
                val fallback = config.events.firstOrNull { it.enabled && it.type == EventType.SCRIPT }
                if (fallback != null) {
                    logger.info(
                        "runForDuration: no successful events during ${maxSeconds}s; " +
                                "forcing one execution of ${fallback.id} for visibility."
                    )
                    executeEventWithPolicy(fallback)
                } else {
                    logger.warn(
                        "runForDuration: no enabled SCRIPT events available for fallback execution."
                    )
                }
            }

            logger.info("Stress test ended after ${maxSeconds}s")
            printSummary()
        } finally {
            if (logcatEnabled) {
                logger.info("Stopping logcat capture after runForDuration.")
                logcat.stopCapture()
            }
        }
    }


    /**
     * Runs the engine for a fixed number of iterations (runOnce calls).
     * Does not control logcat; you can wrap this externally if desired.
     */
    fun runForIterations(iterations: Long, profile: String? = null) {
        for (i in 0 until iterations) {
            if (!running) break

            val success = runOnce(profile)

            if (!success) {
                logger.warn("Stopping replay at iteration $i due to failure.")
                break
            }
        }
        printSummary()
    }


    /**
     * Resets the RNG with a new seed and clears RNG call count.
     */
    fun setSeed(seed: Long) {
        rng = Random(seed)
        debug("Random seed reset to $seed")
        rngCalls = 0
    }

    /**
     * Advances the RNG by the given number of calls (without tracking in [rngCalls]).
     * Use carefully; [nextRandomInt] is preferred for tracked calls.
     */
    fun consumeRngCalls(count: Long) {
        repeat(count.toInt()) { rng.nextInt() }
        debug("Random advanced by $count calls")
    }

    /**
     * Deterministic replay:
     * - Loads seed and RNG call count from replay_state.json.
     * - Replays events from the beginning until [rngCalls] matches.
     * - Executes one additional event (the one that originally failed).
     * - Does NOT use logcat (to avoid side effects during replay).
     */
    fun replay(profile: String? = null, path: String = "replay_state.json") {
        logger.info("[REPLAY] Starting deterministic replay…")

        // 1) Load saved seed + RNG call count
        val replay = loadReplayState(path)
        logger.info("[REPLAY] Loaded state: seed=${replay.seed}, rngCalls=${replay.rngCalls}")

        // 2) Reset RNG and engine state to *fresh* start
        setSeed(replay.seed)      // sets rng = Random(seed) and rngCalls = 0
        resetReplayState()        // clears state, eventStats, failures, metrics

        val targetCalls = replay.rngCalls

        logger.info("[REPLAY] Replaying until rngCalls reaches $targetCalls ...")

        replayMode = true

        // 3) Replay all events from the beginning until rngCalls == targetCalls
        while (rngCalls < targetCalls) {
            val ok = runOnce(profile)

            if (!ok) {
                logger.info("[REPLAY] (info) Event returned false during replay but continuing… (rngCalls=$rngCalls)")
            }
        }

        logger.info("[REPLAY] RNG call count matched original run (rngCalls=$rngCalls).")

        // 4) Execute ONE MORE event (the one that originally failed).
        logger.info("[REPLAY] Executing final failing event…")
        val finalSuccess = runOnce(profile)

        logger.info("[REPLAY] Replay complete. finalSuccess=$finalSuccess")
        printSummary()
    }

    // -------------------------------------------------------------------------
    // SUMMARY REPORTING
    // -------------------------------------------------------------------------

    /**
     * Prints an overall summary of the run:
     * - event-level execution stats
     * - failure log
     * - global metrics
     * - final state
     * - RNG usage
     */
    // -------------------------------------------------------------------------
// COLORIZED SUMMARY REPORTING (Drop-in Replacement)
// -------------------------------------------------------------------------

    fun printSummary() {
        val c = true // color enabled (can later be tied to config/CLI)

        println(
            Ansi.cyan(
                Ansi.bold("\n==================== STRESS TEST SUMMARY ===================="),
                c
            )
        )

        printEventSummary(c)
        printFailureSummary(c)
        printMetricsSummary(c)
        printFinalStateSummary(c)
        printRngSummary(c)

        println(
            Ansi.cyan(
                Ansi.bold("==============================================================\n"),
                c
            )
        )
    }

    private fun printEventSummary(color: Boolean) {
        println(Ansi.bold("\n----- Event Execution Summary -----", color))

        println(
            Ansi.cyan(
                String.format(
                    "%-50s %-10s %-10s %-10s %-20s",
                    "Event", "Execs", "Success", "Failure", "LastDuration"
                ),
                color
            )
        )

        println(
            Ansi.gray(
                "--------------------------------------------------------------------------------------------------",
                color
            )
        )

        config.events.forEach { event ->
            val stats = eventStats[event.id]
            val execs = stats?.executions ?: 0
            val successes = stats?.successes ?: 0
            val failures = stats?.failures ?: 0

            val duration = formatDuration(stats?.lastDurationMs)

            val eventCol = Ansi.magenta(event.id, color)
            val successCol = if (successes > 0) Ansi.green(successes.toString(), color)
            else Ansi.gray("0", color)
            val failureCol = if (failures > 0) Ansi.red(failures.toString(), color)
            else Ansi.gray("0", color)
            val durationCol = Ansi.blue(duration, color)

            println(
                String.format(
                    "%-50s %-10d %-10d %-10d %-20s",
                    event.id,
                    execs,
                    successes,
                    failures,
                    duration
                )
            )
        }
    }

    private fun printFailureSummary(color: Boolean) {
        if (failureLog.isEmpty()) {
            println(Ansi.green("\nNo failures recorded.", color))
            return
        }

        println(Ansi.bold("\n----- Failure Details -----", color))

        failureLog.forEach { failure ->
            println(
                "${Ansi.red("✗", color)} " +
                        "${Ansi.magenta(failure.eventId, color)} " +
                        "(exit=${Ansi.red(failure.exitCode.toString(), color)})"
            )

            if (failure.stderr.isNotBlank()) {
                println(Ansi.yellow("stderr:", color))
                println(Ansi.gray(failure.stderr.trim(), color))
            }
            println()
        }
    }

    private fun printMetricsSummary(color: Boolean) {
        if (globalMetrics.isEmpty()) {
            println(Ansi.gray("\n(No global metrics were recorded)", color))
            return
        }

        println(Ansi.bold("\n----- Metrics Summary -----", color))

        globalMetrics.forEach { (metric, values) ->
            val avg = values.average()
            println(
                "${Ansi.cyan(metric, color)} avg = ${Ansi.green(avg.toString(), color)}"
            )
        }
    }

    private fun printFinalStateSummary(color: Boolean) {
        if (state.isEmpty()) {
            println(Ansi.gray("\n(No final state variables were set.)", color))
            return
        }

        println(Ansi.bold("\n----- Final State -----", color))

        state.forEach { (k, v) ->
            println("${Ansi.magenta(k, color)} = ${Ansi.blue(v, color)}")
        }
    }

    private fun printRngSummary(color: Boolean) {
        println(Ansi.bold("\n----- RNG Tracking -----", color))

        println("${Ansi.cyan("Seed:", color)} ${config.randomSeed}")
        println("${Ansi.cyan("Random calls consumed:", color)} $rngCalls")
    }

    /**
     * Compact duration formatter (ms → "Hh Mm Ss ms").
     */
    private fun formatDuration(ms: Long?): String {
        if (ms == null || ms < 0) return "-"

        val hours = ms / (1000 * 60 * 60)
        val minutes = (ms / (1000 * 60)) % 60
        val seconds = (ms / 1000) % 60
        val millis = ms % 1000

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (hours > 0 || minutes > 0) append("${minutes}m ")
            append("${seconds}s ${millis}ms")
        }
    }

    /**
     * Interactive step-mode prompt shown when debugBreakBeforeEvent is enabled.
     *
     * Option B behavior:
     *  - We already selected the event.
     *  - We display info about the event.
     *  - We wait for a command:
     *      [Enter]/s/step → execute this event
     *      c/cont/continue → run this event AND turn off further breaks
     *      q/quit/exit → request engine stop
     */
    private fun promptBeforeEvent(event: StressEvent): Boolean {

        if (event.debugBreak == true && !debugBreakActive) {
            println("⛔ Event-level breakpoint triggered (debugBreak = true)")
        } else if (debugBreakActive) {
            println("⛔ Global step mode active (debugBreakBeforeEvent = true)")
        }
        println()
        println("───────────────────── STEP BREAK ─────────────────────")
        println("Next event: ${event.id}")
        println("  type : ${event.type}")
        println("  mode : ${event.mode}")
        if (!event.description.isNullOrBlank()) {
            println("  desc : ${event.description}")
        }
        if (!event.tags.isNullOrEmpty()) {
            println("  tags : ${event.tags.joinToString(", ")}")
        }
        if (event.safetyLevel != null) {
            println("  safety : ${event.safetyLevel}")
        }
        println("Commands: [Enter]/s/step = run, c = continue (no more breaks), q = quit")
        print("step> ")

        val line = readLine()?.trim().orEmpty()

        return when (line.lowercase()) {
            "", "s", "step" -> {
                // Run this event, keep breaking before future events
                true
            }

            "c", "cont", "continue" -> {
                println("Continuing without further breaks for this run.")
                debugBreakActive = false
                true
            }

            "q", "quit", "exit" -> {
                println("Stopping engine by user request.")
                running = false
                false
            }

            else -> {
                println("Unrecognized command '$line'. Running event.")
                true
            }
        }
    }

    /**
     * Interactive debug breakpoint.
     *
     * Shows why we stopped and lets the user:
     *  - [Enter]: continue
     *  - i: inspect event + state
     *  - q: stop engine (sets [running] = false)
     */
    private fun debugBreakpoint(reason: String, event: StressEvent) {
        println()
        println("──────────────── ORCA DEBUG BREAKPOINT ────────────────")
        println("Reason : $reason")
        println("Event  : ${event.id} (type=${event.type}, mode=${event.mode})")
        println("Execs  : ${eventStats[event.id]?.executions ?: 0}")
        println("State  : $state")
        println("-------------------------------------------------------")
        println("Commands: [Enter]=continue, i=inspect event/state, q=quit engine")

        while (true) {
            print("debug> ")
            val line = debugReader.readLine() ?: return
            when (line.trim().lowercase()) {
                "" -> return                   // continue execution
                "i", "info" -> {
                    println("\n--- Event definition ---")
                    println(event)
                    println("\n--- Engine state ---")
                    println("state       = $state")
                    println("eventStats  = ${eventStats[event.id]}")
                    println("failures    = ${eventFailureCount[event.id] ?: 0}")
                    println("-------------------------")
                }
                "q", "quit", "exit" -> {
                    println("Debug: marking engine as stopped by user request.")
                    running = false
                    return
                }
                else -> println("Unknown command. Use Enter, i or q.")
            }
        }
    }


}
