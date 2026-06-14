package io.github.cokit.client

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.rpc.JsonRpcSession
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal inline fun <reified T> JsonElement?.decodeResult(): T {
    val element = requireNotNull(this) { "Expected a JSON-RPC result payload" }
    return CodexProtocolJson.decodeFromJsonElement(element)
}

internal fun <T> JsonElement?.decodeResult(
    deserializer: DeserializationStrategy<T>,
): T {
    val element = requireNotNull(this) { "Expected a JSON-RPC result payload" }
    return CodexProtocolJson.decodeFromJsonElement(deserializer, element)
}

internal fun <T : Any> JsonElement?.decodeResult(
    method: CodexRpcMethod<*, T>,
): T {
    if (this == null) {
        return requireNotNull(method.emptyResult) {
            "Expected a JSON-RPC result payload"
        }
    }
    return CodexProtocolJson.decodeFromJsonElement(method.resultSerializer, this)
}

internal suspend fun <P : Any, R : Any> JsonRpcSession.request(
    method: CodexRpcMethod<P, R>,
    params: P,
): R {
    val result = request(
        method = method.method,
        params = CodexProtocolJson.encodeToJsonElement(method.paramsSerializer, params),
    )
    return result.decodeResult(method)
}
