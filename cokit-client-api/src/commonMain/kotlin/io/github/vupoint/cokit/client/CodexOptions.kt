package io.github.vupoint.cokit.client

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CodexHostPath(val value: String)

@Serializable
@JvmInline
value class ModelName(val value: String)

@Serializable
@JvmInline
value class ApprovalPolicy(val value: String) {
    companion object {
        val Untrusted = ApprovalPolicy("untrusted")
        val OnFailure = ApprovalPolicy("on-failure")
        val OnRequest = ApprovalPolicy("on-request")
        val Never = ApprovalPolicy("never")
    }
}

@Serializable
@JvmInline
value class SandboxPolicy(val value: String) {
    companion object {
        val ReadOnly = SandboxPolicy("readOnly")
        val WorkspaceWrite = SandboxPolicy("workspaceWrite")
        val DangerFullAccess = SandboxPolicy("dangerFullAccess")
    }
}

@Serializable
@JvmInline
value class ReasoningEffort(val value: String) {
    companion object {
        val Low = ReasoningEffort("low")
        val Medium = ReasoningEffort("medium")
        val High = ReasoningEffort("high")
    }
}
