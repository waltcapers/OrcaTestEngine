/**
 * MockScriptRunner
 * -----------------
 * This ScriptRunner implementation is used when Orca is running
 * in "--mock" mode. It allows the engine to execute events
 * *without issuing any real ADB commands*.
 *
 * Purpose:
 *  - Validate JSON configuration structure.
 *  - Debug the sequencing, weighting, retry policies, and onFailure rules.
 *  - Test Orca's summary output, state transitions, and conditional triggers.
 *  - Run the interactive shell ("orca>") without a device connected.
 *
 * Behavior:
 *  ---------
 *  By default:
 *    • All events succeed (exitCode = 0).
 *    • A short canned stdout message is returned.
 *    • stderr is empty.
 *
 *  Simulated Failure Support:
 *    You can force a mock failure — useful for testing retry behavior,
 *    conditional triggers, or failure summaries — by:
 *
 *      1) Including the word "FAIL" in the event.description, OR
 *      2) Adding a tag named "fail" to the event.
 *
 *    Example event:
 *      {
 *         "id": "test-failure",
 *         "type": "SCRIPT",
 *         "description": "This should FAIL",
 *         "tags": ["fail"]
 *      }
 *
 *    These are OPTIONAL conventions and do NOT require any extra fields in
 *    the event model.
 */
import orca.engine.model.ScriptResult
import orca.engine.model.ScriptRunner
import orca.engine.model.StressEvent


class MockScriptRunner : ScriptRunner {


    override fun run(event: StressEvent): ScriptResult {

        // Allow tests to simulate failures using event.description or tags if desired.
        val forceFail =
            event.description?.contains("FAIL", ignoreCase = true) == true ||
                    event.tags.any { it.equals("fail", ignoreCase = true) }

        return if (forceFail) {
            ScriptResult(
                exitCode = 1,
                stdout = "",
                stderr = "Mock failure for event ${event.id}",
                metrics = emptyMap()
            )
        } else {
            ScriptResult(
                exitCode = 0,
                stdout = "Mocked script output for event ${event.id}",
                stderr = "",
                metrics = emptyMap()
            )
        }
    }
}
