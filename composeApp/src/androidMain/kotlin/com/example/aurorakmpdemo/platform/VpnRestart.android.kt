package com.example.aurorakmpdemo.platform

actual fun readVpnRestartSettings(): Result<VpnRestartSettings> = Result.success(VpnRestartSettings())
actual fun saveVpnRestartSettings(settings: VpnRestartSettings): Result<Unit> = Result.failure(UnsupportedOperationException("Проверка перезагрузки доступна только на Aurora"))
actual fun currentVpnBootId(): Result<String> = Result.failure(UnsupportedOperationException("Проверка перезагрузки доступна только на Aurora"))
