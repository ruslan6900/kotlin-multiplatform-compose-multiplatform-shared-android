package com.example.aurorakmpdemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import com.example.aurorakmpdemo.di.AuroraFriendlyKoin
import com.example.aurorakmpdemo.platform.shouldUseAuroraRenderSmokeTest
import com.example.aurorakmpdemo.ui.AuroraBootMode
import com.example.aurorakmpdemo.ui.AuroraRenderSmokeScreen
import com.example.aurorakmpdemo.ui.AuroraStagedBootContainer
import com.example.aurorakmpdemo.ui.AuroraSuperSmokeScreen
import com.example.aurorakmpdemo.ui.UiTrace
import kotlin.time.Clock

@Composable
fun App() {
    val useAuroraFriendlyBoot = shouldUseAuroraRenderSmokeTest()
    var bootMode by remember {
        mutableStateOf(
            if (useAuroraFriendlyBoot) {
                AuroraBootMode.SuperSmoke
            } else {
                AuroraBootMode.NormalDemo
            },
        )
    }

    val startupId = remember {
        "launch-${Clock.System.now().toEpochMilliseconds()}"
    }

    UiTrace.log("App", "startupId=$startupId bootMode=$bootMode logPath=${UiTrace.currentLogPath()}")

    if (useAuroraFriendlyBoot) {
        remember {
            if (AuroraFriendlyKoin.ensureStarted()) {
                UiTrace.log("App", "Global Koin started for Aurora-friendly boot")
            }
            true
        }
    }

    when (bootMode) {
        AuroraBootMode.SuperSmoke -> {
            AuroraStagedBootContainer(
                startupId = startupId,
                stageLabel = "Starting Aurora super smoke...",
            ) {
                UiTrace.log("App", "rendering super-smoke for $startupId")
                AuroraSuperSmokeScreen(
                    startupId = startupId,
                    lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                    onOpenRenderSmoke = { bootMode = AuroraBootMode.RenderSmoke },
                    onOpenNormalDemo = { bootMode = AuroraBootMode.NormalDemo },
                )
            }
            return
        }

        AuroraBootMode.RenderSmoke -> {
            AuroraStagedBootContainer(
                startupId = startupId,
                stageLabel = "Starting Aurora dashboard...",
            ) {
                UiTrace.log("App", "rendering render-smoke for $startupId")
                AuroraRenderSmokeScreen(
                    startupId = startupId,
                    lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                    onOpenSuperSmoke = { bootMode = AuroraBootMode.SuperSmoke },
                    onOpenNormalDemo = { bootMode = AuroraBootMode.NormalDemo },
                )
            }
            return
        }

        AuroraBootMode.NormalDemo -> {
            AuroraStagedBootContainer(
                startupId = startupId,
                stageLabel = "Starting Aurora normal demo...",
            ) {
                UiTrace.log("App", "rendering Aurora-friendly normal demo for $startupId")
                key(startupId) {
                    Surface {
                        AuroraRenderSmokeScreen(
                            startupId = startupId,
                            lifecycleSummary = "log=${UiTrace.currentLogPath()}",
                            modeTitle = "Normal demo mode",
                            dashboardTitle = "Aurora KMP Demo App",
                            koinStatusLabel = "Initialized",
                            onOpenSuperSmoke = { bootMode = AuroraBootMode.SuperSmoke },
                        )
                    }
                }
            }
        }
    }
}
