package com.example.aurorakmpdemo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aurorakmpdemo.ui.theme.DemoColors

@Composable
fun NormalDemoContent(
    startupId: String,
    lifecycleSummary: String,
    modeTitle: String,
    dashboardTitle: String,
    koinStatusLabel: String,
    onOpenSuperSmoke: (() -> Unit)? = null,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuroraDemoRoute.Home.route,
    ) {
        AuroraDemoRoute.entries.forEach { route ->
            composable(route.route) {
                AuroraRenderSmokeScreen(
                    startupId = startupId,
                    lifecycleSummary = lifecycleSummary,
                    modeTitle = modeTitle,
                    dashboardTitle = dashboardTitle,
                    koinStatusLabel = koinStatusLabel,
                    backgroundColor = DemoColors.normalDemoBackground,
                    page = route,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSuperSmoke = onOpenSuperSmoke,
                    onOpenNormalDemo = if (route != AuroraDemoRoute.Home) null else ({
                        navController.navigate(AuroraDemoRoute.Home.route) {
                            launchSingleTop = true
                        }
                    }),
                )
            }
        }
    }
}
