package com.example.aurorakmpdemo.platform

/** A file exposed by the application's own writable storage area. */
data class FileManagerEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
)

expect fun fileManagerRoot(): String

expect fun listFileManagerEntries(path: String): List<FileManagerEntry>

expect fun fileManagerParent(path: String): String?

/** Creates a directory below [path] and returns a readable result for the UI. */
expect fun createFileManagerDirectory(path: String, preferredName: String): Result<FileManagerEntry>

expect fun createFileManagerFile(path: String, preferredName: String): Result<FileManagerEntry>

expect fun copyFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry>

expect fun moveFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry>

expect fun deleteFileManagerEntry(path: String): Result<Unit>
