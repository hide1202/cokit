package io.github.cokit.client

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ThreadId(val value: String)

@Serializable
data class Thread(
    val id: ThreadId,
    val preview: String? = null,
    val modelProvider: String? = null,
    val createdAt: Long? = null,
)

@Serializable
data class ThreadList(
    val threads: List<Thread> = emptyList(),
    val cursor: String? = null,
)

@Serializable
data class StartThreadRequest(
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandbox: SandboxPolicy? = null,
    val permissions: CodexJsonPayload? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val personality: String? = null,
)

@Serializable
data class ResumeThreadRequest(
    val threadId: ThreadId,
    val excludeTurns: List<TurnId> = emptyList(),
    val initialTurnsPage: CodexJsonPayload? = null,
)

@Serializable
data class ForkThreadRequest(
    val threadId: ThreadId,
    val ephemeral: Boolean? = null,
    val excludeTurns: List<TurnId> = emptyList(),
)

@Serializable
data class ListThreadsRequest(
    val cursor: String? = null,
    val limit: Int? = null,
    val cwd: CodexHostPath? = null,
    val archived: Boolean? = null,
    val searchTerm: String? = null,
)

@Serializable
data class ReadThreadRequest(
    val threadId: ThreadId,
    val includeTurns: Boolean? = null,
)

@Serializable
data class SetThreadNameRequest(
    val threadId: ThreadId,
    val name: String,
)
