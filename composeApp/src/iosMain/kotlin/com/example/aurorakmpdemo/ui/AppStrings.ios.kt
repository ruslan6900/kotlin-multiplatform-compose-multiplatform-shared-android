package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable
import com.example.aurorakmpdemo.resources.Res
import com.example.aurorakmpdemo.resources.database_button
import com.example.aurorakmpdemo.resources.database_input_label
import com.example.aurorakmpdemo.resources.database_input_placeholder
import com.example.aurorakmpdemo.resources.database_latest_label
import com.example.aurorakmpdemo.resources.database_save_button
import com.example.aurorakmpdemo.resources.database_subtitle
import com.example.aurorakmpdemo.resources.database_title
import com.example.aurorakmpdemo.resources.home_subtitle
import com.example.aurorakmpdemo.resources.home_title
import com.example.aurorakmpdemo.resources.nav_database
import com.example.aurorakmpdemo.resources.nav_home
import com.example.aurorakmpdemo.resources.nav_network
import com.example.aurorakmpdemo.resources.network_button
import com.example.aurorakmpdemo.resources.network_subtitle
import com.example.aurorakmpdemo.resources.network_title
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun appString(text: AppText): String = when (text) {
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
