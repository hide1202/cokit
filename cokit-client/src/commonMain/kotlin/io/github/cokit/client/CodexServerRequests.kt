package io.github.cokit.client

import io.github.cokit.client.approvals.ApprovalDecision
import io.github.cokit.client.approvals.CommandApprovalRequest
import io.github.cokit.client.approvals.FileChangeApprovalRequest
import io.github.cokit.client.approvals.PermissionApprovalRequest
import io.github.cokit.client.approvals.PermissionApprovalResponse
import io.github.cokit.client.mcp.McpElicitationRequest
import io.github.cokit.client.mcp.McpElicitationResponse
import io.github.cokit.client.server.UserInputRequest
import io.github.cokit.client.server.UserInputResponse
import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcErrorObject
import io.github.cokit.protocol.JsonRpcRequest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal const val COMMAND_APPROVAL_METHOD = "item/commandExecution/requestApproval"
internal const val FILE_CHANGE_APPROVAL_METHOD = "item/fileChange/requestApproval"
internal const val PERMISSION_APPROVAL_METHOD = "item/permissions/requestApproval"
internal const val USER_INPUT_REQUEST_METHOD = "item/tool/requestUserInput"
internal const val MCP_ELICITATION_REQUEST_METHOD = "mcpServer/elicitation/request"

sealed interface CodexServerRequest {
    val method: String

    data class CommandApproval(
        val request: CommandApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = COMMAND_APPROVAL_METHOD
    }

    data class FileChangeApproval(
        val request: FileChangeApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = FILE_CHANGE_APPROVAL_METHOD
    }

    data class PermissionApproval(
        val request: PermissionApprovalRequest,
    ) : CodexServerRequest {
        override val method: String = PERMISSION_APPROVAL_METHOD
    }

    data class UserInput(
        val request: UserInputRequest,
    ) : CodexServerRequest {
        override val method: String = USER_INPUT_REQUEST_METHOD
    }

    data class McpElicitation(
        val request: McpElicitationRequest,
    ) : CodexServerRequest {
        override val method: String = MCP_ELICITATION_REQUEST_METHOD
    }

    data class Unsupported(
        override val method: String,
    ) : CodexServerRequest
}

data class CodexRawServerRequest(
    val method: String,
    val params: CodexJsonPayload? = null,
)

internal fun JsonRpcRequest.toCodexServerRequest(): CodexServerRequest {
    return when (method) {
        COMMAND_APPROVAL_METHOD -> runCatching {
            CodexServerRequest.CommandApproval(decodeCommandApprovalRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        FILE_CHANGE_APPROVAL_METHOD -> runCatching {
            CodexServerRequest.FileChangeApproval(decodeFileChangeApprovalRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        PERMISSION_APPROVAL_METHOD -> runCatching {
            CodexServerRequest.PermissionApproval(decodePermissionApprovalRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        USER_INPUT_REQUEST_METHOD -> runCatching {
            CodexServerRequest.UserInput(decodeUserInputRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        MCP_ELICITATION_REQUEST_METHOD -> runCatching {
            CodexServerRequest.McpElicitation(decodeMcpElicitationRequest())
        }.getOrElse {
            CodexServerRequest.Unsupported(method)
        }
        else -> CodexServerRequest.Unsupported(method)
    }
}

internal fun JsonRpcRequest.decodeCommandApprovalRequest(): CommandApprovalRequest {
    val paramsElement = requireNotNull(params) {
        "Expected command approval request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(CommandApprovalRequest.serializer(), paramsElement)
}

internal fun JsonRpcRequest.decodeFileChangeApprovalRequest(): FileChangeApprovalRequest {
    val paramsElement = requireNotNull(params) {
        "Expected file change approval request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(FileChangeApprovalRequest.serializer(), paramsElement)
}

internal fun JsonRpcRequest.decodePermissionApprovalRequest(): PermissionApprovalRequest {
    val paramsElement = requireNotNull(params) {
        "Expected permission approval request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(PermissionApprovalRequest.serializer(), paramsElement)
}

internal fun JsonRpcRequest.decodeUserInputRequest(): UserInputRequest {
    val paramsElement = requireNotNull(params) {
        "Expected user input request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(UserInputRequest.serializer(), paramsElement)
}

internal fun JsonRpcRequest.decodeMcpElicitationRequest(): McpElicitationRequest {
    val paramsElement = requireNotNull(params) {
        "Expected MCP elicitation request params"
    }
    return CodexProtocolJson.decodeFromJsonElement(McpElicitationRequest.serializer(), paramsElement)
}

internal fun ApprovalDecision.toProtocolPayload(): CodexJsonPayload {
    val value = when (this) {
        ApprovalDecision.Accept -> "accept"
        ApprovalDecision.AcceptForSession -> "acceptForSession"
        ApprovalDecision.Decline -> "decline"
        ApprovalDecision.Cancel -> "cancel"
    }
    return buildJsonObject {
        put("decision", value)
    }.toCodexPayload()
}

internal fun PermissionApprovalResponse.toProtocolPayload(): CodexJsonPayload {
    return CodexProtocolJson.encodeToJsonElement(
        PermissionApprovalResponse.serializer(),
        this,
    ).toCodexPayload()
}

internal fun UserInputResponse.toProtocolPayload(): CodexJsonPayload {
    return when (this) {
        UserInputResponse.Cancel -> buildJsonObject {
            put("decision", "cancel")
        }.toCodexPayload()
        is UserInputResponse.Answers -> buildJsonObject {
            putJsonObject("answers") {
                answers.forEach { (questionId, answer) ->
                    putJsonObject(questionId.value) {
                        putJsonArray("answers") {
                            answer.answers.forEach { add(it) }
                        }
                    }
                }
            }
        }.toCodexPayload()
    }
}

internal fun McpElicitationResponse.toProtocolPayload(): CodexJsonPayload {
    return when (this) {
        is McpElicitationResponse.Accept -> buildJsonObject {
            put("action", "accept")
            put("content", content.toJsonElement() ?: JsonNull)
            meta?.let { put("_meta", it.toJsonElement() ?: JsonNull) }
        }.toCodexPayload()
        McpElicitationResponse.Decline -> mcpElicitationTerminalPayload("decline")
        McpElicitationResponse.Cancel -> mcpElicitationTerminalPayload("cancel")
    }
}

private fun mcpElicitationTerminalPayload(action: String): CodexJsonPayload {
    return buildJsonObject {
        put("action", action)
        put("content", JsonNull)
    }.toCodexPayload()
}

internal fun defaultServerRequestResult(method: String): CodexJsonPayload? {
    return when (method) {
        COMMAND_APPROVAL_METHOD,
        FILE_CHANGE_APPROVAL_METHOD,
        "item/tool/call",
        -> buildJsonObject { put("decision", "decline") }.toCodexPayload()

        PERMISSION_APPROVAL_METHOD -> PermissionApprovalResponse.Decline.toProtocolPayload()
        USER_INPUT_REQUEST_METHOD -> UserInputResponse.Cancel.toProtocolPayload()
        MCP_ELICITATION_REQUEST_METHOD -> McpElicitationResponse.Decline.toProtocolPayload()
        "attestation/generate" -> buildJsonObject { put("status", "unsupported") }.toCodexPayload()
        else -> null
    }
}

internal fun noHandlerError(method: String): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32601,
        message = "No handler registered for $method",
    )
}

internal fun invalidServerRequestParamsError(method: String): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32602,
        message = "Invalid params for $method",
    )
}

internal fun serverRequestHandlerError(): JsonRpcErrorObject {
    return JsonRpcErrorObject(
        code = -32000,
        message = "Server request handler failed",
    )
}
