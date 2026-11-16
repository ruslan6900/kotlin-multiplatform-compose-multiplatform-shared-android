package com.example.aurorakmpdemo.platform

actual suspend fun sendDemoNotification(): Result<String> =
    Result.failure(UnsupportedOperationException("Эта проверка пока доступна только на Aurora"))

actual suspend fun runDemoTaskAction(action: String): Result<String> =
    Result.failure(UnsupportedOperationException("Планирование доступно только на Aurora"))
actual suspend fun readDemoTaskLog(): String = "Планирование доступно только на Aurora"
