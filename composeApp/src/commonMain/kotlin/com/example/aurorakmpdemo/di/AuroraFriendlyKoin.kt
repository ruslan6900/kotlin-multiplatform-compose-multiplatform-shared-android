package com.example.aurorakmpdemo.di

import org.koin.core.context.startKoin

object AuroraFriendlyKoin {
    private var started = false

    fun ensureStarted(): Boolean {
        if (!started) {
            startKoin {
                modules(appModule())
            }
            started = true
        }
        return started
    }

    fun isStarted(): Boolean = started
}
