package com.example.aurorakmpdemo.platform

expect suspend fun sendDemoNotification(): Result<String>
expect suspend fun runDemoTaskAction(action: String): Result<String>
expect suspend fun readDemoTaskLog(): String
