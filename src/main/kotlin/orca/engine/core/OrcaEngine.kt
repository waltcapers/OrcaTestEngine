package orca.engine.core

import orca.engine.logging.DefaultLogcatManager
import orca.engine.logging.LogcatManager
import orca.engine.model.*
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
 */
class OrcaEngine(
    private val config: OrcaTestConfig,
    private val systemInspector: SystemInspector,
    private val scriptRunner: ScriptRunner,
    private val logger: EngineLogger,
    private val logcat: LogcatManager = DefaultLogcatManager()
) {

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

    @Volatile
    private var running = true

    // -------------------------------------------------------------------------
    // RNG tracking for deterministic replay
    // -------------------------------------------------------------------------

    /** Total number of RNG calls performed through nextRandomInt(). */
    private var rngCalls: Long = 0

    /** Flag indicating the engine is running in replay mode. */
    private var replayMode = false

    /**
     * Wrapper for RNG access that increments rngCalls.
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
     * @param path path to replay_state.json, defaults to "replay_state.json".
     */
    fun loadReplayState(path: String = "replay_state.json"): ReplayState {
        return ReplayStateSerializer.loadReplayState(path)
    }

    /**
     * Save replay state (seed + RNG call count) to JSON file.
     *
     * @param path path to replay_state.json, defaults to "replay_state.json".
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
        return executeEventWithPolicy(event)
    }

    // -------------------------------------------------------------------------
    // Event selection
    // -------------------------------------------------------------------------

    /**
     * Selects the next RANDOM-mode event based on:
     * - enabled flag
     * - disabled flag in EventStats
     * - maxExecutions
     * - cooldownSeconds
     * - weight / profileWeights
     */
    private fun selectNextEvent(profile: String?): StressEvent? {
        val now = System.currentTimeMillis()

        val candidates = config.events.filter { event ->
            if (!event.enabled) return@filter false
            val stats = eventStats[event.id]

            // Skip if disabled
            if (stats?.disabled == true) return@filter false

            // Respect maxExecutions
            if (event.maxExecutions != null && (stats?.executions ?: 0) >= event.maxExecutions) {
                return@filter false
            }

            // Respect cooldown (based on timestamp, not duration)
            if (event.cooldownSeconds != null && stats?.lastExecutionTimeMillis != null) {
                val elapsedSec = (now - stats.lastExecutionTimeMillis!!) / 1000
                if (elapsedSec < event.cooldownSeconds) return@filter false
            }

            // Only RANDOM-mode events participate here
            event.mode == EventMode.RANDOM
        }

        if (candidates.isEmpty()) return null

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
        return selected
    }

    /**
     * Clears all engine runtime state used for replay or for a fresh run.
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
     * @return true if the event succeeded within its retry policy.
     */
    private fun executeEventWithPolicy(event: StressEvent): Boolean {
        if (!checkPreconditions(event)) {
            logger.info("Preconditions not met for ${event.id}, skipping.")
            return false
        }

        val retry = event.retryPolicy ?: config.defaultRetry ?: RetryPolicy(maxAttempts = 1)
        var attempt = 0

        while (attempt < retry.maxAttempts) {
            attempt++
            val success = executeSingleAttempt(event)

            if (success) {
                return true
            }

            if (event.onFailure != FailurePolicy.RETRY) break

            if (attempt < retry.maxAttempts) {
                val delaySec = when (retry.strategy) {
                    RetryStrategy.LINEAR -> retry.backoffSeconds
                    RetryStrategy.EXPONENTIAL -> retry.backoffSeconds * (1 shl (attempt - 1))
                }
                if (delaySec > 0) {
                    logger.info("Retrying ${event.id} in $delaySec seconds (attempt $attempt/${retry.maxAttempts})")
                    Thread.sleep(delaySec * 1000L)
                }
            }
        }

        when (event.onFailure) {
            FailurePolicy.STOP_TEST -> {
                logger.error("FailurePolicy.STOP_TEST triggered by ${event.id}")
                // Capture replay state so the failure can be reproduced deterministically.
                saveReplayState()
            }
            FailurePolicy.SKIP_FUTURE -> {
                logger.warn("Disabling event ${event.id} after failure.")
                eventStats.getOrPut(event.id) { EventStats() }.disabled = true
            }
            FailurePolicy.LOG_ONLY, FailurePolicy.RETRY -> {
                // Nothing extra; failure already logged.
            }
        }

        return false
    }

    /**
     * Executes a single attempt of an event (no retries).
     * Handles:
     * - SEQUENCE
     * - NO_OP
     * - WAIT_FOR_DEVICE
     * - SCRIPT execution via ScriptRunner
     * - reboot recovery and process monitoring
     *
     * 🔍 NEW: Metrics integration
     * If the event defines a MetricsConfig (event.metrics != null), the engine:
     *  - captures a pre-execution metrics snapshot from SystemInspector
     *  - runs the script
     *  - captures a post-execution metrics snapshot
     *  - computes deltas (post - pre) where possible
     *  - merges the resulting metrics into ScriptResult.metrics
     *  - contributes them to globalMetrics for summary reporting
     */
    private fun executeSingleAttempt(event: StressEvent): Boolean {
        val stats = eventStats.getOrPut(event.id) { EventStats() }

        // -------------------------------------------------------------------------
        // 1. Handle SEQUENCE events (no script execution here)
        // -------------------------------------------------------------------------
        if (event.type == EventType.SEQUENCE) {
            return executeSequence(event)
        }

        // -------------------------------------------------------------------------
        // 2. Handle NO_OP events (instant success, no external interaction)
        // -------------------------------------------------------------------------
        if (event.type == EventType.NO_OP) {
            logger.info("NO-OP event ${event.id}: ${event.description}")

            // Give NO-OP a 0ms duration and update stats accordingly.
            stats.lastDurationMs = 0
            stats.totalDuration += 0
            stats.minDuration = minOf(stats.minDuration, 0)
            stats.maxDuration = maxOf(stats.maxDuration, 0)

            // Mark as successful; no metrics or state changes beyond setState.
            markSuccess(event, stats, emptyMap())
            debug("NO_OP event executed: ${event.id}")
            return true
        }

        // -------------------------------------------------------------------------
        // 3. Handle WAIT_FOR_DEVICE events (reboot recovery glue)
        // -------------------------------------------------------------------------
        if (event.type == EventType.WAIT_FOR_DEVICE) {
            systemInspector.awaitDeviceOnline()
            if (event.waitForBoot) systemInspector.awaitBootCompleted()
            return true
        }

        // -------------------------------------------------------------------------
        // 4. SCRIPT events — this is where metrics + execution timing live
        // -------------------------------------------------------------------------
        logger.info("Executing event ${event.id} (${event.language})")

        // We will build up the ScriptResult in stages:
        //  - First by executing the script via ScriptRunner
        //  - Then by augmenting it with metrics captured by the SystemInspector
        lateinit var result: ScriptResult

        // Resolve any metrics configuration for this event.
        // If event.metrics is null, we simply skip all metrics work.
        val metricsConfig = event.metrics

        // -------------------------------------------------------------------------
        // 4a. Pre-execution metrics snapshot (optional)
        // -------------------------------------------------------------------------
        // If metricsConfig is present, ask the SystemInspector for a snapshot
        // *before* the script runs. This might include CPU, memory, battery, etc.
        val preMetrics: Map<String, Double> = if (metricsConfig != null) {
            debug("Capturing pre-execution metrics for event ${event.id}")
            systemInspector.captureMetrics(metricsConfig)
        } else {
            emptyMap()
        }

        // -------------------------------------------------------------------------
        // 4b. Measure the script execution duration
        // -------------------------------------------------------------------------
        // We measure wall-clock time for the script via ScriptRunner.
        val durationMillis = measureTimeMillis {
            result = scriptRunner.run(event)
        }

        // Update duration-related stats (timestamp and execution counts are
        // updated later in markSuccess()).
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

        // -------------------------------------------------------------------------
        // 4c. Post-execution metrics snapshot + delta computation (optional)
        // -------------------------------------------------------------------------
        // If a metrics config was provided, capture a second snapshot *after*
        // script execution and compute per-metric deltas (post - pre).
        if (metricsConfig != null) {
            debug("Capturing post-execution metrics for event ${event.id}")
            val postMetrics = systemInspector.captureMetrics(metricsConfig)

            // Build a delta map: for each metric that appears in both pre and post,
            // we store post - pre. If only one side exists, we keep that value.
            val deltaMetrics = mutableMapOf<String, Double>()
            val allKeys = preMetrics.keys union postMetrics.keys

            for (key in allKeys) {
                val before = preMetrics[key]
                val after = postMetrics[key]

                val value = when {
                    before != null && after != null -> after - before
                    after != null -> after                 // only post snapshot available
                    else -> before!!                       // only pre snapshot available
                }

                deltaMetrics[key] = value
            }

            if (deltaMetrics.isNotEmpty()) {
                debug("Merging ${deltaMetrics.size} metric(s) into ScriptResult for event ${event.id}")

                // Merge computed deltas into the ScriptResult.metrics map.
                // We preserve any metrics the script itself may have produced
                // and only add metrics that are not already defined.
                val mergedMetrics = result.metrics.toMutableMap()
                for ((k, v) in deltaMetrics) {
                    // If a script already provided a value for a metric, we
                    // leave it untouched to avoid surprising overrides.
                    if (!mergedMetrics.containsKey(k)) {
                        mergedMetrics[k] = v
                    }
                }

                // Replace the original result with an augmented copy.
                result = result.copy(metrics = mergedMetrics)
            }
        }

        // At this point, result.metrics now contains:
        //  - any metrics produced by the script itself, plus
        //  - any system-level deltas contributed by SystemInspector (if configured).

        val success = result.exitCode == 0
        val metrics = result.metrics

        // -------------------------------------------------------------------------
        // 4d. Failure handling and logging
        // -------------------------------------------------------------------------
        if (!success) {
            // Track how many times this event has failed.
            eventFailureCount[event.id] = (eventFailureCount[event.id] ?: 0) + 1
            failureLog += FailureRecord(event.id, result.exitCode, result.stderr)

            logger.warn("Event ${event.id} failed with exitCode=${result.exitCode}")
            if (event.logErrors && result.stderr.isNotBlank()) {
                logger.error("stderr for ${event.id}:\n${result.stderr}")
            }
        } else {
            if (event.logOutput && result.stdout.isNotBlank()) {
                logger.info("stdout for ${event.id}:\n${result.stdout}")
            }
        }

        // On success we:
        //  - update per-event statistics
        //  - apply state transitions
        //  - fire postEvents and conditional triggers
        if (success) {
            markSuccess(event, stats, metrics)
            handlePostEvents(event)
            handleConditionalTriggers(event, result, metrics)
        }

        // -------------------------------------------------------------------------
        // 4e. Merge metrics into global summary
        // -------------------------------------------------------------------------
        // Regardless of success/failure, we fold the metrics into globalMetrics
        // so they appear in the final run summary.
        metrics.forEach { (k, v) ->
            globalMetrics.getOrPut(k) { mutableListOf() }.add(v)
        }

        // -------------------------------------------------------------------------
        // 5. Reboot handling
        // -------------------------------------------------------------------------
        if (event.causesReboot) {
            logger.warn("Event ${event.id} initiated a reboot. Entering reboot recovery mode...")

            handleRebootRecovery(event)

            // After recovery, consider the event successful if no fatal error.
            // Note: we do not re-run the script; we just mark success and preserve metrics.
            markSuccess(event, stats, metrics)
            return true
        }

        // -------------------------------------------------------------------------
        // 6. Process monitoring — detect unexpected app death
        // -------------------------------------------------------------------------
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
            logger.info("[DEBUG] $msg")
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
     * with normal policy handling.
     */
    private fun executeSequence(event: StressEvent): Boolean {
        logger.info("Executing SEQUENCE ${event.id}: ${event.sequence}")
        for (childId in event.sequence) {
            val child = eventsById[childId]
            if (child == null) {
                logger.warn("Sequence ${event.id} references unknown event $childId")
                continue
            }
            debug("Sequence ${event.id}: executing child event $childId")
            val success = executeEventWithPolicy(child)
            if (!success) {
                logger.warn("Sequence ${event.id} stopped because $childId failed.")
                return false
            }
        }
        val stats = eventStats.getOrPut(event.id) { EventStats() }
        markSuccess(event, stats, emptyMap())
        return true
    }

    /**
     * Marks a successful event:
     * - updates lastExecutionTimeMillis
     * - increments executions
     * - applies setState
     */
    private fun markSuccess(event: StressEvent, stats: EventStats, metrics: Map<String, Double>) {
        stats.lastExecutionTimeMillis = System.currentTimeMillis()
        stats.executions++

        event.setState.forEach { (k, v) -> state[k] = v }
        // metrics already merged globally in executeSingleAttempt
    }

    // -------------------------------------------------------------------------
    // Preconditions & triggers
    // -------------------------------------------------------------------------

    /**
     * Evaluates state-based and system preconditions for the event.
     */
    private fun checkPreconditions(event: StressEvent): Boolean {
        // State-based requirements
        for ((key, value) in event.requireState) {
            if (state[key] != value) {
                logger.info("Event ${event.id} requires state[$key]=$value but got ${state[key]}")
                return false
            }
        }

        val p = event.preconditions ?: return true

        val battery = systemInspector.getBatteryLevel()
        if (p.batteryAbove != null && battery != null && battery < p.batteryAbove) return false
        if (p.batteryBelow != null && battery != null && battery > p.batteryBelow) return false

        if (p.networkRequired == true && systemInspector.isNetworkAvailable() == false) return false
        if (p.deviceIdle == true && systemInspector.isDeviceIdle() == false) return false
        if (p.screenOn == true && systemInspector.isScreenOn() == false) return false
        if (p.chargingRequired == true && systemInspector.isCharging() == false) return false
        if (p.rootRequired == true && systemInspector.isRootAvailable() == false) return false
        if (p.adbAvailable == true && systemInspector.adbAvailable() == false) return false

        for (path in p.fileMustExist) {
            if (!systemInspector.fileExists(path)) return false
        }

        return true
    }

    /**
     * Executes postEvents after a successful event.
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
     * Evaluates and executes conditional triggers based on ScriptResult + metrics.
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
     * Continuous run loop (until stop() is called).
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
                Thread.sleep(delayMs)
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
     * If logcat is enabled, capture is started before the run and stopped
     * when the run completes (even on exception).
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
            // Start the target app once at the beginning of the run
            systemInspector.startApp(config.targetPackage)

            while (System.currentTimeMillis() < stopTime) {
                val success = runOnce(profile)

                // Optional: log failures but do not abort by default
                if (!success) {
                    logger.warn("Last event failed.")
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
     * Advances the RNG by the given number of calls (without tracking in rngCalls).
     * Use carefully; nextRandomInt() is preferred for tracked calls.
     */
    fun consumeRngCalls(count: Long) {
        repeat(count.toInt()) { rng.nextInt() }
        debug("Random advanced by $count calls")
    }

    /**
     * Deterministic replay:
     * - Loads seed and RNG call count from replay_state.json.
     * - Replays events from the beginning until rngCalls matches.
     * - Executes one additional event (the one that originally failed).
     * - Does NOT use logcat (to avoid side effects during replay).
     */
    fun replay(profile: String? = null, path: String = "replay_state.json") {
        println("[REPLAY] Starting deterministic replay…")

        // 1) Load saved seed + RNG call count
        val replay = loadReplayState(path)
        println("[REPLAY] Loaded state: seed=${replay.seed}, rngCalls=${replay.rngCalls}")

        // 2) Reset RNG and engine state to *fresh* start
        setSeed(replay.seed)      // sets rng = Random(seed) and rngCalls = 0
        resetReplayState()        // clears state, eventStats, failures, metrics

        val targetCalls = replay.rngCalls

        println("[REPLAY] Replaying until rngCalls reaches $targetCalls ...")

        replayMode = true

        // 3) Replay all events from the beginning until rngCalls == targetCalls
        //    NOTE: runOnce() MUST NOT stop replay early even if it returns false.
        while (rngCalls < targetCalls) {
            val ok = runOnce(profile)

            if (!ok) {
                println("[REPLAY] (info) Event returned false during replay but continuing… (rngCalls=$rngCalls)")
            }
        }

        println("[REPLAY] RNG call count matched original run (rngCalls=$rngCalls).")

        // 4) Execute ONE MORE event (the one that originally failed).
        println("[REPLAY] Executing final failing event…")
        val finalSuccess = runOnce(profile)

        println("[REPLAY] Replay complete. finalSuccess=$finalSuccess")
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
    fun printSummary() {
        println("\n==================== STRESS TEST SUMMARY ========================================================")

        printEventSummary()
        printFailureSummary()
        printMetricsSummary()
        printFinalStateSummary()
        printRngSummary()

        println("==================================================================================================")
    }

    private fun printEventSummary() {
        println("\n----- Event Execution Summary (Option B) -----")
        println(
            String.format(
                "%-50s %-10s %-10s %-10s %-20s",
                "Event", "Execs", "Success", "Failure", "LastDuration"
            )
        )
        println("--------------------------------------------------------------------------------------------------")

        config.events.forEach { event ->
            val stats = eventStats[event.id]
            val execs = stats?.executions ?: 0
            val failures = eventFailureCount[event.id] ?: 0
            val successes = execs - failures
            val duration = formatDuration(stats?.lastDurationMs)

            println(
                String.format(
                    "%-50s %-10d %-10d %-10d %-20s",
                    event.id, execs, successes, failures, duration
                )
            )
        }
    }

    private fun printFailureSummary() {
        if (failureLog.isEmpty()) {
            println("\nNo failures recorded.")
            return
        }

        println("\n----- Failure Details -----")
        failureLog.forEach { failure ->
            println("[${failure.eventId}] exitCode=${failure.exitCode}")
            if (failure.stderr.isNotBlank()) {
                println("stderr=${failure.stderr.trim()}")
            }
        }
    }

    private fun printMetricsSummary() {
        if (globalMetrics.isEmpty()) {
            println("\n(No global metrics were recorded)")
            return
        }

        println("\n----- Metrics Summary (Option C) -----")
        globalMetrics.forEach { (metric, values) ->
            val avg = values.average()
            println("$metric avg = $avg")
        }
    }

    private fun printFinalStateSummary() {
        if (state.isEmpty()) {
            println("\n(No final state variables were set.)")
            return
        }

        println("\n----- Final State -----")
        state.forEach { (k, v) ->
            println("$k = $v")
        }
    }

    private fun printRngSummary() {
        println("\n----- RNG Tracking -----")
        println("Seed: ${config.randomSeed}")
        println("Random calls consumed: $rngCalls")
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
}
