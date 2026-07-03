package com.example.aurorakmpdemo.platform

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomPostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import android.os.Process
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers

actual fun platformName(): String = "Android"

actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = OkHttp

private object AndroidRoomStorageHolder {
    val storage: PostsStorage by lazy {
        val context = AndroidAppContext.require()
        val databasePath = context.getDatabasePath("posts-room.db").absolutePath
        val database = Room
            .databaseBuilder<PostsRoomDatabase>(
                context = context,
                name = databasePath,
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        RoomPostsStorage(database.postsDao())
    }
}

actual fun createPostsStorage(): PostsStorage = AndroidRoomStorageHolder.storage

actual fun shouldUseAuroraRenderSmokeTest(): Boolean = true

actual fun configuredDrawableExperiment(): String? = null

actual fun currentProcessId(): Long = Process.myPid().toLong()

actual fun diagnosticLogPath(): String? = null

actual fun appendPlatformDiagnosticLog(message: String) {
    println(message)
}

actual class PlatformDiagnosticsProvider actual constructor() {
    actual fun snapshot(): PlatformDiagnostics = PlatformDiagnostics(
        platform = platformName(),
        details = listOf("Aurora KInterop is not available on Android."),
    )
}
