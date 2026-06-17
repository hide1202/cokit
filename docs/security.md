# Security

CoKit is a client library for an app-server that can request filesystem access,
command execution, permission changes, and other sensitive operations. Consumers
must treat app-server requests as security-sensitive.

## Approval Defaults

When no handler is registered, CoKit declines approval-like server requests by
default. This prevents a client from silently allowing command execution, file
changes, permission grants, dynamic tool calls, or elicitation flows.

Applications should register explicit handlers only when they can show the
request to a user, apply a policy, or otherwise make an intentional decision.
Prefer typed handlers such as command, file-change, and permission approval
handlers over raw method-string compatibility hooks.

Command execution approval requests are deny-by-default. Without a registered
command approval handler, CoKit responds with `{"decision":"decline"}` without
attempting to approve or execute the command. With a typed command approval
handler, CoKit decodes the upstream params before invoking application code.
Malformed params return JSON-RPC `-32602` and do not call the handler. Handler
exceptions return JSON-RPC `-32000` with a generic failure message so exception
text is not sent back to app-server.

File-change approval requests are deny-by-default. Without a registered
file-change approval handler, CoKit responds with `{"decision":"decline"}`
without approving the patch or granting write access. With a typed file-change
approval handler, CoKit exposes the request ids, optional reason, and optional
host `grantRoot` as typed values before encoding the handler's explicit
decision. Handler failures use the same generic JSON-RPC error behavior as
command approvals.

Permission approval requests are deny-by-default. Without a registered
permission approval handler, CoKit responds with `{"permissions": {}}`, which
grants no requested filesystem or network permissions. With a typed permission
approval handler, CoKit exposes the request ids, optional environment id,
request directory, reason, and requested permission profile as typed values.
Handler responses must return only the granted subset of requested permissions.
`PermissionGrantScope.Session` encodes `scope: "session"` for the app-server
session approval cache; `PermissionGrantScope.Turn` keeps the grant scoped to
the current turn. Malformed params and handler failures use the same JSON-RPC
error behavior as command approvals.

User-input prompt requests are cancel-by-default. Without a registered typed
user-input request handler, CoKit responds with `{"decision":"cancel"}` and
never fabricates prompt answers. With a typed user-input request handler, CoKit
exposes the request ids, prompt headers, question text, choice options,
free-form flags, secret flags, and optional auto-resolution timeout before
invoking application code. Handler responses must return either typed cancel or
explicit answer maps keyed by question id. Malformed params and handler failures
use the same JSON-RPC error behavior as command approvals.

MCP elicitation requests are decline-by-default. Without a registered typed MCP
elicitation handler, CoKit responds with `{"action":"decline","content":null}`
and never supplies structured form content or opens URL flows on behalf of an
application. With a typed MCP elicitation handler, CoKit exposes the server
name, thread id, optional turn id, message, and either a typed form schema or a
URL elicitation target before invoking application code. Application policy owns
all user decisions and should treat MCP-provided text, URLs, and requested form
fields as untrusted input. Handler responses must return explicit accept,
decline, or cancel actions. Malformed params and handler failures use the same
JSON-RPC error behavior as command approvals.

## Host Semantics

Paths, commands, process APIs, and filesystem APIs refer to the app-server host.
That host may be different from the Kotlin client process host in remote-control
or proxy scenarios.

## Secrets

CoKit should not log secrets by default. Consumers should avoid logging:

- API keys.
- Access tokens.
- Auth URLs.
- Attestation tokens.
- Private file paths when logs may leave the machine.

## Sandbox And Permissions

Sandbox and permission profile values are passed through to app-server. CoKit
does not weaken or reinterpret upstream sandbox semantics. Application-level
policy should make approval decisions explicit and visible.

## Experimental Transports

The WebSocket transport is experimental. Use stdio for the default local
integration path unless an application has a specific reason to opt into
WebSocket behavior.
