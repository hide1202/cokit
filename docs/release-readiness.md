# Release Readiness

Use this checklist before publishing a CoKit release candidate or tagging a
public preview. The goal is to prove that the typed API, protocol coverage,
generated-schema provenance, docs, and examples all describe the same surface.

## Required Checks

- Confirm the working tree is on `main`, up to date with `origin/main`, and
  clean before starting release validation.
- Run the full verification gate:

```bash
./gradlew check --stacktrace
```

- Run the real app-server smoke test on a machine with `codex` installed:

```bash
COKIT_CODEX_INTEGRATION=1 ./gradlew :cokit-client:jvmTest --stacktrace
```

- Run the sample CLI help path and verify that it still uses public CoKit APIs:

```bash
./gradlew :cokit-sample-cli:run --args="--help"
```

- Run `git diff --check` and confirm there are no whitespace errors.
- Run `git ls-files`, then run the credential and local-path exposure greps
  listed in `AGENTS.md`. Review every match; expected matches should be limited
  to documented security terms, placeholder protocol fields, or intentional
  loopback test values.
- Re-run the coverage guard tests when protocol descriptors changed, and review
  the generated coverage block in `docs/protocol-compatibility.md`.
- Verify schema provenance in
  `cokit-protocol/src/commonMain/resources/codex-schema-provenance.properties`
  whenever generated schema artifacts or protocol assumptions changed. The
  provenance should record the Codex version, upstream commit or equivalent
  source, generation command, mode, digest or fixture source, and timestamp.
- Confirm README, getting-started docs, and sample CLI examples use typed CoKit
  APIs rather than raw app-server method strings.
- Draft changelog notes covering public API changes, protocol coverage changes,
  experimental API changes, security behavior changes, migration notes, and any
  deferred upstream surfaces.

## Maven Central Publishing

CoKit publishes Maven Central artifacts through the Vanniktech Gradle Maven
Publish plugin, which Sonatype lists as a community Gradle option for the
Central Portal. The repository avoids a manual bundle upload release path so the
same Gradle tasks can run locally and from GitHub Actions.

The published library modules are:

- `cokit-protocol`
- `cokit-rpc`
- `cokit-client`
- `cokit-transport-stdio`
- `cokit-transport-websocket`
- `cokit-testing`

The `cokit-sample-cli` module is intentionally excluded from Maven Central
publications.

Before publishing, complete the external setup:

- Verify the `io.github.vupoint` namespace in the Central Portal.
- Generate a Central Portal user token for publishing.
- Generate a PGP key pair for artifact signing and distribute the public key to
  a supported key server.
- Add the following GitHub Actions secrets, preferably scoped to the
  `maven-central` environment:
  - `MAVEN_CENTRAL_USERNAME`
  - `MAVEN_CENTRAL_PASSWORD`
  - `SIGNING_IN_MEMORY_KEY`
  - `SIGNING_IN_MEMORY_KEY_ID`
  - `SIGNING_IN_MEMORY_KEY_PASSWORD`

Run the local pre-publish verification gate:

```bash
./gradlew checkMavenCentralPublishingConfiguration --stacktrace
./gradlew checkPomFileForJvmPublication checkPomFileForKotlinMultiplatformPublication --stacktrace
./gradlew check --stacktrace
```

For local release testing without publishing, inspect the available publish
tasks and generated publications:

```bash
./gradlew tasks --all
```

Actual Maven Central publication should run through
`.github/workflows/maven-central.yml`. It publishes only library modules and can
be triggered in two ways:

- Push a semantic release tag such as `0.1.0`; the workflow publishes version
  `0.1.0`.
- Run `workflow_dispatch` with an explicit semantic non-SNAPSHOT version.

The workflow runs `checkMavenCentralPublishingConfiguration` and `check` before
publishing. Its final command is:

```bash
./gradlew publishAndReleaseLibrariesToMavenCentral -PVERSION_NAME=<version> --no-configuration-cache --stacktrace
```

Do not run a publish task until the release candidate has passed the required
checks and the release notes or release issue records the validation results.

## Review Gates

- Public docs distinguish supported, experimental, deferred, and compatibility
  surfaces.
- Experimental APIs require explicit Kotlin opt-in and initialization capability
  opt-in where applicable.
- Approval-like server requests remain deny-by-default without application
  handlers.
- Process launch documentation avoids shell interpolation and private machine
  paths.
- No generated artifact is committed without current provenance.
- The release tag or published artifact should not be created until the checks
  above are recorded in release notes or the release issue.
