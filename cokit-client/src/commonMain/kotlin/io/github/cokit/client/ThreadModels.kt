package io.github.cokit.client

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

@Serializable
@JvmInline
value class ThreadId(val value: String)

@Serializable
@JvmInline
value class CodexCursor(val value: String)

@Serializable
@JvmInline
value class CodexTimestamp(val epochSeconds: Long)

@Serializable
@JvmInline
value class ThreadStatusType(val value: String) {
    companion object {
        val NotLoaded = ThreadStatusType("notLoaded")
    }
}

@Serializable
data class Thread(
    val id: ThreadId,
    val preview: String? = null,
    val modelProvider: String? = null,
    val createdAt: CodexTimestamp? = null,
    val updatedAt: CodexTimestamp? = null,
    val gitInfo: ThreadGitInfo? = null,
)

@Serializable
data class ThreadGitInfo(
    val sha: String? = null,
    val branch: String? = null,
    val originUrl: String? = null,
)

sealed interface ThreadMetadataPatchValue {
    data object Unchanged : ThreadMetadataPatchValue
    data object Clear : ThreadMetadataPatchValue
    data class Set(val value: String) : ThreadMetadataPatchValue
}

@Serializable(with = ThreadGitInfoPatchSerializer::class)
data class ThreadGitInfoPatch(
    val sha: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val branch: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
    val originUrl: ThreadMetadataPatchValue = ThreadMetadataPatchValue.Unchanged,
)

object ThreadGitInfoPatchSerializer : KSerializer<ThreadGitInfoPatch> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThreadGitInfoPatch") {
        element<String?>("sha", isOptional = true)
        element<String?>("branch", isOptional = true)
        element<String?>("originUrl", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: ThreadGitInfoPatch) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ThreadGitInfoPatch requires JSON encoding")
        jsonEncoder.encodeJsonElement(
            buildJsonObject {
                putPatchValue("sha", value.sha)
                putPatchValue("branch", value.branch)
                putPatchValue("originUrl", value.originUrl)
            },
        )
    }

    override fun deserialize(decoder: Decoder): ThreadGitInfoPatch {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ThreadGitInfoPatch requires JSON decoding")
        val element = jsonDecoder.decodeJsonElement()
        val jsonObject = element as? JsonObject
            ?: throw SerializationException("ThreadGitInfoPatch must be a JSON object")
        return ThreadGitInfoPatch(
            sha = jsonObject.decodePatchValue("sha"),
            branch = jsonObject.decodePatchValue("branch"),
            originUrl = jsonObject.decodePatchValue("originUrl"),
        )
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putPatchValue(
        key: String,
        value: ThreadMetadataPatchValue,
    ) {
        when (value) {
            ThreadMetadataPatchValue.Unchanged -> Unit
            ThreadMetadataPatchValue.Clear -> put(key, JsonNull)
            is ThreadMetadataPatchValue.Set -> put(key, value.value)
        }
    }

    private fun JsonObject.decodePatchValue(key: String): ThreadMetadataPatchValue {
        val value = this[key] ?: return ThreadMetadataPatchValue.Unchanged
        return when (value) {
            JsonNull -> ThreadMetadataPatchValue.Clear
            is JsonPrimitive -> {
                val text = value.contentOrNull
                    ?: throw SerializationException("ThreadGitInfoPatch.$key must be a string or null")
                ThreadMetadataPatchValue.Set(text)
            }
            else -> throw SerializationException("ThreadGitInfoPatch.$key must be a string or null")
        }
    }
}

@Serializable
data class ThreadList(
    val threads: List<Thread> = emptyList(),
    val cursor: CodexCursor? = null,
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
    val cursor: CodexCursor? = null,
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
