package io.github.cokit.sample.cli

import com.github.ajalt.clikt.testing.test
import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.CodexNotification
import io.github.cokit.client.ItemId
import io.github.cokit.client.Thread
import io.github.cokit.client.ThreadId
import io.github.cokit.client.Turn
import io.github.cokit.client.TurnId
import io.github.cokit.client.TurnInput
import io.github.cokit.client.TurnStatus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleCliTest {
    @Test
    fun runsWithDefaultOptionsWhenNoArgumentsAreProvided() {
        val runner = RecordingSampleRunner()

        val result = SampleCliCommand(
            runner = runner,
            defaultCwd = "/path/to/project",
        ).test(emptyArray())

        assertEquals(0, result.statusCode)
        assertEquals("Started sample turn\nAssistant: Hello from Codex\n", result.stdout)
        assertEquals(
            SampleOptions(
                cwd = "/path/to/project",
                message = SampleCliDefaults.message,
            ),
            runner.options,
        )
    }

    @Test
    fun usesOptionalOverridesWhenProvided() {
        val runner = RecordingSampleRunner()

        val result = SampleCliCommand(
            runner = runner,
            defaultCwd = "/path/to/project",
        ).test(
            arrayOf(
                "--cwd",
                "/another/project",
                "--message",
                "Summarize this repository",
            ),
        )

        assertEquals(0, result.statusCode)
        assertEquals(
            SampleOptions(
                cwd = "/another/project",
                message = "Summarize this repository",
            ),
            runner.options,
        )
    }

    @Test
    fun generatedHelpDescribesNoArgumentUsage() {
        val help = SampleCliCommand(
            runner = RecordingSampleRunner(),
            defaultCwd = "/path/to/project",
        ).getFormattedHelp().orEmpty()

        assertTrue(help.contains("Usage:"))
        assertTrue(help.contains("Run a minimal codex app-server thread and stream the assistant response."))
        assertTrue(help.contains("--cwd"))
        assertTrue(help.contains("--message"))
        assertTrue(help.contains("COKIT_CODEX_COMMAND"))
        assertTrue(!help.contains("codex app-server --stdio"))
    }

    @Test
    fun codexRunnerStreamsAssistantDeltasUntilTurnCompletes() {
        val client = RecordingConversationClient(
            events = listOf(
                CodexNotification.AgentMessageDelta(
                    threadId = ThreadId("thr_123"),
                    turnId = TurnId("turn_123"),
                    itemId = ItemId("item_msg"),
                    delta = "Hello",
                ),
                CodexNotification.AgentMessageDelta(
                    threadId = ThreadId("thr_123"),
                    turnId = TurnId("turn_123"),
                    itemId = ItemId("item_msg"),
                    delta = " from Codex",
                ),
                CodexNotification.TurnCompleted(
                    Turn(
                        id = TurnId("turn_123"),
                        status = TurnStatus.Completed,
                    ),
                ),
            ),
        )
        val output = RecordingSampleOutput()

        runBlocking {
            CodexSampleRunner(
                conversationClientFactory = { client },
            ).run(
                SampleOptions(
                    cwd = "/path/to/project",
                    message = "Say hello",
                ),
                output,
            )
        }

        assertEquals(CodexHostPath("/path/to/project"), client.startedCwd)
        assertEquals(
            listOf(TurnInput.Text("Say hello")),
            client.startedInput,
        )
        assertEquals(
            "Started thread thr_123 and turn turn_123\n\nAssistant:\nHello from Codex\n",
            output.content,
        )
    }

    @Test
    fun codexRunnerReportsTurnFailure() {
        val client = RecordingConversationClient(
            events = listOf(
                CodexNotification.TurnFailed(
                    Turn(
                        id = TurnId("turn_123"),
                        status = TurnStatus.Failed,
                        error = io.github.cokit.client.TurnError("model request failed"),
                    ),
                ),
            ),
        )
        val output = RecordingSampleOutput()

        val result = runCatching {
            runBlocking {
                CodexSampleRunner(
                    conversationClientFactory = { client },
                ).run(
                    SampleOptions(
                        cwd = "/path/to/project",
                        message = "Say hello",
                    ),
                    output,
                )
            }
        }

        assertTrue(result.isFailure)
        assertEquals(
            "Started thread thr_123 and turn turn_123\n\nAssistant:\n",
            output.content,
        )
        assertEquals("Turn turn_123 failed: model request failed", result.exceptionOrNull()?.message)
    }

    private class RecordingSampleRunner : SampleRunner {
        var options: SampleOptions? = null

        override suspend fun run(options: SampleOptions, output: SampleOutput) {
            this.options = options
            output.line("Started sample turn")
            output.text("Assistant: ")
            output.text("Hello from Codex")
            output.line()
        }
    }

    private class RecordingSampleOutput : SampleOutput {
        private val builder = StringBuilder()

        val content: String
            get() = builder.toString()

        override fun text(value: String) {
            builder.append(value)
        }

        override fun line(value: String) {
            builder.append(value)
            builder.append('\n')
        }
    }

    private class RecordingConversationClient(
        private val events: List<CodexNotification>,
    ) : SampleConversationClient {
        private val mutableNotifications = MutableSharedFlow<CodexNotification>(
            extraBufferCapacity = 16,
        )
        var startedCwd: CodexHostPath? = null
        var startedInput: List<TurnInput>? = null

        override val notifications = mutableNotifications

        override suspend fun startThread(cwd: CodexHostPath): Thread {
            startedCwd = cwd
            return Thread(id = ThreadId("thr_123"))
        }

        override suspend fun startTurn(threadId: ThreadId, input: List<TurnInput>): Turn {
            startedInput = input
            events.forEach { mutableNotifications.emit(it) }
            return Turn(
                id = TurnId("turn_123"),
                status = TurnStatus.InProgress,
            )
        }

        override fun close() = Unit
    }
}
