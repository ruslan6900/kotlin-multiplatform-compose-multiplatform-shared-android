package com.example.aurorakmpdemo.platform

import com.example.aurorakmpdemo.ui.UiTrace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

enum class NetworkAvailability { Online, ConnectedUnverified, Offline, Unknown }
data class NetworkSnapshot(
    val availability: NetworkAvailability,
    val systemState: String,
    val transport: String = "не определён",
    val detail: String = "",
    val technologies: List<String> = emptyList(),
) {
    val label: String get() = when (availability) {
        NetworkAvailability.Online -> "Интернет подтверждён системой"
        NetworkAvailability.ConnectedUnverified -> "Сеть подключена, интернет не подтверждён"
        NetworkAvailability.Offline -> "Нет активного подключения"
        NetworkAvailability.Unknown -> "Состояние сети неизвестно"
    }
}

internal fun classifyConnmanState(state: String): NetworkAvailability = when (state) {
    "online" -> NetworkAvailability.Online
    "ready" -> NetworkAvailability.ConnectedUnverified
    "idle", "offline", "failure" -> NetworkAvailability.Offline
    else -> NetworkAvailability.Unknown
}

/** Returns the change in wall-clock offset, ignoring normal passage of time. */
internal fun clockAdjustmentMillis(previousWall: Long, wall: Long, previousElapsed: Long, elapsed: Long): Long? {
    if (elapsed < previousElapsed) return null
    val adjustment = (wall - previousWall) - (elapsed - previousElapsed)
    return adjustment.takeIf { it >= 500 || it <= -500 }
}

enum class SystemTimeSignal { ClockChanged, TimeZoneChanged, MinuteTick, SettingsChanged, SignalSourceUnavailable, SignalSourceRestored }
expect suspend fun queryNetworkSnapshot(): NetworkSnapshot
expect fun observeNetworkSnapshots(): Flow<NetworkSnapshot>
expect fun observeSystemTimeSignals(): Flow<SystemTimeSignal>
/** Monotonic elapsed time that includes device suspension when the platform supports it. */
expect fun elapsedRealtimeMillis(): Long

data class TimeSnapshot(
    val display: String = "—",
    val zone: String = "—",
    val lastEvent: String = "Изменений пока не обнаружено",
    val eventCount: Int = 0,
    val minuteTicks: Int = 0,
    val referenceStatus: String = "Ожидание исходного времени",
)

/** Runs while the app process lives, including navigation away from the diagnostics screen. */
object SystemDiagnosticsMonitor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var networkJob: Job? = null
    private var timeJob: Job? = null
    private val _network = MutableStateFlow(NetworkSnapshot(NetworkAvailability.Unknown, "не проверено"))
    val network = _network.asStateFlow()
    private val _networkRunning = MutableStateFlow(false)
    val networkRunning = _networkRunning.asStateFlow()
    private val _time = MutableStateFlow(TimeSnapshot())
    val time = _time.asStateFlow()
    private val _timeRunning = MutableStateFlow(false)
    val timeRunning = _timeRunning.asStateFlow()
    private val _events = MutableStateFlow<List<String>>(emptyList())
    val events = _events.asStateFlow()

    private fun event(area: String, message: String) {
        val line = "[${Clock.System.now()}] $area: $message"
        _events.update { (it + line).takeLast(60) }
        UiTrace.log("SystemDiagnostics", "$area: $message")
    }
    private fun networkChanged(snapshot: NetworkSnapshot, reason: String) {
        val previous = _network.value
        _network.value = snapshot
        if (previous.transport != "не определён" && snapshot.transport != previous.transport)
            event("Сеть", "Тип подключения изменился: ${previous.transport} → ${snapshot.transport}")
        if (snapshot != previous || reason == "ручная проверка") {
            event("Сеть", "$reason; ${snapshot.label}; состояние=${snapshot.systemState}; тип=${snapshot.transport}; ${snapshot.detail}; технологии=${snapshot.technologies.joinToString()}")
        }
    }
    fun checkNetworkNow() {
        scope.launch {
            event("Сеть", "Запрошена проверка текущего состояния")
            val snapshot = runCatching { queryNetworkSnapshot() }.getOrElse {
                NetworkSnapshot(NetworkAvailability.Unknown, "ошибка", detail = it.message ?: "нет ответа")
            }
            networkChanged(snapshot, "ручная проверка")
        }
    }
    fun startNetwork() {
        if (networkJob?.isActive == true) return
        _networkRunning.value = true
        event("Сеть", "Наблюдение запущено")
        networkJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                observeNetworkSnapshots().collect { networkChanged(it, "обновление системы") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                networkChanged(NetworkSnapshot(NetworkAvailability.Unknown, "ошибка наблюдения", detail = e.message ?: "нет данных"), "ошибка")
                event("Сеть", "Наблюдение завершилось с ошибкой: ${e.message}")
            } finally { if (networkJob === coroutineContext.job) _networkRunning.value = false }
        }
        networkJob?.start()
    }
    fun stopNetwork() { networkJob?.cancel(); networkJob = null; _networkRunning.value = false; event("Сеть", "Наблюдение остановлено") }

    private fun currentDisplay(epochMs: Long): Pair<String, String> {
        val zone = TimeZone.currentSystemDefault()
        val local = kotlin.time.Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        return local.toString().replace('T', ' ').substringBefore('.') to zone.id
    }
    private fun reportTimeEvent(kind: String, description: String) {
        _time.update { it.copy(lastEvent = "$kind: $description", eventCount = it.eventCount + 1) }
        event("Время", "$kind; $description")
    }
    fun startTime() {
        if (timeJob?.isActive == true) return
        _timeRunning.value = true
        event("Время", "Наблюдение запущено: часы, часовой пояс и минутные события")
        timeJob = scope.launch(start = CoroutineStart.LAZY) {
            coroutineScope {
                launch {
                    try {
                        observeSystemTimeSignals().collect { signal ->
                            when (signal) {
                                SystemTimeSignal.ClockChanged -> reportTimeEvent("Системное событие", "изменено системное время")
                                SystemTimeSignal.TimeZoneChanged -> reportTimeEvent("Системное событие", "изменён часовой пояс")
                                SystemTimeSignal.MinuteTick -> event("Время", "Получено системное минутное событие")
                                SystemTimeSignal.SettingsChanged -> event("Время", "Система сообщила об изменении настроек времени")
                                SystemTimeSignal.SignalSourceUnavailable -> event("Время", "Системный источник событий недоступен; сравнение часов продолжается")
                                SystemTimeSignal.SignalSourceRestored -> event("Время", "Системный источник событий доступен")
                            }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { event("Время", "Системные события недоступны: ${e.message}") }
                }
                var previousWall = Clock.System.now().toEpochMilliseconds()
                var previousElapsed = elapsedRealtimeMillis()
                val referenceWall = previousWall
                val referenceElapsed = previousElapsed
                var displaced = false
                var previousMinute = MathFloorDiv.floorMinute(previousWall)
                var previousZone = runCatching { currentDisplay(previousWall).second }.getOrDefault("неизвестно")
                while (isActive) {
                    val wall = Clock.System.now().toEpochMilliseconds()
                    val elapsed = elapsedRealtimeMillis()
                    val offset = clockAdjustmentMillis(previousWall, wall, previousElapsed, elapsed)
                    val (display, zone) = runCatching { currentDisplay(wall) }.getOrElse { "Ошибка: ${it.message}" to "неизвестно" }
                    if (offset != null) reportTimeEvent("Изменение часов", "скачок относительно времени работы устройства: ${offset} мс")
                    val deviation = (wall - referenceWall) - (elapsed - referenceElapsed)
                    val isDisplaced = elapsed >= referenceElapsed && (deviation >= 500 || deviation <= -500)
                    if (isDisplaced && !displaced) reportTimeEvent("Отклонение от исходного времени", "${deviation} мс")
                    if (!isDisplaced && displaced) reportTimeEvent("Возврат времени", "Часы вернулись к исходной траектории; отклонение ${deviation} мс")
                    displaced = isDisplaced
                    val referenceStatus = if (isDisplaced) "Изменено относительно начала наблюдения: ${deviation} мс" else "Соответствует времени на начало наблюдения"
                    if (zone != previousZone) reportTimeEvent("Часовой пояс", "$previousZone → $zone")
                    val minute = MathFloorDiv.floorMinute(wall)
                    if (minute != previousMinute) {
                        _time.update { it.copy(minuteTicks = it.minuteTicks + 1) }
                        event("Время", "Наступила новая минута: $display ($zone)")
                    }
                    _time.update { it.copy(display = display, zone = zone, referenceStatus = referenceStatus) }
                    previousWall = wall; previousElapsed = elapsed; previousZone = zone; previousMinute = minute
                    delay(1000)
                }
            }
        }.also { job -> job.invokeOnCompletion { if (timeJob === job) _timeRunning.value = false } }
        timeJob?.start()
    }
    fun stopTime() { timeJob?.cancel(); timeJob = null; _timeRunning.value = false; event("Время", "Наблюдение остановлено") }
    fun clearEvents() { _events.value = emptyList(); event("Диагностика", "Журнал экрана очищен") }
}

private object MathFloorDiv {
    fun floorMinute(epochMs: Long): Long = if (epochMs >= 0) epochMs / 60_000 else (epochMs - 59_999) / 60_000
}
