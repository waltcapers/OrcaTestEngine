package orca.engine.system

/**
 * Represents the result of executing an ADB command.
 *
 * @property exitCode the process exit code (0 = success)
 * @property stdout   standard output text
 * @property stderr   standard error text
 */
data class AdbResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
)
