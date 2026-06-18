package io.github.cokit.client.commands

import io.github.cokit.client.CodexHostPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommandExecParams(
    val command: List<String>,
    val cwd: CodexHostPath? = null,
    val disableOutputCap: Boolean? = null,
    val disableTimeout: Boolean? = null,
    val env: Map<String, String?>? = null,
    val outputBytesCap: Long? = null,
    val tty: Boolean? = null,
    val processId: CommandProcessId? = null,
    val sandboxPolicy: CommandSandboxPolicy? = null,
    val size: CommandExecTerminalSize? = null,
    val streamStdin: Boolean? = null,
    val streamStdoutStderr: Boolean? = null,
    val timeoutMs: Long? = null,
)

@Serializable
data class CommandExecResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

@Serializable
data class CommandExecWriteParams(
    val processId: CommandProcessId,
    val deltaBase64: String? = null,
    val closeStdin: Boolean? = null,
)

@Serializable
data class CommandExecResizeParams(
    val processId: CommandProcessId,
    val size: CommandExecTerminalSize,
)

@Serializable
data class CommandExecTerminateParams(
    val processId: CommandProcessId,
)

@Serializable
@JvmInline
value class CommandExecOutputStream(val value: String) {
    companion object {
        val Stdout = CommandExecOutputStream("stdout")
        val Stderr = CommandExecOutputStream("stderr")
    }
}

@Serializable
@JvmInline
value class CommandProcessId(val value: String)

@Serializable
data class CommandExecTerminalSize(
    val cols: Int,
    val rows: Int,
)

@Serializable
sealed interface CommandSandboxPolicy {
    @Serializable
    @SerialName("dangerFullAccess")
    data object DangerFullAccess : CommandSandboxPolicy

    @Serializable
    @SerialName("readOnly")
    data class ReadOnly(
        val networkAccess: Boolean? = null,
    ) : CommandSandboxPolicy

    @Serializable
    @SerialName("externalSandbox")
    data class ExternalSandbox(
        val networkAccess: CommandNetworkAccess? = null,
    ) : CommandSandboxPolicy

    @Serializable
    @SerialName("workspaceWrite")
    data class WorkspaceWrite(
        val writableRoots: List<CodexHostPath> = emptyList(),
        val networkAccess: Boolean? = null,
        val excludeTmpdirEnvVar: Boolean? = null,
        val excludeSlashTmp: Boolean? = null,
    ) : CommandSandboxPolicy
}

@Serializable
@JvmInline
value class CommandNetworkAccess(val value: String) {
    companion object {
        val Restricted = CommandNetworkAccess("restricted")
        val Enabled = CommandNetworkAccess("enabled")
    }
}
