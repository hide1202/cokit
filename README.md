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
- A JSON-RPC-first `CodexRpcClient` with typed descriptors for the currently
  modeled thread and turn request APIs.
- Default handling for server-initiated approval-like requests.
- JVM stdio JSONL transport.
- A guarded integration smoke test for a real local `codex app-server`.
- A schema generation Gradle workflow for app-server JSON Schema.

The upstream app-server README documents a much broader protocol surface than
CoKit currently models. See [Protocol Compatibility](docs/protocol-compatibility.md)
for the current coverage snapshot and implementation roadmap.

## Modules

- `cokit-protocol`: JSON-RPC messages, protocol serializers, and schema
  metadata. This module does not depend on client, runtime, or transport code.
- `cokit-rpc`: request correlation, notification routing, and server request
  flow. This module depends on protocol types only.
- `cokit-client`: high-level Codex app-server client APIs. Public operations use
  typed option and request models while the raw JSON-RPC session stays internal.
- `cokit-transport-stdio`: JVM stdio transport for the local Codex app-server.
- `cokit-transport-websocket`: experimental WebSocket transport placeholder.
- `cokit-testing`: fake transports and protocol test helpers for consumers and
  CoKit module tests.
- `cokit-sample-cli`: small JVM command-line sample that exercises the public
  client and stdio transport APIs.

## Basic Example

```kotlin
val transport = StdioCodexTransport()

val client = CodexRpcClient.connect(
    CodexRpcConnection(
        transport = transport,
        clientInfo = ClientInfo(
            name = "cokit_sample",
            title = "CoKit Sample",
            version = "0.1.0",
        ),
        scope = scope,
    ),
)

val thread = client.request(
    CodexRpc.Thread.Start,
    ThreadStartParams(cwd = CodexHostPath("/path/to/project")),
).thread
val turn = client.request(
    CodexRpc.Turn.Start,
    TurnStartParams(
        threadId = thread.id,
        input = listOf(TurnInput.Text("Summarize this repository")),
    ),
).turn
```

## Sample CLI

Show the included sample CLI help:

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

Start a real app-server thread, send the default message, and stream the
assistant response when `codex` is installed locally:

```bash
./gradlew :cokit-sample-cli:run
```

Override the default app-server host path or message when needed:

```bash
./gradlew :cokit-sample-cli:run --args='--cwd /path/to/project --message "Summarize this repository"'
```

The sample uses `StdioCodexTransport` defaults. Set `COKIT_CODEX_COMMAND` to a
whitespace-separated command when testing a different local app-server
executable.

The command prints the created thread and turn ids, then streams assistant text
from typed `CodexNotification.AgentMessageDelta` events. If the turn completes
without assistant text, the sample reports that no assistant message was
produced; if the turn fails, it exits with the app-server error message.

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

`check` includes unit tests, coverage verification, module-boundary validation,
public API exposure checks, public API baseline checks, and primary docs/sample
alignment checks.

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

Before publishing a release candidate, follow the
[Release Readiness](docs/release-readiness.md) checklist.
