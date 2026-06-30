package com.example.aurorakmpdemo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Aurora KMP Demo",
    ) {
        App()
    }
}
