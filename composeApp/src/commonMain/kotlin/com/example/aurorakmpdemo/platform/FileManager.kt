package com.example.aurorakmpdemo.platform

/** Файл из собственной доступной для записи области приложения. */
data class FileManagerEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
)

expect fun fileManagerRoot(): String

expect fun listFileManagerEntries(path: String): List<FileManagerEntry>

expect fun fileManagerParent(path: String): String?

/** Создаёт каталог внутри [path] и возвращает понятный UI результат. */
expect fun createFileManagerDirectory(path: String, preferredName: String): Result<FileManagerEntry>

expect fun createFileManagerFile(path: String, preferredName: String): Result<FileManagerEntry>

expect fun copyFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry>

expect fun moveFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry>

expect fun deleteFileManagerEntry(path: String): Result<Unit>
