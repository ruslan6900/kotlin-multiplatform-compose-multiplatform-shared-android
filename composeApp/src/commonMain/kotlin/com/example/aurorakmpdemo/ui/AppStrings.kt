package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable
import com.example.aurorakmpdemo.resources.*
import org.jetbrains.compose.resources.stringResource

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
fun appString(text: AppText): String = when (text) {
    AppText.NavHome -> stringResource(Res.string.nav_home)
    AppText.NavNetwork -> stringResource(Res.string.nav_network)
    AppText.NavDatabase -> stringResource(Res.string.nav_database)
    AppText.HomeTitle -> stringResource(Res.string.home_title)
    AppText.HomeSubtitle -> stringResource(Res.string.home_subtitle)
    AppText.NetworkTitle -> stringResource(Res.string.network_title)
    AppText.NetworkSubtitle -> stringResource(Res.string.network_subtitle)
    AppText.NetworkButton -> stringResource(Res.string.network_button)
    AppText.DatabaseTitle -> stringResource(Res.string.database_title)
    AppText.DatabaseSubtitle -> stringResource(Res.string.database_subtitle)
    AppText.DatabaseButton -> stringResource(Res.string.database_button)
    AppText.DatabaseInputLabel -> stringResource(Res.string.database_input_label)
    AppText.DatabaseInputPlaceholder -> stringResource(Res.string.database_input_placeholder)
    AppText.DatabaseSaveButton -> stringResource(Res.string.database_save_button)
    AppText.DatabaseLatestLabel -> stringResource(Res.string.database_latest_label)
}
