package io.github.cokit.client

import io.github.cokit.client.commands.CommandExecOutputStream
import io.github.cokit.client.commands.CommandProcessId
import io.github.cokit.protocol.JsonRpcNotification
import io.github.cokit.protocol.JsonRpcId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CodexNotificationTest {
    @Test
    fun decodesCoreThreadNotificationsAsTypedModels() {
        val started = JsonRpcNotification(
            method = "thread/started",
            params = buildJsonObject {
                put(
                    "thread",
                    buildJsonObject {
                        put("id", "thr_123")
                        put("preview", "Review changes")
                    },
                )
            },
        ).toCodexNotification()

        val threadStarted = assertIs<CodexNotification.ThreadStarted>(started)
        assertEquals(ThreadId("thr_123"), threadStarted.threadId)
        assertEquals("Review changes", threadStarted.thread?.preview)

        val statusChanged = JsonRpcNotification(
            method = "thread/status/changed",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("status", "inProgress")
            },
        ).toCodexNotification()

        val threadStatusChanged = assertIs<CodexNotification.ThreadStatusChanged>(statusChanged)
        assertEquals(ThreadId("thr_123"), threadStatusChanged.threadId)
        assertEquals(ThreadStatusType("inProgress"), threadStatusChanged.status)
    }

    @Test
    fun decodesTurnLifecycleNotificationsAsTypedModels() {
        val started = JsonRpcNotification(
            method = "turn/started",
            params = turnParams("turn_123", "inProgress"),
        ).toCodexNotification()

        val turnStarted = assertIs<CodexNotification.TurnStarted>(started)
        assertEquals(TurnId("turn_123"), turnStarted.turn.id)
        assertEquals(TurnStatus.InProgress, turnStarted.turn.status)

        val completed = JsonRpcNotification(
            method = "turn/completed",
            params = turnParams("turn_123", "completed"),
        ).toCodexNotification()

        val turnCompleted = assertIs<CodexNotification.TurnCompleted>(completed)
        assertEquals(TurnId("turn_123"), turnCompleted.turn.id)
        assertEquals(TurnStatus.Completed, turnCompleted.turn.status)

        val failed = JsonRpcNotification(
            method = "turn/completed",
            params = buildJsonObject {
                put(
                    "turn",
                    buildJsonObject {
                        put("id", "turn_456")
                        put("status", "failed")
                        put("items", buildJsonArray {})
                        put(
                            "error",
                            buildJsonObject {
                                put("message", "model request failed")
                            },
                        )
                    },
                )
            },
        ).toCodexNotification()

        val turnFailed = assertIs<CodexNotification.TurnFailed>(failed)
        assertEquals("turn/completed", turnFailed.method)
        assertEquals(TurnId("turn_456"), turnFailed.turn.id)
        assertEquals(TurnStatus.Failed, turnFailed.turn.status)
        assertEquals("model request failed", turnFailed.turn.error?.message)
    }

    @Test
    fun decodesItemLifecycleNotificationsAsTypedModels() {
        val started = JsonRpcNotification(
            method = "item/started",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put(
                    "item",
                    buildJsonObject {
                        put("id", "item_msg")
                        put("type", "agentMessage")
                        put("text", "Hello")
                    },
                )
            },
        ).toCodexNotification()

        val itemStarted = assertIs<CodexNotification.ItemStarted>(started)
        assertEquals(ThreadId("thr_123"), itemStarted.threadId)
        assertEquals(TurnId("turn_123"), itemStarted.turnId)
        assertEquals(ItemId("item_msg"), itemStarted.item.id)
        assertEquals(ItemType.AgentMessage, itemStarted.item.type)
        assertEquals("Hello", itemStarted.item.text)

        val completed = JsonRpcNotification(
            method = "item/completed",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put(
                    "item",
                    buildJsonObject {
                        put("id", "item_cmd")
                        put("type", "commandExecution")
                        put("command", "git status --short")
                        put("cwd", "/path/to/project")
                        put("status", "completed")
                        put("exitCode", 0)
                        put("durationMs", 42)
                    },
                )
            },
        ).toCodexNotification()

        val itemCompleted = assertIs<CodexNotification.ItemCompleted>(completed)
        assertEquals(ThreadId("thr_123"), itemCompleted.threadId)
        assertEquals(TurnId("turn_123"), itemCompleted.turnId)
        assertEquals(ItemId("item_cmd"), itemCompleted.item.id)
        assertEquals(ItemType.CommandExecution, itemCompleted.item.type)
        assertEquals(ItemStatus.Completed, itemCompleted.item.status)
        assertEquals("git status --short", itemCompleted.item.command)
        assertEquals(CodexHostPath("/path/to/project"), itemCompleted.item.cwd)
        assertEquals(0, itemCompleted.item.exitCode)
        assertEquals(42L, itemCompleted.item.durationMs)
    }

    @Test
    fun decodesRepresentativeItemDeltaNotificationsAsTypedModels() {
        val agentMessageDelta = JsonRpcNotification(
            method = "item/agentMessage/delta",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put("itemId", "item_msg")
                put("delta", "Hello")
            },
        ).toCodexNotification()

        val agentDelta = assertIs<CodexNotification.AgentMessageDelta>(agentMessageDelta)
        assertEquals(ThreadId("thr_123"), agentDelta.threadId)
        assertEquals(TurnId("turn_123"), agentDelta.turnId)
        assertEquals(ItemId("item_msg"), agentDelta.itemId)
        assertEquals("Hello", agentDelta.delta)

        val reasoningSummaryDelta = JsonRpcNotification(
            method = "item/reasoning/summaryTextDelta",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put("itemId", "item_reasoning")
                put("summaryIndex", 0)
                put("delta", "Inspecting files")
            },
        ).toCodexNotification()

        val reasoningDelta = assertIs<CodexNotification.ReasoningSummaryTextDelta>(reasoningSummaryDelta)
        assertEquals(ThreadId("thr_123"), reasoningDelta.threadId)
        assertEquals(TurnId("turn_123"), reasoningDelta.turnId)
        assertEquals(ItemId("item_reasoning"), reasoningDelta.itemId)
        assertEquals(0, reasoningDelta.summaryIndex)
        assertEquals("Inspecting files", reasoningDelta.delta)
    }

    @Test
    fun decodesThreadTokenUsageNotificationAsTypedModel() {
        val notification = JsonRpcNotification(
            method = "thread/tokenUsage/updated",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put(
                    "tokenUsage",
                    buildJsonObject {
                        put(
                            "total",
                            tokenUsageBreakdown(
                                totalTokens = 150,
                                inputTokens = 120,
                                cachedInputTokens = 20,
                                outputTokens = 30,
                                reasoningOutputTokens = 10,
                            ),
                        )
                        put(
                            "last",
                            tokenUsageBreakdown(
                                totalTokens = 90,
                                inputTokens = 70,
                                cachedInputTokens = 15,
                                outputTokens = 20,
                                reasoningOutputTokens = 5,
                            ),
                        )
                        put("modelContextWindow", 200_000)
                    },
                )
            },
        ).toCodexNotification()

        val tokenUsage = assertIs<CodexNotification.ThreadTokenUsageUpdated>(notification)
        assertEquals(ThreadId("thr_123"), tokenUsage.threadId)
        assertEquals(TurnId("turn_123"), tokenUsage.turnId)
        assertEquals(
            TokenUsageBreakdown(
                totalTokens = 150,
                inputTokens = 120,
                cachedInputTokens = 20,
                outputTokens = 30,
                reasoningOutputTokens = 10,
            ),
            tokenUsage.tokenUsage.total,
        )
        assertEquals(90L, tokenUsage.tokenUsage.last.totalTokens)
        assertEquals(200_000L, tokenUsage.tokenUsage.modelContextWindow)
    }

    @Test
    fun decodesWarningNotificationsAsTypedModels() {
        val warning = JsonRpcNotification(
            method = "warning",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("message", "Some enabled skills were not included.")
            },
        ).toCodexNotification()

        val typedWarning = assertIs<CodexNotification.Warning>(warning)
        assertEquals(ThreadId("thr_123"), typedWarning.threadId)
        assertEquals("Some enabled skills were not included.", typedWarning.message)

        val configWarning = JsonRpcNotification(
            method = "configWarning",
            params = buildJsonObject {
                put("summary", "Config error: using defaults")
                put("details", "error loading config: bad config")
                put("path", "/path/to/config.toml")
                put(
                    "range",
                    buildJsonObject {
                        put(
                            "start",
                            buildJsonObject {
                                put("line", 2)
                                put("column", 4)
                            },
                        )
                        put(
                            "end",
                            buildJsonObject {
                                put("line", 2)
                                put("column", 20)
                            },
                        )
                    },
                )
            },
        ).toCodexNotification()

        val typedConfigWarning = assertIs<CodexNotification.ConfigWarning>(configWarning)
        assertEquals("Config error: using defaults", typedConfigWarning.summary)
        assertEquals("error loading config: bad config", typedConfigWarning.details)
        assertEquals("/path/to/config.toml", typedConfigWarning.path)
        assertEquals(
            ConfigTextRange(
                start = ConfigTextPosition(line = 2, column = 4),
                end = ConfigTextPosition(line = 2, column = 20),
            ),
            typedConfigWarning.range,
        )
    }

    @Test
    fun decodesErrorNotificationAsTypedModelWithoutRawErrorPayload() {
        val notification = JsonRpcNotification(
            method = "error",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("turnId", "turn_123")
                put("willRetry", true)
                put(
                    "error",
                    buildJsonObject {
                        put("message", "upstream stream disconnected")
                        put("additionalDetails", "retrying automatically")
                        put(
                            "codexErrorInfo",
                            buildJsonObject {
                                put("type", "ResponseStreamDisconnected")
                            },
                        )
                    },
                )
            },
        ).toCodexNotification()

        val error = assertIs<CodexNotification.Error>(notification)
        assertEquals(ThreadId("thr_123"), error.threadId)
        assertEquals(TurnId("turn_123"), error.turnId)
        assertEquals("upstream stream disconnected", error.error.message)
        assertEquals("retrying automatically", error.error.additionalDetails)
        assertEquals(true, error.willRetry)
    }

    @Test
    fun decodesServerRequestResolvedNotificationAsTypedModel() {
        val notification = JsonRpcNotification(
            method = "serverRequest/resolved",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("requestId", 99)
            },
        ).toCodexNotification()

        val resolved = assertIs<CodexNotification.ServerRequestResolved>(notification)
        assertEquals(ThreadId("thr_123"), resolved.threadId)
        assertEquals(JsonRpcId.Number(99), resolved.requestId)

        val stringIdNotification = JsonRpcNotification(
            method = "serverRequest/resolved",
            params = buildJsonObject {
                put("threadId", "thr_123")
                put("requestId", "req_abc")
            },
        ).toCodexNotification()

        val stringIdResolved = assertIs<CodexNotification.ServerRequestResolved>(stringIdNotification)
        assertEquals(JsonRpcId.StringId("req_abc"), stringIdResolved.requestId)
    }

    @Test
    fun decodesCommandExecOutputDeltaNotificationAsTypedModel() {
        val stdoutNotification = JsonRpcNotification(
            method = "command/exec/outputDelta",
            params = buildJsonObject {
                put("processId", "cmd_stream_1")
                put("stream", "stdout")
                put("deltaBase64", "bGluZSAxCg==")
                put("capReached", false)
            },
        ).toCodexNotification()

        val stdout = assertIs<CodexNotification.CommandExecOutputDelta>(stdoutNotification)
        assertEquals(CommandProcessId("cmd_stream_1"), stdout.processId)
        assertEquals(CommandExecOutputStream.Stdout, stdout.stream)
        assertEquals("bGluZSAxCg==", stdout.deltaBase64)
        assertEquals(false, stdout.capReached)

        val stderrNotification = JsonRpcNotification(
            method = "command/exec/outputDelta",
            params = buildJsonObject {
                put("processId", "cmd_stream_1")
                put("stream", "stderr")
                put("deltaBase64", "ZXJyb3IK")
                put("capReached", true)
            },
        ).toCodexNotification()

        val stderr = assertIs<CodexNotification.CommandExecOutputDelta>(stderrNotification)
        assertEquals(CommandExecOutputStream.Stderr, stderr.stream)
        assertEquals(true, stderr.capReached)
    }

    @Test
    fun unknownNotificationKeepsOnlyMethodName() {
        val notification = JsonRpcNotification(
            method = "item/future/delta",
            params = buildJsonObject {
                put("delta", "hello")
            },
        ).toCodexNotification()

        assertEquals(CodexNotification.Unknown("item/future/delta"), notification)
    }

    private fun turnParams(turnId: String, status: String) = buildJsonObject {
        put(
            "turn",
            buildJsonObject {
                put("id", turnId)
                put("status", status)
                put("items", buildJsonArray {})
            },
        )
    }

    private fun tokenUsageBreakdown(
        totalTokens: Long,
        inputTokens: Long,
        cachedInputTokens: Long,
        outputTokens: Long,
        reasoningOutputTokens: Long,
    ) = buildJsonObject {
        put("totalTokens", totalTokens)
        put("inputTokens", inputTokens)
        put("cachedInputTokens", cachedInputTokens)
        put("outputTokens", outputTokens)
        put("reasoningOutputTokens", reasoningOutputTokens)
    }
}
