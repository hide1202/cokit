package io.github.cokit.client

import io.github.cokit.client.config.ConfigBatchWriteParams
import io.github.cokit.client.config.ConfigEdit
import io.github.cokit.client.config.ConfigKeyPath
import io.github.cokit.client.config.ConfigLayerSource
import io.github.cokit.client.config.ConfigMergeStrategy
import io.github.cokit.client.config.ConfigReadParams
import io.github.cokit.client.config.ConfigValue
import io.github.cokit.client.config.ConfigValueWriteParams
import io.github.cokit.client.config.ConfigWriteStatus
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ConfigRpcTest {
    @Test
    fun configReadDescriptorSendsScopeAndDecodesRawConfigBehindValueWrappers() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Config.Read,
                ConfigReadParams(
                    cwd = CodexHostPath("/path/to/project"),
                    includeLayers = true,
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("config/read", request.method)
        val params = request.params!!.jsonObject
        assertEquals("/path/to/project", params["cwd"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, params["includeLayers"]?.jsonPrimitive?.booleanOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put("config", buildJsonObject {
                        put("model", "gpt-5")
                        put("approval_policy", "on-request")
                    })
                    put(
                        "layers",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("version", "user-v1")
                                    put("name", buildJsonObject {
                                        put("type", "user")
                                        put("file", "/path/to/config.toml")
                                        put("profile", "work")
                                    })
                                    put("config", buildJsonObject {
                                        put("model", "gpt-5")
                                    })
                                },
                            )
                        },
                    )
                    put("origins", buildJsonObject {
                        put("model", buildJsonObject {
                            put("version", "user-v1")
                            put("name", buildJsonObject {
                                put("type", "user")
                                put("file", "/path/to/config.toml")
                                put("profile", "work")
                            })
                        })
                    })
                },
            ),
        )

        val decoded = result.await()
        assertEquals("""{"model":"gpt-5","approval_policy":"on-request"}""", decoded.config.toJsonString())
        val layer = decoded.layers.orEmpty().single()
        assertEquals("""{"model":"gpt-5"}""", layer.config.toJsonString())
        val source = assertIs<ConfigLayerSource.User>(layer.name)
        assertEquals(CodexHostPath("/path/to/config.toml"), source.file)
        assertEquals("work", source.profile)
        assertEquals("user-v1", decoded.origins.getValue("model").version)
    }

    @Test
    fun configWriteDescriptorsSendRawValuesAndDecodeWriteMetadata() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val writeResult = async {
            fixture.client.request(
                CodexRpc.Config.WriteValue,
                ConfigValueWriteParams(
                    keyPath = ConfigKeyPath("model"),
                    value = ConfigValue.parse(""""gpt-5""""),
                    mergeStrategy = ConfigMergeStrategy.Upsert,
                    filePath = CodexHostPath("/path/to/config.toml"),
                    expectedVersion = "user-v1",
                ),
            )
        }
        runCurrent()

        val write = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("config/value/write", write.method)
        val writeParams = write.params!!.jsonObject
        assertEquals("model", writeParams["keyPath"]?.jsonPrimitive?.contentOrNull)
        assertEquals("gpt-5", writeParams["value"]?.jsonPrimitive?.contentOrNull)
        assertEquals("upsert", writeParams["mergeStrategy"]?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/config.toml", writeParams["filePath"]?.jsonPrimitive?.contentOrNull)
        assertEquals("user-v1", writeParams["expectedVersion"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                write.id,
                result = buildJsonObject {
                    put("filePath", "/path/to/config.toml")
                    put("status", "ok")
                    put("version", "user-v2")
                },
            ),
        )

        val writeDecoded = writeResult.await()
        assertEquals(CodexHostPath("/path/to/config.toml"), writeDecoded.filePath)
        assertEquals(ConfigWriteStatus.Ok, writeDecoded.status)
        assertEquals("user-v2", writeDecoded.version)

        val batchResult = async {
            fixture.client.request(
                CodexRpc.Config.BatchWrite,
                ConfigBatchWriteParams(
                    edits = listOf(
                        ConfigEdit(
                            keyPath = ConfigKeyPath("model_reasoning_effort"),
                            value = ConfigValue.parse(""""high""""),
                            mergeStrategy = ConfigMergeStrategy.Replace,
                        ),
                    ),
                    filePath = CodexHostPath("/path/to/config.toml"),
                    expectedVersion = "user-v2",
                    reloadUserConfig = true,
                ),
            )
        }
        runCurrent()

        val batch = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("config/batchWrite", batch.method)
        val batchParams = batch.params!!.jsonObject
        assertEquals("user-v2", batchParams["expectedVersion"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, batchParams["reloadUserConfig"]?.jsonPrimitive?.booleanOrNull)
        val edit = batchParams["edits"]!!.jsonArray.single().jsonObject
        assertEquals("model_reasoning_effort", edit["keyPath"]?.jsonPrimitive?.contentOrNull)
        assertEquals("high", edit["value"]?.jsonPrimitive?.contentOrNull)
        assertEquals("replace", edit["mergeStrategy"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                batch.id,
                result = buildJsonObject {
                    put("filePath", "/path/to/config.toml")
                    put("status", "okOverridden")
                    put("version", "user-v3")
                    put("overriddenMetadata", buildJsonObject {
                        put("effectiveValue", "medium")
                        put("message", "Enterprise policy overrides this value.")
                        put("overridingLayer", buildJsonObject {
                            put("version", "managed-v1")
                            put("name", buildJsonObject {
                                put("type", "enterpriseManaged")
                                put("id", "policy_1")
                                put("name", "Managed defaults")
                            })
                        })
                    })
                },
            ),
        )

        val batchDecoded = batchResult.await()
        assertEquals(ConfigWriteStatus.OkOverridden, batchDecoded.status)
        assertEquals("medium", batchDecoded.overriddenMetadata?.effectiveValue?.toJsonString()?.trim('"'))
        assertEquals("Enterprise policy overrides this value.", batchDecoded.overriddenMetadata?.message)
        val overridingLayer = assertIs<ConfigLayerSource.EnterpriseManaged>(
            batchDecoded.overriddenMetadata?.overridingLayer?.name,
        )
        assertEquals("policy_1", overridingLayer.id)
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

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
