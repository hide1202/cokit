package io.github.cokit.client.mcp

import io.github.cokit.client.CodexJsonPayload
import io.github.cokit.client.ThreadId
import io.github.cokit.client.TurnId
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable(with = McpElicitationRequestSerializer::class)
sealed interface McpElicitationRequest {
    val serverName: String
    val threadId: ThreadId
    val turnId: TurnId?
    val message: String

    @Serializable
    data class Form(
        override val serverName: String,
        override val threadId: ThreadId,
        override val turnId: TurnId? = null,
        override val message: String,
        @SerialName("_meta")
        val meta: CodexJsonPayload? = null,
        val requestedSchema: McpElicitationSchema,
    ) : McpElicitationRequest

    @Serializable
    data class Url(
        override val serverName: String,
        override val threadId: ThreadId,
        override val turnId: TurnId? = null,
        override val message: String,
        val url: String,
        val elicitationId: String,
        @SerialName("_meta")
        val meta: CodexJsonPayload? = null,
    ) : McpElicitationRequest
}

object McpElicitationRequestSerializer :
    JsonContentPolymorphicSerializer<McpElicitationRequest>(McpElicitationRequest::class) {
    override fun selectDeserializer(
        element: JsonElement,
    ): DeserializationStrategy<McpElicitationRequest> {
        return when (element.jsonObject["mode"]?.jsonPrimitive?.contentOrNull) {
            "form" -> McpElicitationRequest.Form.serializer()
            "url" -> McpElicitationRequest.Url.serializer()
            else -> throw SerializationException("Unknown MCP elicitation request mode")
        }
    }
}

@Serializable
data class McpElicitationSchema(
    @SerialName("\$schema")
    val schema: String? = null,
    val type: McpElicitationSchemaType,
    val properties: Map<String, McpElicitationField>,
    val required: List<String>? = null,
)

@Serializable
enum class McpElicitationSchemaType {
    @SerialName("object")
    Object,
}

@Serializable
data class McpElicitationField(
    val type: McpElicitationFieldType,
    val title: String? = null,
    val description: String? = null,
    val default: CodexJsonPayload? = null,
    val format: String? = null,
    val enum: List<String>? = null,
    val enumNames: List<String>? = null,
    val oneOf: List<McpElicitationConstOption>? = null,
    val anyOf: List<McpElicitationConstOption>? = null,
    val items: McpElicitationArrayItems? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
)

@Serializable
enum class McpElicitationFieldType {
    @SerialName("string")
    String,

    @SerialName("number")
    Number,

    @SerialName("integer")
    Integer,

    @SerialName("boolean")
    Boolean,

    @SerialName("array")
    Array,
}

@Serializable
data class McpElicitationConstOption(
    @SerialName("const")
    val value: String,
    val title: String,
)

@Serializable
data class McpElicitationArrayItems(
    val type: McpElicitationFieldType? = null,
    val enum: List<String>? = null,
    val anyOf: List<McpElicitationConstOption>? = null,
)

sealed interface McpElicitationResponse {
    data class Accept(
        val content: CodexJsonPayload,
        val meta: CodexJsonPayload? = null,
    ) : McpElicitationResponse

    data object Decline : McpElicitationResponse

    data object Cancel : McpElicitationResponse
}

fun interface McpElicitationHandler {
    suspend fun respond(request: McpElicitationRequest): McpElicitationResponse
}
