package com.example.aurorakmpdemo.platform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import com.example.aurorakmpdemo.db.postsMigration2To3
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.Dispatchers
import platform.posix.getpid
import platform.posix.mkdir

private val iosRoomDirectory: String by lazy {
    "${platform.Foundation.NSHomeDirectory()}/Library/Application Support/aurora-kmp-demo"
}

private val iosRoomDatabasePath: String by lazy { "$iosRoomDirectory/posts-room.db" }

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

private object IosRoomDatabaseHolder {
    val database: PostsRoomDatabase by lazy {
        ensureDirectory(iosRoomDirectory)
        val database = Room
            .databaseBuilder<PostsRoomDatabase>(name = iosRoomDatabasePath)
            .addMigrations(postsMigration2To3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
        database
    }
}

actual fun platformName(): String = "iOS"

actual fun shouldUseBundledDemoFonts(): Boolean = true

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin

actual fun createPostsDatabase(): PostsRoomDatabase = IosRoomDatabaseHolder.database

actual fun createPostsStorage(): PostsStorage = RoomPostsStorage(createPostsDatabase().postsDao())

actual fun shouldUseAuroraRenderSmokeTest(): Boolean = true

actual fun configuredDrawableExperiment(): String? = null

actual fun currentProcessId(): Long = getpid().toLong()

actual fun diagnosticLogPath(): String? = null

actual fun appendPlatformDiagnosticLog(message: String) {
    println(message)
}

actual fun libraryChecksLogPath(): String? = null
actual fun overwriteLibraryChecksLog(message: String) { println(message) }
actual fun appendLibraryChecksLog(message: String) { println(message) }

actual class PlatformDiagnosticsProvider actual constructor() {
    actual fun snapshot(): PlatformDiagnostics = PlatformDiagnostics(
        platform = platformName(),
        details = listOf(
            "iOS Room database: $iosRoomDatabasePath",
            "Aurora KInterop is not available on iOS.",
        ),
    )
}
