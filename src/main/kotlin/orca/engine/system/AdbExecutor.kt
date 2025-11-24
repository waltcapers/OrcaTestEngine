package orca.engine.system

/**
 * Abstraction for executing ADB commands.
 *
 * This allows the engine to:
 *  - use real ADB (DefaultAdbExecutor)
 *  - or fake ADB for testing (FakeAdbExecutor)
 *
 * Provides two entry points:
 *  - exec()  → host-side commands (e.g., `adb get-state`)
 *  - shell() → device-side commands (`adb shell cmd`)
 */
interface AdbExecutor {

    /**
     * Executes a HOST-side ADB command:
     *   adb <args...>
     */
    fun exec(args: List<String>, timeoutSeconds: Long): AdbResult

    /**
     * Executes a DEVICE-side shell command:
     *   adb shell "<command>"
     */
    fun shell(command: String, timeoutSeconds: Long): AdbResult
}
