package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcErrorObject
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class JsonRpcSessionTest {
    @Test
    fun sendRequestSendsIncrementingNumericIds() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)

        val first = session.sendRequest("model/list")
        val second = session.sendRequest("thread/list")

        assertEquals(JsonRpcId.Number(1), first)
        assertEquals(JsonRpcId.Number(2), second)
        assertEquals(2, transport.sent.size)
    }

    @Test
    fun notifySendsNotification() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)

        session.notify("initialized")

        assertEquals(JsonRpcNotification(method = "initialized"), transport.sent.single())
    }

    @Test
    fun closeIsIdempotentAndCancelsPendingRequests() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val result = async {
            runCatching { session.request("model/list", JsonObject(emptyMap())) }
        }
        runCurrent()

        session.close()
        session.close()

        val error = assertIs<CancellationException>(result.await().exceptionOrNull())
        assertEquals("JSON-RPC session closed", error.message)
        assertEquals(1, transport.closeCount)
    }

    @Test
    fun requestAfterCloseFailsBeforeTransportSend() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        session.close()

        val error = assertFailsWith<CancellationException> {
            withTimeout(100) {
                session.request("model/list", JsonObject(emptyMap()))
            }
        }

        assertEquals("JSON-RPC session closed", error.message)
        assertTrue(transport.sent.isEmpty())
    }

    @Test
    fun requestCompletesWhenMatchingResponseArrives() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val expected = buildJsonObject { put("ok", true) }

        val deferred = async {
            session.request("model/list", JsonObject(emptyMap()))
        }
        runCurrent()

        val sent = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(id = sent.id, result = expected))

        assertEquals(expected, deferred.await())
    }

    @Test
    fun overloadErrorCompletesRequestAsRetryableRemoteExceptionWithoutAutomaticRetry() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val result = async {
            runCatching { session.request("model/list", JsonObject(emptyMap())) }
        }
        runCurrent()

        val sent = transport.sent.single() as JsonRpcRequest
        transport.receive(
            JsonRpcResponse(
                id = sent.id,
                error = JsonRpcErrorObject(
                    code = -32001,
                    message = "Server overloaded; retry later.",
                ),
            ),
        )

        val error = assertIs<JsonRpcRemoteException>(result.await().exceptionOrNull())

        assertTrue(error.isRetryableOverload)
        assertEquals(-32001, error.error.code)
        assertEquals(1, transport.sent.size)
    }

    @Test
    fun nonOverloadErrorsAreNotRetryableByDefault() {
        val error = JsonRpcRemoteException(
            JsonRpcErrorObject(
                code = -32000,
                message = "Server error",
            ),
        )

        assertFalse(error.isRetryableOverload)
    }

    @Test
    fun routesNotificationsToNotificationFlow() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val received = async { session.notifications.first() }
        runCurrent()

        transport.receive(JsonRpcNotification(method = "turn/completed"))

        assertEquals(JsonRpcNotification(method = "turn/completed"), received.await())
    }

    @Test
    fun notificationFlowDropsOldestWhenSubscriberFallsBehind() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val gate = CompletableDeferred<Unit>()
        val received = mutableListOf<JsonRpcNotification>()
        val collector = launch {
            session.notifications
                .onEach { gate.await() }
                .collect { notification -> received += notification }
        }
        runCurrent()

        repeat(128) { index ->
            session.publishForTests(JsonRpcNotification(method = "highRate/$index"))
        }
        runCurrent()
        gate.complete(Unit)
        runCurrent()
        collector.cancel()

        assertTrue(received.size <= 65)
        assertTrue(received.none { notification -> notification.method == "highRate/1" })
        assertEquals("highRate/127", received.last().method)
    }

    @Test
    fun oversizedIncomingMessageIsRejectedBeforeNotificationBuffering() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(
            transport = transport,
            scope = backgroundScope,
            maxMessageBytes = 64,
        )

        val error = assertFailsWith<JsonRpcMessageSizeException> {
            session.publishForTests(largeNotification())
        }

        assertEquals(64, error.maxMessageBytes)
        assertTrue(error.actualMessageBytes > error.maxMessageBytes)
    }

    @Test
    fun oversizedOutgoingRequestIsRejectedBeforeTransportSend() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(
            transport = transport,
            scope = backgroundScope,
            maxMessageBytes = 64,
        )

        assertFailsWith<JsonRpcMessageSizeException> {
            session.sendRequest("thread/start", largeParams())
        }

        assertTrue(transport.sent.isEmpty())
    }

    private fun largeNotification(): JsonRpcNotification = JsonRpcNotification(
        method = "large/payload",
        params = largeParams(),
    )

    private fun largeParams(): JsonObject = buildJsonObject {
        put("payload", "x".repeat(128))
    }

    private class FakeJsonRpcTransport : JsonRpcTransport {
        private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>()
        private val mutableSent = mutableListOf<JsonRpcMessage>()

        override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

        val sent: List<JsonRpcMessage>
            get() = mutableSent.toList()

        override suspend fun send(message: JsonRpcMessage) {
            mutableSent += message
        }

        suspend fun receive(message: JsonRpcMessage) {
            mutableIncoming.emit(message)
        }

        var closeCount: Int = 0
            private set

        override fun close() {
            closeCount += 1
        }
    }
}
