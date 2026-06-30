package com.example.aurorakmpdemo.data

import com.example.aurorakmpdemo.platform.httpClientEngineFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun buildHttpClient(): HttpClient = HttpClient(httpClientEngineFactory()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            },
        )
    }
}
