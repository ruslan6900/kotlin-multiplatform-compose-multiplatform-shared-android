package com.example.aurorakmpdemo

import androidx.compose.ui.window.application
import platform.posix.setenv

fun main() {
    // Aurora 5.2 emulator has no working Zink/EGL path inside Sailjail.  Skiko
    // supports an explicit software backend, which keeps normal sandboxing
    // (including VPN.permission) intact while avoiding a blank Compose window.
    setenv("SKIKO_RENDER_API", "SOFTWARE", 1)
    application {
        App()
    }
}
