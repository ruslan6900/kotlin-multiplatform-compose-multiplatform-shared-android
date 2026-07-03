package com.example.aurorakmpdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aurorakmpdemo.di.AuroraFriendlyKoin
import com.example.aurorakmpdemo.platform.configuredDrawableExperiment
import com.example.aurorakmpdemo.platform.shouldUseAuroraRenderSmokeTest
import com.example.aurorakmpdemo.resources.*
import com.example.aurorakmpdemo.ui.AuroraRenderSmokeScreen
import com.example.aurorakmpdemo.ui.AuroraStagedBootContainer
import com.example.aurorakmpdemo.ui.AuroraSuperSmokeScreen
import com.example.aurorakmpdemo.ui.DrawableExperimentScreen
import com.example.aurorakmpdemo.ui.NormalDemoContent
import com.example.aurorakmpdemo.ui.UiTrace
import com.example.aurorakmpdemo.ui.theme.DemoColors
import com.example.aurorakmpdemo.ui.theme.DemoTheme
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource

private enum class AppRoute(val route: String) {
    SuperSmoke("supersmoke"),
    RenderSmoke("rendersmoke"),
    DrawableExperiment("drawableexperiment"),
    NormalDemo("normaldemo"),
}

@Composable
fun App() {
    DemoTheme {
        val useAuroraFriendlyBoot = shouldUseAuroraRenderSmokeTest()
        val configuredDrawableExperiment = configuredDrawableExperiment()
        val startRoute = if (useAuroraFriendlyBoot) {
            AppRoute.SuperSmoke.route
        } else {
            AppRoute.NormalDemo.route
        }

        val startupId = remember {
            "launch-${Clock.System.now().toEpochMilliseconds()}"
        }
        val navController = rememberNavController()

        UiTrace.log("App", "startupId=$startupId startRoute=$startRoute logPath=${UiTrace.currentLogPath()}")

        if (useAuroraFriendlyBoot) {
            remember {
                if (AuroraFriendlyKoin.ensureStarted()) {
                    UiTrace.log("App", "Global Koin started for Aurora-friendly boot")
                }
                true
            }
        }

        NavHost(
            navController = navController,
            startDestination = startRoute,
        ) {
            composable(AppRoute.SuperSmoke.route) {
                AuroraStagedBootContainer(
                    startupId = startupId,
                    stageLabel = stringResource(Res.string.starting_super_smoke),
                ) {
                    UiTrace.log("App", "rendering super-smoke for $startupId")
                    AuroraSuperSmokeScreen(
                        startupId = startupId,
                        lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                        onOpenRenderSmoke = {
                            navController.navigate(AppRoute.RenderSmoke.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenNormalDemo = {
                            navController.navigate(AppRoute.NormalDemo.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenDrawableExperiments = {
                            navController.navigate(AppRoute.DrawableExperiment.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            composable(AppRoute.RenderSmoke.route) {
                AuroraStagedBootContainer(
                    startupId = startupId,
                    stageLabel = stringResource(Res.string.starting_dashboard),
                ) {
                    UiTrace.log("App", "rendering render-smoke for $startupId")
                    AuroraRenderSmokeScreen(
                        startupId = startupId,
                        lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                        onOpenSuperSmoke = {
                            navController.navigate(AppRoute.SuperSmoke.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenNormalDemo = {
                            navController.navigate(AppRoute.NormalDemo.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            composable(AppRoute.DrawableExperiment.route) {
                AuroraStagedBootContainer(
                    startupId = startupId,
                    stageLabel = stringResource(Res.string.starting_drawable_experiments),
                ) {
                    UiTrace.log("App", "rendering drawable experiments for $startupId mode=$configuredDrawableExperiment")
                    DrawableExperimentScreen(
                        startupId = startupId,
                        initialCaseKey = configuredDrawableExperiment,
                        onBack = {
                            navController.navigate(AppRoute.SuperSmoke.route) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            composable(AppRoute.NormalDemo.route) {
                AuroraStagedBootContainer(
                    startupId = startupId,
                    stageLabel = stringResource(Res.string.starting_normal_demo),
                    backgroundColor = DemoColors.normalDemoBackground,
                ) {
                    UiTrace.log("App", "rendering Aurora-friendly normal demo for $startupId")
                    key(startupId) {
                        NormalDemoContent(
                            startupId = startupId,
                            lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                            modeTitle = stringResource(Res.string.mode_normal_demo),
                            dashboardTitle = stringResource(Res.string.dashboard_normal_title),
                            koinStatusLabel = stringResource(Res.string.status_value_initialized),
                            onOpenSuperSmoke = {
                                navController.navigate(AppRoute.SuperSmoke.route) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
