package io.github.cokit.client

import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.rpc.JsonRpcSession
import io.github.cokit.rpc.JsonRpcTransport
import kotlinx.coroutines.CoroutineScope

class CodexAppServerClient private constructor(
    val rpc: JsonRpcSession,
) : AutoCloseable {
    val threads: ThreadsApi = ThreadsApi(rpc)
    val turns: TurnsApi = TurnsApi(rpc)

    override fun close() {
        rpc.close()
    }

    companion object {
        suspend fun connect(
            transport: JsonRpcTransport,
            clientInfo: ClientInfo,
            scope: CoroutineScope,
            capabilities: InitializeCapabilities? = null,
        ): CodexAppServerClient {
            val session = JsonRpcSession(transport, scope)
            session.sendRequest("initialize")
            session.notify("initialized")
            return CodexAppServerClient(session)
        }
    }
}

class ThreadsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(): JsonRpcId = rpc.sendRequest("thread/start")
}

class TurnsApi internal constructor(
    private val rpc: JsonRpcSession,
) {
    suspend fun start(): JsonRpcId = rpc.sendRequest("turn/start")
}
