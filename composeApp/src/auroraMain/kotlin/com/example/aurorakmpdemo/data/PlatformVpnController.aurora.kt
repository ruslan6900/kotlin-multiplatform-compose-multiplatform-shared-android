package com.example.aurorakmpdemo.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.cinterop.ExperimentalForeignApi
import ru.auroraos.kmp.dbus.DBusClient
import ru.auroraos.kmp.dbus.dbus.DBusType
import kotlin.time.Clock

actual fun createPlatformVpnController(): VpnController = AuroraConnmanVpnController()

/**
 * Реализация [VpnController] для Aurora.
 *
 * Последовательность вызовов в текущей development-сборке:
 * `Compose UI -> этот controller -> VpnBridge (system D-Bus) -> ConnMan ->
 * plugin aurora_test_route -> provider-приёмник пакетов -> vpn0`.
 *
 * Bridge владеет привилегированными вызовами ConnMan и принимает только один
 * фиксированный split-tunnel provider. Благодаря этому видимая Compose-сессия
 * без sandbox не получает произвольный доступ к system D-Bus. Вызов D-Bus без
 * возвращаемых аргументов Kotlin-библиотека представляет как `null`, поэтому
 * успехом считается только последующее подтверждение ConnMan `State=ready`/`online`.
 */
@OptIn(ExperimentalForeignApi::class)
private class AuroraConnmanVpnController : VpnController {
    private val operationMutex = Mutex()
    private val _connectionState = MutableStateFlow(VpnConnectionState.Disconnected)
    private val _logs = MutableStateFlow<List<VpnLogEntry>>(emptyList())

    override val connectionState: StateFlow<VpnConnectionState> = _connectionState.asStateFlow()
    override val logs: StateFlow<List<VpnLogEntry>> = _logs.asStateFlow()

    init {
        record(VpnLogLevel.Info, "AuroraConnman", "Контроллер создан; транспорт: system D-Bus → net.connman.vpn")
        record(VpnLogLevel.Info, "Safety", "Тестовый provider обслуживает только 198.51.100.0/24; default route и DNS не меняются")
    }

    /** Последовательно обрабатывает нажатия и ждёт ConnMan, а не считает ответ Connect успехом. */
    override suspend fun connect(profileId: String): Result<Unit> = operationMutex.withLock {
        runCatching {
            check(_connectionState.value == VpnConnectionState.Disconnected) {
                "Подключение доступно только из состояния «Отключено»"
            }
            transition(VpnConnectionState.Connecting, "ConnectionState")
            record(VpnLogLevel.Info, "UI", "Запрошено подключение: profileId=$profileId")
            record(VpnLogLevel.Info, "VpnBridge", "Шаг 1/4: запрошено создание фиксированного тестового профиля")
            val bridgeProfile = bridgeRequired("EnsureTestProfile", listOf(profileId))
            val bridgePath = bridgeProfile.removePrefix("ok:")
            record(VpnLogLevel.Info, "VpnBridge", "Шаг 2/4: профиль подтверждён bridge: path=$bridgePath")
            record(VpnLogLevel.Info, "VpnBridge", "Шаг 3/4: запрошено подключение ConnMan")
            bridgeRequired("ConnectTestProfile")
            val bridgeState = waitForBridgeState(CONNECTED_STATES, CONNECT_WAIT_MS)
                ?: error("bridge/ConnMan не подтвердил State=ready/online за ${CONNECT_WAIT_MS} мс")
            record(VpnLogLevel.Info, "VpnBridge", "Шаг 4/4: ConnMan подтвердил State=$bridgeState")
            transition(VpnConnectionState.Connected, "ConnectionState")
            return@runCatching

            /* Прямой путь к ConnMan сохранён ниже как целевой production-вариант.
             * В development-сборке emulator он недостижим: временный root bridge
             * выше отделяет видимый Compose UI без sandbox от Sailjail policy ConnMan. */
            record(VpnLogLevel.Debug, "ConnMan", "Шаг 1/5: читаем net.connman.vpn.Manager.GetConnections")

            var connection = findTestConnection(profileId)
            if (connection == null) {
                record(VpnLogLevel.Info, "ConnMan", "Шаг 2/5: профиль не найден; вызываем Manager.Create")
                val createReply = callManager(
                    method = "Create",
                    args = listOf(profileProperties(profileId)),
                    timeoutMs = CREATE_TIMEOUT_MS,
                )
                val createdPath = createReply as? String
                record(
                    VpnLogLevel.Debug,
                    "ConnMan",
                    "Manager.Create reply=${createdPath ?: "<empty: проверяем появление профиля опросом>"}",
                )
                record(VpnLogLevel.Info, "ConnMan", "Шаг 3/5: ожидаем регистрацию профиля в GetConnections")
                connection = waitForConnection(profileId, createdPath)
                    ?: error("ConnMan не подтвердил появление тестового профиля за ${PROFILE_WAIT_MS} мс")
            } else {
                record(VpnLogLevel.Info, "ConnMan", "Шаг 2/5: найден существующий тестовый профиль path=${connection.path}")
            }

            val beforeState = connectionState(connection.path)
            record(VpnLogLevel.Info, "ConnMan", "Шаг 4/5: состояние до Connect: ${beforeState ?: "не прочитано"}")
            if (beforeState in CONNECTED_STATES) {
                record(VpnLogLevel.Info, "ConnMan", "ConnMan уже подтверждает активное соединение")
                transition(VpnConnectionState.Connected, "ConnectionState")
                return@runCatching
            }

            record(VpnLogLevel.Info, "ConnMan", "Шаг 5/5: вызываем Connection.Connect path=${connection.path}")
            callConnection(connection.path, "Connect", CONNECT_TIMEOUT_MS)
            record(VpnLogLevel.Debug, "ConnMan", "Connect отправлен; ожидаем State=ready/online, не считаем пустой reply успехом")
            val confirmed = waitForState(connection.path, CONNECTED_STATES, CONNECT_WAIT_MS)
            check(confirmed != null) {
                "ConnMan не подтвердил подключение за ${CONNECT_WAIT_MS} мс; итоговое состояние=${connectionState(connection.path) ?: "не прочитано"}"
            }
            record(VpnLogLevel.Info, "ConnMan", "Подключение подтверждено ConnMan: State=$confirmed")
            transition(VpnConnectionState.Connected, "ConnectionState")
        }.onFailure { failure ->
            record(VpnLogLevel.Error, "AuroraConnman", "Подключение не подтверждено: ${failure.message ?: failure::class.simpleName}")
            _connectionState.value = VpnConnectionState.Error
            record(VpnLogLevel.Warn, "ConnectionState", "state=Error; возвращаем UI в Disconnected")
            _connectionState.value = VpnConnectionState.Disconnected
        }
    }

    /** Повторяет принцип [connect]: после ответа bridge ожидается состояние ConnMan. */
    override suspend fun disconnect(): Result<Unit> = operationMutex.withLock {
        runCatching {
            check(_connectionState.value == VpnConnectionState.Connected) {
                "Отключение доступно только после подтверждённого подключения"
            }
            transition(VpnConnectionState.Disconnecting, "ConnectionState")
            record(VpnLogLevel.Info, "VpnBridge", "Запрошено отключение тестового профиля")
            bridgeRequired("DisconnectTestProfile")
            val bridgeState = waitForBridgeState(DISCONNECTED_STATES, CONNECT_WAIT_MS)
                ?: error("bridge/ConnMan не подтвердил отключение за ${CONNECT_WAIT_MS} мс")
            record(VpnLogLevel.Info, "VpnBridge", "ConnMan подтвердил отключение: State=$bridgeState")
            transition(VpnConnectionState.Disconnected, "ConnectionState")
            return@runCatching

            val connection = findAnyTestConnection()
                ?: error("Тестовый профиль ConnMan не найден")
            record(VpnLogLevel.Info, "ConnMan", "Вызываем Connection.Disconnect path=${connection.path}")
            callConnection(connection.path, "Disconnect", CONNECT_TIMEOUT_MS)
            val confirmed = waitForState(connection.path, DISCONNECTED_STATES, CONNECT_WAIT_MS)
            check(confirmed != null) { "ConnMan не подтвердил отключение за ${CONNECT_WAIT_MS} мс" }
            record(VpnLogLevel.Info, "ConnMan", "Отключение подтверждено ConnMan: State=$confirmed")
            transition(VpnConnectionState.Disconnected, "ConnectionState")
        }.onFailure { failure ->
            record(VpnLogLevel.Error, "AuroraConnman", "Отключение не подтверждено: ${failure.message ?: failure::class.simpleName}")
            _connectionState.value = VpnConnectionState.Connected
        }
    }

    override fun clearLogs() {
        _logs.value = emptyList()
        record(VpnLogLevel.Info, "AuroraConnman", "Журнал очищен пользователем")
    }

    private fun findTestConnection(profileId: String): Connection? = connections().firstOrNull {
        it.properties["Name"] == profileId && it.properties["Type"] == TEST_TYPE &&
            it.properties["Host"] == TEST_HOST && it.properties["Domain"] == TEST_DOMAIN
    }

    private fun findAnyTestConnection(): Connection? = connections().firstOrNull {
        it.properties["Type"] == TEST_TYPE && it.properties["Host"] == TEST_HOST && it.properties["Domain"] == TEST_DOMAIN
    }

    private suspend fun waitForConnection(profileId: String, createReplyPath: String?): Connection? {
        repeat(PROFILE_ATTEMPTS) { attempt ->
            val found = findTestConnection(profileId)
                ?: createReplyPath?.let { path -> connections().firstOrNull { it.path == path } }
            if (found != null) {
                record(VpnLogLevel.Info, "ConnMan", "Профиль подтверждён: path=${found.path}; попытка=${attempt + 1}")
                return found
            }
            record(VpnLogLevel.Debug, "ConnMan", "Профиль ещё не виден; попытка=${attempt + 1}/$PROFILE_ATTEMPTS")
            delay(POLL_INTERVAL_MS)
        }
        return null
    }

    private suspend fun waitForState(path: String, expected: Set<String>, timeoutMs: Long): String? {
        val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt().coerceAtLeast(1)
        repeat(attempts) { attempt ->
            val state = connectionState(path)
            record(VpnLogLevel.Debug, "ConnMan", "State poll ${attempt + 1}/$attempts: ${state ?: "<no reply>"}")
            if (state in expected) return state
            if (state in FAILURE_STATES) error("ConnMan сообщил terminal state=$state")
            delay(POLL_INTERVAL_MS)
        }
        return null
    }

    private fun connections(): List<Connection> {
        val reply = callManager("GetConnections", emptyList(), QUERY_TIMEOUT_MS)
        val map = reply as? Map<*, *> ?: run {
            record(VpnLogLevel.Warn, "ConnMan", "GetConnections вернул ${reply?.let { it::class.simpleName } ?: "null"}")
            return emptyList()
        }
        val result = map.mapNotNull { (path, value) ->
            val objectPath = path as? String ?: return@mapNotNull null
            val properties = (value as? Map<*, *>)
                ?.mapNotNull { (key, item) -> (key as? String)?.let { it to item } }
                ?.toMap()
                ?: emptyMap()
            Connection(objectPath, properties)
        }
        record(VpnLogLevel.Debug, "ConnMan", "GetConnections: ${result.size} connection(s)")
        return result
    }

    private fun connectionState(path: String): String? {
        val reply = callConnection(path, "GetProperties", QUERY_TIMEOUT_MS)
        val properties = reply as? Map<*, *> ?: run {
            record(VpnLogLevel.Warn, "ConnMan", "GetProperties path=$path вернул ${reply?.let { it::class.simpleName } ?: "null"}")
            return null
        }
        return properties["State"] as? String
    }

    private fun callManager(method: String, args: List<Any?>, timeoutMs: Int): Any? =
        DBusClient.call(DBusType.SYSTEM, CONNMAN_SERVICE, MANAGER_PATH, MANAGER_INTERFACE, method, timeoutMs, args)

    private fun callConnection(path: String, method: String, timeoutMs: Int): Any? =
        DBusClient.call(DBusType.SYSTEM, CONNMAN_SERVICE, path, CONNECTION_INTERFACE, method, timeoutMs)

    /**
     * Вызывает узкий API bridge. `ok:` — стабильная граница его протокола;
     * `error:` превращается в видимую в UI ошибку с исходной причиной.
     */
    private fun bridgeRequired(method: String, args: List<Any?> = emptyList()): String {
        val reply = DBusClient.call(
            DBusType.SYSTEM,
            BRIDGE_SERVICE,
            BRIDGE_PATH,
            BRIDGE_INTERFACE,
            method,
            BRIDGE_TIMEOUT_MS,
            args,
        ) as? String ?: error("VPN bridge не ответил на $method")
        check(reply.startsWith("ok:")) { "VPN bridge $method: ${reply.removePrefix("error:")}" }
        record(VpnLogLevel.Debug, "VpnBridge", "$method → $reply")
        return reply
    }

    /** Опрашивает состояние ConnMan через bridge, поскольку Connect/Disconnect асинхронны. */
    private suspend fun waitForBridgeState(expected: Set<String>, timeoutMs: Long): String? {
        val attempts = (timeoutMs / POLL_INTERVAL_MS).toInt().coerceAtLeast(1)
        repeat(attempts) { attempt ->
            val reply = DBusClient.call(
                DBusType.SYSTEM,
                BRIDGE_SERVICE,
                BRIDGE_PATH,
                BRIDGE_INTERFACE,
                "TestProfileState",
                BRIDGE_TIMEOUT_MS,
            ) as? String
            val state = reply?.takeIf { it.startsWith("ok:") }?.removePrefix("ok:")
            record(VpnLogLevel.Debug, "VpnBridge", "State poll ${attempt + 1}/$attempts: ${state ?: reply ?: "<no reply>"}")
            if (state in expected) return state
            if (state in FAILURE_STATES) error("ConnMan сообщил terminal state=$state")
            delay(POLL_INTERVAL_MS)
        }
        return null
    }

    private fun profileProperties(profileId: String): Map<String, Any> = mapOf(
        "Name" to profileId,
        "Type" to TEST_TYPE,
        "Host" to TEST_HOST,
        "Domain" to TEST_DOMAIN,
    )

    private fun transition(next: VpnConnectionState, component: String) {
        _connectionState.value = next
        record(VpnLogLevel.Info, component, "state=${next.name}")
    }

    private fun record(level: VpnLogLevel, component: String, message: String) {
        val entry = VpnLogEntry(Clock.System.now().toString(), level, component, message)
        _logs.update { entries -> (entries + entry).takeLast(MAX_LOG_ENTRIES) }
    }

    private data class Connection(val path: String, val properties: Map<String, Any?>)

    private companion object {
        const val CONNMAN_SERVICE = "net.connman.vpn"
        const val MANAGER_PATH = "/"
        const val MANAGER_INTERFACE = "net.connman.vpn.Manager"
        const val CONNECTION_INTERFACE = "net.connman.vpn.Connection"
        // Эти три значения должны совпадать с D-Bus policy и C++-сервисом bridge.
        const val BRIDGE_SERVICE = "com.example.aurorakmpdemo.VpnBridge"
        const val BRIDGE_PATH = "/com/example/aurorakmpdemo/VpnBridge"
        const val BRIDGE_INTERFACE = "com.example.aurorakmpdemo.VpnBridge"
        // Type/Host/Domain должны совпадать с нативным plugin и bridge.
        const val TEST_TYPE = "aurora_test_route"
        const val TEST_HOST = "198.51.100.1"
        const val TEST_DOMAIN = "example.test"
        const val QUERY_TIMEOUT_MS = 2_000
        const val CREATE_TIMEOUT_MS = 5_000
        const val CONNECT_TIMEOUT_MS = 5_000
        const val BRIDGE_TIMEOUT_MS = 5_000
        const val POLL_INTERVAL_MS = 250L
        const val PROFILE_ATTEMPTS = 20
        const val PROFILE_WAIT_MS = PROFILE_ATTEMPTS * POLL_INTERVAL_MS
        const val CONNECT_WAIT_MS = 10_000L
        const val MAX_LOG_ENTRIES = 120
        val CONNECTED_STATES = setOf("ready", "online")
        val DISCONNECTED_STATES = setOf("idle", "disconnect")
        val FAILURE_STATES = setOf("failure", "disconnect")
    }
}
