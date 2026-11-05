package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurorakmpdemo.ui.theme.DemoColors

private enum class AuroraRootPage { Home, Files, Database, Resources, Images, Fonts }

/** Aurora navigation that uses no Compose string or drawable resources. */
@Composable
fun AuroraRootScreen() {
    var page by remember { mutableStateOf(AuroraRootPage.Home) }
    LaunchedEffect(Unit) {
        UiTrace.log("AuroraRoot", "root composition applied; page=$page")
    }
    when (page) {
        AuroraRootPage.Home -> AuroraHome(onOpen = { page = it })
        AuroraRootPage.Files -> FileManagerScreen(onBack = { page = AuroraRootPage.Home })
        AuroraRootPage.Database -> AuroraDatabaseScreen(onBack = { page = AuroraRootPage.Home })
        AuroraRootPage.Resources -> AuroraResourcesScreen(
            onBack = { page = AuroraRootPage.Home },
            onOpenImages = { page = AuroraRootPage.Images },
            onOpenFonts = { page = AuroraRootPage.Fonts },
        )
        AuroraRootPage.Images -> AuroraImagesScreen(onBack = { page = AuroraRootPage.Home })
        AuroraRootPage.Fonts -> AuroraFontsScreen(onBack = { page = AuroraRootPage.Resources })
    }
}

@Composable
private fun AuroraHome(onOpen: (AuroraRootPage) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DemoColors.normalDemoBackground).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText("Aurora KMP Demo", style = TextStyle(DemoColors.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold))
        BasicText("Главная страница", style = TextStyle(DemoColors.textMuted, fontSize = 16.sp))
        AuroraRouteCard("Файловый менеджер", "Каталоги и операции с файлами", { onOpen(AuroraRootPage.Files) })
        AuroraRouteCard("База данных", "Проверка локального Room-хранилища", { onOpen(AuroraRootPage.Database) })
        AuroraRouteCard("Ресурсы", "Изображения и шрифты из composeResources", { onOpen(AuroraRootPage.Resources) })
    }
}

@Composable
private fun AuroraRouteCard(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(DemoColors.panelNavy, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(title, style = TextStyle(DemoColors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold))
        BasicText(subtitle, style = TextStyle(DemoColors.textMuted, fontSize = 14.sp))
    }
}
