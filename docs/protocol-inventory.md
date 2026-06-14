# Protocol Inventory

This inventory groups the upstream `codex app-server` JSON-RPC surface so CoKit
can track modeled, partial, deferred, and experimental protocol coverage without
publishing generated schema artifacts.

Source of truth:
https://github.com/openai/codex/blob/main/codex-rs/app-server/README.md

Reviewed against the upstream README on 2026-06-15.

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
| Advanced thread history and metadata | deferred | `thread/loaded/list`, `thread/turns/list`, `thread/turns/items/list`, `thread/metadata/update`, `thread/settings/update`, `thread/memoryMode/set`, `memory/reset`, `thread/goal/set`, `thread/goal/get`, `thread/goal/clear`, `thread/delete`, `thread/compact/start`, `thread/shellCommand`, `thread/backgroundTerminals/clean`, `thread/backgroundTerminals/list`, `thread/backgroundTerminals/terminate`, `thread/rollback`, `thread/inject_items` | No typed descriptors yet. Experimental and currently unsupported entries stay out of the stable client API. |
| Turn lifecycle | partial | `turn/start`, `turn/steer`, `turn/interrupt` | Typed `CodexRpc.Turn` descriptors exist. Experimental params such as environment selection and some schema-shaped fields are not fully modeled. |
| Thread realtime | experimental | `thread/realtime/start`, `thread/realtime/appendAudio`, `thread/realtime/appendText`, `thread/realtime/stop` | Deferred. Realtime requires explicit experimental opt-in before CoKit should expose descriptors. |
| Review | deferred | `review/start` | No typed descriptor yet. Review item notifications are also deferred. |
| Sandboxed command execution | deferred | `command/exec`, `command/exec/write`, `command/exec/resize`, `command/exec/terminate` | No typed descriptors yet. Approval handling for agent-driven command execution is tracked under server requests. |
| Standalone process lifecycle | experimental | `process/spawn`, `process/writeStdin`, `process/resizePty`, `process/kill` | Deferred. Upstream marks this surface experimental and unsandboxed. |
| Filesystem utilities | deferred | `fs/readFile`, `fs/writeFile`, `fs/createDirectory`, `fs/getMetadata`, `fs/readDirectory`, `fs/remove`, `fs/copy`, `fs/watch`, `fs/unwatch` | No typed descriptors yet. Future models must keep host-path semantics explicit. |
| Models and provider catalog | deferred | `model/list`, `modelProvider/capabilities/read` | No typed descriptors yet. |
| Feature and permission catalog | experimental | `experimentalFeature/list`, `experimentalFeature/enablement/set`, `permissionProfile/list` | Deferred. Upstream marks permission profiles beta and feature enablement experimental. |
| Environments and collaboration modes | experimental | `environment/add`, `collaborationMode/list` | Deferred. These methods affect execution context and must stay opt-in. |
| Skills and hooks | deferred | `skills/list`, `skills/extraRoots/set`, `skills/config/write`, `hooks/list` | No typed descriptors yet. |
| Apps, marketplaces, and plugins | deferred | `app/list`, `marketplace/add`, `marketplace/remove`, `marketplace/upgrade`, `plugin/list`, `plugin/installed`, `plugin/read`, `plugin/skill/read`, `plugin/install`, `plugin/uninstall` | No typed descriptors yet. Turn input mentions model app and plugin invocation payloads, but catalog, install, auth, and marketplace APIs are deferred. Plugin APIs marked under development upstream are treated as experimental. |
| MCP APIs | deferred | `mcpServer/oauth/login`, `config/mcpServer/reload`, `mcpServerStatus/list`, `mcpServer/resource/read`, `mcpServer/tool/call` | No typed descriptors yet. Server-initiated MCP elicitation is tracked under server requests. |
| Windows sandbox setup | deferred | `windowsSandbox/setupStart` | No typed descriptor yet. |
| Feedback upload | deferred | `feedback/upload` | No typed descriptor yet. |
| Config read and write | deferred | `config/read`, `config/value/write`, `config/batchWrite`, `configRequirements/read` | No typed descriptors yet. Managed-policy models should be added before broad config APIs. |
| External agent migration | deferred | `externalAgentConfig/detect`, `externalAgentConfig/import` | No typed descriptors yet. |
| Remote control | experimental | `remoteControl/enable`, `remoteControl/disable`, `remoteControl/status/read`, `remoteControl/pairing/start`, `remoteControl/pairing/status`, `remoteControl/client/list`, `remoteControl/client/revoke` | Deferred. Remote-control methods require explicit experimental opt-in and trust-boundary documentation. |
| Tool user-input utility | experimental | `tool/requestUserInput` | Deferred. This is distinct from the server-initiated `item/tool/requestUserInput` flow. |
| Account and auth | deferred | `account/read`, `account/login/start`, `account/login/cancel`, `account/logout`, `account/rateLimits/read`, `account/usage/read`, `account/sendAddCreditsNudgeEmail` | No typed descriptors yet. Future auth models must avoid logging credentials, auth URLs, account identifiers, and tokens. |

## Notification Groups

| Group | Status | Upstream notifications | CoKit coverage |
| --- | --- | --- | --- |
| Thread lifecycle | partial | `thread/started`, `thread/archived`, `thread/unarchived`, `thread/closed`, `thread/deleted`, `thread/name/updated`, `thread/status/changed`, `thread/tokenUsage/updated` | `CodexNotification.ThreadStarted` is typed. Other thread notifications currently map to `CodexNotification.Unknown`. |
| Thread goal and settings | experimental | `thread/goal/updated`, `thread/goal/cleared`, `thread/settings/updated` | Deferred. Settings updates are experimental upstream. |
| Turn lifecycle | deferred | `turn/started`, `turn/completed`, `turn/diff/updated`, `turn/plan/updated`, `turn/moderationMetadata` | No typed notifications yet. |
| Model routing and verification | deferred | `model/rerouted`, `model/verification` | No typed notifications yet. |
| Item lifecycle | deferred | `item/started`, `item/completed` | No typed item notifications yet. |
| Temporary auto-approval review | experimental | `item/autoApprovalReview/started`, `item/autoApprovalReview/completed` | Deferred. Upstream marks the shape unstable. |
| Agent message streaming | deferred | `item/agentMessage/delta` | No typed notification yet. |
| Plan streaming | experimental | `item/plan/delta` | Deferred. Upstream marks plan deltas experimental. |
| Reasoning streaming | deferred | `item/reasoning/summaryTextDelta`, `item/reasoning/summaryPartAdded`, `item/reasoning/textDelta` | No typed notifications yet. |
| Command execution item streaming | deferred | `item/commandExecution/outputDelta` | No typed notification yet. |
| File change item streaming | deferred | `item/fileChange/patchUpdated`, `item/fileChange/outputDelta` | No typed notification yet. File-change approvals are deny-by-default as server requests. |
| Command execution output | deferred | `command/exec/outputDelta` | No typed notification yet. |
| Process output and exit | experimental | `process/outputDelta`, `process/exited` | Deferred. |
| Filesystem watch | deferred | `fs/changed` | No typed notification yet. |
| Fuzzy file search | experimental | `fuzzyFileSearch/sessionUpdated`, `fuzzyFileSearch/sessionCompleted` | Deferred. |
| Thread realtime | experimental | `thread/realtime/started`, `thread/realtime/sdp`, `thread/realtime/itemAdded`, `thread/realtime/transcript/delta`, `thread/realtime/transcript/done`, `thread/realtime/outputAudio/delta`, `thread/realtime/error`, `thread/realtime/closed` | Deferred. |
| Windows sandbox setup | deferred | `windowsSandbox/setupCompleted` | No typed notification yet. |
| MCP startup and OAuth | deferred | `mcpServer/startupStatus/updated`, `mcpServer/oauthLogin/completed` | No typed notifications yet. |
| Skill and app catalog changes | deferred | `skills/changed`, `app/list/updated` | No typed notifications yet. |
| Account and auth | deferred | `account/login/completed`, `account/updated`, `account/rateLimits/updated` | No typed notifications yet. |
| Remote-control status | experimental | `remoteControl/status/changed` | Deferred. |
| External agent migration | deferred | `externalAgentConfig/import/completed` | No typed notification yet. |
| Server-request lifecycle | deferred | `serverRequest/resolved` | No typed notification yet. Approval handlers rely on request handling, not a typed resolved event. |
| Warnings and errors | deferred | `configWarning`, `warning`, `error` | No typed notifications yet. |

## Server-Request Groups

| Group | Status | Upstream server requests | CoKit coverage |
| --- | --- | --- | --- |
| Command execution approval | partial | `item/commandExecution/requestApproval` | Typed `CodexServerRequest.CommandApproval` and approval handlers exist. Some newer upstream decisions and experimental additional-permission payloads are not fully modeled. Without a handler, CoKit declines by default. |
| File change approval | partial | `item/fileChange/requestApproval` | No typed request model yet. Without a handler, CoKit declines by default. |
| Permission grant approval | partial | `item/permissions/requestApproval` | No typed request model yet. Without a handler, CoKit declines by default. |
| Dynamic tool call | experimental | `item/tool/call` | Deferred. Without a handler, CoKit declines by default. |
| Tool user-input prompt | experimental | `item/tool/requestUserInput` | Deferred. Without a handler, CoKit cancels by default. |
| MCP elicitation | deferred | `mcpServer/elicitation/request` | No typed request model yet. Without a handler, CoKit declines by default. |
| Attestation generation | partial | `attestation/generate` | No typed request model yet. Without a handler, CoKit returns an unsupported status by default. |

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
- `turn/start`
- `turn/steer`
- `turn/interrupt`

Current typed notification:

- `thread/started`

Current typed server request:

- `item/commandExecution/requestApproval`
