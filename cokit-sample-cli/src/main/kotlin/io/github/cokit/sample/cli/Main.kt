package io.github.cokit.sample.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import io.github.cokit.client.ClientInfo
import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.CodexNotification
import io.github.cokit.client.CodexRpc
import io.github.cokit.client.CodexRpcClient
import io.github.cokit.client.CodexRpcConnection
import io.github.cokit.client.ItemType
import io.github.cokit.client.Thread
import io.github.cokit.client.ThreadId
import io.github.cokit.client.ThreadStartParams
import io.github.cokit.client.Turn
import io.github.cokit.client.TurnId
import io.github.cokit.client.TurnInput
import io.github.cokit.client.TurnStartParams
import io.github.cokit.transport.stdio.StdioCodexTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    SampleCliCommand().main(args)
}

class SampleCliCommand(
    private val runner: SampleRunner = CodexSampleRunner(),
    defaultCwd: String = System.getProperty("user.dir"),
) : CliktCommand(
    name = "cokit-sample-cli",
) {
    override fun help(context: Context): String = """
        Run a minimal codex app-server thread and stream the assistant response.

        With no arguments, the sample uses the current working directory and a
        default message. Set COKIT_CODEX_COMMAND to override the local app-server command.
    """.trimIndent()

    private val cwd by option(
        "--cwd",
        help = "App-server host path for the thread.",
    ).default(defaultCwd)

    private val message by option(
        "--message",
        help = "User message to send as turn input.",
    ).default(SampleCliDefaults.message)

    override fun run() {
        try {
            runBlocking {
                runner.run(
                    SampleOptions(cwd = cwd, message = message),
                    object : SampleOutput {
                        override fun text(value: String) {
                            echo(value, trailingNewline = false)
                        }

                        override fun line(value: String) {
                            echo(value)
                        }
                    },
                )
            }
        } catch (error: SampleRunException) {
            throw CliktError(error.message ?: "Sample run failed.")
        }
    }
}

interface SampleRunner {
    suspend fun run(options: SampleOptions, output: SampleOutput)
}

interface SampleOutput {
    fun text(value: String)

    fun line(value: String = "")
}

data class SampleOptions(
    val cwd: String,
    val message: String,
)

object SampleCliDefaults {
    const val message: String = "Say hello from the CoKit sample CLI."
}

internal class CodexSampleRunner(
    conversationClientFactory: (suspend CoroutineScope.() -> SampleConversationClient)? = null,
) : SampleRunner {
    private val createConversationClient: suspend CoroutineScope.() -> SampleConversationClient =
        conversationClientFactory ?: {
            StdioSampleConversationClient.connect(
                transport = this@CodexSampleRunner.codexTransport(),
                scope = this,
            )
        }

    override suspend fun run(options: SampleOptions, output: SampleOutput): Unit = coroutineScope {
        createConversationClient().use { client ->
            val events = Channel<CodexNotification>(Channel.UNLIMITED)
            val eventCollector = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    client.notifications.collect { event ->
                        events.send(event)
                    }
                } finally {
                    events.close()
                }
            }

            try {
                val thread = client.startThread(CodexHostPath(options.cwd))
                val turn = client.startTurn(
                    threadId = thread.id,
                    input = listOf(TurnInput.Text(options.message)),
                )

                output.line("Started thread ${thread.id.value} and turn ${turn.id.value}")
                output.line()
                output.line("Assistant:")

                streamAssistantResponse(
                    turnId = turn.id,
                    events = events,
                    output = output,
                )
            } finally {
                eventCollector.cancelAndJoin()
            }
        }
    }

    private fun codexTransport(): StdioCodexTransport {
        val overrideCommand = codexCommandOverride()
        return if (overrideCommand == null) {
            StdioCodexTransport()
        } else {
            StdioCodexTransport(command = overrideCommand)
        }
    }

    private fun codexCommandOverride(): List<String>? {
        return System.getenv("COKIT_CODEX_COMMAND")
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
    }
}

interface SampleConversationClient : AutoCloseable {
    val notifications: Flow<CodexNotification>

    suspend fun startThread(cwd: CodexHostPath): Thread

    suspend fun startTurn(threadId: ThreadId, input: List<TurnInput>): Turn
}

private class StdioSampleConversationClient private constructor(
    private val client: CodexRpcClient,
) : SampleConversationClient {
    override val notifications: Flow<CodexNotification> = client.notifications

    override suspend fun startThread(cwd: CodexHostPath): Thread {
        return client.request(
            CodexRpc.Thread.Start,
            ThreadStartParams(cwd = cwd),
        ).thread
    }

    override suspend fun startTurn(threadId: ThreadId, input: List<TurnInput>): Turn {
        return client.request(
            CodexRpc.Turn.Start,
            TurnStartParams(
                threadId = threadId,
                input = input,
            ),
        ).turn
    }

    override fun close() {
        client.close()
    }

    companion object {
        suspend fun connect(
            transport: StdioCodexTransport,
            scope: CoroutineScope,
        ): StdioSampleConversationClient {
            return try {
                StdioSampleConversationClient(
                    CodexRpcClient.connect(
                        CodexRpcConnection(
                            transport = transport,
                            clientInfo = ClientInfo(
                                name = "cokit_sample_cli",
                                title = "CoKit Sample CLI",
                                version = "0.1.0",
                            ),
                            scope = scope,
                        ),
                    ),
                )
            } catch (error: Throwable) {
                transport.close()
                throw error
            }
        }
    }
}

private suspend fun streamAssistantResponse(
    turnId: TurnId,
    events: Channel<CodexNotification>,
    output: SampleOutput,
) {
    var wroteAssistantText = false
    while (true) {
        val event = events.receiveCatching().getOrNull()
            ?: throw SampleRunException("Turn ${turnId.value} ended before completion.")
        when (event) {
            is CodexNotification.AgentMessageDelta -> {
                if (event.turnId == turnId) {
                    output.text(event.delta)
                    wroteAssistantText = true
                }
            }

            is CodexNotification.ItemCompleted -> {
                val text = event.item.text
                if (
                    event.turnId == turnId &&
                    event.item.type == ItemType.AgentMessage &&
                    !wroteAssistantText &&
                    !text.isNullOrBlank()
                ) {
                    output.text(text)
                    wroteAssistantText = true
                }
            }

            is CodexNotification.TurnCompleted -> {
                if (event.turn.id == turnId) {
                    if (wroteAssistantText) {
                        output.line()
                    } else {
                        output.line("(no assistant message)")
                    }
                    return
                }
            }

            is CodexNotification.TurnFailed -> {
                if (event.turn.id == turnId) {
                    throw SampleRunException(event.turn.failureMessage())
                }
            }

            is CodexNotification.Error -> {
                if (event.turnId == turnId && !event.willRetry) {
                    throw SampleRunException(
                        "Turn ${turnId.value} failed: ${event.error.message}",
                    )
                }
            }

            else -> Unit
        }
    }
}

private fun Turn.failureMessage(): String {
    val message = error?.message ?: "unknown error"
    return "Turn ${id.value} failed: $message"
}

private class SampleRunException(message: String) : RuntimeException(message)
