package io.github.cokit.rpc

import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
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
    fun routesNotificationsToNotificationFlow() = runTest {
        val transport = FakeJsonRpcTransport()
        val session = JsonRpcSession(transport, backgroundScope)
        val received = async { session.notifications.first() }
        runCurrent()

        transport.receive(JsonRpcNotification(method = "turn/completed"))

        assertEquals(JsonRpcNotification(method = "turn/completed"), received.await())
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

        override fun close() = Unit
    }
}
