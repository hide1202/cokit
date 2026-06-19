package io.github.cokit.rpc

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcErrorObject
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString

class JsonRpcSession(
    private val transport: JsonRpcTransport,
    private val scope: CoroutineScope,
    private val maxMessageBytes: Int = DEFAULT_MAX_MESSAGE_BYTES,
) : AutoCloseable {
    init {
        require(maxMessageBytes > 0) { "maxMessageBytes must be greater than zero" }
    }

    private var nextRequestId = 1L
    private var closed = false
    private val mutex = Mutex()
    private val pendingRequests = mutableMapOf<JsonRpcId, CompletableDeferred<JsonElementResult>>()
    private val mutableNotifications = MutableSharedFlow<JsonRpcNotification>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val mutableServerRequests = MutableSharedFlow<JsonRpcRequest>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val collectorJob: Job = scope.launch {
        try {
            transport.incoming.collect { routeIncoming(it) }
        } catch (error: Throwable) {
            cancelPending(error)
        }
    }

    val notifications: SharedFlow<JsonRpcNotification> = mutableNotifications
    val serverRequests: SharedFlow<JsonRpcRequest> = mutableServerRequests

    suspend fun notify(method: String) {
        requireOpen()
        val message = JsonRpcNotification(method = method)
        requireWithinMessageLimit(message)
        transport.send(message)
    }

    suspend fun sendRequest(method: String, params: JsonElementResult = null): JsonRpcId {
        val id = nextId()
        val message = JsonRpcRequest(id = id, method = method, params = params)
        requireWithinMessageLimit(message)
        transport.send(message)
        return id
    }

    suspend fun request(method: String, params: JsonElementResult = null): JsonElementResult {
        val id = nextId()
        val message = JsonRpcRequest(id = id, method = method, params = params)
        requireWithinMessageLimit(message)
        val deferred = CompletableDeferred<JsonElementResult>()
        mutex.withLock {
            if (closed) throw closedCancellationException()
            pendingRequests[id] = deferred
        }
        try {
            transport.send(message)
        } catch (error: Throwable) {
            cancelPending(id, error)
            throw error
        }
        return deferred.await()
    }

    suspend fun sendResponse(response: JsonRpcResponse) {
        requireOpen()
        requireWithinMessageLimit(response)
        transport.send(response)
    }

    suspend fun publishForTests(message: JsonRpcMessage) {
        routeIncoming(message)
    }

    private suspend fun nextId(): JsonRpcId = mutex.withLock {
        if (closed) throw closedCancellationException()
        JsonRpcId.Number(nextRequestId++)
    }

    private suspend fun requireOpen() {
        mutex.withLock {
            if (closed) throw closedCancellationException()
        }
    }

    private suspend fun routeIncoming(message: JsonRpcMessage) {
        requireWithinMessageLimit(message)
        when (message) {
            is JsonRpcResponse -> completeResponse(message)
            is JsonRpcNotification -> mutableNotifications.emit(message)
            is JsonRpcRequest -> mutableServerRequests.emit(message)
        }
    }

    private suspend fun completeResponse(response: JsonRpcResponse) {
        val deferred = mutex.withLock {
            pendingRequests.remove(response.id)
        } ?: return

        val error = response.error
        if (error != null) {
            deferred.completeExceptionally(JsonRpcRemoteException(error))
        } else {
            deferred.complete(response.result)
        }
    }

    private suspend fun cancelPending(id: JsonRpcId, error: Throwable) {
        val deferred = mutex.withLock {
            pendingRequests.remove(id)
        } ?: return
        deferred.completeExceptionally(error)
    }

    private suspend fun cancelPending(error: Throwable) {
        val requests = mutex.withLock {
            pendingRequests.values.toList().also { pendingRequests.clear() }
        }
        requests.forEach { it.completeExceptionally(error) }
    }

    override fun close() {
        if (closed) return
        closed = true
        collectorJob.cancel()
        transport.close()
        val cancellation = closedCancellationException()
        pendingRequests.values.forEach { it.completeExceptionally(cancellation) }
        pendingRequests.clear()
    }

    private fun requireWithinMessageLimit(message: JsonRpcMessage) {
        val actualMessageBytes = CodexProtocolJson.encodeToString(message).encodeToByteArray().size
        if (actualMessageBytes > maxMessageBytes) {
            throw JsonRpcMessageSizeException(
                actualMessageBytes = actualMessageBytes,
                maxMessageBytes = maxMessageBytes,
            )
        }
    }

    companion object {
        const val DEFAULT_MAX_MESSAGE_BYTES: Int = 16 * 1024 * 1024
    }
}

private fun closedCancellationException(): CancellationException =
    CancellationException("JSON-RPC session closed")

typealias JsonElementResult = kotlinx.serialization.json.JsonElement?

class JsonRpcRemoteException(
    val error: JsonRpcErrorObject,
) : RuntimeException(error.message) {
    val isRetryableOverload: Boolean
        get() = error.code == SERVER_OVERLOADED_CODE

    companion object {
        const val SERVER_OVERLOADED_CODE: Int = -32001
    }
}

class JsonRpcMessageSizeException(
    val actualMessageBytes: Int,
    val maxMessageBytes: Int,
) : RuntimeException(
    "JSON-RPC message is $actualMessageBytes bytes, exceeding the configured $maxMessageBytes byte limit",
)
