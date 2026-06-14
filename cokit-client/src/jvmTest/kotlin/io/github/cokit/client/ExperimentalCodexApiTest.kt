package io.github.cokit.client

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExperimentalCodexApiTest {
    @Test
    fun markerRequiresExplicitErrorLevelOptIn() {
        val source = loadDocument("cokit-client/src/commonMain/kotlin/io/github/cokit/client/ExperimentalCodexApi.kt")

        assertTrue(
            source.contains("@RequiresOptIn("),
            "ExperimentalCodexApi must be a Kotlin opt-in marker.",
        )
        assertTrue(
            source.contains("level = RequiresOptIn.Level.ERROR"),
            "ExperimentalCodexApi must reject accidental usage at compile time.",
        )
        assertTrue(
            source.contains("initialization capability opt-in"),
            "ExperimentalCodexApi must document the app-server initialization gate.",
        )
    }

    @OptIn(ExperimentalCodexApi::class)
    @Test
    fun sampleExperimentalDeclarationCompilesBehindExplicitOptIn() {
        assertEquals("sample-experimental-api", SampleExperimentalDeclaration.identifier)
    }

    private fun loadDocument(path: String): String {
        val start = File(System.getProperty("user.dir")).canonicalFile
        val document = generateSequence(start) { directory -> directory.parentFile }
            .map { directory -> File(directory, path) }
            .firstOrNull { file -> file.isFile }

        requireNotNull(document) {
            "Could not find $path from ${start.path} or its parent directories."
        }
        return document.readText()
    }
}

@ExperimentalCodexApi
private object SampleExperimentalDeclaration {
    const val identifier: String = "sample-experimental-api"
}
