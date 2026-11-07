package com.example.aurorakmpdemo

import androidx.compose.ui.window.application
import platform.posix.setenv

fun main() {
    // В emulator Aurora 5.2 нет работоспособного пути Zink/EGL внутри Sailjail.
    // Явный программный backend Skiko сохраняет обычный sandbox, включая
    // VPN.permission, и помогает избежать пустого окна Compose.
    setenv("SKIKO_RENDER_API", "SOFTWARE", 1)
    application {
        App()
    }
}
