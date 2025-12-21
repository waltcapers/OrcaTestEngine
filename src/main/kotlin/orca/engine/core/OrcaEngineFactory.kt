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

import MockScriptRunner
import orca.engine.config.OrcaConfigLoader
import orca.engine.model.OrcaTestConfig
import orca.engine.model.ScriptRunner
import orca.engine.model.SystemInspector
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import orca.engine.system.MockSystemInspector
import java.io.File

/**
 * Small factory to construct OrcaEngine instances for:
 *  - real device mode (ADB + real logcat)
 *  - mock/dry-run mode (no ADB, no real logcat)
 *
 * This is OPTIONAL sugar to keep RunCommand cleaner.
 */
object OrcaEngineFactory {


    /**
     * Creates and configures a new instance of [OrcaEngine] using either a real
     * ADB-backed system inspector or a simulated mock environment.
     *
     * This function centralizes engine construction to ensure consistent logger
     * assignment, inspector selection, script-runner selection, and config loading.
     *
     * ### Supported `configAttrib` types:
     * - **String** → treated as a filesystem path to a JSON config file.
     *   The file is loaded using [OrcaConfigLoader.load].
     * - **OrcaTestConfig** → used directly, without reloading.
     *
     * Any other type will cause an [IllegalArgumentException].
     *
     * ### Mock Mode
     * When `mockMode = true`:
     * - Uses [MockSystemInspector] (no ADB required)
     * - Uses [MockScriptRunner] (deterministic behavior)
     * - Safe for dry-runs, schema testing, and debugging without a device
     *
     * When `mockMode = false`:
     * - Uses [AdbSystemInspector] (real device/emulator)
     * - Uses [ScriptRunnerDispatcher] to execute actual scripts
     * - Requires ADB to be installed and accessible via PATH
     *
     * ### Parameters:
     * @param targetPackage The primary Android package to monitor for stability
     *   (used by inspectors and process checks).
     *
     * @param configAttrib Either a file path (String) to a JSON config, or an
     *   already-constructed [OrcaTestConfig] instance, or a File.
     *
     * @param mockMode If true, runs in simulation mode. No ADB commands are used.
     *   Default is `true` to avoid accidental device interaction.
     *
     * @param logger A shared [EngineLogger] instance. Defaults to
     *   [LoggerProvider.get] if not provided. This ensures consistent logging
     *   across engine instances.
     *
     * @param debug Enables verbose debugging in system inspectors and script
     *   runners. Does not alter config-level debug flags.
     *
     * ### Returns:
     * A fully initialized [OrcaEngine] ready for execution, or `null` if:
     * - configuration fails to load
     * - configAttrib is invalid
     *
     * ### Throws:
     * - None directly. Exceptions are caught and logged.
     *
     * ### Usage Example:
     * ```
     * val engine = newEngine(
     *     targetPackage = "com.example.myapp",
     *     configAttrib = "stress.json",
     *     mockMode = false,
     *     logger = LoggerProvider.get(),
     *     debug = true
     * ) ?: error("Engine creation failed")
     *
     * engine.runLoop()
     * ```
     */

    fun newEngine(
        targetPackage: String?,
        configAttrib: Any,
        mockMode: Boolean = true,
        logger: EngineLogger,
        debug: Boolean = false
    ): OrcaEngine? {

        // ---------------------------------------------------------------
        // 1. Resolve config (String → load JSON, or use OrcaTestConfig)
        // ---------------------------------------------------------------
        val config: OrcaTestConfig = try {
            when (configAttrib) {
                is String -> OrcaConfigLoader.load(configAttrib)
                is OrcaTestConfig -> configAttrib
                is File -> OrcaConfigLoader.load(configAttrib.absolutePath)
                else -> throw IllegalArgumentException("configAttrib must be String or OrcaTestConfig")
            }
        } catch (t: Throwable) {
            logger.error("CONFIG LOAD FAILED: ${t.message}")
            return null
        }

        // ---------------------------------------------------------------
        // 2. System inspector & Script runner selection
        // ---------------------------------------------------------------
        val inspector: SystemInspector
        val runner: ScriptRunner

        if (mockMode) {
            logger.info("Mock mode enabled (Simulated SystemInspector + MockScriptRunner).")

            inspector = MockSystemInspector(
                debug = debug,
            )

            runner = MockScriptRunner()

        } else {
            logger.info("Running with ADB bindings.")

            inspector = AdbSystemInspector(
                adb = DefaultAdbExecutor(
                    adbPath = "adb",
                    deviceSerial = null,
                ),
                defaultPackageName = targetPackage,
                debug = debug,
            )

            runner = ScriptRunnerDispatcher()
        }

        // ---------------------------------------------------------------
        // 3. Create the engine
        // ---------------------------------------------------------------
        return OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = runner,
            logger = logger
        )
    }

}
