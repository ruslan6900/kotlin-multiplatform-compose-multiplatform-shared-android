package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable

enum class AppText {
    NavHome,
    NavNetwork,
    NavDatabase,
    HomeTitle,
    HomeSubtitle,
    NetworkTitle,
    NetworkSubtitle,
    NetworkButton,
    DatabaseTitle,
    DatabaseSubtitle,
    DatabaseButton,
    DatabaseInputLabel,
    DatabaseInputPlaceholder,
    DatabaseSaveButton,
    DatabaseLatestLabel,
}

@Composable
expect fun appString(text: AppText): String
