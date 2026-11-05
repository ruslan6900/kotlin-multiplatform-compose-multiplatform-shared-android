package com.example.aurorakmpdemo.platform

import java.io.File

private fun desktopFileRoot(): File = File(System.getProperty("user.home"), ".aurora-kmp-demo").apply { mkdirs() }

actual fun fileManagerRoot(): String = desktopFileRoot().absolutePath

actual fun listFileManagerEntries(path: String): List<FileManagerEntry> = File(path)
    .listFiles()
    ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    ?.map { file -> FileManagerEntry(file.name, file.absolutePath, file.isDirectory, file.length()) }
    .orEmpty()

actual fun fileManagerParent(path: String): String? = File(path).parentFile?.absolutePath

actual fun createFileManagerDirectory(path: String, preferredName: String): Result<FileManagerEntry> = runCatching {
    val parent = File(path).also { check(it.isDirectory) { "Папка недоступна" } }
    var index = 0
    var candidate: File
    do {
        candidate = File(parent, if (index == 0) preferredName else "$preferredName $index")
        index++
    } while (candidate.exists())
    check(candidate.mkdir()) { "Не удалось создать каталог" }
    FileManagerEntry(candidate.name, candidate.absolutePath, true, 0)
}

private fun File.asEntry() = FileManagerEntry(name, absolutePath, isDirectory, length())
private fun uniqueFile(parent: File, name: String): File {
    var index = 0; var result: File
    do { result = File(parent, if (index++ == 0) name else "$name $index") } while (result.exists())
    return result
}
actual fun createFileManagerFile(path: String, preferredName: String): Result<FileManagerEntry> = runCatching { uniqueFile(File(path), preferredName).also { check(it.createNewFile()) }.asEntry() }
actual fun copyFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = runCatching {
    val source = File(sourcePath); val target = uniqueFile(File(destinationDirectory), source.name)
    if (source.isDirectory) source.copyRecursively(target) else source.copyTo(target); target.asEntry()
}
actual fun moveFileManagerEntry(sourcePath: String, destinationDirectory: String): Result<FileManagerEntry> = runCatching {
    val source = File(sourcePath); val target = uniqueFile(File(destinationDirectory), source.name); check(source.renameTo(target)); target.asEntry()
}
actual fun deleteFileManagerEntry(path: String): Result<Unit> = runCatching { check(File(path).deleteRecursively()) }
