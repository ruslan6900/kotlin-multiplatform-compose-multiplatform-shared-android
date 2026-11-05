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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurorakmpdemo.ui.theme.DemoColors

private enum class AuroraRootPage(val route: String) {
    Home("home"), Files("files"), Database("database"), Compatibility("libraries"), Resources("resources"), Images("images"), Fonts("fonts"),
}

/** Aurora navigation that uses no Compose string or drawable resources. */
@Composable
fun AuroraRootScreen() {
    val navController = rememberNavController()
    LaunchedEffect(Unit) {
        UiTrace.log("AuroraRoot", "Navigation root composition applied; start=${AuroraRootPage.Home.route}")
    }
    fun open(page: AuroraRootPage) {
        UiTrace.log("AuroraNavigation", "navigate=${page.route}")
        navController.navigate(page.route) { launchSingleTop = true }
    }
    fun backToHome() { navController.popBackStack(AuroraRootPage.Home.route, inclusive = false) }
    NavHost(navController = navController, startDestination = AuroraRootPage.Home.route) {
        composable(AuroraRootPage.Home.route) { AuroraHome(onOpen = ::open) }
        composable(AuroraRootPage.Files.route) { FileManagerScreen(onBack = ::backToHome) }
        composable(AuroraRootPage.Database.route) { AuroraDatabaseScreen(onBack = ::backToHome) }
        composable(AuroraRootPage.Compatibility.route) { AuroraCompatibilityScreen(onBack = ::backToHome) }
        composable(AuroraRootPage.Resources.route) {
            AuroraResourcesScreen(onBack = ::backToHome, onOpenImages = { open(AuroraRootPage.Images) }, onOpenFonts = { open(AuroraRootPage.Fonts) })
        }
        composable(AuroraRootPage.Images.route) { AuroraImagesScreen(onBack = { navController.popBackStack() }) }
        composable(AuroraRootPage.Fonts.route) { AuroraFontsScreen(onBack = { navController.popBackStack() }) }
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
        AuroraRouteCard("Проверка библиотек", "Koin, Room и стандартный Kotlin API", { onOpen(AuroraRootPage.Compatibility) })
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
