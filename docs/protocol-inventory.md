# Protocol Inventory

This inventory groups the upstream `codex app-server` JSON-RPC surface so CoKit
can track modeled, partial, deferred, and experimental protocol coverage without
publishing generated schema artifacts.

Source of truth:
https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

Reviewed against the upstream README on 2026-06-18.

## Status Legend

- `modeled`: CoKit exposes typed public models or descriptors for the whole
  group.
- `partial`: CoKit models at least one method or payload in the group, or
  implements bounded compatibility handling for safety, but the upstream group
  has unmodeled methods, fields, notifications, or decisions.
- `deferred`: CoKit intentionally has no typed public surface for the group yet.
- `experimental`: Upstream gates the method or field behind
  `capabilities.experimentalApi`, marks it unstable, or says it is under
  development.

Unknown notifications and unsupported server requests are not counted as
modeled coverage. They are compatibility behavior only.

## Request Groups

| Group | Status | Upstream methods | CoKit coverage |
| --- | --- | --- | --- |
| Connection lifecycle | partial | `initialize`, `initialized` | `CodexRpcClient.connect()` and `CodexAppServerClient.connect()` perform the handshake internally with typed `InitializeParams` and `InitializeCapabilities`. There is no public `CodexRpc` descriptor for `initialize`. |
| Core thread lifecycle | partial | `thread/start`, `thread/resume`, `thread/fork`, `thread/list`, `thread/read`, `thread/archive`, `thread/unarchive`, `thread/unsubscribe`, `thread/name/set` | Typed `CodexRpc.Thread` descriptors exist. Some params and result fields remain compatibility-limited or intentionally narrow. |
| Advanced thread history and metadata | partial | `thread/loaded/list`, `thread/turns/list`, `thread/turns/items/list`, `thread/metadata/update`, `thread/settings/update`, `thread/memoryMode/set`, `memory/reset`, `thread/goal/set`, `thread/goal/get`, `thread/goal/clear`, `thread/delete`, `thread/compact/start`, `thread/shellCommand`, `thread/backgroundTerminals/clean`, `thread/backgroundTerminals/list`, `thread/backgroundTerminals/terminate`, `thread/rollback`, `thread/inject_items` | `CodexRpc.Thread.ListTurns` models the experimental turn-history page shape. `CodexRpc.Thread.UpdateMetadata` models the stable `gitInfo` metadata patch and refreshed thread response. `CodexRpc.Thread.SetGoal`, `GetGoal`, `ClearGoal`, `Delete`, and `StartCompaction` model stable lifecycle helpers. Other advanced, experimental, or currently unsupported entries remain deferred. |
| Turn lifecycle | partial | `turn/start`, `turn/steer`, `turn/interrupt` | Typed `CodexRpc.Turn` descriptors exist. Experimental params such as environment selection and some schema-shaped fields are not fully modeled. |
| Thread realtime | experimental | `thread/realtime/start`, `thread/realtime/appendAudio`, `thread/realtime/appendText`, `thread/realtime/stop` | Deferred. Realtime requires explicit experimental opt-in before CoKit should expose descriptors. |
| Review | modeled | `review/start` | `CodexRpc.Review.Start` models typed review targets, inline or detached delivery, and the returned review thread id plus turn. CoKit exposes review protocol data but does not render review UI. |
| Sandboxed command execution | partial | `command/exec`, `command/exec/write`, `command/exec/resize`, `command/exec/terminate` | `CodexRpc.Command.Exec`, `WriteStdin`, `Resize`, and `Terminate` model the command request/control shapes from the `codex-cli 0.140.0` generated schema. Output notifications remain deferred. Approval handling for agent-driven command execution is tracked under server requests. |
| Standalone process lifecycle | experimental | `process/spawn`, `process/writeStdin`, `process/resizePty`, `process/kill` | `CodexRpc.Process.Spawn`, `WriteStdin`, `ResizePty`, and `Kill` model the experimental unsandboxed process lifecycle descriptors behind `ExperimentalCodexApi`. |
| Filesystem utilities | modeled | `fs/readFile`, `fs/writeFile`, `fs/createDirectory`, `fs/getMetadata`, `fs/readDirectory`, `fs/remove`, `fs/copy`, `fs/watch`, `fs/unwatch` | `CodexRpc.Filesystem.ReadFile`, `GetMetadata`, `ReadDirectory`, `WriteFile`, `CreateDirectory`, `Copy`, `Remove`, `Watch`, and `Unwatch` model host filesystem descriptors, including connection-scoped watch identifiers. |
| Models and provider catalog | modeled | `model/list`, `modelProvider/capabilities/read` | `CodexRpc.Model.List` models typed catalog entries, cursors, display names, reasoning options, service tiers, and input modalities. `CodexRpc.Model.ReadProviderCapabilities` models provider-level web search, image generation, and namespace-tool capability flags. |
| Feature and permission catalog | partial | `experimentalFeature/list`, `experimentalFeature/enablement/set`, `permissionProfile/list` | `CodexRpc.PermissionProfile.List` models permission profile ids, descriptions, optional cwd scoping, and pagination. Experimental feature list and enablement methods remain deferred. |
| Environments and collaboration modes | experimental | `environment/add`, `collaborationMode/list` | `CodexRpc.Environment.Add` and `CodexRpc.CollaborationMode.List` model the current `codex-cli 0.140.0` experimental schema behind `ExperimentalCodexApi`. The current schema does not define environment list/read or collaboration mode read descriptors. |
| Skills and hooks | modeled | `skills/list`, `skills/extraRoots/set`, `skills/config/write`, `hooks/list` | `CodexRpc.Skills.List`, `SetExtraRoots`, and `WriteConfig` model current skills catalog, extra-root, and config-write descriptors. `CodexRpc.Hooks.List` models hook metadata, source, enabled state, trust status, warnings, and parse errors. Current `codex-cli 0.140.0` schema does not define a `skills/config/read` request. |
| Apps, marketplaces, and plugins | partial | `app/list`, `marketplace/add`, `marketplace/remove`, `marketplace/upgrade`, `plugin/list`, `plugin/installed`, `plugin/read`, `plugin/skill/read`, `plugin/install`, `plugin/uninstall` | `CodexRpc.Apps.List` models the experimental app catalog page shape behind `ExperimentalCodexApi`, including branding and optional app metadata. Marketplace and plugin descriptors remain deferred. Plugin APIs marked under development upstream are treated as experimental. |
| MCP APIs | deferred | `mcpServer/oauth/login`, `config/mcpServer/reload`, `mcpServerStatus/list`, `mcpServer/resource/read`, `mcpServer/tool/call` | No typed descriptors yet. Server-initiated MCP elicitation is tracked under server requests. |
| Windows sandbox setup | deferred | `windowsSandbox/setupStart` | No typed descriptor yet. |
| Feedback upload | deferred | `feedback/upload` | No typed descriptor yet. |
| Config read and write | partial | `config/read`, `config/value/write`, `config/batchWrite`, `configRequirements/read` | `CodexRpc.Config.Read`, `WriteValue`, and `BatchWrite` model effective config reads, single value writes, batch writes, and batch write reload flags. Arbitrary config values stay behind `ConfigValue` and `CodexJsonPayload`. `configRequirements/read` remains deferred for managed-policy models. |
| External agent migration | deferred | `externalAgentConfig/detect`, `externalAgentConfig/import` | No typed descriptors yet. |
| Remote control | experimental | `remoteControl/enable`, `remoteControl/disable`, `remoteControl/status/read`, `remoteControl/pairing/start`, `remoteControl/pairing/status`, `remoteControl/client/list`, `remoteControl/client/revoke` | Deferred. Remote-control methods require explicit experimental opt-in and trust-boundary documentation. |
| Tool user-input utility | experimental | `tool/requestUserInput` | Deferred. This is distinct from the server-initiated `item/tool/requestUserInput` flow. |
| Account and auth | deferred | `account/read`, `account/login/start`, `account/login/cancel`, `account/logout`, `account/rateLimits/read`, `account/usage/read`, `account/sendAddCreditsNudgeEmail` | No typed descriptors yet. Future auth models must avoid logging credentials, auth URLs, account identifiers, and tokens. |

## Notification Groups

| Group | Status | Upstream notifications | CoKit coverage |
| --- | --- | --- | --- |
| Thread lifecycle | partial | `thread/started`, `thread/archived`, `thread/unarchived`, `thread/closed`, `thread/deleted`, `thread/name/updated`, `thread/status/changed`, `thread/tokenUsage/updated` | `CodexNotification.ThreadStarted`, `ThreadStatusChanged`, and `ThreadTokenUsageUpdated` are typed. Other thread notifications currently map to `CodexNotification.Unknown`. |
| Thread goal and settings | experimental | `thread/goal/updated`, `thread/goal/cleared`, `thread/settings/updated` | Deferred. Settings updates are experimental upstream. |
| Turn lifecycle | partial | `turn/started`, `turn/completed`, `turn/diff/updated`, `turn/plan/updated`, `turn/moderationMetadata` | `CodexNotification.TurnStarted`, `TurnCompleted`, and `TurnFailed` are typed. `TurnFailed` is decoded from `turn/completed` when `turn.status` is `failed`. Other turn notifications currently map to `CodexNotification.Unknown`. |
| Model routing and verification | deferred | `model/rerouted`, `model/verification` | No typed notifications yet. |
| Item lifecycle | partial | `item/started`, `item/completed` | `CodexNotification.ItemStarted` and `ItemCompleted` expose the common `ThreadItemSummary` compatibility wrapper. Unsupported item fields remain out of the primary API. |
| Temporary auto-approval review | experimental | `item/autoApprovalReview/started`, `item/autoApprovalReview/completed` | Deferred. Upstream marks the shape unstable. |
| Agent message streaming | modeled | `item/agentMessage/delta` | `CodexNotification.AgentMessageDelta` models the streamed text payload. |
| Plan streaming | experimental | `item/plan/delta` | Deferred. Upstream marks plan deltas experimental. |
| Reasoning streaming | partial | `item/reasoning/summaryTextDelta`, `item/reasoning/summaryPartAdded`, `item/reasoning/textDelta` | `CodexNotification.ReasoningSummaryTextDelta` models summary text streaming. Summary part boundaries and raw reasoning text remain deferred. |
| Command execution item streaming | deferred | `item/commandExecution/outputDelta` | No typed notification yet. |
| File change item streaming | deferred | `item/fileChange/patchUpdated`, `item/fileChange/outputDelta` | No typed notification yet. File-change approvals are deny-by-default as server requests. |
| Command execution output | modeled | `command/exec/outputDelta` | `CodexNotification.CommandExecOutputDelta` models base64 stdout/stderr chunks, connection-scoped process ids, and `capReached` truncation flags. Current upstream schema does not define separate command exit or error notifications. |
| Process output and exit | experimental | `process/outputDelta`, `process/exited` | Deferred. |
| Filesystem watch | modeled | `fs/changed` | `CodexNotification.FilesystemChanged` exposes the connection-scoped watch id and changed host paths for `fs/watch` subscribers. |
| Fuzzy file search | experimental | `fuzzyFileSearch/sessionUpdated`, `fuzzyFileSearch/sessionCompleted` | Deferred. |
| Thread realtime | experimental | `thread/realtime/started`, `thread/realtime/sdp`, `thread/realtime/itemAdded`, `thread/realtime/transcript/delta`, `thread/realtime/transcript/done`, `thread/realtime/outputAudio/delta`, `thread/realtime/error`, `thread/realtime/closed` | Deferred. |
| Windows sandbox setup | deferred | `windowsSandbox/setupCompleted` | No typed notification yet. |
| MCP startup and OAuth | deferred | `mcpServer/startupStatus/updated`, `mcpServer/oauthLogin/completed` | No typed notifications yet. |
| Skill and app catalog changes | deferred | `skills/changed`, `app/list/updated` | No typed notifications yet. |
| Account and auth | deferred | `account/login/completed`, `account/updated`, `account/rateLimits/updated` | No typed notifications yet. |
| Remote-control status | experimental | `remoteControl/status/changed` | Deferred. |
| External agent migration | deferred | `externalAgentConfig/import/completed` | No typed notification yet. |
| Server-request lifecycle | modeled | `serverRequest/resolved` | `CodexNotification.ServerRequestResolved` exposes the thread id and JSON-RPC request id so applications can clear pending request UI after the server reports resolution or cleanup. Current upstream schema does not include request method or status fields on this notification. |
| Warnings and errors | modeled | `configWarning`, `warning`, `error` | `CodexNotification.ConfigWarning`, `Warning`, and `Error` expose safe typed fields without raw notification params. Structured `codexErrorInfo` remains deferred. |

## Server-Request Groups

| Group | Status | Upstream server requests | CoKit coverage |
| --- | --- | --- | --- |
| Command execution approval | partial | `item/commandExecution/requestApproval` | Typed `CodexServerRequest.CommandApproval` and approval handlers exist. Some newer upstream decisions and experimental additional-permission payloads are not fully modeled. Without a handler, CoKit declines by default. |
| File change approval | partial | `item/fileChange/requestApproval` | Typed `CodexServerRequest.FileChangeApproval` and approval handlers exist. Unstable grant-root semantics are exposed as host paths. Without a handler, CoKit declines by default. |
| Permission grant approval | partial | `item/permissions/requestApproval` | Typed `CodexServerRequest.PermissionApproval` and approval handlers exist for requested filesystem/network grants. Without a handler, CoKit returns an empty granted-permissions profile. |
| Dynamic tool call | experimental | `item/tool/call` | Deferred. Without a handler, CoKit declines by default. |
| Tool user-input prompt | experimental | `item/tool/requestUserInput` | Typed `CodexServerRequest.UserInput` and user-input handlers exist for questions, options, and answer maps. Upstream marks this flow experimental. Without a handler, CoKit cancels by default. |
| MCP elicitation | partial | `mcpServer/elicitation/request` | Typed `CodexServerRequest.McpElicitation` and MCP elicitation handlers exist for form and URL requests. Without a handler, CoKit declines by default. |
| Attestation generation | partial | `attestation/generate` | Typed `CodexServerRequest.AttestationGenerate` and attestation handlers exist for empty generate requests and opaque token responses. Without a handler, CoKit returns an unsupported status by default. |

## Current Modeled Request Descriptors

This table is checked by `CodexRpcInventoryTest`. Every public `CodexRpc`
request descriptor must have one row here so new descriptors cannot be added
without updating the public inventory.

| Descriptor | Method | Coverage note |
| --- | --- | --- |
| `CodexRpc.Thread.Start` | `thread/start` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Resume` | `thread/resume` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Fork` | `thread/fork` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.List` | `thread/list` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Read` | `thread/read` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Archive` | `thread/archive` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Unarchive` | `thread/unarchive` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.Unsubscribe` | `thread/unsubscribe` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.SetName` | `thread/name/set` | Thread lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Thread.UpdateMetadata` | `thread/metadata/update` | Metadata descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Thread.ListTurns` | `thread/turns/list` | Experimental turn history paging descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Thread.Delete` | `thread/delete` | Thread lifecycle helper; advanced thread coverage remains partial. |
| `CodexRpc.Thread.SetGoal` | `thread/goal/set` | Thread goal descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Thread.GetGoal` | `thread/goal/get` | Thread goal descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Thread.ClearGoal` | `thread/goal/clear` | Thread goal descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Thread.StartCompaction` | `thread/compact/start` | Thread compaction descriptor; advanced thread coverage remains partial. |
| `CodexRpc.Turn.Start` | `turn/start` | Turn lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Turn.Steer` | `turn/steer` | Turn lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Turn.Interrupt` | `turn/interrupt` | Turn lifecycle descriptor; group coverage remains partial. |
| `CodexRpc.Command.Exec` | `command/exec` | Sandboxed command execution descriptor; group coverage remains partial. |
| `CodexRpc.Command.WriteStdin` | `command/exec/write` | Sandboxed command stdin descriptor; group coverage remains partial. |
| `CodexRpc.Command.Resize` | `command/exec/resize` | Sandboxed command PTY resize descriptor; group coverage remains partial. |
| `CodexRpc.Command.Terminate` | `command/exec/terminate` | Sandboxed command termination descriptor; group coverage remains partial. |
| `CodexRpc.Filesystem.ReadFile` | `fs/readFile` | Read-only host filesystem descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.GetMetadata` | `fs/getMetadata` | Read-only host filesystem metadata descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.ReadDirectory` | `fs/readDirectory` | Read-only host filesystem directory descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.WriteFile` | `fs/writeFile` | Host filesystem mutation descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.CreateDirectory` | `fs/createDirectory` | Host filesystem mutation descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.Copy` | `fs/copy` | Host filesystem mutation descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.Remove` | `fs/remove` | Host filesystem mutation descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.Watch` | `fs/watch` | Host filesystem watch descriptor; group coverage is modeled. |
| `CodexRpc.Filesystem.Unwatch` | `fs/unwatch` | Host filesystem unwatch descriptor; group coverage is modeled. |
| `CodexRpc.Process.Spawn` | `process/spawn` | Experimental unsandboxed process descriptor; group coverage remains experimental. |
| `CodexRpc.Process.WriteStdin` | `process/writeStdin` | Experimental unsandboxed process stdin descriptor; group coverage remains experimental. |
| `CodexRpc.Process.Kill` | `process/kill` | Experimental unsandboxed process termination descriptor; group coverage remains experimental. |
| `CodexRpc.Process.ResizePty` | `process/resizePty` | Experimental unsandboxed process PTY resize descriptor; group coverage remains experimental. |
| `CodexRpc.Review.Start` | `review/start` | Review start descriptor; group coverage is modeled without review UI. |
| `CodexRpc.Model.List` | `model/list` | Read-only model catalog descriptor; group coverage is modeled. |
| `CodexRpc.Model.ReadProviderCapabilities` | `modelProvider/capabilities/read` | Provider capability descriptor; group coverage is modeled. |
| `CodexRpc.Config.Read` | `config/read` | Effective config read descriptor; config group coverage remains partial. |
| `CodexRpc.Config.WriteValue` | `config/value/write` | Single config value write descriptor; config group coverage remains partial. |
| `CodexRpc.Config.BatchWrite` | `config/batchWrite` | Batch config write descriptor with reload flag; config group coverage remains partial. |
| `CodexRpc.Skills.List` | `skills/list` | Skills catalog descriptor; skills and hooks coverage is modeled. |
| `CodexRpc.Skills.SetExtraRoots` | `skills/extraRoots/set` | Extra skill roots descriptor; skills and hooks coverage is modeled. |
| `CodexRpc.Skills.WriteConfig` | `skills/config/write` | Skill enablement config descriptor; skills and hooks coverage is modeled. |
| `CodexRpc.Hooks.List` | `hooks/list` | Hook catalog descriptor; skills and hooks coverage is modeled. |
| `CodexRpc.Apps.List` | `app/list` | Experimental app catalog descriptor; apps, marketplaces, and plugins coverage remains partial. |
| `CodexRpc.PermissionProfile.List` | `permissionProfile/list` | Permission profile catalog descriptor; feature and permission catalog coverage remains partial. |
| `CodexRpc.CollaborationMode.List` | `collaborationMode/list` | Experimental collaboration mode descriptor; group coverage remains experimental. |
| `CodexRpc.Environment.Add` | `environment/add` | Experimental environment registration descriptor; group coverage remains experimental. |

## Current Modeled Method Summary

Current public request descriptors:

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
- `fs/writeFile`
- `fs/createDirectory`
- `fs/copy`
- `fs/remove`
- `fs/watch`
- `fs/unwatch`
- `process/spawn`
- `process/writeStdin`
- `process/kill`
- `process/resizePty`
- `review/start`
- `model/list`
- `modelProvider/capabilities/read`
- `config/read`
- `config/value/write`
- `config/batchWrite`
- `skills/list`
- `skills/extraRoots/set`
- `skills/config/write`
- `hooks/list`
- `app/list`
- `permissionProfile/list`
- `collaborationMode/list`
- `environment/add`

Current typed notification:

- `thread/started`
- `thread/status/changed`
- `thread/tokenUsage/updated`
- `turn/started`
- `turn/completed`
- `item/started`
- `item/completed`
- `item/agentMessage/delta`
- `item/reasoning/summaryTextDelta`
- `warning`
- `configWarning`
- `error`
- `serverRequest/resolved`
- `command/exec/outputDelta`
- `fs/changed`

Current typed server request:

- `item/commandExecution/requestApproval`
- `item/fileChange/requestApproval`
- `item/permissions/requestApproval`
- `item/tool/requestUserInput`
- `mcpServer/elicitation/request`
