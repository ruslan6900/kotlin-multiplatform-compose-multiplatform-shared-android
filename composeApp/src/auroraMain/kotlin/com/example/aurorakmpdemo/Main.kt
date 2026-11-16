package com.example.aurorakmpdemo

import androidx.compose.ui.window.application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.aurorakmpdemo.ui.LocalAuroraKeyboardHeightPx
import com.example.aurorakmpdemo.ui.UiTrace
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import platform.posix.setenv
import platform.posix.getenv
import platform.posix.access
import platform.posix.F_OK
import ru.auroraos.kmp.dbus.DBusClient
import ru.auroraos.kmp.dbus.dbus.DBusType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import com.example.aurorakmpdemo.platform.runDemoTaskAction
import com.example.aurorakmpdemo.platform.startDialogAnswerService
import ru.auroraos.kmp.keyboard.maliit.Keyboard

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    if ("--dialog-service" in args) {
        kotlinx.coroutines.runBlocking {
            val service = startDialogAnswerService(this, stopAfterOneRequest = true)
            kotlinx.coroutines.withTimeoutOrNull(45_000) { service.join() }
            service.cancelAndJoin()
        }
        return
    }
    // Headless smoke check goes through the same sandbox and D-Bus path as the UI.
    val vpnRestartProbe = getenv("AURORA_VPN_RESTART_PROBE")?.toKString()
    if (!vpnRestartProbe.isNullOrBlank()) {
        val boot = com.example.aurorakmpdemo.platform.currentVpnBootId()
        val settings = com.example.aurorakmpdemo.platform.readVpnRestartSettings()
        println("[VpnRestart][probe] boot=$boot settings=$settings")
        if (vpnRestartProbe == "roundtrip") {
            val original = settings.getOrThrow()
            val id = boot.getOrThrow()
            try {
                val test = com.example.aurorakmpdemo.platform.VpnRestartSettings(true, "test-profile", id)
                com.example.aurorakmpdemo.platform.saveVpnRestartSettings(test).getOrThrow()
                check(com.example.aurorakmpdemo.platform.readVpnRestartSettings().getOrThrow() == test)
                check(!com.example.aurorakmpdemo.platform.shouldPromptVpnAfterReboot(test, id))
                check(com.example.aurorakmpdemo.platform.shouldPromptVpnAfterReboot(test, "00000000-0000-0000-0000-000000000000"))
                println("[VpnRestart][probe] roundtrip=PASS sameBoot=PASS changedBoot=PASS")
            } finally { com.example.aurorakmpdemo.platform.saveVpnRestartSettings(original).getOrThrow() }
        }
        return
    }
    val testAction = getenv("AURORA_DEMO_TASK_ACTION")?.toKString()
    if (!testAction.isNullOrBlank()) {
        val result = runBlocking { runDemoTaskAction(testAction) }
        println("[SystemEvents][headless] action=$testAction result=$result")
        return
    }

    // APM normally supplies these; the experimental system RPM is installed outside APM.
    setenv("AURORA_APP_ID", "com.example.aurorakmpdemo", 0)
    setenv("AURORA_PACKAGE_NAME", "com.example.aurorakmpdemo", 0)

    // APM may sweep metadata for a system RPM after every close. Restore only the
    // fixed manifest through the development bridge before the window reads it.
    val manifest = "/opt/app/com.example.aurorakmpdemo/current/manifest/com.example.aurorakmpdemo.desktop"
    if (access(manifest, F_OK) != 0) {
        runCatching {
            runBlocking { DBusClient.call(DBusType.SYSTEM, "com.example.aurorakmpdemo.VpnBridge", "/com/example/aurorakmpdemo/VpnBridge", "com.example.aurorakmpdemo.VpnBridge", "EnsureRuntimeManifest", 3000) }
        }.onFailure { println("[App] Manifest restoration failed: ${it.message}") }
    }

    // В emulator Aurora 5.2 нет работоспособного пути Zink/EGL внутри Sailjail.
    // Явный программный backend Skiko сохраняет обычный sandbox, включая
    // VPN.permission, и помогает избежать пустого окна Compose.
    setenv("SKIKO_RENDER_API", "SOFTWARE", 1)
    application {
        var keyboardHeight by remember { mutableIntStateOf(0) }
        var lastKeyboardHeight by remember { mutableIntStateOf(0) }
        val keyboardScope = rememberCoroutineScope()
        DisposableEffect(Unit) {
            val answerService = startDialogAnswerService(keyboardScope)
            onDispose { kotlinx.coroutines.runBlocking { answerService.cancelAndJoin() } }
        }
        DisposableEffect(Unit) {
            UiTrace.log("Keyboard", "Maliit: подписка на изменения состояния")
            val listener = Keyboard.listenState { id, event ->
                val height = event.height.toInt()
                keyboardScope.launch {
                    if (height > 0) lastKeyboardHeight = height
                    val open = Keyboard.isOpen()
                    keyboardHeight = if (!open) 0 else if (height > 0) height else lastKeyboardHeight
                    UiTrace.log("Keyboard", "Maliit: событие=$id, visible=$open, heightPx=$height, cachedPx=$lastKeyboardHeight, resultPx=$keyboardHeight")
                }
            }
            onDispose {
                Keyboard.unlistenState(listener)
                UiTrace.log("Keyboard", "Maliit: подписка снята, listener=$listener")
            }
        }
        LaunchedEffect(Unit) {
            var previousState = ""
            while (true) {
                val open = Keyboard.isOpen()
                val height = Keyboard.height().toInt()
                if (height > 0) lastKeyboardHeight = height
                val effective = if (!open) 0 else if (height > 0) height else lastKeyboardHeight
                val state = "visible=$open, heightPx=$height, cachedPx=$lastKeyboardHeight, resultPx=$effective"
                if (state != previousState) {
                    UiTrace.log("Keyboard", "Maliit: опрос $state")
                    previousState = state
                }
                keyboardHeight = effective
                delay(50)
            }
        }
        CompositionLocalProvider(LocalAuroraKeyboardHeightPx provides keyboardHeight) { App() }
    }
}
