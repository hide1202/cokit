package io.github.cokit.client

import io.github.cokit.client.mcp.McpElicitationFieldType
import io.github.cokit.client.mcp.McpElicitationRequest
import io.github.cokit.client.mcp.McpElicitationResponse
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class McpServerRequestTest {
    @Test
    fun mcpElicitationServerRequestsAreTypedAndDeclineByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(mcpFormElicitationRequest(id = 99))
        runCurrent()

        val elicitation = assertIs<CodexServerRequest.McpElicitation>(serverRequest.await())
        val form = assertIs<McpElicitationRequest.Form>(elicitation.request)
        val ticketId = requireNotNull(form.requestedSchema.properties["ticketId"])
        assertEquals("github", form.serverName)
        assertEquals(ThreadId("thr_123"), form.threadId)
        assertEquals(TurnId("turn_123"), form.turnId)
        assertEquals("Approve lookup details.", form.message)
        assertEquals(
            "mcp_tool_call",
            form.meta?.toJsonElement()?.jsonObject?.get("codex_approval_kind")?.jsonPrimitive?.content,
        )
        assertEquals(
            "always",
            form.meta
                ?.toJsonElement()
                ?.jsonObject
                ?.get("persist")
                ?.jsonArray
                ?.last()
                ?.jsonPrimitive
                ?.content,
        )
        assertEquals(listOf("ticketId"), form.requestedSchema.required)
        assertEquals(McpElicitationFieldType.String, ticketId.type)
        assertEquals("Ticket ID", ticketId.title)
        assertEquals(3, ticketId.minLength)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("decline", response.result?.jsonObject?.get("action")?.jsonPrimitive?.content)
        assertEquals(JsonNull, response.result?.jsonObject?.get("content"))
    }

    @Test
    fun mcpElicitationHandlerCanAcceptFormContent() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerMcpElicitationHandler { request ->
            val form = assertIs<McpElicitationRequest.Form>(request)
            assertEquals("github", form.serverName)
            assertEquals(McpElicitationFieldType.String, form.requestedSchema.properties["ticketId"]?.type)
            McpElicitationResponse.Accept(
                content = CodexJsonPayload.parse("""{"ticketId":"ABC-123"}"""),
            )
        }

        fixture.transport.receive(mcpFormElicitationRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        val result = response.result?.jsonObject
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("accept", result?.get("action")?.jsonPrimitive?.content)
        assertEquals("ABC-123", result?.get("content")?.jsonObject?.get("ticketId")?.jsonPrimitive?.content)
    }

    @Test
    fun mcpElicitationUrlRequestsDecodeNullableTurnAndCanCancel() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerMcpElicitationHandler { request ->
            val url = assertIs<McpElicitationRequest.Url>(request)
            assertEquals("browser", url.serverName)
            assertEquals(ThreadId("thr_123"), url.threadId)
            assertNull(url.turnId)
            assertEquals("Open OAuth approval.", url.message)
            assertEquals("elicit_123", url.elicitationId)
            assertEquals("https://example.invalid/oauth", url.url)
            McpElicitationResponse.Cancel
        }

        fixture.transport.receive(mcpUrlElicitationRequest(id = 100))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(100), response.id)
        assertEquals("cancel", response.result?.jsonObject?.get("action")?.jsonPrimitive?.content)
        assertEquals(JsonNull, response.result?.jsonObject?.get("content"))
    }

    @Test
    fun malformedMcpElicitationParamsReturnInvalidParamsWithoutCallingTypedHandler() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        var handlerCalled = false
        fixture.client.registerMcpElicitationHandler {
            handlerCalled = true
            McpElicitationResponse.Decline
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "mcpServer/elicitation/request",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("mode", "form")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32602, response.error?.code)
        assertEquals(
            "Invalid params for mcpServer/elicitation/request",
            response.error?.message,
        )
        assertFalse(handlerCalled)
    }

    @Test
    fun mcpElicitationHandlerExceptionsReturnGenericJsonRpcError() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerMcpElicitationHandler {
            error("private elicitation context should not leak")
        }

        fixture.transport.receive(mcpFormElicitationRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32000, response.error?.code)
        assertEquals("Server request handler failed", response.error?.message)
        assertFalse(response.toString().contains("private elicitation context"))
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private fun mcpFormElicitationRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "mcpServer/elicitation/request",
        params = buildJsonObject {
            put("serverName", "github")
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("mode", "form")
            put("message", "Approve lookup details.")
            putJsonObject("_meta") {
                put("codex_approval_kind", "mcp_tool_call")
                putJsonArray("persist") {
                    add("session")
                    add("always")
                }
            }
            putJsonObject("requestedSchema") {
                put("type", "object")
                putJsonArray("required") {
                    add("ticketId")
                }
                putJsonObject("properties") {
                    putJsonObject("ticketId") {
                        put("type", "string")
                        put("title", "Ticket ID")
                        put("description", "Issue or ticket identifier.")
                        put("minLength", 3)
                    }
                }
            }
        },
    )

    private fun mcpUrlElicitationRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "mcpServer/elicitation/request",
        params = buildJsonObject {
            put("serverName", "browser")
            put("threadId", "thr_123")
            put("turnId", JsonNull)
            put("mode", "url")
            put("message", "Open OAuth approval.")
            put("url", "https://example.invalid/oauth")
            put("elicitationId", "elicit_123")
        },
    )

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
