package orca.engine.core

/**
 * Logging abstraction used throughout the OrcaEngine.
 *
 * This interface provides a minimal logging contract that allows the engine
 * to emit informational messages, warnings, and errors without depending on a
 * specific logging framework (e.g., Log4j, SLF4J, Android Logcat).
 *
 * Rationale:
 * ----------
 *  - Keeps the core engine independent from underlying logging technology.
 *  - Allows different front-ends (console, logcat, file logging, UI, etc.)
 *    to provide their own implementations.
 *  - Simplifies testing by allowing test doubles or no-op logger implementations.
 *
 * Typical usage:
 *  --------------
 *  val logger: EngineLogger = ConsoleLogger()
 *  logger.info("Test started")
 *  logger.warn("Slow operation detected")
 *  logger.error("Script failed", exception)
 */
interface EngineLogger {

    /**
     * Logs an informational message.
     *
     * Use this method for:
     *  - Normal runtime flow messages
     *  - Script execution output (stdout)
     *  - Status updates or progress tracking
     *
     * @param message The message to log.
     */
    fun info(message: String)

    /**
     * Logs a warning message indicating a non-critical issue.
     *
     * Use this method for:
     *  - Non-fatal script failures
     *  - Slow event detection
     *  - Deprecated or unexpected behavior that does not stop execution
     *
     * @param message The message describing the warning condition.
     */
    fun warn(message: String)

    /**
     * Logs an error message with an optional exception.
     *
     * Use this method for:
     *  - Critical failures
     *  - Unexpected exceptions
     *  - Script errors that require attention
     *
     * @param message A description of the error.
     * @param t Optional throwable representing the underlying cause.
     */
    fun error(message: String, t: Throwable? = null)
}
