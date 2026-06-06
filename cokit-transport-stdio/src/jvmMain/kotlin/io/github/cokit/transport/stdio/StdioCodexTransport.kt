package io.github.cokit.transport.stdio

import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcMessage
import io.github.cokit.rpc.JsonRpcTransport
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class StdioCodexTransport internal constructor(
    val command: List<String>,
    input: InputStream,
    output: OutputStream,
    scope: CoroutineScope,
    onClose: () -> Unit = {},
) : JsonRpcTransport {
    private val delegate = StreamStdioTransport(
        input = input,
        output = output,
        scope = scope,
        onClose = onClose,
    )

    constructor(
        command: List<String> = listOf("codex", "app-server", "--stdio"),
        cwd: File? = null,
        env: Map<String, String> = emptyMap(),
    ) : this(command, startProcess(command, cwd, env))

    internal constructor(
        input: InputStream,
        output: OutputStream,
        scope: CoroutineScope,
    ) : this(
        command = emptyList(),
        input = input,
        output = output,
        scope = scope,
    )

    private constructor(
        command: List<String>,
        process: Process,
    ) : this(
        command = command,
        input = process.inputStream,
        output = process.outputStream,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        onClose = { process.destroy() },
    )

    override val incoming: SharedFlow<JsonRpcMessage> = delegate.incoming

    override suspend fun send(message: JsonRpcMessage) {
        delegate.send(message)
    }

    override fun close() {
        delegate.close()
    }

    companion object {
        fun proxy(sockPath: String? = null): StdioCodexTransport {
            val command = buildList {
                add("codex")
                add("app-server")
                add("proxy")
                sockPath?.let { add(it) }
            }
            return StdioCodexTransport(command)
        }

        private fun startProcess(
            command: List<String>,
            cwd: File?,
            env: Map<String, String>,
        ): Process {
            return ProcessBuilder(command)
                .also { builder ->
                    cwd?.let { builder.directory(it) }
                    builder.environment().putAll(env)
                }
                .start()
        }
    }
}

private class StreamStdioTransport(
    input: InputStream,
    output: OutputStream,
    scope: CoroutineScope,
    private val onClose: () -> Unit = {},
) : JsonRpcTransport {
    private val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
    private val writer = BufferedWriter(OutputStreamWriter(output, Charsets.UTF_8))
    private val writeMutex = Mutex()
    private val mutableIncoming = MutableSharedFlow<JsonRpcMessage>(
        replay = 64,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val readJob: Job = scope.launch {
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isNotBlank()) {
                mutableIncoming.emit(CodexProtocolJson.decodeFromString<JsonRpcMessage>(line))
            }
        }
    }

    override val incoming: SharedFlow<JsonRpcMessage> = mutableIncoming

    override suspend fun send(message: JsonRpcMessage) {
        val line = CodexProtocolJson.encodeToString(message)
        writeMutex.withLock {
            writer.write(line)
            writer.newLine()
            writer.flush()
        }
    }

    override fun close() {
        readJob.cancel()
        runCatching { writer.close() }
        runCatching { reader.close() }
        onClose()
    }
}
