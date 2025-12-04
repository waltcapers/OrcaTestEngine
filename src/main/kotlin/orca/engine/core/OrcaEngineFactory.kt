/*
 * Dual License Notice
 * -------------------
 *
 * This file is part of the OrcaTestEngine project.
 */

package orca.engine.core

import orca.engine.logging.ConsoleEngineLogger
import orca.engine.logging.LogcatManager
import orca.engine.logging.MockLogcatManager
import orca.engine.model.OrcaTestConfig
import orca.engine.model.ScriptRunner
import orca.engine.system.AdbSystemInspector
import orca.engine.system.DefaultAdbExecutor
import orca.engine.system.DefaultSystemInspector
import orca.engine.system.MockAdbExecutor
import orca.engine.system.MockSystemInspector

/**
 * Small factory to construct OrcaEngine instances for:
 *  - real device mode (ADB + real logcat)
 *  - mock/dry-run mode (no ADB, no real logcat)
 *
 * This is OPTIONAL sugar to keep RunCommand cleaner.
 */
object OrcaEngineFactory {

    data class RealEngineDeps(
        val adbPath: String = "adb",
        val deviceSerial: String? = null
    )

    /**
     * Real device engine:
     *  - AdbSystemInspector + DefaultAdbExecutor
     *  - DefaultLogcatManager() via OrcaEngine default
     */
    fun createReal(
        config: OrcaTestConfig,
        deps: RealEngineDeps = RealEngineDeps(),
        logger: ConsoleEngineLogger = ConsoleEngineLogger(),
        scriptRunner: ScriptRunner = ScriptRunnerDispatcher()
    ): OrcaEngine {
        val adb = DefaultAdbExecutor(
            adbPath = deps.adbPath,
            deviceSerial = deps.deviceSerial,
            logger = logger
        )

        val inspector = AdbSystemInspector(
            adb = adb,
            defaultPackageName = config.targetPackage,
            debug = config.debug,
            logger = logger
        )

        return OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger
            // logcat: default DefaultLogcatManager()
        )
    }

    /**
     * Mock/dry-run engine:
     *  - MockSystemInspector (no ADB)
     *  - MockLogcatManager (no logcat)
     */
    fun createMock(
        config: OrcaTestConfig,
        logger: ConsoleEngineLogger = ConsoleEngineLogger(),
        scriptRunner: ScriptRunner = ScriptRunnerDispatcher(),
        logcatManager: LogcatManager = MockLogcatManager()
    ): OrcaEngine {
        val inspector = MockSystemInspector(
            debug = config.debug
        )

        return OrcaEngine(
            config = config,
            systemInspector = inspector,
            scriptRunner = scriptRunner,
            logger = logger,
            logcat = logcatManager
        )
    }
}
