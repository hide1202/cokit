package io.github.cokit.client

import io.github.cokit.client.extensions.AppBranding
import io.github.cokit.client.extensions.AppId
import io.github.cokit.client.extensions.AppReview
import io.github.cokit.client.extensions.AppScreenshot
import io.github.cokit.client.extensions.AppsListParams
import io.github.cokit.client.extensions.HookEventName
import io.github.cokit.client.extensions.HookHandlerType
import io.github.cokit.client.extensions.HookSource
import io.github.cokit.client.extensions.HookTrustStatus
import io.github.cokit.client.extensions.HooksListParams
import io.github.cokit.protocol.JsonRpcRequest
import io.github.cokit.protocol.JsonRpcResponse
import io.github.cokit.testing.FakeJsonRpcTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@OptIn(ExperimentalCodexApi::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ExtensionRpcTest {
    @Test
    fun hooksListDescriptorSendsCwdsAndDecodesHookMetadata() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Hooks.List,
                HooksListParams(
                    cwds = listOf(CodexHostPath("/path/to/project")),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("hooks/list", request.method)
        assertEquals(
            "/path/to/project",
            request.params!!.jsonObject["cwds"]!!.jsonArray.single().jsonPrimitive.contentOrNull,
        )

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("cwd", "/path/to/project")
                                    put(
                                        "errors",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("message", "Invalid hook config")
                                                    put("path", "/path/to/project/.codex/hooks.toml")
                                                },
                                            )
                                        },
                                    )
                                    put(
                                        "warnings",
                                        buildJsonArray {
                                            add("Hook command is disabled")
                                        },
                                    )
                                    put(
                                        "hooks",
                                        buildJsonArray {
                                            add(
                                                buildJsonObject {
                                                    put("key", "pre-tool-use-format")
                                                    put("eventName", "preToolUse")
                                                    put("handlerType", "command")
                                                    put("source", "project")
                                                    put("sourcePath", "/path/to/project/.codex/hooks.toml")
                                                    put("enabled", true)
                                                    put("timeoutSec", 10)
                                                    put("trustStatus", "trusted")
                                                    put("currentHash", "abc123")
                                                    put("displayOrder", 7)
                                                    put("isManaged", false)
                                                    put("command", "ktlint --format")
                                                    put("matcher", "command/exec")
                                                    put("pluginId", "plugin_lint")
                                                    put("statusMessage", "Ready")
                                                },
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            ),
        )

        val entry = result.await().data.single()
        assertEquals(CodexHostPath("/path/to/project"), entry.cwd)
        assertEquals("Invalid hook config", entry.errors.single().message)
        assertEquals(CodexHostPath("/path/to/project/.codex/hooks.toml"), entry.errors.single().path)
        assertEquals("Hook command is disabled", entry.warnings.single())

        val hook = entry.hooks.single()
        assertEquals("pre-tool-use-format", hook.key)
        assertEquals(HookEventName.PreToolUse, hook.eventName)
        assertEquals(HookHandlerType.Command, hook.handlerType)
        assertEquals(HookSource.Project, hook.source)
        assertEquals(CodexHostPath("/path/to/project/.codex/hooks.toml"), hook.sourcePath)
        assertEquals(true, hook.enabled)
        assertEquals(10L, hook.timeoutSec)
        assertEquals(HookTrustStatus.Trusted, hook.trustStatus)
        assertEquals("abc123", hook.currentHash)
        assertEquals(7L, hook.displayOrder)
        assertEquals(false, hook.isManaged)
        assertEquals("ktlint --format", hook.command)
        assertEquals("command/exec", hook.matcher)
        assertEquals("plugin_lint", hook.pluginId)
        assertEquals("Ready", hook.statusMessage)
    }

    @Test
    fun experimentalAppsListDescriptorSendsPagingParamsAndDecodesAppMetadata() = runTest {
        val fixture = connectedRpcClientFixture(backgroundScope)

        val result = async {
            fixture.client.request(
                CodexRpc.Apps.List,
                AppsListParams(
                    cursor = CodexCursor("cursor_apps"),
                    forceRefetch = true,
                    limit = 10,
                    threadId = ThreadId("thread_123"),
                ),
            )
        }
        runCurrent()

        val request = fixture.transport.sent.last() as JsonRpcRequest
        assertEquals("app/list", request.method)
        val params = request.params!!.jsonObject
        assertEquals("cursor_apps", params["cursor"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, params["forceRefetch"]?.jsonPrimitive?.booleanOrNull)
        assertEquals(10, params["limit"]?.jsonPrimitive?.intOrNull)
        assertEquals("thread_123", params["threadId"]?.jsonPrimitive?.contentOrNull)

        fixture.transport.receive(
            JsonRpcResponse(
                request.id,
                result = buildJsonObject {
                    put(
                        "data",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("id", "app_weather")
                                    put("name", "Weather")
                                    put("description", "Forecast lookups")
                                    put("distributionChannel", "marketplace")
                                    put("installUrl", "https://apps.example.invalid/weather")
                                    put("isAccessible", true)
                                    put("isEnabled", false)
                                    put(
                                        "labels",
                                        buildJsonObject {
                                            put("category", "weather")
                                        },
                                    )
                                    put("logoUrl", "https://apps.example.invalid/weather/logo-light.png")
                                    put("logoUrlDark", "https://apps.example.invalid/weather/logo-dark.png")
                                    put(
                                        "pluginDisplayNames",
                                        buildJsonArray {
                                            add("Weather Plugin")
                                        },
                                    )
                                    put(
                                        "branding",
                                        buildJsonObject {
                                            put("isDiscoverableApp", true)
                                            put("category", "Weather")
                                            put("developer", "Example Apps")
                                            put("website", "https://apps.example.invalid/weather")
                                            put("privacyPolicy", "https://apps.example.invalid/privacy")
                                            put("termsOfService", "https://apps.example.invalid/terms")
                                        },
                                    )
                                    put(
                                        "appMetadata",
                                        buildJsonObject {
                                            put(
                                                "categories",
                                                buildJsonArray {
                                                    add("weather")
                                                },
                                            )
                                            put(
                                                "subCategories",
                                                buildJsonArray {
                                                    add("forecast")
                                                },
                                            )
                                            put("developer", "Example Apps")
                                            put("firstPartyRequiresInstall", false)
                                            put("firstPartyType", "connector")
                                            put(
                                                "review",
                                                buildJsonObject {
                                                    put("status", "approved")
                                                },
                                            )
                                            put(
                                                "screenshots",
                                                buildJsonArray {
                                                    add(
                                                        buildJsonObject {
                                                            put("fileId", "file_weather")
                                                            put("url", "https://apps.example.invalid/weather/screen.png")
                                                            put("userPrompt", "Show the forecast")
                                                        },
                                                    )
                                                },
                                            )
                                            put("seoDescription", "Forecast data")
                                            put("showInComposerWhenUnlinked", true)
                                            put("version", "1.2.3")
                                            put("versionId", "version_123")
                                            put("versionNotes", "Initial listing")
                                        },
                                    )
                                },
                            )
                        },
                    )
                    put("nextCursor", "cursor_next")
                },
            ),
        )

        val decoded = result.await()
        assertEquals(CodexCursor("cursor_next"), decoded.nextCursor)

        val app = decoded.data.single()
        assertEquals(AppId("app_weather"), app.id)
        assertEquals("Weather", app.name)
        assertEquals("Forecast lookups", app.description)
        assertEquals("marketplace", app.distributionChannel)
        assertEquals(false, app.isEnabled)
        assertEquals(true, app.isAccessible)
        assertEquals(mapOf("category" to "weather"), app.labels)
        assertEquals(listOf("Weather Plugin"), app.pluginDisplayNames)
        assertEquals(
            AppBranding(
                isDiscoverableApp = true,
                category = "Weather",
                developer = "Example Apps",
                website = "https://apps.example.invalid/weather",
                privacyPolicy = "https://apps.example.invalid/privacy",
                termsOfService = "https://apps.example.invalid/terms",
            ),
            app.branding,
        )
        assertEquals("connector", app.appMetadata?.firstPartyType)
        assertEquals(AppReview(status = "approved"), app.appMetadata?.review)
        assertEquals(
            AppScreenshot(
                fileId = "file_weather",
                url = "https://apps.example.invalid/weather/screen.png",
                userPrompt = "Show the forecast",
            ),
            app.appMetadata?.screenshots?.single(),
        )
        assertEquals(true, app.appMetadata?.showInComposerWhenUnlinked)
    }

    private suspend fun TestScope.connectedRpcClientFixture(
        scope: CoroutineScope,
    ): ConnectedRpcClientFixture {
        val transport = FakeJsonRpcTransport()
        val client = async {
            CodexRpcClient.connect(
                CodexRpcConnection(
                    transport = transport,
                    clientInfo = ClientInfo("cokit_test", "CoKit Test", "0.1.0"),
                    scope = scope,
                ),
            )
        }
        runCurrent()
        val initialize = transport.sent.single() as JsonRpcRequest
        transport.receive(JsonRpcResponse(initialize.id, result = JsonObject(emptyMap())))
        return ConnectedRpcClientFixture(client.await(), transport)
    }

    private data class ConnectedRpcClientFixture(
        val client: CodexRpcClient,
        val transport: FakeJsonRpcTransport,
    )
}
