package io.github.cokit.transport.stdio

import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import kotlin.test.assertTrue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StdioCodexTransportTest {
    @Test
    fun defaultsToCodexAppServerStdioCommand() {
        val transport = StdioCodexTransport()

        assertEquals(listOf("codex", "app-server", "--stdio"), transport.command)
    }

    @Test
    fun sendWritesOneJsonMessagePerLine() = runTest {
        val stdin = ByteArrayOutputStream()
        val transport = StdioCodexTransport(
            input = ByteArrayInputStream(ByteArray(0)),
            output = stdin,
            scope = backgroundScope,
        )

        transport.send(JsonRpcNotification(method = "initialized"))

        val line = stdin.toString(Charsets.UTF_8.name())
        assertTrue(line.endsWith("\n"))
        assertTrue(line.contains("initialized"))
    }

    @Test
    fun incomingDecodesJsonLines() = runTest {
        val stdout = ByteArrayInputStream(
            """{"id":1,"method":"initialize"}${'\n'}""".toByteArray(Charsets.UTF_8),
        )
        val transport = StdioCodexTransport(
            input = stdout,
            output = ByteArrayOutputStream(),
            scope = backgroundScope,
        )

        val message = async { transport.incoming.first() }
        runCurrent()

        assertEquals("initialize", (message.await() as JsonRpcRequest).method)
    }
}
