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
import kotlinx.coroutines.launch
import platform.posix.setenv
import ru.auroraos.kmp.keyboard.maliit.Keyboard

fun main() {
    // В emulator Aurora 5.2 нет работоспособного пути Zink/EGL внутри Sailjail.
    // Явный программный backend Skiko сохраняет обычный sandbox, включая
    // VPN.permission, и помогает избежать пустого окна Compose.
    setenv("SKIKO_RENDER_API", "SOFTWARE", 1)
    application {
        var keyboardHeight by remember { mutableIntStateOf(0) }
        var lastKeyboardHeight by remember { mutableIntStateOf(0) }
        val keyboardScope = rememberCoroutineScope()
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
