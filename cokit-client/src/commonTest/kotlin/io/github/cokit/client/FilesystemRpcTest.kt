package io.github.cokit.client

import io.github.cokit.client.filesystem.FilesystemGetMetadataParams
import io.github.cokit.client.filesystem.FilesystemReadDirectoryParams
import io.github.cokit.client.filesystem.FilesystemReadFileParams
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FilesystemRpcTest {
    @Test
    fun filesystemReadDescriptorsUseHostPathsAndDecodeResponses() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val readFileResult = async {
            fixture.client.request(
                CodexRpc.Filesystem.ReadFile,
                FilesystemReadFileParams(path = CodexHostPath("/path/to/project/README.md")),
            )
        }
        runCurrent()

        val readFile = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("fs/readFile", readFile.method)
        assertEquals(
            "/path/to/project/README.md",
            readFile.params!!.jsonObject["path"]?.jsonPrimitive?.contentOrNull,
        )
        fixture.transport.receive(
            JsonRpcResponse(
                readFile.id,
                result = buildJsonObject {
                    put("dataBase64", "SGVsbG8K")
                },
            ),
        )
        assertEquals("SGVsbG8K", readFileResult.await().dataBase64)

        val metadataResult = async {
            fixture.client.request(
                CodexRpc.Filesystem.GetMetadata,
                FilesystemGetMetadataParams(path = CodexHostPath("/path/to/project/src")),
            )
        }
        runCurrent()

        val metadata = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("fs/getMetadata", metadata.method)
        assertEquals(
            "/path/to/project/src",
            metadata.params!!.jsonObject["path"]?.jsonPrimitive?.contentOrNull,
        )
        fixture.transport.receive(
            JsonRpcResponse(
                metadata.id,
                result = buildJsonObject {
                    put("isDirectory", true)
                    put("isFile", false)
                    put("isSymlink", false)
                    put("createdAtMs", 1_710_000_000_000L)
                    put("modifiedAtMs", 1_710_000_001_000L)
                },
            ),
        )
        val metadataDecoded = metadataResult.await()
        assertEquals(true, metadataDecoded.isDirectory)
        assertEquals(false, metadataDecoded.isFile)
        assertEquals(false, metadataDecoded.isSymlink)
        assertEquals(1_710_000_000_000L, metadataDecoded.createdAtMs)
        assertEquals(1_710_000_001_000L, metadataDecoded.modifiedAtMs)

        val readDirectoryResult = async {
            fixture.client.request(
                CodexRpc.Filesystem.ReadDirectory,
                FilesystemReadDirectoryParams(path = CodexHostPath("/path/to/project")),
            )
        }
        runCurrent()

        val readDirectory = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("fs/readDirectory", readDirectory.method)
        assertEquals(
            "/path/to/project",
            readDirectory.params!!.jsonObject["path"]?.jsonPrimitive?.contentOrNull,
        )
        fixture.transport.receive(
            JsonRpcResponse(
                readDirectory.id,
                result = buildJsonObject {
                    put(
                        "entries",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("fileName", "src")
                                    put("isDirectory", true)
                                    put("isFile", false)
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("fileName", "README.md")
                                    put("isDirectory", false)
                                    put("isFile", true)
                                },
                            )
                        },
                    )
                },
            ),
        )

        val entries = readDirectoryResult.await().entries
        assertEquals(2, entries.size)
        assertEquals("src", entries[0].fileName)
        assertEquals(true, entries[0].isDirectory)
        assertEquals(false, entries[0].isFile)
        assertEquals("README.md", entries[1].fileName)
        assertEquals(false, entries[1].isDirectory)
        assertEquals(true, entries[1].isFile)
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
