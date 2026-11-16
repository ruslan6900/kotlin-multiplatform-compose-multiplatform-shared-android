package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import libdbus.*
import com.example.aurorakmpdemo.ui.UiTrace
import com.example.aurorakmpdemo.data.createPlatformVpnController
import kotlin.time.TimeSource

/** Test-only session-bus endpoint. Its lifetime is the UI application's lifetime. */
@OptIn(ExperimentalForeignApi::class)
fun startDialogAnswerService(scope: CoroutineScope, stopAfterOneRequest: Boolean = false): Job = scope.launch(Dispatchers.IO) {
    try {
        memScoped {
            val error = alloc<DBusError>()
            dbus_error_init(error.ptr)
            val connection = dbus_bus_get_private(DBusBusType.DBUS_BUS_SESSION, error.ptr)
                ?: error("session D-Bus unavailable: ${error.message?.toKString()}")
            try {
                dbus_connection_set_exit_on_disconnect(connection, 0u)
                var status = dbus_bus_request_name(connection, "com.example.aurorakmpdemo", 4u, error.ptr)
                while (status == 3 && isActive) {
                    delay(1000)
                    status = dbus_bus_request_name(connection, "com.example.aurorakmpdemo", 4u, error.ptr)
                }
                if (!isActive) return@memScoped
                check(status == 1) { "Cannot own D-Bus name: status=$status ${error.message?.toKString()}" }
                UiTrace.log("DialogAnswer", "D-Bus проверка ответа запущена")
                var handledRequest = false
                var lastHandled = TimeSource.Monotonic.markNow()
                while (isActive && dbus_connection_read_write(connection, 200) != 0u) {
                    if (stopAfterOneRequest && handledRequest && lastHandled.elapsedNow().inWholeMilliseconds >= 1500) {
                        UiTrace.log("DialogAnswer", "Фоновые запросы обработаны, завершаем сервис")
                        break
                    }
                    val message = dbus_connection_pop_message(connection) ?: continue
                    try {
                        val checkCall = dbus_message_is_method_call(message, "com.example.aurorakmpdemo.DialogAnswer", "Check") != 0u
                        val vpnCall = dbus_message_is_method_call(message, "com.example.aurorakmpdemo.DialogAnswer", "ConnectVpn") != 0u
                        if ((!checkCall && !vpnCall) || dbus_message_get_path(message)?.toKString() != "/com/example/aurorakmpdemo/DialogAnswer") continue
                        memScoped {
                            val args = alloc<DBusMessageIter>()
                            val value = alloc<CPointerVar<ByteVar>>()
                            val valid = dbus_message_iter_init(message, args.ptr) != 0u && dbus_message_iter_get_arg_type(args.ptr) == DBUS_TYPE_STRING
                            if (valid) dbus_message_iter_get_basic(args.ptr, value.ptr)
                            val answer = if (valid) value.value?.toKString() else null
                            val accepted = if (vpnCall) {
                                UiTrace.log("DialogAnswer", "Запрос подключения VPN из системного окна")
                                val result = runBlocking { createPlatformVpnController().connect(readVpnRestartSettings().getOrDefault(VpnRestartSettings()).profileId) }
                                UiTrace.log("DialogAnswer", "Подключение VPN: ${result.exceptionOrNull()?.message ?: "подтверждено"}")
                                result.isSuccess
                            } else answer?.trim() == "4"
                            val reply = dbus_message_new_method_return(message) ?: error("Cannot create D-Bus response")
                            try {
                                val iterator = alloc<DBusMessageIter>()
                                val result = alloc<dbus_bool_tVar>()
                                result.value = if (accepted) 1u else 0u
                                dbus_message_iter_init_append(reply, iterator.ptr)
                                check(dbus_message_iter_append_basic(iterator.ptr, DBUS_TYPE_BOOLEAN, result.ptr) != 0u)
                                check(dbus_connection_send(connection, reply, null) != 0u)
                                dbus_connection_flush(connection)
                                UiTrace.log("DialogAnswer", "Проверка ответа из системного окна: результат=$accepted")
                            } finally { dbus_message_unref(reply) }
                        }
                    } finally { dbus_message_unref(message) }
                    if (stopAfterOneRequest) {
                        // Keep dispatching for a short idle grace period: the sandbox proxy
                        // needs time to forward the reply, and a following request may arrive.
                        handledRequest = true
                        lastHandled = TimeSource.Monotonic.markNow()
                        UiTrace.log("DialogAnswer", "Ответ отправлен; ожидаем доставку и возможные запросы")
                    }
                }
            } finally {
                dbus_connection_close(connection)
                dbus_connection_unref(connection)
                dbus_error_free(error.ptr)
            }
        }
    } catch (e: Exception) {
        UiTrace.log("DialogAnswer", "Ошибка сервиса проверки: ${e.message}")
    }
}
