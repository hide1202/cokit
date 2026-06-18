package io.github.cokit.client.filesystem

import io.github.cokit.client.CodexHostPath
import kotlinx.serialization.Serializable

@Serializable
data class FilesystemReadFileParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemReadFileResult(
    val dataBase64: String,
)

@Serializable
data class FilesystemGetMetadataParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemGetMetadataResult(
    val isDirectory: Boolean,
    val isFile: Boolean,
    val isSymlink: Boolean,
    val createdAtMs: Long,
    val modifiedAtMs: Long,
)

@Serializable
data class FilesystemReadDirectoryParams(
    val path: CodexHostPath,
)

@Serializable
data class FilesystemReadDirectoryResult(
    val entries: List<FilesystemDirectoryEntry> = emptyList(),
)

@Serializable
data class FilesystemDirectoryEntry(
    val fileName: String,
    val isDirectory: Boolean,
    val isFile: Boolean,
)
