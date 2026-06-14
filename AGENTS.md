# CoKit Agent Guide

CoKit is planned as a Kotlin Multiplatform client library for `codex app-server`.
It should prioritize a small public API, strict protocol fidelity, and clear module
boundaries over fast one-file implementations.

## Language

- Use English for all documentation, comments, commit messages, public API names,
  test names, sample text, and issue or pull request templates in this repository.
- Avoid adding Korean text to source files, generated artifacts, examples, or docs.

## Project Principles

- Treat the upstream `codex app-server` README and generated schemas as the source
  of truth for protocol behavior.
- Keep stable and experimental protocol surfaces explicit. Experimental APIs must
  require an opt-in at initialization and in CoKit APIs.
- Prefer typed Kotlin APIs backed by `kotlinx.serialization`, while preserving
  unknown JSON payloads for forward compatibility.
- Keep transports separate from protocol and client logic.
- Do not build UI concerns into the library. The library may expose state and
  events that make UI implementation straightforward, but it should not render UI.

## Expected Modules

- `cokit-protocol`: JSON-RPC envelopes, generated protocol DTOs, stable and
  experimental type surfaces, and protocol errors.
- `cokit-rpc`: request correlation, notification dispatch, server-initiated
  request handling, retries, and lifecycle coordination.
- `cokit-client`: high-level thread, turn, auth, model, config, skills, apps,
  filesystem, and process APIs.
- `cokit-transport-stdio`: JVM stdio transport for `codex app-server --stdio`
  and `codex app-server proxy`.
- `cokit-transport-websocket`: opt-in WebSocket transport for experimental and
  local-control scenarios.
- `cokit-testing`: fake transports, transcript fixtures, and protocol test helpers.

## Engineering Workflow

- Start with tests for protocol parsing, request correlation, lifecycle handshake,
  streaming notifications, and approval flows.
- Validate generated protocol code against schema fixtures whenever the upstream
  Codex schema changes.
- Keep public APIs small and documented. Internal APIs may be split aggressively
  when doing so makes request, transport, or protocol behavior easier to reason
  about.
- Do not commit generated schema artifacts without recording the Codex version or
  command that produced them.

## Security Review

- Treat this repository as public by default. Before publishing, releasing, or
  committing generated artifacts, scan tracked files for credentials, tokens,
  private keys, auth URLs, private account data, private local paths, and machine
  specific configuration.
- Keep `.draft/`, `.env*`, `local.properties`, IDE files, build outputs, and logs
  out of Git. If a local-only file must be inspected during a review, verify that
  it is ignored and not listed by `git ls-files`.
- Use `git ls-files` as the source of truth for public exposure reviews. Include
  source, tests, fixtures, docs, Gradle wrapper files, generated metadata, issue
  templates, and sample code in the review.
- Re-run a security pass whenever code changes JSON-RPC parsing, request
  correlation, notification buffering, server-initiated requests, approval
  defaults, process launch, environment handling, filesystem/path parameters,
  sandbox or permission pass-through, generated schema handling, or experimental
  transports.
- Approval-like app-server requests must remain deny-by-default unless an
  application registers an explicit handler. New handlers must avoid accepting
  command execution, file changes, permission grants, tool calls, elicitation,
  attestation, or user input by default.
- Do not add unbounded parsing, buffering, request queues, or retry loops without
  tests and documentation that cover malformed, oversized, missing-response, and
  high-rate app-server behavior.
- Stdio process launch must avoid shell interpolation. Changes that touch
  command resolution, `PATH`, inherited environment variables, stderr handling,
  or process shutdown require a security review.
- Experimental protocol and transport surfaces must keep explicit opt-in gates
  and must document trust boundaries, authentication assumptions, and local-only
  limitations before becoming usable.
- Generated protocol artifacts must record enough provenance for public audit:
  generation command, Codex version or upstream commit when available, stable vs
  experimental mode, and any schema digest or fixture source used for validation.
- Gradle wrapper and dependency changes should be reviewed for supply-chain
  hardening. Prefer checksum-pinned wrapper distributions and avoid unexplained
  repository or plugin additions.
- Public examples, tests, fixtures, and docs must use placeholders such as
  `/path/to/project` and local loopback addresses only when they are clearly test
  values. Do not include real user names, home directories, workspace paths,
  tokens, auth URLs, account identifiers, or private service endpoints.
- Suggested recurring checks:
  - `git status --short`
  - `git ls-files`
  - `git grep -n -I -E '(api[_-]?key|secret|token|password|passwd|credential|private[_-]?key|authorization|bearer|sk-[A-Za-z0-9]|xox[baprs]-|ghp_|github_pat_|BEGIN (RSA|OPENSSH|EC|PRIVATE) KEY|auth[_-]?url)' -- .`
  - `git grep -n -I -E '(/Users/|/home/|C:\\Users\\|localhost:[0-9]+|127\.0\.0\.1:[0-9]+)' -- .`
  - `./gradlew check`
- When a security assumption changes, update `docs/security.md` and any protocol
  compatibility documentation before or alongside the implementation change.

## Documentation

- Draft planning documents live under `.draft/docs/` until they are ready to be
  published. The `.draft/` directory is intentionally ignored by Git.
- Public documentation lives under `docs/` when it is ready to be versioned.
- After development work, update affected documentation, README examples,
  protocol compatibility notes, and sample usage in the same change whenever
  public behavior, supported API coverage, or developer workflow changes.
- When protocol assumptions change, update both the specification and the
  implementation plan before changing library code.
- Public documentation should distinguish supported, experimental, and deferred
  surfaces.
