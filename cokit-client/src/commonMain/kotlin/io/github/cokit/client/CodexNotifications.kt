package io.github.cokit.client

import io.github.cokit.client.commands.CommandExecOutputStream
import io.github.cokit.client.commands.CommandProcessId
import io.github.cokit.protocol.CodexProtocolJson
import io.github.cokit.protocol.JsonRpcId
import io.github.cokit.protocol.JsonRpcNotification
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

sealed interface CodexNotification {
    val method: String

    data class ThreadStarted(
        val threadId: ThreadId,
        val thread: Thread? = null,
    ) : CodexNotification {
        override val method: String = "thread/started"
    }

    data class ThreadStatusChanged(
        val threadId: ThreadId,
        val status: ThreadStatusType,
    ) : CodexNotification {
        override val method: String = "thread/status/changed"
    }

    data class ThreadTokenUsageUpdated(
        val threadId: ThreadId,
        val turnId: TurnId,
        val tokenUsage: ThreadTokenUsage,
    ) : CodexNotification {
        override val method: String = "thread/tokenUsage/updated"
    }

    data class TurnStarted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/started"
    }

    data class TurnCompleted(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class TurnFailed(
        val turn: Turn,
    ) : CodexNotification {
        override val method: String = "turn/completed"
    }

    data class ItemStarted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/started"
    }

    data class ItemCompleted(
        val threadId: ThreadId,
        val turnId: TurnId,
        val item: ThreadItemSummary,
    ) : CodexNotification {
        override val method: String = "item/completed"
    }

    data class AgentMessageDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/agentMessage/delta"
    }

    data class ReasoningSummaryTextDelta(
        val threadId: ThreadId,
        val turnId: TurnId,
        val itemId: ItemId,
        val summaryIndex: Int,
        val delta: String,
    ) : CodexNotification {
        override val method: String = "item/reasoning/summaryTextDelta"
    }

    data class Warning(
        val threadId: ThreadId? = null,
        val message: String,
    ) : CodexNotification {
        override val method: String = "warning"
    }

    data class ConfigWarning(
        val summary: String,
        val details: String? = null,
        val path: String? = null,
        val range: ConfigTextRange? = null,
    ) : CodexNotification {
        override val method: String = "configWarning"
    }

    data class Error(
        val threadId: ThreadId,
        val turnId: TurnId,
        val error: CodexNotificationError,
        val willRetry: Boolean,
    ) : CodexNotification {
        override val method: String = "error"
    }

    data class ServerRequestResolved(
        val threadId: ThreadId,
        val requestId: JsonRpcId,
    ) : CodexNotification {
        override val method: String = "serverRequest/resolved"
    }

    data class CommandExecOutputDelta(
        val processId: CommandProcessId,
        val stream: CommandExecOutputStream,
        val deltaBase64: String,
        val capReached: Boolean,
    ) : CodexNotification {
        override val method: String = "command/exec/outputDelta"
    }

    data class Unknown(
        override val method: String,
    ) : CodexNotification
}

@Serializable
data class ThreadTokenUsage(
    val total: TokenUsageBreakdown,
    val last: TokenUsageBreakdown,
    val modelContextWindow: Long? = null,
)

@Serializable
data class TokenUsageBreakdown(
    val totalTokens: Long,
    val inputTokens: Long,
    val cachedInputTokens: Long,
    val outputTokens: Long,
    val reasoningOutputTokens: Long,
)

@Serializable
data class ConfigTextPosition(
    val line: Int,
    val column: Int,
)

@Serializable
data class ConfigTextRange(
    val start: ConfigTextPosition,
    val end: ConfigTextPosition,
)

@Serializable
data class CodexNotificationError(
    val message: String,
    val additionalDetails: String? = null,
)

internal fun JsonRpcNotification.toCodexNotification(): CodexNotification {
    return when (method) {
        "thread/started" -> {
            val payload = params.decodeNotificationParams<ThreadStartedPayload>()
            val thread = payload?.thread
            val threadId = thread?.id ?: payload?.threadId

            if (threadId != null) {
                CodexNotification.ThreadStarted(threadId = threadId, thread = thread)
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "thread/status/changed" -> {
            val payload = params.decodeNotificationParams<ThreadStatusChangedPayload>()
            if (payload != null) {
                CodexNotification.ThreadStatusChanged(
                    threadId = payload.threadId,
                    status = payload.status,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "thread/tokenUsage/updated" -> {
            val payload = params.decodeNotificationParams<ThreadTokenUsagePayload>()
            if (payload != null) {
                CodexNotification.ThreadTokenUsageUpdated(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    tokenUsage = payload.tokenUsage,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "turn/started" -> {
            val turn = params.decodeNotificationParams<TurnPayload>()?.turn
            if (turn != null) {
                CodexNotification.TurnStarted(turn)
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "turn/completed" -> {
            val turn = params.decodeNotificationParams<TurnPayload>()?.turn
            when (turn?.status) {
                TurnStatus.Failed -> CodexNotification.TurnFailed(turn)
                null -> CodexNotification.Unknown(method)
                else -> CodexNotification.TurnCompleted(turn)
            }
        }
        "item/started" -> {
            val payload = params.decodeNotificationParams<ItemPayload>()
            if (payload != null) {
                CodexNotification.ItemStarted(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    item = payload.item,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/completed" -> {
            val payload = params.decodeNotificationParams<ItemPayload>()
            if (payload != null) {
                CodexNotification.ItemCompleted(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    item = payload.item,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/agentMessage/delta" -> {
            val payload = params.decodeNotificationParams<ItemDeltaPayload>()
            if (payload != null) {
                CodexNotification.AgentMessageDelta(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    itemId = payload.itemId,
                    delta = payload.delta,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "item/reasoning/summaryTextDelta" -> {
            val payload = params.decodeNotificationParams<ReasoningSummaryTextDeltaPayload>()
            if (payload != null) {
                CodexNotification.ReasoningSummaryTextDelta(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    itemId = payload.itemId,
                    summaryIndex = payload.summaryIndex,
                    delta = payload.delta,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "warning" -> {
            val payload = params.decodeNotificationParams<WarningPayload>()
            if (payload != null) {
                CodexNotification.Warning(
                    threadId = payload.threadId,
                    message = payload.message,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "configWarning" -> {
            val payload = params.decodeNotificationParams<ConfigWarningPayload>()
            if (payload != null) {
                CodexNotification.ConfigWarning(
                    summary = payload.summary,
                    details = payload.details,
                    path = payload.path,
                    range = payload.range,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "error" -> {
            val payload = params.decodeNotificationParams<ErrorPayload>()
            if (payload != null) {
                CodexNotification.Error(
                    threadId = payload.threadId,
                    turnId = payload.turnId,
                    error = payload.error,
                    willRetry = payload.willRetry,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "serverRequest/resolved" -> {
            val payload = params.decodeNotificationParams<ServerRequestResolvedPayload>()
            if (payload != null) {
                CodexNotification.ServerRequestResolved(
                    threadId = payload.threadId,
                    requestId = payload.requestId,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        "command/exec/outputDelta" -> {
            val payload = params.decodeNotificationParams<CommandExecOutputDeltaPayload>()
            if (payload != null) {
                CodexNotification.CommandExecOutputDelta(
                    processId = payload.processId,
                    stream = payload.stream,
                    deltaBase64 = payload.deltaBase64,
                    capReached = payload.capReached,
                )
            } else {
                CodexNotification.Unknown(method)
            }
        }
        else -> CodexNotification.Unknown(method)
    }
}

@Serializable
private data class ThreadStartedPayload(
    val threadId: ThreadId? = null,
    val thread: Thread? = null,
)

@Serializable
private data class ThreadStatusChangedPayload(
    val threadId: ThreadId,
    val status: ThreadStatusType,
)

@Serializable
private data class ThreadTokenUsagePayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val tokenUsage: ThreadTokenUsage,
)

@Serializable
private data class TurnPayload(
    val turn: Turn,
)

@Serializable
private data class ItemPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val item: ThreadItemSummary,
)

@Serializable
private data class ItemDeltaPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val delta: String,
)

@Serializable
private data class ReasoningSummaryTextDeltaPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val itemId: ItemId,
    val summaryIndex: Int,
    val delta: String,
)

@Serializable
private data class WarningPayload(
    val threadId: ThreadId? = null,
    val message: String,
)

@Serializable
private data class ConfigWarningPayload(
    val summary: String,
    val details: String? = null,
    val path: String? = null,
    val range: ConfigTextRange? = null,
)

@Serializable
private data class ErrorPayload(
    val threadId: ThreadId,
    val turnId: TurnId,
    val error: CodexNotificationError,
    val willRetry: Boolean,
)

@Serializable
private data class ServerRequestResolvedPayload(
    val threadId: ThreadId,
    val requestId: JsonRpcId,
)

@Serializable
private data class CommandExecOutputDeltaPayload(
    val processId: CommandProcessId,
    val stream: CommandExecOutputStream,
    val deltaBase64: String,
    val capReached: Boolean,
)

private inline fun <reified T> JsonElement?.decodeNotificationParams(): T? {
    val element = this ?: return null
    return runCatching {
        CodexProtocolJson.decodeFromJsonElement<T>(element)
    }.getOrNull()
}
