# Coverage

CoKit uses Kover for JVM test coverage reporting.

Kover currently collects coverage from JVM tests. In Kotlin Multiplatform
modules, common source sets are covered through the JVM target, while
non-JVM targets are outside the current coverage scope.

## Commands

Run aggregate coverage for all CoKit modules:

```bash
./gradlew coverage
```

The `coverage` task runs the root `test` task and generates Kover HTML, XML,
and console reports.

You can also run Kover report tasks directly:

```bash
./gradlew koverHtmlReport
./gradlew koverXmlReport
./gradlew koverLog
```

## Reports

- HTML: `build/reports/kover/html/index.html`
- XML: `build/reports/kover/report.xml`
- Console summary: output from `./gradlew koverLog`

## Notes

- `./gradlew check` remains the main verification gate.
- `./gradlew test` is a root convenience task that runs all module `jvmTest`
  tasks.
- Coverage thresholds are intentionally not enforced yet because CoKit is in
  early development.
