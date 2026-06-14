package io.github.cokit.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault

@Serializable
@JvmInline
value class TurnId(val value: String)

@Serializable
data class Turn(
    val id: TurnId,
    val status: String,
    val items: List<CodexJsonPayload> = emptyList(),
    val error: CodexJsonPayload? = null,
)

@Serializable
data class StartTurnRequest(
    val threadId: ThreadId,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val input: List<TurnInput> = emptyList(),
    val cwd: CodexHostPath? = null,
    val approvalPolicy: ApprovalPolicy? = null,
    val sandboxPolicy: SandboxPolicy? = null,
    val permissions: CodexJsonPayload? = null,
    val model: ModelName? = null,
    val effort: ReasoningEffort? = null,
    val outputSchema: CodexJsonPayload? = null,
)

@Serializable
data class SteerTurnRequest(
    val threadId: ThreadId,
    val expectedTurnId: TurnId,
    val input: List<TurnInput>,
    val clientUserMessageId: String? = null,
)

@Serializable
data class InterruptTurnRequest(
    val threadId: ThreadId,
    val turnId: TurnId,
)
