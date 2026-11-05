package com.example.aurorakmpdemo.platform

import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import io.ktor.client.engine.HttpClientEngineFactory

data class PlatformDiagnostics(
    val platform: String,
    val details: List<String>,
)

expect fun platformName(): String

expect fun httpClientEngineFactory(): HttpClientEngineFactory<*>

/** Creates the single platform-configured Room database for the Koin graph. */
expect fun createPostsDatabase(): PostsRoomDatabase

/** Legacy adapter retained for the existing render diagnostic screen. */
expect fun createPostsStorage(): PostsStorage

expect fun shouldUseAuroraRenderSmokeTest(): Boolean

expect fun configuredDrawableExperiment(): String?

expect fun currentProcessId(): Long

expect fun diagnosticLogPath(): String?

expect fun appendPlatformDiagnosticLog(message: String)

/** A single overwritten file containing only the latest "Library checks" run. */
expect fun libraryChecksLogPath(): String?

expect fun overwriteLibraryChecksLog(message: String)

expect fun appendLibraryChecksLog(message: String)

expect fun shouldUseBundledDemoFonts(): Boolean

expect class PlatformDiagnosticsProvider() {
    fun snapshot(): PlatformDiagnostics
}
