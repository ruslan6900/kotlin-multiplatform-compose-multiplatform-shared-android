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

/** Создаёт единственную настроенную для платформы Room БД для графа Koin. */
expect fun createPostsDatabase(): PostsRoomDatabase

/** Устаревший адаптер, оставленный для существующего экрана диагностики рендера. */
expect fun createPostsStorage(): PostsStorage

expect fun shouldUseAuroraRenderSmokeTest(): Boolean

expect fun configuredDrawableExperiment(): String?

expect fun currentProcessId(): Long

expect fun diagnosticLogPath(): String?

expect fun appendPlatformDiagnosticLog(message: String)

/** Единственный перезаписываемый файл, содержащий только последний запуск «Проверки библиотек». */
expect fun libraryChecksLogPath(): String?

expect fun overwriteLibraryChecksLog(message: String)

expect fun appendLibraryChecksLog(message: String)

expect fun shouldUseBundledDemoFonts(): Boolean

expect class PlatformDiagnosticsProvider() {
    fun snapshot(): PlatformDiagnostics
}
