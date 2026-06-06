package io.github.cokit.rpc

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

class JsonRpcSession(
    private val transport: JsonRpcTransport,
    private val scope: CoroutineScope,
) : AutoCloseable {
    private var nextRequestId = 1L
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
        transport.send(JsonRpcNotification(method = method))
    }

    suspend fun sendRequest(method: String, params: JsonElementResult = null): JsonRpcId {
        val id = nextId()
        transport.send(JsonRpcRequest(id = id, method = method, params = params))
        return id
    }

    suspend fun request(method: String, params: JsonElementResult = null): JsonElementResult {
        val id = nextId()
        val deferred = CompletableDeferred<JsonElementResult>()
        mutex.withLock {
            pendingRequests[id] = deferred
        }
        transport.send(JsonRpcRequest(id = id, method = method, params = params))
        return deferred.await()
    }

    suspend fun sendResponse(response: JsonRpcResponse) {
        transport.send(response)
    }

    suspend fun publishForTests(message: JsonRpcMessage) {
        routeIncoming(message)
    }

    private suspend fun nextId(): JsonRpcId = mutex.withLock {
        JsonRpcId.Number(nextRequestId++)
    }

    private suspend fun routeIncoming(message: JsonRpcMessage) {
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

    private suspend fun cancelPending(error: Throwable) {
        val requests = mutex.withLock {
            pendingRequests.values.toList().also { pendingRequests.clear() }
        }
        requests.forEach { it.completeExceptionally(error) }
    }

    override fun close() {
        collectorJob.cancel()
        transport.close()
        val cancellation = CancellationException("JSON-RPC session closed")
        pendingRequests.values.forEach { it.completeExceptionally(cancellation) }
        pendingRequests.clear()
    }
}

typealias JsonElementResult = kotlinx.serialization.json.JsonElement?

class JsonRpcRemoteException(
    val error: JsonRpcErrorObject,
) : RuntimeException(error.message)
