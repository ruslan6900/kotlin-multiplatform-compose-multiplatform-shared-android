package com.example.aurorakmpdemo.platform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import com.example.aurorakmpdemo.db.postsMigration2To3
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.curl.Curl
import kotlinx.coroutines.Dispatchers
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fprintf
import platform.posix.getenv
import platform.posix.getpid
import platform.posix.mkdir

@OptIn(ExperimentalForeignApi::class)
private var launchCount = 0
@OptIn(ExperimentalForeignApi::class)
private val auroraLogPath: String by lazy {
    val home = getenv("HOME")?.toKString() ?: "."
    "$home/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log"
}
@OptIn(ExperimentalForeignApi::class)
private val auroraLibraryChecksLogPath: String by lazy {
    val home = getenv("HOME")?.toKString() ?: "."
    "$home/.local/share/com.example/aurorakmpdemo/library-checks.log"
}
@OptIn(ExperimentalForeignApi::class)
private val auroraRoomDirectory: String by lazy {
    val home = getenv("HOME")?.toKString() ?: "."
    "$home/Documents/aurora-kmp-demo"
}
private val auroraRoomDatabasePath: String by lazy { "$auroraRoomDirectory/posts-room.db" }

actual fun platformName(): String = "Aurora OS"

actual fun shouldUseBundledDemoFonts(): Boolean = true

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Curl

@OptIn(ExperimentalForeignApi::class)
private fun ensureDirectory(path: String) {
    if (path.isBlank() || path == "/") return
    val normalized = if (path.endsWith('/')) path.dropLast(1) else path
    val parts = normalized.split('/').filter { it.isNotBlank() }
    var current = if (normalized.startsWith('/')) "/" else ""
    parts.forEach { part ->
        current = when {
            current.isEmpty() -> part
            current == "/" -> "/$part"
            else -> "$current/$part"
        }
        mkdir(current, 0x1EDu)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ensureAuroraLogDirectory() {
    ensureDirectory(auroraLogPath.substringBeforeLast('/', missingDelimiterValue = "."))
}

@OptIn(ExperimentalForeignApi::class)
private object AuroraRoomDatabaseHolder {
    val database: PostsRoomDatabase by lazy {
        ensureDirectory(auroraRoomDirectory)
        appendPlatformDiagnosticLog("[AuroraRoomStorageHolder] databaseDir=$auroraRoomDirectory")
        appendPlatformDiagnosticLog("[AuroraRoomStorageHolder] databasePath=$auroraRoomDatabasePath")
        val database = Room
            .databaseBuilder<PostsRoomDatabase>(name = auroraRoomDatabasePath)
            .addMigrations(postsMigration2To3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        database
    }
}

actual fun createPostsDatabase(): PostsRoomDatabase = AuroraRoomDatabaseHolder.database

actual fun createPostsStorage(): PostsStorage = RoomPostsStorage(createPostsDatabase().postsDao())

actual fun shouldUseAuroraRenderSmokeTest(): Boolean = true

@OptIn(ExperimentalForeignApi::class)
actual fun configuredDrawableExperiment(): String? = getenv("AURORA_DRAWABLE_EXPERIMENT")?.toKString()

@OptIn(ExperimentalForeignApi::class)
actual fun currentProcessId(): Long = getpid().toLong()

actual fun diagnosticLogPath(): String? = auroraLogPath

@OptIn(ExperimentalForeignApi::class)
actual fun appendPlatformDiagnosticLog(message: String) {
    ensureAuroraLogDirectory()
    val file = fopen(auroraLogPath, "a") ?: return
    try {
        fprintf(file, "%s\n", message)
    } finally {
        fclose(file)
    }
}

actual fun libraryChecksLogPath(): String? = auroraLibraryChecksLogPath

@OptIn(ExperimentalForeignApi::class)
actual fun overwriteLibraryChecksLog(message: String) {
    ensureAuroraLogDirectory()
    val file = fopen(auroraLibraryChecksLogPath, "w") ?: return
    try { fprintf(file, "%s\n", message) } finally { fclose(file) }
}

@OptIn(ExperimentalForeignApi::class)
actual fun appendLibraryChecksLog(message: String) {
    ensureAuroraLogDirectory()
    val file = fopen(auroraLibraryChecksLogPath, "a") ?: return
    try { fprintf(file, "%s\n", message) } finally { fclose(file) }
}

actual class PlatformDiagnosticsProvider actual constructor() {
    actual fun snapshot(): PlatformDiagnostics {
        launchCount += 1

        return PlatformDiagnostics(
            platform = platformName(),
            details = listOf(
                "Aurora Room database: $auroraRoomDatabasePath",
                "Aurora UI log file: $auroraLogPath",
                "Aurora KInterop dependencies are disabled in this build due broken LFS artifacts in aurora-maven.",
                "Launches in current process: $launchCount",
            ),
        )
    }
}
