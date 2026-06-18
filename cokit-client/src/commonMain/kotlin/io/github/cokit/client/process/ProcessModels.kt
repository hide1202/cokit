package io.github.cokit.client.process

import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.ExperimentalCodexApi
import kotlinx.serialization.Serializable

@ExperimentalCodexApi
@JvmInline
@Serializable
value class ProcessHandle(val value: String)

@ExperimentalCodexApi
@Serializable
data class ProcessTerminalSize(
    val cols: Int,
    val rows: Int,
)

@ExperimentalCodexApi
@Serializable
data class ProcessSpawnParams(
    val command: List<String>,
    val cwd: CodexHostPath,
    val processHandle: ProcessHandle,
    val env: Map<String, String?>? = null,
    val outputBytesCap: Long? = null,
    val timeoutMs: Long? = null,
    val tty: Boolean? = null,
    val size: ProcessTerminalSize? = null,
    val streamStdin: Boolean? = null,
    val streamStdoutStderr: Boolean? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProcessWriteStdinParams(
    val processHandle: ProcessHandle,
    val deltaBase64: String? = null,
    val closeStdin: Boolean? = null,
)

@ExperimentalCodexApi
@Serializable
data class ProcessKillParams(
    val processHandle: ProcessHandle,
)

@ExperimentalCodexApi
@Serializable
data class ProcessResizePtyParams(
    val processHandle: ProcessHandle,
    val size: ProcessTerminalSize,
)
