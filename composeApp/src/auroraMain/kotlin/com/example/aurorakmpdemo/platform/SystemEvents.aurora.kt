package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.*
import libdbus.*
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.auroraos.kmp.dbus.DBusClient
import ru.auroraos.kmp.dbus.dbus.DBusType

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual suspend fun sendDemoNotification(): Result<String> = withContext(Dispatchers.Default) {
    runCatching {
        val reply = DBusClient.call(
            DBusType.SESSION,
            "org.freedesktop.Notifications",
            "/org/freedesktop/Notifications",
            "org.freedesktop.Notifications",
            "Notify",
            5000,
            listOf("Aurora KMP Demo", 0u, "com.example.aurorakmpdemo", "Проверка уведомлений", "Системное уведомление отправлено из демо-приложения.", emptyList<String>(), mapOf("x-nemo-preview-summary" to "Проверка уведомлений", "x-nemo-preview-body" to "Привет от Aurora KMP Demo"), -1),
        )
        check(reply is Number && reply.toLong() > 0) { "Система не подтвердила публикацию уведомления" }
        "Система приняла уведомление №${reply.toLong()}"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun recordTaskRequest(message: String) {
    val home = platform.posix.getenv("HOME")?.toKString() ?: return
    val path = "$home/.local/share/com.example/aurorakmpdemo/system-events.log"
    val file = platform.posix.fopen(path, "a") ?: return
    platform.posix.fprintf(file, "%s\n", "[${Clock.System.now()}][pid=${platform.posix.getpid()}]$message")
    platform.posix.fclose(file)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun runDemoTaskAction(action: String): Result<String> = withContext(Dispatchers.Default) {
    runCatching {
        val stop = action.startsWith("stop-")
        val kind = action.removePrefix("stop-")
        val id = when (kind) {
            "worker" -> "DemoWorker"
            "periodic-work" -> "DemoPeriodicWork"
            "periodic-notification" -> "DemoPeriodicNotification"
            else -> error("Неизвестная задача: $action")
        }
        require(!(stop && kind == "worker"))
        val fullId = "com.example.aurorakmpdemo.task_$id"
        recordTaskRequest("[REQUEST] id=$id action=${if (stop) "stop" else "start"} interval=${if (kind == "worker") 0 else 900} runningTime=60 autostart=false")
        try {
            memScoped {
                val error = alloc<DBusError>()
                dbus_error_init(error.ptr)
                val connection = dbus_bus_get_private(DBusBusType.DBUS_BUS_SESSION, error.ptr)
                    ?: error("D-Bus: ${error.message?.toKString() ?: "нет соединения"}")
                dbus_connection_set_exit_on_disconnect(connection, 0u)
                val message = dbus_message_new_method_call("ru.omp.RuntimeManager", "/ru/omp/RuntimeManager/Tasks1", "ru.omp.RuntimeManager.Tasks1", if (stop) "Stop" else "Start")
                    ?: error("Не удалось сформировать запрос D-Bus")
                try {
                    val root = alloc<DBusMessageIter>()
                    dbus_message_iter_init_append(message, root.ptr)
                    val idPtr = alloc<CPointerVar<ByteVar>>(); idPtr.value = fullId.cstr.getPointer(this)
                    check(dbus_message_iter_append_basic(root.ptr, DBUS_TYPE_STRING, idPtr.ptr) != 0u)
                    if (stop) {
                        val enabled = alloc<dbus_bool_tVar>(); enabled.value = 1u
                        check(dbus_message_iter_append_basic(root.ptr, DBUS_TYPE_BOOLEAN, enabled.ptr) != 0u)
                    } else {
                        val array = alloc<DBusMessageIter>()
                        check(dbus_message_iter_open_container(root.ptr, DBUS_TYPE_ARRAY, "s", array.ptr) != 0u)
                        check(dbus_message_iter_close_container(root.ptr, array.ptr) != 0u)
                        val dict = alloc<DBusMessageIter>()
                        check(dbus_message_iter_open_container(root.ptr, DBUS_TYPE_ARRAY, "{sv}", dict.ptr) != 0u)
                        fun addInt(key: String, value: Int) {
                            val entry = alloc<DBusMessageIter>(); val variant = alloc<DBusMessageIter>()
                            check(dbus_message_iter_open_container(dict.ptr, DBUS_TYPE_DICT_ENTRY, null, entry.ptr) != 0u)
                            val k = alloc<CPointerVar<ByteVar>>(); k.value = key.cstr.getPointer(this)
                            check(dbus_message_iter_append_basic(entry.ptr, DBUS_TYPE_STRING, k.ptr) != 0u)
                            check(dbus_message_iter_open_container(entry.ptr, DBUS_TYPE_VARIANT, "i", variant.ptr) != 0u)
                            val v = alloc<IntVar>(); v.value = value
                            check(dbus_message_iter_append_basic(variant.ptr, DBUS_TYPE_INT32, v.ptr) != 0u)
                            check(dbus_message_iter_close_container(entry.ptr, variant.ptr) != 0u)
                            check(dbus_message_iter_close_container(dict.ptr, entry.ptr) != 0u)
                        }
                        addInt("RunningTime", 60)
                        if (kind != "worker") addInt("Interval", 900)
                        check(dbus_message_iter_close_container(root.ptr, dict.ptr) != 0u)
                    }
                    val reply = dbus_connection_send_with_reply_and_block(connection, message, 10000, error.ptr)
                        ?: error("RuntimeManager: ${error.name?.toKString() ?: "нет ответа"}: ${error.message?.toKString() ?: ""}")
                    dbus_message_unref(reply)
                } finally {
                    dbus_message_unref(message)
                    dbus_connection_close(connection)
                    dbus_connection_unref(connection)
                    dbus_error_free(error.ptr)
                }
            }
            recordTaskRequest("[REQUEST_ACCEPTED] id=$id action=${if (stop) "stop" else "start"}")
            "RuntimeManager принял запрос: $action"
        } catch (e: Exception) {
            recordTaskRequest("[REQUEST_ERROR] id=$id action=$action error=${e.message}")
            throw e
        }
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual suspend fun readDemoTaskLog(): String = withContext(Dispatchers.Default) {
    val home = platform.posix.getenv("HOME")?.toKString() ?: return@withContext "HOME недоступен"
    val path = "$home/.local/share/com.example/aurorakmpdemo/system-events.log"
    val file = platform.posix.fopen(path, "r") ?: return@withContext "Журнал ещё не создан: $path"
    try {
        kotlinx.cinterop.memScoped {
            val buffer = allocArray<kotlinx.cinterop.ByteVar>(2048)
            val lines = ArrayDeque<String>()
            while (platform.posix.fgets(buffer, 2048, file) != null) {
                lines.addLast(buffer.toKString().trimEnd())
                if (lines.size > 100) lines.removeFirst()
            }
            lines.joinToString("\n").ifEmpty { "Журнал пуст" }
        }
    } finally { platform.posix.fclose(file) }
}
