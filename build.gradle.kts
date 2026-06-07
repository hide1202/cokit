plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover)
}

allprojects {
    group = "io.github.cokit"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}

dependencies {
    subprojects.forEach { subproject ->
        add("kover", project(subproject.path))
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs all JVM unit tests in CoKit modules."
    dependsOn(
        subprojects.map { project ->
            "${project.path}:jvmTest"
        },
    )
}

tasks.register("coverage") {
    group = "verification"
    description = "Runs tests and generates aggregate Kover coverage reports."
    dependsOn("test", "koverHtmlReport", "koverXmlReport", "koverLog")
}
