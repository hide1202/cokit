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

Client-level experimental protocol surfaces use `ExperimentalCodexApi`.
Experimental descriptors must require `@ExperimentalCodexApi` at the Kotlin API
usage site and must also require the matching app-server initialization
capability opt-in before they become usable.

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
`ModelName`. Thread, turn, item, pagination, and client-message scalar fields
should likewise use wrappers such as `CodexCursor`, `CodexTimestamp`,
`ClientMessageId`, `ThreadStatusType`, `TurnStatus`, `ItemId`, and `ItemStatus`.
Prefer value classes with documented constants over closed enums when upstream
may add new string values.

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
2026-06-18:

https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

The checked protocol inventory groups upstream request, notification, and
server-request surfaces by current CoKit coverage:
[Protocol Inventory](protocol-inventory.md).

The current `CodexRpc` descriptor catalog covers the core modeled thread, turn,
command, and read-only filesystem request methods:

- `thread/start`
- `thread/resume`
- `thread/fork`
- `thread/list`
- `thread/read`
- `thread/archive`
- `thread/unarchive`
- `thread/unsubscribe`
- `thread/name/set`
- `thread/metadata/update`
- `thread/turns/list`
- `thread/delete`
- `thread/goal/set`
- `thread/goal/get`
- `thread/goal/clear`
- `thread/compact/start`
- `turn/start`
- `turn/steer`
- `turn/interrupt`
- `command/exec`
- `command/exec/write`
- `command/exec/resize`
- `command/exec/terminate`
- `fs/readFile`
- `fs/getMetadata`
- `fs/readDirectory`

`CodexRpcClient.connect()` also performs the required `initialize` request and
`initialized` notification internally.

The following summary is checked by `CodexRpcCoverageTest`. Inventory group
counts are derived from `docs/protocol-inventory.md`; the public `CodexRpc`
request descriptor count is exact.

<!-- codex-rpc-coverage:start -->
| Inventory section | `modeled` | `partial` | `deferred` | `experimental` | Exact current coverage |
| --- | ---: | ---: | ---: | ---: | --- |
| Request groups | 0 | 6 | 10 | 6 | 26 public `CodexRpc` request descriptors |
| Notification groups | 4 | 4 | 9 | 7 | Not counted by this helper |
| Server-request groups | 0 | 5 | 0 | 2 | Not counted by this helper |
<!-- codex-rpc-coverage:end -->

The upstream README currently documents roughly 100 request methods when the
main API overview, auth/account surface, and initialization handshake are counted
together. On that basis, CoKit's typed request descriptor coverage is about 26%
of the full upstream request surface, or about 27% if the internal initialize
handshake is counted as implemented coverage.

Typed notification and server-request coverage is intentionally smaller than the
upstream surface today:

- Notifications: `CodexNotification.ThreadStarted`, `ThreadStatusChanged`,
  `ThreadTokenUsageUpdated`, `TurnStarted`, `TurnCompleted`, `TurnFailed`,
  `ItemStarted`, `ItemCompleted`, `AgentMessageDelta`,
  `ReasoningSummaryTextDelta`, `Warning`, `ConfigWarning`, `Error`,
  `ServerRequestResolved`, and `CommandExecOutputDelta` are modeled.
  `TurnFailed` is decoded from upstream `turn/completed` notifications with
  `turn.status == "failed"`. Item lifecycle events expose a `ThreadItemSummary`
  compatibility wrapper for common rendering fields.
  Error notifications expose safe message fields; structured `codexErrorInfo`
  remains deferred until it has a typed compatibility model.
  `serverRequest/resolved` currently carries only `threadId` and `requestId`;
  CoKit preserves that schema shape instead of fabricating method or status
  fields. `command/exec/outputDelta` currently carries base64 stdout/stderr
  chunks and `capReached`; command exit status remains part of the final
  `command/exec` response, and command failures use JSON-RPC errors rather than
  separate notifications. Unknown notifications expose only the method name in
  the primary API.
- Server requests: command execution approval, file-change approval, permission
  approval, tool user-input prompts, and MCP elicitations are modeled with typed
  handlers. Permission approvals return granted permission subsets instead of
  command-style decision strings. User-input prompts return typed answer maps
  only from explicit handlers and cancel by default. MCP elicitations expose
  form and URL requests and decline by default. Approval-like request families
  without typed handlers remain deny-by-default.

The following upstream request groups are not yet modeled as primary typed
descriptors:

- Advanced thread APIs: loaded-thread listing, turn-item hydration, settings,
  memory mode, shell command, background terminals, rollback, realtime, and raw
  item injection.
- Review and execution APIs: review start, standalone process lifecycle,
  filesystem mutation/watch utilities.
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

## Implementation Roadmap

This roadmap orders the remaining protocol work by SDK usefulness, protocol
risk, and security sensitivity. It is not a release commitment; it is the
preferred implementation sequence when extending CoKit toward the full upstream
app-server surface.

### Phase 1: Protocol Inventory And Generation

Goal: make the upstream README and generated schema the repeatable source of
truth before broadening the public API.

- Maintain a checked protocol inventory that groups every upstream method into
  stable, experimental, notification, and server-request surfaces.
- Generate stable and experimental schema fixtures from a recorded Codex version
  or upstream commit, and validate generated DTOs against those fixtures.
- Add a public coverage table that reports modeled request descriptors, typed
  notifications, typed server requests, and explicit compatibility gaps.
- Keep `CodexJsonPayload` limited to documented compatibility fields and avoid
  adding new primary APIs that require callers to construct arbitrary JSON.

Exit criteria:

- Coverage can be recalculated from a documented upstream README/schema version.
- New method descriptors cannot be added without params/result serializer tests.
- Stable and experimental surfaces are visibly separated in source and docs.

### Phase 2: Core Thread And Turn Fidelity

Goal: complete the ordinary conversation lifecycle before utility and account
surfaces.

- Expand typed models for thread metadata, status, settings, permission profile
  selection, runtime workspace roots, environments, token usage, and paged turn
  history.
- Add descriptors for loaded-thread listing, turn history paging, metadata
  updates, settings updates, memory mode, goals, delete, compaction, shell
  command, rollback, background terminals, and realtime methods.
- Model the event stream needed by normal clients: `thread/*`, `turn/*`,
  `item/started`, `item/completed`, item deltas, token usage, warnings, and
  errors.
- Keep unsupported upstream methods, such as currently unsupported item
  hydration endpoints, out of the stable convenience surface until CoKit can
  represent their unsupported state intentionally.

Exit criteria:

- A client can start, resume, fork, page, render, steer, interrupt, archive,
  restore, delete, and observe a thread without raw JSON.
- Event rendering code can rely on typed sealed models for common thread, turn,
  item, warning, and error notifications.
- Experimental thread and realtime APIs require explicit initialization and API
  opt-in.

### Phase 3: Server-Initiated Requests And Approvals

Goal: make app-server initiated work safe, typed, and deny-by-default.

- Add typed server-request models and handlers for command approval, file-change
  approval, permission requests, dynamic tool calls, MCP elicitation, user input
  requests, and attestation generation.
- Preserve automatic decline or cancel defaults for approval-like requests when
  no handler is registered.
- Permission request handlers grant only explicit filesystem and network
  subsets; an empty grant is the default denial shape and session grants must be
  requested with `scope: "session"`.
- Attestation generation handlers require explicit application registration and
  return opaque client-owned tokens; the absence of a handler returns
  unsupported by default.
- Emit typed request lifecycle notifications such as `serverRequest/resolved`
  so applications can clear pending UI state reliably.
- Document handler trust boundaries, host path semantics, and persistence
  behavior for session-scoped grants.

Exit criteria:

- Applications can choose safe defaults or explicit handlers without seeing
  JSON-RPC envelopes.
- Security tests cover malformed requests, missing responses, default denial,
  and handler failures.
- No handler accepts command execution, file changes, permission grants, tool
  calls, elicitation, attestation, or user input by default.

### Phase 4: Host Utilities And Execution APIs

Goal: expose host-side utility APIs after the approval and event model is strong
enough to observe side effects.

- Add typed descriptors and models for `command/exec`, streaming command input
  and output, command resizing, and command termination.
- Add typed descriptors and models for filesystem read/write, directory,
  metadata, copy, remove, watch, and unwatch methods.
- Add experimental typed descriptors for standalone process lifecycle APIs.
- Add review-start support once review items and detached review threads are
  modeled well enough for consumers to render results safely.

Exit criteria:

- Command and process APIs document sandbox differences, host semantics, output
  caps, timeout behavior, and connection-scoped lifecycle.
- Filesystem APIs require explicit absolute host paths in typed value models.
- Streaming output and filesystem watch notifications are typed and bounded.

### Phase 5: Catalog, Configuration, And Extension Surfaces

Goal: let applications inspect app-server capabilities and extension metadata
without implementing local UI policy inside CoKit.

- Add model catalog, model-provider capability, experimental feature,
  permission-profile, environment, collaboration-mode, and config read/write
  descriptors.
- Add skills, skill config, extra skill roots, hooks, apps, marketplace, plugin,
  and plugin-skill descriptors.
- Add MCP status, resource read, tool call, OAuth login, and config reload
  descriptors.
- Keep plugin, app, hook, and MCP APIs data-oriented. CoKit should expose typed
  state and events, not render UI or decide user policy.

Exit criteria:

- Clients can build settings, catalog, extension, and MCP screens from typed
  models.
- Experimental and under-development plugin/app APIs remain opt-in and clearly
  marked.
- Documentation distinguishes data surfaces from UI responsibilities.

### Phase 6: Auth, Account, Remote Control, And Managed Policy

Goal: complete account and remote-control support after core local protocol
surfaces are reliable.

- Add account read, login start, login cancel, logout, usage, rate limit, and
  add-credits notification descriptors and models.
- Add account update, login completed, rate-limit update, and MCP OAuth
  completion notifications.
- Add remote-control enable, disable, status, pairing, client list, client
  revoke, and status-change models behind experimental opt-in.
- Add config requirements and managed policy models so applications can explain
  disabled capabilities without duplicating raw config parsing.

Exit criteria:

- Auth flows avoid logging API keys, auth URLs, tokens, emails, and account
  identifiers by default.
- Remote-control APIs document enrollment, pairing, revocation, and local trust
  boundaries before becoming usable.
- Managed policy constraints can be surfaced through typed read-only models.

### Phase 7: Compatibility, Hardening, And Release Readiness

Goal: make the API safe to publish and maintain as upstream evolves.

- Add binary/API inspection or source-level checks that prevent accidental
  primary API exposure of `JsonElement` and JSON-RPC envelope types.
- Add high-rate notification, malformed message, oversized message, overload,
  retry, and shutdown tests across protocol, RPC, transport, and client modules.
- Document retry behavior for app-server overload errors and connection
  lifecycle failures.
- Keep sample CLI and getting-started docs aligned with the primary typed API.
- Re-run public exposure and security scans before release candidates.

Exit criteria:

- `./gradlew check --stacktrace` and public exposure/security grep pass.
- Docs include supported, experimental, deferred, and compatibility surfaces.
- A new upstream README/schema update has a documented workflow for updating
  descriptors, DTOs, tests, sample code, and coverage tables together.

## Schema Generation

Schema provenance is recorded in
`cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`.
The file records the Codex CLI version, upstream Codex commit, stable schema
command, experimental schema command, and the timestamp for the provenance
snapshot.

Run:

```bash
./gradlew :cokit-protocol:generateCodexSchema
```

This task runs both stable and experimental schema generation modes:

```bash
codex app-server generate-json-schema --out build/generated/codex-schema/stable
codex app-server generate-json-schema --out build/generated/codex-schema/experimental --experimental
```

The command requires a local `codex` executable. Before refreshing generated
schema fixtures or generated DTOs, update the provenance file with:

```bash
codex --version
git ls-remote https://github.com/openai/codex.git refs/heads/main
```

Then update `generatedAt` to the refresh timestamp. The Gradle schema generation
tasks validate that the provenance file exists, includes every required key, and
records the stable or experimental command for the generation mode being run.
Do not commit generated schema outputs unless the provenance file is updated in
the same change.

## Fixture Policy

Protocol fixtures should come from upstream examples, generated schema samples,
or reduced examples that exercise specific parser behavior. Fixtures must not
include secrets, access tokens, private account data, auth URLs, or private local
paths.
