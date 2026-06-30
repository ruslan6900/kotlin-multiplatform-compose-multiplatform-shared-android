package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable

@Composable
actual fun appString(text: AppText): String = when (text) {
    AppText.NavHome -> "Home"
    AppText.NavNetwork -> "Network"
    AppText.NavDatabase -> "Database"
    AppText.HomeTitle -> "Home"
    AppText.HomeSubtitle -> "Current time and platform diagnostics"
    AppText.NetworkTitle -> "Network"
    AppText.NetworkSubtitle -> "Load a sample post with Ktor"
    AppText.NetworkButton -> "Load post"
    AppText.DatabaseTitle -> "Database"
    AppText.DatabaseSubtitle -> "Save text to Room and restore it from storage"
    AppText.DatabaseButton -> "Refresh storage"
    AppText.DatabaseInputLabel -> "Text to save"
    AppText.DatabaseInputPlaceholder -> "Type a note here"
    AppText.DatabaseSaveButton -> "Save latest text"
    AppText.DatabaseLatestLabel -> "Latest saved text:"
}
