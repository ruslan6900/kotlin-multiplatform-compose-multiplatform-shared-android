package com.example.aurorakmpdemo.platform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import com.example.aurorakmpdemo.db.postsMigration2To3
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.io.path.createDirectories

actual fun platformName(): String = "Desktop JVM"

actual fun shouldUseBundledDemoFonts(): Boolean = true

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Java

private val desktopRoomDirectory: Path by lazy {
    Path.of(System.getProperty("user.home"), ".aurora-kmp-demo")
}

private val desktopRoomDatabasePath: String by lazy {
    desktopRoomDirectory.resolve("posts-room.db").toString()
}

private object DesktopRoomDatabaseHolder {
    val database: PostsRoomDatabase by lazy {
        desktopRoomDirectory.createDirectories()
        val database = Room
            .databaseBuilder<PostsRoomDatabase>(name = desktopRoomDatabasePath)
            .addMigrations(postsMigration2To3)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        database
    }
}

actual fun createPostsDatabase(): PostsRoomDatabase = DesktopRoomDatabaseHolder.database

actual fun createPostsStorage(): PostsStorage = RoomPostsStorage(createPostsDatabase().postsDao())

actual fun shouldUseAuroraRenderSmokeTest(): Boolean = true

actual fun configuredDrawableExperiment(): String? = null

actual fun currentProcessId(): Long = ProcessHandle.current().pid()

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
            "Desktop Room database: $desktopRoomDatabasePath",
            "Aurora KInterop is not available on Desktop JVM.",
        ),
    )
}
