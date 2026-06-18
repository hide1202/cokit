import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CokitPublicApiChecksTest {
    @Test
    fun reportsPublicJsonAndJsonRpcEnvelopeTypes() {
        val file = sourceFile(
            "BadModels.kt",
            """
            package io.github.cokit.client

            import io.github.cokit.protocol.JsonRpcResponse
            import kotlinx.serialization.json.JsonElement

            data class BadJsonModel(
                val payload: JsonElement,
            )

            class BadEnvelopeModel(
                val response: JsonRpcResponse,
            )

            fun badParameter(payload: kotlinx.serialization.json.JsonObject) = Unit

            fun badReturn(): JsonElement = error("fixture")
            """.trimIndent(),
        )

        val violations = CokitPublicApiChecks.findViolations(listOf(file))

        assertEquals(
            listOf(
                "BadModels.kt:7 uses JsonElement",
                "BadModels.kt:11 uses JsonRpcResponse",
                "BadModels.kt:14 uses JsonObject",
                "BadModels.kt:16 uses JsonElement",
            ),
            violations.map { violation ->
                "${violation.relativePath}:${violation.lineNumber} uses ${violation.typeName}"
            },
        )
    }

    @Test
    fun allowsCompatibilityPayloadsAndInternalSerializerCode() {
        val file = sourceFile(
            "GoodModels.kt",
            """
            package io.github.cokit.client

            import kotlinx.serialization.DeserializationStrategy
            import kotlinx.serialization.json.JsonContentPolymorphicSerializer
            import kotlinx.serialization.json.JsonElement

            data class GoodModel(
                val payload: CodexJsonPayload,
            )

            internal fun JsonElement.toPayload(): CodexJsonPayload = error("fixture")

            object GoodSerializer : JsonContentPolymorphicSerializer<GoodModel>(GoodModel::class) {
                override fun selectDeserializer(
                    element: JsonElement,
                ): DeserializationStrategy<GoodModel> = error("fixture")
            }
            """.trimIndent(),
        )

        assertTrue(CokitPublicApiChecks.findViolations(listOf(file)).isEmpty())
    }

    private fun sourceFile(name: String, text: String): File {
        val file = kotlin.io.path.createTempDirectory("cokit-api-checks").resolve(name).toFile()
        file.writeText(text)
        return file
    }
}
