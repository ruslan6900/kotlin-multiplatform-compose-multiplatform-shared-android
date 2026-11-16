package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import libdbus.*
import platform.posix.*
import ru.auroraos.kmp.dbus.DBusClient
import ru.auroraos.kmp.dbus.dbus.DBusType
import kotlin.time.TimeSource

@OptIn(ExperimentalForeignApi::class)
actual fun elapsedRealtimeMillis(): Long = memScoped {
    val value = alloc<timespec>()
    // CLOCK_BOOTTIME includes suspend, unlike CLOCK_MONOTONIC on Linux.
    check(clock_gettime(7, value.ptr) == 0) { "CLOCK_BOOTTIME unavailable" }
    value.tv_sec * 1000L + value.tv_nsec / 1_000_000L
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun queryNetworkSnapshot(): NetworkSnapshot = withContext(Dispatchers.IO) {
    try {
        val props = DBusClient.call(DBusType.SYSTEM, "net.connman", "/", "net.connman.Manager", "GetProperties", 3000) as? Map<*, *>
            ?: error("ConnMan не вернул свойства сети")
        val state = props["State"] as? String ?: error("ConnMan не вернул State")
        val services = connmanEntries("GetServices")
        val technologies = connmanEntries("GetTechnologies")
        val active = services.firstOrNull { it["State"] == "online" || it["State"] == "ready" }
        fun label(type: String?) = when(type) {
            "wifi" -> "Wi-Fi"; "cellular" -> "Мобильная"; "ethernet" -> "Ethernet"
            "bluetooth" -> "Bluetooth"; "vpn" -> "VPN"; "p2p" -> "Wi-Fi Direct"; "gadget" -> "USB-сеть"; else -> type ?: "не определён"
        }
        fun radio(value: String?) = when (value?.lowercase()) {
            "gsm", "gprs", "edge" -> "2G ($value)"
            "umts", "hspa", "hsupa", "hsdpa", "hspap" -> "3G ($value)"
            "lte" -> "4G/LTE"
            "nr", "5g", "nr5g" -> "5G"
            null, "" -> "поколение не сообщается"
            else -> value
        }
        fun serviceLabel(service: Map<String, Any>): String {
            val type = service["Type"] as? String
            return label(type) + if (type == "cellular") " (${radio(service["Tech"] as? String)})" else ""
        }
        val lines = technologies.filter { it["Type"] in setOf("wifi", "cellular", "ethernet", "bluetooth", "vpn", "p2p", "gadget") }.map { tech ->
            val type = tech["Type"] as? String
            val matching = services.filter { it["Type"] == type }
            val stateText = when {
                matching.any { it["State"] == "online" || it["State"] == "ready" } -> "подключена"
                tech["Powered"] == true -> "включена, не подключена"
                else -> "выключена"
            }
            val mobile = if (type == "cellular") "; " + radio(matching.firstOrNull { it["Tech"] is String }?.get("Tech") as? String) else ""
            "${label(type)}: $stateText$mobile"
        }
        val availability = classifyConnmanState(state)
        NetworkSnapshot(availability, state, active?.let(::serviceLabel) ?: "нет активного подключения",
            "ConnMan: offlineMode=${props["OfflineMode"] ?: "?"}; online — результат проверки системы; поколения мобильной сети показываются только если их сообщает система",
            lines.ifEmpty { listOf("Технологии не предоставлены ConnMan") })
    } catch (e: Exception) {
        NetworkSnapshot(NetworkAvailability.Unknown, "ошибка ConnMan", detail = e.message ?: "нет ответа")
    }
}

/** ConnMan returns a(oa{sv}); decode only non-sensitive properties, never names or credentials. */
@OptIn(ExperimentalForeignApi::class)
private fun connmanEntries(method: String): List<Map<String, Any>> = memScoped {
    val error = alloc<DBusError>(); dbus_error_init(error.ptr)
    val bus = dbus_bus_get_private(DBusBusType.DBUS_BUS_SYSTEM, error.ptr) ?: error("ConnMan: нет подключения к D-Bus")
    dbus_connection_set_exit_on_disconnect(bus, 0u)
    try {
        val request = dbus_message_new_method_call("net.connman", "/", "net.connman.Manager", method) ?: error("ConnMan: не создать запрос")
        val reply = try { dbus_connection_send_with_reply_and_block(bus, request, 3000, error.ptr) }
            finally { dbus_message_unref(request) }
        if (reply == null) error("ConnMan: $method недоступен")
        try {
            val root = alloc<DBusMessageIter>()
            val array = alloc<DBusMessageIter>()
            val result = mutableListOf<Map<String, Any>>()
            if (dbus_message_iter_init(reply, root.ptr) == 0u || dbus_message_iter_get_arg_type(root.ptr) != DBUS_TYPE_ARRAY) return@memScoped result
            dbus_message_iter_recurse(root.ptr, array.ptr)
            while (dbus_message_iter_get_arg_type(array.ptr) == DBUS_TYPE_STRUCT) {
                val structure = alloc<DBusMessageIter>(); dbus_message_iter_recurse(array.ptr, structure.ptr)
                val properties = mutableMapOf<String, Any>()
                if (dbus_message_iter_next(structure.ptr) != 0u && dbus_message_iter_get_arg_type(structure.ptr) == DBUS_TYPE_ARRAY) {
                    val entries = alloc<DBusMessageIter>(); dbus_message_iter_recurse(structure.ptr, entries.ptr)
                    while (dbus_message_iter_get_arg_type(entries.ptr) == DBUS_TYPE_DICT_ENTRY) {
                        val entry = alloc<DBusMessageIter>(); dbus_message_iter_recurse(entries.ptr, entry.ptr)
                        val keyPtr = alloc<CPointerVar<ByteVar>>(); dbus_message_iter_get_basic(entry.ptr, keyPtr.ptr)
                        val key = keyPtr.value?.toKString()
                        if (key in setOf("Type", "State", "Tech", "Powered", "Connected") && dbus_message_iter_next(entry.ptr) != 0u) {
                            val variant = alloc<DBusMessageIter>(); dbus_message_iter_recurse(entry.ptr, variant.ptr)
                            when (dbus_message_iter_get_arg_type(variant.ptr)) {
                                DBUS_TYPE_STRING -> { val v = alloc<CPointerVar<ByteVar>>(); dbus_message_iter_get_basic(variant.ptr, v.ptr); v.value?.toKString()?.let { properties[key!!] = it } }
                                DBUS_TYPE_BOOLEAN -> { val v = alloc<UIntVar>(); dbus_message_iter_get_basic(variant.ptr, v.ptr); properties[key!!] = v.value != 0u }
                            }
                        }
                        if (dbus_message_iter_next(entries.ptr) == 0u) break
                    }
                }
                result.add(properties)
                if (dbus_message_iter_next(array.ptr) == 0u) break
            }
            result
        } finally { dbus_message_unref(reply) }
    } finally { dbus_connection_close(bus); dbus_connection_unref(bus); dbus_error_free(error.ptr) }
}

/** Initial query, ConnMan signals and a periodic refresh for missed signals/restarts. */
@OptIn(ExperimentalForeignApi::class)
actual fun observeNetworkSnapshots(): Flow<NetworkSnapshot> = flow {
    while (currentCoroutineContext().isActive) {
        val (connection, subscribed) = memScoped {
            val error = alloc<DBusError>(); dbus_error_init(error.ptr)
            try {
                val bus = dbus_bus_get_private(DBusBusType.DBUS_BUS_SYSTEM, error.ptr)
                bus?.let {
                    dbus_connection_set_exit_on_disconnect(it, 0u)
                    dbus_bus_add_match(it, "type='signal',sender='net.connman'", error.ptr)
                    dbus_connection_flush(it)
                }
                bus to (bus != null && dbus_error_is_set(error.ptr) == 0u)
            } finally { dbus_error_free(error.ptr) }
        }
        suspend fun current(): NetworkSnapshot {
            val snapshot = queryNetworkSnapshot()
            val mode = if (subscribed) "сигналы ConnMan и контрольный опрос" else "только опрос: подписка недоступна"
            return snapshot.copy(detail = listOf(snapshot.detail, mode).filter { it.isNotBlank() }.joinToString("; "))
        }
        try {
            emit(current())
            if (connection == null) { delay(3000); continue }
            var lastQuery = TimeSource.Monotonic.markNow()
            while (currentCoroutineContext().isActive && dbus_connection_read_write(connection, 1000) != 0u) {
                var changed = false
                while (true) {
                    val msg = dbus_connection_pop_message(connection) ?: break
                    try { if (dbus_message_get_type(msg) == DBUS_MESSAGE_TYPE_SIGNAL) changed = true }
                    finally { dbus_message_unref(msg) }
                }
                if (changed || lastQuery.elapsedNow().inWholeSeconds >= 10) {
                    emit(current()); lastQuery = TimeSource.Monotonic.markNow()
                }
            }
        } finally { connection?.let { dbus_connection_close(it); dbus_connection_unref(it) } }
        if (currentCoroutineContext().isActive) delay(1000)
    }
}.distinctUntilChanged().flowOn(Dispatchers.IO)

/** timed can emit settings changes without a wall-clock jump; don't label them as one. */
@OptIn(ExperimentalForeignApi::class)
actual fun observeSystemTimeSignals(): Flow<SystemTimeSignal> = flow {
    var reportedUnavailable = false
    while (currentCoroutineContext().isActive) {
        val connection = memScoped {
            val error = alloc<DBusError>(); dbus_error_init(error.ptr)
            try {
                val bus = dbus_bus_get_private(DBusBusType.DBUS_BUS_SYSTEM, error.ptr) ?: return@memScoped null
                dbus_connection_set_exit_on_disconnect(bus, 0u)
                dbus_bus_add_match(bus, "type='signal',sender='com.nokia.time',interface='com.nokia.time'", error.ptr)
                dbus_connection_flush(bus)
                if (dbus_error_is_set(error.ptr) != 0u) {
                    dbus_connection_close(bus); dbus_connection_unref(bus); null
                } else bus
            } finally { dbus_error_free(error.ptr) }
        }
        if (connection == null) {
            if (!reportedUnavailable) { emit(SystemTimeSignal.SignalSourceUnavailable); reportedUnavailable = true }
            delay(5000); continue
        }
        if (reportedUnavailable) { emit(SystemTimeSignal.SignalSourceRestored); reportedUnavailable = false }
        try {
            while (currentCoroutineContext().isActive && dbus_connection_read_write(connection, 1000) != 0u) {
                while (true) {
                    val msg = dbus_connection_pop_message(connection) ?: break
                    try {
                        val member = dbus_message_get_member(msg)?.toKString().orEmpty()
                        if (dbus_message_get_type(msg) == DBUS_MESSAGE_TYPE_SIGNAL && member.startsWith("settings_changed")) emit(SystemTimeSignal.SettingsChanged)
                    } finally { dbus_message_unref(msg) }
                }
            }
        } finally { dbus_connection_close(connection); dbus_connection_unref(connection) }
        if (currentCoroutineContext().isActive) delay(1000)
    }
}.flowOn(Dispatchers.IO)
