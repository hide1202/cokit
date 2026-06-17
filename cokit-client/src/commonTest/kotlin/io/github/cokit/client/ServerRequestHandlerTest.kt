package io.github.cokit.client

import io.github.cokit.client.server.UserInputAnswer
import io.github.cokit.client.server.UserInputQuestionId
import io.github.cokit.client.server.UserInputResponse
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ServerRequestHandlerTest {
    @Test
    fun userInputServerRequestsAreTypedAndCancelByDefault() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        val serverRequest = async { fixture.client.serverRequests.first() }

        fixture.transport.receive(userInputRequest(id = 99))
        runCurrent()

        val userInput = assertIs<CodexServerRequest.UserInput>(serverRequest.await())
        assertEquals(ThreadId("thr_123"), userInput.request.threadId)
        assertEquals(TurnId("turn_123"), userInput.request.turnId)
        assertEquals(ItemId("item_123"), userInput.request.itemId)
        assertEquals(120_000, userInput.request.autoResolutionMs)
        assertEquals(UserInputQuestionId("style"), userInput.request.questions.first().id)
        assertEquals("Tone", userInput.request.questions.first().header)
        assertEquals("Pick an answer style.", userInput.request.questions.first().question)
        assertEquals("Concise", userInput.request.questions.first().options?.first()?.label)
        assertEquals(true, userInput.request.questions.last().isOther)
        assertEquals(true, userInput.request.questions.last().isSecret)

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals("cancel", response.result?.jsonObject?.get("decision")?.jsonPrimitive?.content)
    }

    @Test
    fun userInputHandlerCanReturnTypedAnswers() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerUserInputRequestHandler { request ->
            assertEquals(ItemId("item_123"), request.itemId)
            assertEquals(UserInputQuestionId("style"), request.questions.first().id)
            UserInputResponse.Answers(
                answers = mapOf(
                    UserInputQuestionId("style") to UserInputAnswer.choice("Concise"),
                    UserInputQuestionId("notes") to UserInputAnswer.freeForm("Use repository docs."),
                ),
            )
        }

        fixture.transport.receive(userInputRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        val answers = response.result?.jsonObject?.get("answers")?.jsonObject
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(
            "Concise",
            answers?.get("style")?.jsonObject?.get("answers")?.jsonArray?.single()?.jsonPrimitive?.content,
        )
        assertEquals(
            "Use repository docs.",
            answers?.get("notes")?.jsonObject?.get("answers")?.jsonArray?.single()?.jsonPrimitive?.content,
        )
    }

    @Test
    fun malformedUserInputParamsReturnInvalidParamsWithoutCallingTypedHandler() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        var handlerCalled = false
        fixture.client.registerUserInputRequestHandler {
            handlerCalled = true
            UserInputResponse.Cancel
        }

        fixture.transport.receive(
            JsonRpcRequest(
                id = JsonRpcId.Number(99),
                method = "item/tool/requestUserInput",
                params = buildJsonObject {
                    put("threadId", "thr_123")
                    put("turnId", "turn_123")
                    put("itemId", "item_123")
                },
            ),
        )
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32602, response.error?.code)
        assertEquals(
            "Invalid params for item/tool/requestUserInput",
            response.error?.message,
        )
        assertFalse(handlerCalled)
    }

    @Test
    fun userInputHandlerExceptionsReturnGenericJsonRpcError() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)
        fixture.client.registerUserInputRequestHandler {
            error("private prompt context should not leak")
        }

        fixture.transport.receive(userInputRequest(id = 99))
        runCurrent()

        val response = fixture.transport.sent.last() as JsonRpcResponse
        assertEquals(JsonRpcId.Number(99), response.id)
        assertEquals(-32000, response.error?.code)
        assertEquals("Server request handler failed", response.error?.message)
        assertFalse(response.toString().contains("private prompt context"))
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

    private fun userInputRequest(id: Long): JsonRpcRequest = JsonRpcRequest(
        id = JsonRpcId.Number(id),
        method = "item/tool/requestUserInput",
        params = buildJsonObject {
            put("threadId", "thr_123")
            put("turnId", "turn_123")
            put("itemId", "item_123")
            put("autoResolutionMs", 120_000)
            putJsonArray("questions") {
                add(
                    buildJsonObject {
                        put("id", "style")
                        put("header", "Tone")
                        put("question", "Pick an answer style.")
                        putJsonArray("options") {
                            add(
                                buildJsonObject {
                                    put("label", "Concise")
                                    put("description", "Keep the response short.")
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("label", "Detailed")
                                    put("description", "Include implementation details.")
                                },
                            )
                        }
                    },
                )
                add(
                    buildJsonObject {
                        put("id", "notes")
                        put("header", "Notes")
                        put("question", "Add any extra instructions.")
                        put("isOther", true)
                        put("isSecret", true)
                    },
                )
            }
        },
    )

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
