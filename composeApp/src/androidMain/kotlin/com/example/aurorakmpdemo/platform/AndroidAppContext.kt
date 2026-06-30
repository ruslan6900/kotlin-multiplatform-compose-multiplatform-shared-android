package com.example.aurorakmpdemo.platform

import android.content.Context

object AndroidAppContext {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun require(): Context = checkNotNull(appContext) {
        "AndroidAppContext is not initialized. Call AndroidAppContext.initialize() before creating storage."
    }
}
