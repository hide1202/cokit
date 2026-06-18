package io.github.cokit.client.config

import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.CodexJsonPayload
import io.github.cokit.client.toCodexPayload
import io.github.cokit.client.toJsonElement
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonElement

@Serializable
@JvmInline
value class ConfigKeyPath(val value: String)

@Serializable
@JvmInline
value class ConfigMergeStrategy(val value: String) {
    companion object {
        val Replace = ConfigMergeStrategy("replace")
        val Upsert = ConfigMergeStrategy("upsert")
    }
}

@Serializable
@JvmInline
value class ConfigWriteStatus(val value: String) {
    companion object {
        val Ok = ConfigWriteStatus("ok")
        val OkOverridden = ConfigWriteStatus("okOverridden")
    }
}

@Serializable(with = ConfigValueSerializer::class)
class ConfigValue internal constructor(
    val payload: CodexJsonPayload,
) {
    fun toJsonString(): String = payload.toJsonString()

    override fun equals(other: Any?): Boolean =
        other is ConfigValue && payload == other.payload

    override fun hashCode(): Int = payload.hashCode()

    override fun toString(): String = toJsonString()

    companion object {
        fun parse(json: String): ConfigValue = ConfigValue(CodexJsonPayload.parse(json))
    }
}

object ConfigValueSerializer : KSerializer<ConfigValue> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): ConfigValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("ConfigValue requires JSON decoding")
        return ConfigValue(jsonDecoder.decodeJsonElement().toCodexPayload())
    }

    override fun serialize(encoder: Encoder, value: ConfigValue) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("ConfigValue requires JSON encoding")
        jsonEncoder.encodeJsonElement(
            value.payload.toJsonElement()
                ?: throw SerializationException("ConfigValue payload cannot be null"),
        )
    }
}

@Serializable
data class ConfigReadParams(
    val cwd: CodexHostPath? = null,
    val includeLayers: Boolean? = null,
)

@Serializable
data class ConfigReadResult(
    val config: ConfigValue,
    val origins: Map<String, ConfigLayerMetadata> = emptyMap(),
    val layers: List<ConfigLayer>? = null,
)

@Serializable
data class ConfigValueWriteParams(
    val keyPath: ConfigKeyPath,
    val value: ConfigValue,
    val mergeStrategy: ConfigMergeStrategy,
    val filePath: CodexHostPath? = null,
    val expectedVersion: String? = null,
)

@Serializable
data class ConfigBatchWriteParams(
    val edits: List<ConfigEdit>,
    val filePath: CodexHostPath? = null,
    val expectedVersion: String? = null,
    val reloadUserConfig: Boolean? = null,
)

@Serializable
data class ConfigEdit(
    val keyPath: ConfigKeyPath,
    val value: ConfigValue,
    val mergeStrategy: ConfigMergeStrategy,
)

@Serializable
data class ConfigWriteResult(
    val filePath: CodexHostPath,
    val status: ConfigWriteStatus,
    val version: String,
    val overriddenMetadata: ConfigOverriddenMetadata? = null,
)

@Serializable
data class ConfigOverriddenMetadata(
    val effectiveValue: ConfigValue,
    val message: String,
    val overridingLayer: ConfigLayerMetadata,
)

@Serializable
data class ConfigLayer(
    val name: ConfigLayerSource,
    val version: String,
    val config: ConfigValue,
    val disabledReason: String? = null,
)

@Serializable
data class ConfigLayerMetadata(
    val name: ConfigLayerSource,
    val version: String,
)

@Serializable
sealed interface ConfigLayerSource {
    @Serializable
    @SerialName("mdm")
    data class Mdm(
        val domain: String,
        val key: String,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("system")
    data class SystemFile(
        val file: CodexHostPath,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("enterpriseManaged")
    data class EnterpriseManaged(
        val id: String,
        val name: String,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("user")
    data class User(
        val file: CodexHostPath,
        val profile: String? = null,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("project")
    data class Project(
        val dotCodexFolder: CodexHostPath,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("sessionFlags")
    data object SessionFlags : ConfigLayerSource

    @Serializable
    @SerialName("legacyManagedConfigTomlFromFile")
    data class LegacyManagedConfigTomlFromFile(
        val file: CodexHostPath,
    ) : ConfigLayerSource

    @Serializable
    @SerialName("legacyManagedConfigTomlFromMdm")
    data object LegacyManagedConfigTomlFromMdm : ConfigLayerSource
}
