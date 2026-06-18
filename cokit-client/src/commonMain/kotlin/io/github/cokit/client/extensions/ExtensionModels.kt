package io.github.cokit.client.extensions

import io.github.cokit.client.CodexCursor
import io.github.cokit.client.CodexHostPath
import io.github.cokit.client.ThreadId
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class HookEventName(val value: String) {
    companion object {
        val PreToolUse = HookEventName("preToolUse")
        val PermissionRequest = HookEventName("permissionRequest")
        val PostToolUse = HookEventName("postToolUse")
        val PreCompact = HookEventName("preCompact")
        val PostCompact = HookEventName("postCompact")
        val SessionStart = HookEventName("sessionStart")
        val UserPromptSubmit = HookEventName("userPromptSubmit")
        val SubagentStart = HookEventName("subagentStart")
        val SubagentStop = HookEventName("subagentStop")
        val Stop = HookEventName("stop")
    }
}

@Serializable
@JvmInline
value class HookHandlerType(val value: String) {
    companion object {
        val Command = HookHandlerType("command")
        val Prompt = HookHandlerType("prompt")
        val Agent = HookHandlerType("agent")
    }
}

@Serializable
@JvmInline
value class HookSource(val value: String) {
    companion object {
        val System = HookSource("system")
        val User = HookSource("user")
        val Project = HookSource("project")
        val Mdm = HookSource("mdm")
        val SessionFlags = HookSource("sessionFlags")
        val Plugin = HookSource("plugin")
        val CloudRequirements = HookSource("cloudRequirements")
        val CloudManagedConfig = HookSource("cloudManagedConfig")
        val LegacyManagedConfigFile = HookSource("legacyManagedConfigFile")
        val LegacyManagedConfigMdm = HookSource("legacyManagedConfigMdm")
        val Unknown = HookSource("unknown")
    }
}

@Serializable
@JvmInline
value class HookTrustStatus(val value: String) {
    companion object {
        val Managed = HookTrustStatus("managed")
        val Untrusted = HookTrustStatus("untrusted")
        val Trusted = HookTrustStatus("trusted")
        val Modified = HookTrustStatus("modified")
    }
}

@Serializable
data class HooksListParams(
    val cwds: List<CodexHostPath> = emptyList(),
)

@Serializable
data class HooksListResult(
    val data: List<HooksListEntry> = emptyList(),
)

@Serializable
data class HooksListEntry(
    val cwd: CodexHostPath,
    val errors: List<HookErrorInfo> = emptyList(),
    val hooks: List<HookMetadata> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class HookErrorInfo(
    val message: String,
    val path: CodexHostPath,
)

@Serializable
data class HookMetadata(
    val key: String,
    val eventName: HookEventName,
    val handlerType: HookHandlerType,
    val source: HookSource,
    val sourcePath: CodexHostPath,
    val enabled: Boolean,
    val timeoutSec: Long,
    val trustStatus: HookTrustStatus,
    val currentHash: String,
    val displayOrder: Long,
    val isManaged: Boolean,
    val command: String? = null,
    val matcher: String? = null,
    val pluginId: String? = null,
    val statusMessage: String? = null,
)

@Serializable
@JvmInline
value class AppId(val value: String)

@Serializable
data class AppsListParams(
    val cursor: CodexCursor? = null,
    val forceRefetch: Boolean? = null,
    val limit: Int? = null,
    val threadId: ThreadId? = null,
)

@Serializable
data class AppsListResult(
    val data: List<AppInfo> = emptyList(),
    val nextCursor: CodexCursor? = null,
)

@Serializable
data class AppInfo(
    val id: AppId,
    val name: String,
    val appMetadata: AppMetadata? = null,
    val branding: AppBranding? = null,
    val description: String? = null,
    val distributionChannel: String? = null,
    val installUrl: String? = null,
    val isAccessible: Boolean = false,
    val isEnabled: Boolean = true,
    val labels: Map<String, String>? = null,
    val logoUrl: String? = null,
    val logoUrlDark: String? = null,
    val pluginDisplayNames: List<String> = emptyList(),
)

@Serializable
data class AppBranding(
    val isDiscoverableApp: Boolean,
    val category: String? = null,
    val developer: String? = null,
    val privacyPolicy: String? = null,
    val termsOfService: String? = null,
    val website: String? = null,
)

@Serializable
data class AppMetadata(
    val categories: List<String>? = null,
    val developer: String? = null,
    val firstPartyRequiresInstall: Boolean? = null,
    val firstPartyType: String? = null,
    val review: AppReview? = null,
    val screenshots: List<AppScreenshot>? = null,
    val seoDescription: String? = null,
    val showInComposerWhenUnlinked: Boolean? = null,
    val subCategories: List<String>? = null,
    val version: String? = null,
    val versionId: String? = null,
    val versionNotes: String? = null,
)

@Serializable
data class AppReview(
    val status: String,
)

@Serializable
data class AppScreenshot(
    val userPrompt: String,
    val fileId: String? = null,
    val url: String? = null,
)
