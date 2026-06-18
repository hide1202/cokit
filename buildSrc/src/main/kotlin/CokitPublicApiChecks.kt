import java.io.File

object CokitPublicApiChecks {
    data class Violation(
        val relativePath: String,
        val lineNumber: Int,
        val typeName: String,
        val line: String,
    )

    private val forbiddenPrimaryApiTypes = setOf(
        "JsonElement",
        "JsonObject",
        "JsonArray",
        "JsonPrimitive",
        "JsonRpcMessage",
        "JsonRpcRequest",
        "JsonRpcResponse",
        "JsonRpcNotification",
        "JsonRpcErrorObject",
    )

    private val typePattern = Regex(
        "(?:^|[\\s<(:,])(?:[A-Za-z_][A-Za-z0-9_]*\\.)*(${forbiddenPrimaryApiTypes.joinToString("|")})(?:[?\\s>,).]|$)",
    )

    fun findViolations(files: Iterable<File>, rootDir: File? = null): List<Violation> {
        return files
            .filter { file -> file.isFile && file.extension == "kt" && file.name != "CodexJsonPayload.kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val scannedLine = line.substringBefore("//").trim()
                    val typeName = forbiddenPublicType(scannedLine) ?: return@mapIndexedNotNull null
                    Violation(
                        relativePath = file.relativeDisplayPath(rootDir),
                        lineNumber = index + 1,
                        typeName = typeName,
                        line = scannedLine,
                    )
                }
            }
    }

    private fun forbiddenPublicType(line: String): String? {
        if (line.isBlank() || line.startsWith("package ") || line.startsWith("import ")) return null
        if (line.startsWith("@") || line.startsWith("*")) return null
        if (line.contains("internal ") || line.contains("private ")) return null
        if (!line.hasPublicSignatureShape()) return null
        return typePattern.find(line.substringBefore("="))?.groupValues?.get(1)
    }

    private fun String.hasPublicSignatureShape(): Boolean {
        return contains(Regex("\\b(val|var)\\s+[A-Za-z_][A-Za-z0-9_]*\\s*:")) ||
            contains(Regex("\\bfun\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\([^)]*:\\s*[A-Za-z0-9_.]+")) ||
            contains(Regex("\\bfun\\s+[A-Za-z_][A-Za-z0-9_]*\\s*\\([^)]*\\)\\s*:\\s*"))
    }

    private fun File.relativeDisplayPath(rootDir: File?): String {
        return if (rootDir == null) {
            name
        } else {
            relativeTo(rootDir).invariantSeparatorsPath
        }
    }
}
