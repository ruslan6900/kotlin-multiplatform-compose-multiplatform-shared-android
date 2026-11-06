package com.example.aurorakmpdemo.data

actual fun createPlatformVpnController(): VpnController = SimulatedVpnController()
