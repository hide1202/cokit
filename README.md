# CoKit

CoKit is a Kotlin Multiplatform client library for `codex app-server`.

The project provides typed Kotlin APIs over the app-server JSON-RPC protocol,
with transport, protocol, RPC runtime, and high-level client concerns kept in
separate modules.

## Status

CoKit is in early development. The current codebase includes:

- JSON-RPC protocol envelopes and serializers.
- A coroutine JSON-RPC session with response correlation and notification flows.
- A high-level app-server client initialization handshake.
- Typed thread and turn start APIs.
- Default handling for server-initiated approval-like requests.
- JVM stdio JSONL transport.
- A guarded integration smoke test for a real local `codex app-server`.
- A schema generation Gradle workflow for app-server JSON Schema.

## Modules

- `cokit-protocol`: JSON-RPC messages, protocol serializers, and schema metadata.
- `cokit-rpc`: request correlation, notification routing, and server request flow.
- `cokit-client`: high-level Codex app-server client APIs.
- `cokit-transport-stdio`: JVM stdio transport for `codex app-server --stdio`.
- `cokit-transport-websocket`: experimental WebSocket transport placeholder.
- `cokit-testing`: fake transports and protocol test helpers.

## Basic Example

```kotlin
val transport = StdioCodexTransport(
    command = listOf("codex", "app-server", "--stdio"),
)

val client = CodexAppServerClient.connect(
    transport = transport,
    clientInfo = ClientInfo(
        name = "cokit_sample",
        title = "CoKit Sample",
        version = "0.1.0",
    ),
    scope = scope,
)

val thread = client.threads.start(cwd = "/path/to/project")
val turn = client.turns.start(threadId = thread.id)
```

## Upstream Protocol

CoKit follows the upstream app-server protocol documented in:

https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

Generated app-server JSON Schema should be treated as the source of truth for
wire shape changes.

## Verification

Run:

```bash
./gradlew check
```

Run JVM unit tests only:

```bash
./gradlew test
```

Generate aggregate Kover coverage reports:

```bash
./gradlew coverage
```

The aggregate HTML report is written to `build/reports/kover/html/index.html`.
See [Coverage](docs/coverage.md) for report paths and KMP coverage scope.

To run the real app-server integration smoke test on a machine with `codex`
installed:

```bash
COKIT_CODEX_INTEGRATION=1 ./gradlew :cokit-client:jvmTest
```
