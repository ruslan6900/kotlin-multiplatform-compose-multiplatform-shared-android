package com.example.aurorakmpdemo.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Portable contract. Aurora delegates to ConnMan through the system D-Bus. */
interface VpnController {
    val connectionState: StateFlow<VpnConnectionState>
    val logs: StateFlow<List<VpnLogEntry>>

    suspend fun connect(profileId: String): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    fun clearLogs()
}

enum class VpnConnectionState(val label: String) {
    Disconnected("Отключено"),
    Connecting("Подключение"),
    Connected("Подключено"),
    Disconnecting("Отключение"),
    Error("Ошибка"),
}

data class VpnLogEntry(
    val timestamp: String,
    val level: VpnLogLevel,
    val component: String,
    val message: String,
)

enum class VpnLogLevel { Debug, Info, Warn, Error }

/**
 * Deliberately has no D-Bus, ConnMan, TUN, routing or credential side effects.
 * It exercises the same state-machine boundary that a future platform adapter uses.
 */
class SimulatedVpnController : VpnController {
    private val operationMutex = Mutex()
    private val _connectionState = MutableStateFlow(VpnConnectionState.Disconnected)
    private val _logs = MutableStateFlow<List<VpnLogEntry>>(emptyList())

    override val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()
    override val logs: StateFlow<List<VpnLogEntry>> = _logs.asStateFlow()

    init {
        record(VpnLogLevel.Info, "DemoVpn", "Контроллер создан; режим: simulation-only")
        record(VpnLogLevel.Debug, "DemoVpn", "D-Bus, ConnMan, TUN и сетевые маршруты отключены")
    }

    override suspend fun connect(profileId: String): Result<Unit> = operationMutex.withLock {
        runCatching {
            check(_connectionState.value == VpnConnectionState.Disconnected) {
                "Подключение доступно только из состояния «Отключено»"
            }
            record(VpnLogLevel.Info, "UI", "Запрошено фиктивное подключение: profile=$profileId")
            transition(VpnConnectionState.Connecting, "ConnectionState")
            record(VpnLogLevel.Debug, "Profile", "Проверка демонстрационного профиля выполнена")
            delay(120)
            record(VpnLogLevel.Info, "AuroraVpnAdapter", "SIMULATION: вызов ConnMan/D-Bus намеренно пропущен")
            record(VpnLogLevel.Debug, "Provider", "SIMULATION: бинарник VPN-провайдера не запускается")
            delay(120)
            record(VpnLogLevel.Debug, "Tunnel", "SIMULATION: TUN, DNS и маршруты не изменялись")
            transition(VpnConnectionState.Connected, "ConnectionState")
            record(VpnLogLevel.Info, "DemoVpn", "Фиктивная VPN-сессия активна")
        }.onFailure { error ->
            _connectionState.value = VpnConnectionState.Error
            record(VpnLogLevel.Error, "DemoVpn", "Подключение отклонено: ${error.message}")
            _connectionState.value = VpnConnectionState.Disconnected
            record(VpnLogLevel.Warn, "ConnectionState", "Состояние восстановлено: Disconnected")
        }
    }

    override suspend fun disconnect(): Result<Unit> = operationMutex.withLock {
        runCatching {
            check(_connectionState.value == VpnConnectionState.Connected) {
                "Отключение доступно только из состояния «Подключено»"
            }
            record(VpnLogLevel.Info, "UI", "Запрошено фиктивное отключение")
            transition(VpnConnectionState.Disconnecting, "ConnectionState")
            record(VpnLogLevel.Info, "AuroraVpnAdapter", "SIMULATION: Disconnect у ConnMan/D-Bus не вызывается")
            delay(120)
            record(VpnLogLevel.Debug, "Tunnel", "SIMULATION: удаление TUN/DNS/маршрутов пропущено")
            transition(VpnConnectionState.Disconnected, "ConnectionState")
            record(VpnLogLevel.Info, "DemoVpn", "Фиктивная VPN-сессия завершена")
        }.onFailure { error ->
            record(VpnLogLevel.Error, "DemoVpn", "Отключение отклонено: ${error.message}")
        }
    }

    override fun clearLogs() {
        _logs.value = emptyList()
        record(VpnLogLevel.Info, "DemoVpn", "Журнал очищен пользователем")
    }

    private fun transition(next: VpnConnectionState, component: String) {
        _connectionState.value = next
        record(VpnLogLevel.Info, component, "state=${next.name}")
    }

    private fun record(level: VpnLogLevel, component: String, message: String) {
        val entry = VpnLogEntry(Clock.System.now().toString(), level, component, message)
        _logs.update { current -> (current + entry).takeLast(MAX_LOG_ENTRIES) }
    }

    private companion object {
        const val MAX_LOG_ENTRIES = 120
    }
}
