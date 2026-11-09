package com.example.aurorakmpdemo.platform

// null означает, что проверка .so для этой платформы не реализована.
data class NativeInteropCheck(val name: String, val success: Boolean, val detail: String)
expect fun runNativeInteropProbe(): List<NativeInteropCheck>?
