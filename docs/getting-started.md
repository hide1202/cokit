# Getting Started

## Install

During local development, depend on project modules:

```kotlin
dependencies {
    implementation(project(":cokit-client"))
    implementation(project(":cokit-transport-stdio"))
}
```

Published library artifacts use the `io.github.vupoint.cokit` group. The sample
CLI is a repository example and is not published as a library artifact.

```kotlin
dependencies {
    implementation("io.github.vupoint.cokit:cokit-client:<version>")
    implementation("io.github.vupoint.cokit:cokit-transport-stdio:<version>")
}
```

## Run The Sample CLI

The repository includes a small JVM CLI sample in `:cokit-sample-cli`.

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

When `codex` is installed locally, the sample can start a real app-server
thread, send the default message, and stream the assistant response over stdio
without requiring arguments:

```bash
./gradlew :cokit-sample-cli:run
```

Optional `--cwd` and `--message` flags can override the defaults:

```bash
./gradlew :cokit-sample-cli:run --args='--cwd /path/to/project --message "Summarize this repository"'
```

The sample uses CoKit's default stdio app-server transport. Use
`COKIT_CODEX_COMMAND` only when you need to point it at another local command
during development.

The command prints the created thread and turn ids before streaming assistant
text from typed `CodexNotification.AgentMessageDelta` events. A completed turn
with no assistant text is reported explicitly, and failed turns exit with the
app-server error message.

## Connect To app-server

```kotlin
val transport = StdioCodexTransport()

val client = CodexRpcClient.connect(
    CodexRpcConnection(
        transport = transport,
        clientInfo = ClientInfo(
            name = "my_kotlin_client",
            title = "My Kotlin Client",
            version = "0.1.0",
        ),
        scope = scope,
    ),
)
```

`connect()` sends `initialize`, waits for the matching response, then sends the
`initialized` notification.

## Send JSON-RPC Requests

```kotlin
val thread = client.request(
    CodexRpc.Thread.Start,
    ThreadStartParams(
        cwd = CodexHostPath("/path/to/project"),
        approvalPolicy = ApprovalPolicy.OnRequest,
        sandbox = SandboxPolicy.WorkspaceWrite,
    ),
).thread

val turn = client.request(
    CodexRpc.Turn.Start,
    TurnStartParams(
        threadId = thread.id,
        input = listOf(TurnInput.Text("Summarize this repository")),
    ),
).turn
```

`CodexRpc` descriptors expose upstream operations through typed names such as
`CodexRpc.Thread.Start` and `CodexRpc.Turn.Start` without asking callers to pass
raw strings or JSON payloads. Each descriptor binds exactly one params type to
one result type, so `CodexRpc.Thread.Start` accepts `ThreadStartParams` and
returns `ThreadStartResult`.

Identifiers and common options use lightweight SDK value types such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`,
`ModelName`, and `TurnInput` so application code is explicit while preserving
the upstream wire shape.

## Inspect The Model Catalog

```kotlin
val page = client.request(
    CodexRpc.Model.List,
    ModelListParams(includeHidden = false),
)

val capabilities = client.request(
    CodexRpc.Model.ReadProviderCapabilities,
    ModelProviderCapabilitiesReadParams,
)

page.data.forEach { model ->
    println("${model.displayName}: ${model.model.value}")
}

if (capabilities.webSearch) {
    println("The configured provider supports web search.")
}
```

Model catalog descriptors are read-only protocol surfaces. `CodexRpc.Model.List`
returns typed catalog entries, pagination cursors, reasoning-effort options, and
service-tier metadata. `CodexRpc.Model.ReadProviderCapabilities` reports
provider-level feature flags such as web search, image generation, and namespace
tools without requiring callers to build raw JSON-RPC requests.

## Observe Notifications

```kotlin
client.notifications.collect { notification ->
    when (notification) {
        is CodexNotification.ThreadStarted -> println(notification.threadId.value)
        is CodexNotification.ThreadStatusChanged -> println(notification.status.value)
        is CodexNotification.ThreadTokenUsageUpdated -> println(notification.tokenUsage.total.totalTokens)
        is CodexNotification.TurnStarted -> println(notification.turn.id.value)
        is CodexNotification.TurnCompleted -> println(notification.turn.status.value)
        is CodexNotification.TurnFailed -> println(notification.turn.error?.message)
        is CodexNotification.ItemStarted -> println(notification.item.type.value)
        is CodexNotification.ItemCompleted -> println(notification.item.status?.value)
        is CodexNotification.AgentMessageDelta -> print(notification.delta)
        is CodexNotification.ReasoningSummaryTextDelta -> print(notification.delta)
        is CodexNotification.Warning -> println(notification.message)
        is CodexNotification.ConfigWarning -> println(notification.summary)
        is CodexNotification.Error -> println(notification.error.message)
        is CodexNotification.ServerRequestResolved -> println(notification.requestId)
        is CodexNotification.FilesystemChanged -> println(notification.changedPaths.size)
        is CodexNotification.Unknown -> println(notification.method)
    }
}
```

`notifications` exposes typed CoKit notification models. Unknown notifications
keep the method name but do not expose raw JSON through the primary API.
Turn failures are decoded from typed turn-completion notifications whose turn
status is `failed`. Item lifecycle notifications expose `ThreadItemSummary` for
common rendering fields, and streamed text deltas are typed for agent messages
and reasoning summaries. Token usage, warning, config warning, and error
notifications are typed without exposing raw notification params.
`ServerRequestResolved` exposes the thread id and JSON-RPC request id so
applications can clear pending approval, elicitation, attestation, or user-input
UI when app-server reports that the request was answered or cleared.
`FilesystemChanged` exposes the connection-scoped filesystem watch id and the
changed host paths reported by app-server.

## Handle Server Requests

CoKit defaults approval-like requests to safe responses when no handler is
registered. Consumers can register handlers for methods that need custom
decisions:

```kotlin
client.registerCommandApprovalHandler { request ->
    println(request.command)
    ApprovalDecision.Decline
}
```

Command approval handlers use typed request and decision models. Raw JSON-RPC
compatibility hooks remain available for advanced protocol work, but they are
not the default application API.
