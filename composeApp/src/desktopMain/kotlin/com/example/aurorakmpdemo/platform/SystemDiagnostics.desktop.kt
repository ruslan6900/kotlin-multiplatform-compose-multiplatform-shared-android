package com.example.aurorakmpdemo.platform

import java.net.NetworkInterface
import java.util.Collections
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*

actual fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000L
actual suspend fun queryNetworkSnapshot(): NetworkSnapshot = try {
    val active = Collections.list(NetworkInterface.getNetworkInterfaces()).filter { it.isUp && !it.isLoopback && it.interfaceAddresses.isNotEmpty() }
    if (active.isEmpty()) NetworkSnapshot(NetworkAvailability.Offline, "нет активных интерфейсов")
    else NetworkSnapshot(NetworkAvailability.ConnectedUnverified, "интерфейс активен", active.joinToString { it.name }, "Проверка доступности интернета системой недоступна")
} catch (e: Exception) { NetworkSnapshot(NetworkAvailability.Unknown, "ошибка", detail = e.message ?: "нет ответа") }
actual fun observeNetworkSnapshots(): Flow<NetworkSnapshot> = flow {
    while (currentCoroutineContext().isActive) { emit(queryNetworkSnapshot()); delay(2000) }
}.distinctUntilChanged()
actual fun observeSystemTimeSignals(): Flow<SystemTimeSignal> = emptyFlow()
