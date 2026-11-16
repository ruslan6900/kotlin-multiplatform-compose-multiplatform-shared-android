package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.aurorakmpdemo.data.VpnConnectionState
import com.example.aurorakmpdemo.data.VpnController
import com.example.aurorakmpdemo.platform.VpnRestartSettings
import com.example.aurorakmpdemo.platform.currentVpnBootId
import com.example.aurorakmpdemo.platform.readVpnRestartSettings
import com.example.aurorakmpdemo.platform.saveVpnRestartSettings
import com.example.aurorakmpdemo.platform.shouldPromptVpnAfterReboot
import com.example.aurorakmpdemo.ui.theme.DemoColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun VpnRestartPrompt() {
    val controller: VpnController = koinInject()
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<VpnRestartSettings?>(null) }
    var bootId by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val stored = readVpnRestartSettings().getOrElse {
            UiTrace.log("VpnRestart", "Ошибка чтения настройки: ${it.message}")
            return@LaunchedEffect
        }
        if (!stored.enabled) return@LaunchedEffect
        val boot = currentVpnBootId().getOrElse {
            UiTrace.log("VpnRestart", "Ошибка определения загрузки ОС: ${it.message}")
            return@LaunchedEffect
        }
        val needed = shouldPromptVpnAfterReboot(stored, boot)
        UiTrace.log("VpnRestart", "enabled=true prompt=$needed profile=${stored.profileId}")
        if (needed) { bootId = boot; settings = stored }
    }

    val pending = settings ?: return
    Dialog(onDismissRequest = {
        if (!busy) {
            UiTrace.log("VpnRestart", "Окно закрыто без подключения; запрос сохранён для следующего запуска")
            settings = null
        }
    }) {
        Column(
            Modifier.fillMaxWidth().background(DemoColors.panelNavy, RoundedCornerShape(18.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            BasicText("VPN после перезагрузки", style = TextStyle(DemoColors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold))
            BasicText("Устройство перезагружено. Включите тестовый VPN для профиля ${pending.profileId}.", style = TextStyle(DemoColors.textMuted, fontSize = 15.sp))
            error?.let { BasicText(it, style = TextStyle(DemoColors.statusYellow, fontSize = 14.sp)) }
            BasicText(
                if (busy) "Подключение…" else "Включить VPN",
                modifier = Modifier.fillMaxWidth().background(DemoColors.buttonIndigo, RoundedCornerShape(10.dp))
                    .clickable(enabled = !busy) {
                        scope.launch {
                            busy = true
                            error = null
                            UiTrace.log("VpnRestart", "Запрошено подключение профиля ${pending.profileId}")
                            val result = if (controller.connectionState.value == VpnConnectionState.Connected) Result.success(Unit)
                                else controller.connect(pending.profileId)
                            result.fold(
                                onSuccess = {
                                    saveVpnRestartSettings(pending.copy(handledBootId = bootId)).fold(
                                        onSuccess = { UiTrace.log("VpnRestart", "Подключение подтверждено и загрузка отмечена"); settings = null },
                                        onFailure = { error = "VPN включён, но не удалось сохранить состояние: ${it.message}"; UiTrace.logError("VpnRestart", it) },
                                    )
                                },
                                onFailure = { error = "Не удалось включить VPN: ${it.message ?: "неизвестная ошибка"}. Повторите попытку."; UiTrace.logError("VpnRestart", it) },
                            )
                            busy = false
                        }
                    }.padding(14.dp),
                style = TextStyle(DemoColors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}
