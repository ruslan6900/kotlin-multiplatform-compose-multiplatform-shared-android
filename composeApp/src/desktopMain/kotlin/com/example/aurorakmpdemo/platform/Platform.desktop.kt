package com.example.aurorakmpdemo.platform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.io.path.createDirectories

actual fun platformName(): String = "Desktop JVM"

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Java

private val desktopRoomDirectory: Path by lazy {
    Path.of(System.getProperty("user.home"), ".aurora-kmp-demo")
}

private val desktopRoomDatabasePath: String by lazy {
    desktopRoomDirectory.resolve("posts-room.db").toString()
}

private object DesktopRoomStorageHolder {
    val storage: PostsStorage by lazy {
        desktopRoomDirectory.createDirectories()
        val database = Room
            .databaseBuilder<PostsRoomDatabase>(name = desktopRoomDatabasePath)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        RoomPostsStorage(database.postsDao())
    }
}

actual fun createPostsStorage(): PostsStorage = DesktopRoomStorageHolder.storage

actual fun shouldUseAuroraRenderSmokeTest(): Boolean = true

actual fun currentProcessId(): Long = ProcessHandle.current().pid()

actual fun diagnosticLogPath(): String? = null

actual fun appendPlatformDiagnosticLog(message: String) {
    println(message)
}

actual class PlatformDiagnosticsProvider actual constructor() {
    actual fun snapshot(): PlatformDiagnostics = PlatformDiagnostics(
        platform = platformName(),
        details = listOf(
            "Desktop Room database: $desktopRoomDatabasePath",
            "Aurora KInterop is not available on Desktop JVM.",
        ),
    )
}
