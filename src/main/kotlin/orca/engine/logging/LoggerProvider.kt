package orca.engine.logging

import orca.engine.core.EngineLogger

object LoggerProvider {
    @Volatile
    private var logger: EngineLogger = ConsoleEngineLogger()

    fun get(): EngineLogger = logger

    fun set(newLogger: EngineLogger) {
        logger = newLogger
    }
}