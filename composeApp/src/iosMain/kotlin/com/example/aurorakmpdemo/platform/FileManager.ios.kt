package com.example.aurorakmpdemo.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.posix.mkdir

private fun iosFileRoot(): String = "${NSHomeDirectory()}/Documents/aurora-kmp-demo".also { mkdir(it, 0x1EDu) }

actual fun fileManagerRoot(): String = iosFileRoot()

actual fun listFileManagerEntries(path: String): List<FileManagerEntry> =
    NSFileManager.defaultManager.contentsOfDirectoryAtPath(path, error = null)
        ?.map { name ->
            val itemName = name.toString()
            FileManagerEntry(itemName, "$path/$itemName", false, 0)
        }
        .orEmpty()

actual fun fileManagerParent(path: String): String? = path.substringBeforeLast('/', missingDelimiterValue = path)
    .takeIf { it.isNotBlank() && it != path }

actual fun createFileManagerDirectory(path: String, preferredName: String): Result<FileManagerEntry> = runCatching {
    var index = 0
    var name = preferredName
    while (NSFileManager.defaultManager.fileExistsAtPath("$path/$name")) {
        index += 1
        name = "$preferredName $index"
    }
    val itemPath = "$path/$name"
    check(NSFileManager.defaultManager.createDirectoryAtPath(itemPath, withIntermediateDirectories = false, attributes = null, error = null)) {
        "Не удалось создать каталог"
    }
    FileManagerEntry(name, itemPath, true, 0)
}

private fun unsupportedFileOperation(): Result<Nothing> = Result.failure(UnsupportedOperationException("Операция пока доступна на Aurora, Android и Desktop"))
actual fun createFileManagerFile(path: String, preferredName: String): Result<FileManagerEntry> = unsupportedFileOperation()
actual fun copyFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = unsupportedFileOperation()
actual fun moveFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = unsupportedFileOperation()
actual fun deleteFileManagerEntry(path: String): Result<Unit> = unsupportedFileOperation()
