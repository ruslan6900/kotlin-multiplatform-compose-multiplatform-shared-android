package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import platform.posix.DT_DIR
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.mkdir
import platform.posix.opendir
import platform.posix.readdir
import platform.posix.remove
import platform.posix.rename
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
private fun auroraFileRoot(): String = getenv("HOME")?.toKString() ?: "/"

actual fun fileManagerRoot(): String = auroraFileRoot()

@OptIn(ExperimentalForeignApi::class)
actual fun listFileManagerEntries(path: String): List<FileManagerEntry> {
    val directory = opendir(path) ?: return emptyList()
    return try {
        buildList {
            while (true) {
                val entry = readdir(directory) ?: break
                val name = entry.pointed.d_name.toKString()
                if (name == "." || name == "..") continue
                val isDirectory = entry.pointed.d_type.toInt() == DT_DIR
                add(FileManagerEntry(name, "$path/$name", isDirectory, 0))
            }
        }.sortedWith(compareByDescending<FileManagerEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    } finally {
        closedir(directory)
    }
}

actual fun fileManagerParent(path: String): String? = when (path) {
    "/" -> null
    else -> path.substringBeforeLast('/', missingDelimiterValue = path).let { if (it.isBlank()) "/" else it }
        .takeIf { it != path }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createFileManagerDirectory(path: String, preferredName: String): Result<FileManagerEntry> = runCatching {
    var index = 0
    var name = preferredName
    while (listFileManagerEntries(path).any { it.name == name }) {
        index += 1
        name = "$preferredName $index"
    }
    val itemPath = "$path/$name"
    check(mkdir(itemPath, 0x1EDu) == 0) { "Не удалось создать каталог" }
    FileManagerEntry(name, itemPath, true, 0)
}

@OptIn(ExperimentalForeignApi::class)
private fun uniquePath(directory: String, name: String): String {
    var index = 0
    var candidate = "$directory/$name"
    while (listFileManagerEntries(directory).any { it.path == candidate }) candidate = "$directory/$name ${++index}"
    return candidate
}

@OptIn(ExperimentalForeignApi::class)
actual fun createFileManagerFile(path: String, preferredName: String): Result<FileManagerEntry> = runCatching {
    val itemPath = uniquePath(path, preferredName)
    fclose(fopen(itemPath, "wb") ?: error("Не удалось создать файл"))
    FileManagerEntry(itemPath.substringAfterLast('/'), itemPath, false, 0)
}

@OptIn(ExperimentalForeignApi::class)
private fun copyFile(source: String, destination: String) {
    val input = fopen(source, "rb") ?: error("Не удалось открыть исходный файл")
    val output = fopen(destination, "wb") ?: run { fclose(input); error("Не удалось создать файл назначения") }
    try { memScoped {
        val buffer = allocArray<ByteVar>(8192)
        while (true) {
            val read = fread(buffer, 1u, 8192u, input)
            if (read == 0uL) break
            check(fwrite(buffer, 1u, read, output) == read) { "Ошибка записи" }
        }
    }} finally { fclose(input); fclose(output) }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyEntry(source: FileManagerEntry, destination: String) {
    if (!source.isDirectory) return copyFile(source.path, destination)
    check(mkdir(destination, 0x1EDu) == 0) { "Не удалось создать каталог назначения" }
    listFileManagerEntries(source.path).forEach { copyEntry(it, "$destination/${it.name}") }
}

@OptIn(ExperimentalForeignApi::class)
private fun entryAt(path: String): FileManagerEntry =
    listFileManagerEntries(fileManagerParent(path) ?: "/").firstOrNull { it.path == path } ?: error("Источник не найден")

@OptIn(ExperimentalForeignApi::class)
actual fun copyFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = runCatching {
    val source = entryAt(sourcePath); val destination = uniquePath(destinationDirectory, source.name)
    copyEntry(source, destination); source.copy(path = destination)
}

@OptIn(ExperimentalForeignApi::class)
actual fun moveFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = runCatching {
    val source = entryAt(sourcePath); val destination = uniquePath(destinationDirectory, source.name)
    check(rename(sourcePath, destination) == 0) { "Не удалось переместить" }; source.copy(path = destination)
}

@OptIn(ExperimentalForeignApi::class)
private fun deleteEntry(entry: FileManagerEntry) {
    if (entry.isDirectory) listFileManagerEntries(entry.path).forEach(::deleteEntry)
    check(remove(entry.path) == 0) { "Не удалось удалить ${entry.name}" }
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileManagerEntry(path: String): Result<Unit> = runCatching { deleteEntry(entryAt(path)) }
