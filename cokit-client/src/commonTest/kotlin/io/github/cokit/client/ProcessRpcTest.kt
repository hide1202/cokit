package io.github.cokit.client

import io.github.cokit.client.process.ProcessHandle
import io.github.cokit.client.process.ProcessKillParams
import io.github.cokit.client.process.ProcessResizePtyParams
import io.github.cokit.client.process.ProcessSpawnParams
import io.github.cokit.client.process.ProcessTerminalSize
import io.github.cokit.client.process.ProcessWriteStdinParams
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProcessRpcTest {
    @Test
    fun experimentalProcessDescriptorsUseSchemaMethodNamesAndWireShapes() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val spawnResult = async {
            fixture.client.request(
                CodexRpc.Process.Spawn,
                ProcessSpawnParams(
                    command = listOf("npm", "test"),
                    cwd = CodexHostPath("/path/to/project"),
                    processHandle = ProcessHandle("proc_test"),
                    outputBytesCap = 8192,
                    timeoutMs = 30_000,
                    tty = true,
                    size = ProcessTerminalSize(cols = 120, rows = 40),
                    streamStdin = true,
                    streamStdoutStderr = true,
                ),
            )
        }
        runCurrent()

        val spawn = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("process/spawn", spawn.method)
        val spawnParams = spawn.params!!.jsonObject
        assertEquals("npm", spawnParams["command"]?.jsonArray?.get(0)?.jsonPrimitive?.contentOrNull)
        assertEquals("test", spawnParams["command"]?.jsonArray?.get(1)?.jsonPrimitive?.contentOrNull)
        assertEquals("/path/to/project", spawnParams["cwd"]?.jsonPrimitive?.contentOrNull)
        assertEquals("proc_test", spawnParams["processHandle"]?.jsonPrimitive?.contentOrNull)
        assertEquals("8192", spawnParams["outputBytesCap"]?.jsonPrimitive?.contentOrNull)
        assertEquals("30000", spawnParams["timeoutMs"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, spawnParams["tty"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, spawnParams["streamStdin"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(true, spawnParams["streamStdoutStderr"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("120", spawnParams["size"]?.jsonObject?.get("cols")?.jsonPrimitive?.contentOrNull)
        assertEquals("40", spawnParams["size"]?.jsonObject?.get("rows")?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(JsonRpcResponse(spawn.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, spawnResult.await())

        val stdinResult = async {
            fixture.client.request(
                CodexRpc.Process.WriteStdin,
                ProcessWriteStdinParams(
                    processHandle = ProcessHandle("proc_test"),
                    deltaBase64 = "aGVsbG8K",
                    closeStdin = true,
                ),
            )
        }
        runCurrent()

        val stdin = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("process/writeStdin", stdin.method)
        val stdinParams = stdin.params!!.jsonObject
        assertEquals("proc_test", stdinParams["processHandle"]?.jsonPrimitive?.contentOrNull)
        assertEquals("aGVsbG8K", stdinParams["deltaBase64"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, stdinParams["closeStdin"]?.jsonPrimitive?.booleanOrNull)
        fixture.transport.receive(JsonRpcResponse(stdin.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, stdinResult.await())

        val killResult = async {
            fixture.client.request(
                CodexRpc.Process.Kill,
                ProcessKillParams(processHandle = ProcessHandle("proc_test")),
            )
        }
        runCurrent()

        val kill = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("process/kill", kill.method)
        assertEquals("proc_test", kill.params!!.jsonObject["processHandle"]?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(JsonRpcResponse(kill.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, killResult.await())

        val resizeResult = async {
            fixture.client.request(
                CodexRpc.Process.ResizePty,
                ProcessResizePtyParams(
                    processHandle = ProcessHandle("proc_test"),
                    size = ProcessTerminalSize(cols = 100, rows = 30),
                ),
            )
        }
        runCurrent()

        val resize = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("process/resizePty", resize.method)
        val resizeParams = resize.params!!.jsonObject
        assertEquals("proc_test", resizeParams["processHandle"]?.jsonPrimitive?.contentOrNull)
        assertEquals("100", resizeParams["size"]?.jsonObject?.get("cols")?.jsonPrimitive?.contentOrNull)
        assertEquals("30", resizeParams["size"]?.jsonObject?.get("rows")?.jsonPrimitive?.contentOrNull)
        fixture.transport.receive(JsonRpcResponse(resize.id, result = JsonObject(emptyMap())))
        assertEquals(CodexRpcUnit, resizeResult.await())
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
