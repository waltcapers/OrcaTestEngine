package orca.engine.system

/**
 * A richer, test-friendly fake implementation of [AdbExecutor].
 *
 * This test double serves three purposes:
 * -------------------------------------------------------------
 * 1. **Unit Testing without ADB**
 *    Allows OrcaEngine, AdbSystemInspector, and ScriptRunner
 *    to be tested on the JVM with no Android device.
 *
 * 2. **Deterministic Behavior**
 *    Can return pre-programmed results in a queue or based on
 *    command matching (e.g., regex, contains, exact match).
 *
 * 3. **Verification**
 *    Test code can check which ADB commands were invoked and
 *    in what order.
 *
 * NOTES:
 * - Nothing in this implementation spawns processes.
 * - All responses are instant and deterministic.
 */
class FakeAdbExecutor(
    /**
     * If set, this lambda is invoked for ANY exec() call.
     * Useful for simple tests.
     */
    private val cannedExec: ((List<String>) -> AdbResult)? = null,

    /**
     * If set, this lambda is invoked for ANY shell() call.
     */
    private val cannedShell: ((String) -> AdbResult)? = null
) : AdbExecutor {

    // ---------------------------------------------------------------------
    // Recorded history for verification
    // ---------------------------------------------------------------------

    /** Stores every exec() call made by the system under test. */
    val execHistory = mutableListOf<List<String>>()

    /** Stores every shell() call made by the system under test. */
    val shellHistory = mutableListOf<String>()

    // ---------------------------------------------------------------------
    // Optional queue-based responses
    // ---------------------------------------------------------------------

    /**
     * Queue of AdbResult values returned by successive exec() calls.
     * If not empty → entries are consumed in FIFO order.
     */
    val execQueue = ArrayDeque<AdbResult>()

    /**
     * Queue of AdbResult for shell() calls.
     */
    val shellQueue = ArrayDeque<AdbResult>()

    // ---------------------------------------------------------------------
    // Pattern-based matchers
    // (optional convenience for writing tests)
    // ---------------------------------------------------------------------

    /** Exact match map for exec() commands. */
    val execExact = mutableMapOf<List<String>, AdbResult>()

    /** Exact string match map for shell() commands. */
    val shellExact = mutableMapOf<String, AdbResult>()

    /** Substring match for shell() commands. */
    val shellContains = mutableMapOf<String, AdbResult>()

    /** Regex match for shell() commands. */
    val shellRegex = mutableMapOf<Regex, AdbResult>()

    // ---------------------------------------------------------------------
    // Core API implementation
    // ---------------------------------------------------------------------

    override fun exec(args: List<String>, timeoutSeconds: Long): AdbResult {
        execHistory += args

        // 1. FIFO override
        if (execQueue.isNotEmpty()) {
            return execQueue.removeFirst()
        }

        // 2. Exact match override
        execExact[args]?.let { return it }

        // 3. Global canned handler
        cannedExec?.let { return it(args) }

        // 4. Default benign success
        return AdbResult(
            exitCode = 0,
            stdout = "",
            stderr = ""
        )
    }

    override fun shell(command: String, timeoutSeconds: Long): AdbResult {
        shellHistory += command

        // 1. FIFO shell queue
        if (shellQueue.isNotEmpty()) {
            return shellQueue.removeFirst()
        }

        // 2. Exact match
        shellExact[command]?.let { return it }

        // 3. Contains match
        for ((substring, result) in shellContains) {
            if (command.contains(substring, ignoreCase = true)) {
                return result
            }
        }

        // 4. Regex match
        for ((regex, result) in shellRegex) {
            if (regex.containsMatchIn(command)) {
                return result
            }
        }

        // 5. Global canned handler
        cannedShell?.let { return it(command) }

        // 6. Default benign success
        return AdbResult(
            exitCode = 0,
            stdout = "",
            stderr = ""
        )
    }

    // ---------------------------------------------------------------------
    // Helpers for test setup
    // ---------------------------------------------------------------------

    /** Pushes a canned exec() response into the FIFO queue. */
    fun enqueueExec(result: AdbResult) = execQueue.addLast(result)

    /** Pushes a canned shell() response into the FIFO queue. */
    fun enqueueShell(result: AdbResult) = shellQueue.addLast(result)
}
