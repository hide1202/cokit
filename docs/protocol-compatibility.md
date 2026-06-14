# Protocol Compatibility

CoKit tracks `codex app-server` as an upstream JSON-RPC protocol.

## Versioning

- CoKit library versions follow semantic versioning for public Kotlin APIs.
- Protocol schema metadata records the app-server schema generation command.
- Generated schema outputs are not produced during normal `check`; they are
  generated only when the schema task is explicitly invoked.

## Stable And Experimental APIs

Stable APIs should be available without special opt-in. Experimental APIs should
require explicit Kotlin opt-in annotations and app-server initialization
capabilities where upstream requires them.

The WebSocket transport is currently marked experimental because upstream marks
that transport as experimental.

## Public Client Model Policy

The primary client API is JSON-RPC-first. It should expose upstream method names
through typed descriptors such as `CodexRpc.Thread.Start`, not through ad hoc
raw strings. Each descriptor should bind one typed params model to one typed
result model.

All modeled thread and turn request methods should be present in the
`CodexRpc` descriptor catalog. Compatibility facades such as thread and turn
helpers should delegate through those descriptors instead of carrying separate
method strings.

Client APIs should accept request objects instead of long parameter lists.
Identifiers and common options should use small value classes such as
`ThreadId`, `TurnId`, `CodexHostPath`, `ApprovalPolicy`, `SandboxPolicy`, and
`ModelName`. Prefer value classes with documented constants over closed enums
when upstream may add new string values.

Primary client models should not expose `JsonElement`, JSON-RPC envelope types,
or other raw protocol payloads directly. Protocol areas that are not yet modeled
should be deferred from the primary API or kept behind explicit compatibility
types such as `CodexJsonPayload`; examples and getting-started documentation
should not require consumers to construct arbitrary JSON.

Turn input is a public client surface and should use `TurnInput` variants such
as `Text`, `Image`, `LocalImage`, `Skill`, and `Mention` instead of exposing raw
JSON as the primary API. Use `TurnInput.Custom` with `CodexJsonPayload` as an
explicit compatibility escape hatch for upstream variants that CoKit has not
modeled yet.

Notifications and server-initiated requests should be modeled as typed sealed
interfaces. Unknown notifications may expose the upstream method name, but they
should not expose raw JSON in the primary API. Approval-like server requests must
remain deny-by-default unless a typed handler is registered.

## Upstream Coverage Snapshot

This snapshot was reviewed against the upstream app-server README on
2026-06-14:

https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

The current `CodexRpc` descriptor catalog covers the core modeled thread and
turn request methods:

- `thread/start`
- `thread/resume`
- `thread/fork`
- `thread/list`
- `thread/read`
- `thread/archive`
- `thread/unarchive`
- `thread/unsubscribe`
- `thread/name/set`
- `turn/start`
- `turn/steer`
- `turn/interrupt`

`CodexRpcClient.connect()` also performs the required `initialize` request and
`initialized` notification internally.

The upstream README currently documents roughly 100 request methods when the
main API overview, auth/account surface, and initialization handshake are counted
together. On that basis, CoKit's typed request descriptor coverage is about 12%
of the full upstream request surface, or about 13% if the internal initialize
handshake is counted as implemented coverage.

Typed notification and server-request coverage is intentionally smaller than the
upstream surface today:

- Notifications: `CodexNotification.ThreadStarted` is modeled; unknown
  notifications expose only the method name in the primary API.
- Server requests: command execution approval is modeled with a typed handler.
  Approval-like request families without typed handlers remain deny-by-default.

The following upstream request groups are not yet modeled as primary typed
descriptors:

- Advanced thread APIs: loaded-thread listing, turn history paging, metadata,
  settings, memory mode, goals, delete, compaction, shell command, background
  terminals, rollback, realtime, and raw item injection.
- Review and execution APIs: review start, sandboxed command execution,
  standalone process lifecycle, and filesystem utilities.
- Catalog and configuration APIs: model, model-provider capabilities,
  experimental feature flags, permission profiles, environments, collaboration
  modes, MCP status/resources/tools, config read/write/reload, Windows sandbox
  setup, feedback upload, and external-agent import.
- Skills, hooks, apps, and plugins: skills list/config/extra roots, hooks list,
  marketplace operations, plugin list/install/read/uninstall, and app list.
- Remote control APIs: enable, disable, status, pairing, client list, and client
  revoke.
- Auth/account APIs: account read, login, logout, rate limits, usage, and add
  credits notification requests.

Future work should add these groups as typed descriptor namespaces without
changing the rule that primary APIs do not expose `JsonElement`, raw method
strings, or JSON-RPC envelopes.

## Schema Generation

Run:

```bash
./gradlew :cokit-protocol:generateCodexSchema
```

This task runs both stable and experimental schema generation modes:

```bash
codex app-server generate-json-schema --out build/generated/codex-schema/stable
codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental
```

The command requires a local `codex` executable.

## Fixture Policy

Protocol fixtures should come from upstream examples, generated schema samples,
or reduced examples that exercise specific parser behavior. Fixtures must not
include secrets, access tokens, private account data, auth URLs, or private local
paths.
