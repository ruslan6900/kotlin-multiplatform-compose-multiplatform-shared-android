package com.example.aurorakmpdemo.platform

import com.example.aurorakmpdemo.data.PostsStorage
import io.ktor.client.engine.HttpClientEngineFactory

data class PlatformDiagnostics(
    val platform: String,
    val details: List<String>,
)

expect fun platformName(): String

expect fun httpClientEngineFactory(): HttpClientEngineFactory<*>

expect fun createPostsStorage(): PostsStorage

expect fun shouldUseAuroraRenderSmokeTest(): Boolean

expect fun configuredDrawableExperiment(): String?

expect fun currentProcessId(): Long

expect fun diagnosticLogPath(): String?

expect fun appendPlatformDiagnosticLog(message: String)

expect class PlatformDiagnosticsProvider() {
    fun snapshot(): PlatformDiagnostics
}
