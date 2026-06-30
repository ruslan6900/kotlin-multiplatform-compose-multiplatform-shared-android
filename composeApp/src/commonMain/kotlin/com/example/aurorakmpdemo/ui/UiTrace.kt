package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import com.example.aurorakmpdemo.platform.appendPlatformDiagnosticLog
import com.example.aurorakmpdemo.platform.currentProcessId
import com.example.aurorakmpdemo.platform.diagnosticLogPath
import kotlin.time.Clock

object UiTrace {
    fun log(tag: String, message: String) {
        val line = "[${Clock.System.now()}][pid=${currentProcessId()}][$tag] $message"
        println(line)
        appendPlatformDiagnosticLog(line)
    }

    fun logError(tag: String, throwable: Throwable) {
        log(tag, "ERROR ${throwable::class.simpleName}: ${throwable.message ?: "unknown"}")
    }

    fun currentLogPath(): String = diagnosticLogPath() ?: "console-only"
}

@Composable
fun TraceComposableLifecycle(tag: String, details: String) {
    DisposableEffect(tag, details) {
        UiTrace.log(tag, "enter $details")
        onDispose {
            UiTrace.log(tag, "dispose $details")
        }
    }
    SideEffect {
        UiTrace.log(tag, "sideEffect $details")
    }
}
