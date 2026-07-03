package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aurorakmpdemo.data.Post
import com.example.aurorakmpdemo.data.PostsRepository
import com.example.aurorakmpdemo.data.buildHttpClient
import com.example.aurorakmpdemo.data.currentTimeTickerFlow
import com.example.aurorakmpdemo.data.formattedCurrentTime
import com.example.aurorakmpdemo.di.AuroraFriendlyKoin
import com.example.aurorakmpdemo.platform.PlatformDiagnosticsProvider
import com.example.aurorakmpdemo.platform.createPostsStorage
import com.example.aurorakmpdemo.resources.*
import com.example.aurorakmpdemo.ui.theme.DemoColors
import com.example.aurorakmpdemo.ui.theme.demoTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

enum class AuroraBootMode {
    SuperSmoke,
    RenderSmoke,
    NormalDemo,
}

enum class AuroraDemoRoute(
    val route: String,
) {
    Home("home"),
    Network("network"),
    Database("database"),
}

@Composable
fun AuroraStagedBootContainer(
    startupId: String,
    stageLabel: String,
    backgroundColor: Color = DemoColors.superSmokeBackground,
    contentDelayMs: Long = 100L,
    content: @Composable () -> Unit,
) {
    TraceComposableLifecycle("AuroraStagedBootContainer", "startupId=$startupId stage=$stageLabel")
    val typography = demoTypography()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showContent by remember(startupId) { mutableStateOf(false) }
    var renderPulse by remember(startupId) { mutableStateOf(0) }
    var resumeCount by remember(startupId) { mutableStateOf(0) }
    var startCount by remember(startupId) { mutableStateOf(0) }
    var subtreeGeneration by remember(startupId) { mutableStateOf(0) }
    var lastLifecycleEvent by remember(startupId) { mutableStateOf("created") }

    LaunchedEffect(startupId, contentDelayMs) {
        UiTrace.log("AuroraStagedBootContainer", "startupId=$startupId waiting ${contentDelayMs}ms before showing content")
        if (contentDelayMs > 0) {
            delay(contentDelayMs)
        }
        showContent = true
        UiTrace.log("AuroraStagedBootContainer", "startupId=$startupId showContent=true")
    }

    DisposableEffect(lifecycleOwner, startupId) {
        val observer = LifecycleEventObserver { _, event ->
            lastLifecycleEvent = event.name
            UiTrace.log("Lifecycle", "startupId=$startupId event=${event.name}")
            when (event) {
                Lifecycle.Event.ON_START -> {
                    startCount += 1
                    subtreeGeneration += 1
                    UiTrace.log("Lifecycle", "startupId=$startupId ON_START generation=$subtreeGeneration startCount=$startCount")
                }

                Lifecycle.Event.ON_RESUME -> {
                    resumeCount += 1
                    subtreeGeneration += 1
                    UiTrace.log("Lifecycle", "startupId=$startupId ON_RESUME generation=$subtreeGeneration resumeCount=$resumeCount")
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(startupId) {
        while (true) {
            delay(1000)
            renderPulse += 1
            if (renderPulse % 5 == 0) {
                UiTrace.log("AuroraStagedBootContainer", "startupId=$startupId heartbeat=$renderPulse generation=$subtreeGeneration visible=$showContent")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        if (showContent) {
            @Suppress("UNUSED_EXPRESSION")
            renderPulse
            UiTrace.log("AuroraStagedBootContainer", "startupId=$startupId rendering subtree generation=$subtreeGeneration")
            key("$startupId-$subtreeGeneration") {
                content()
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BasicText(
                    text = stringResource(Res.string.starting_ui),
                    style = TextStyle(color = DemoColors.textPrimary).merge(typography.hero.copy(fontSize = 30.sp, lineHeight = 34.sp)),
                )
                BasicText(
                    text = stageLabel,
                    style = TextStyle(color = DemoColors.textAccent).merge(typography.subtitle.copy(fontSize = 20.sp, lineHeight = 24.sp)),
                )
                BasicText(
                    text = startupId,
                    style = TextStyle(color = DemoColors.textMuted).merge(typography.caption),
                )
                BasicText(
                    text = "start=$startCount resume=$resumeCount event=$lastLifecycleEvent gen=$subtreeGeneration",
                    style = TextStyle(color = DemoColors.textSupport).merge(typography.caption),
                )
            }
        }
    }
}

@Composable
fun AuroraSuperSmokeScreen(
    startupId: String,
    lifecycleSummary: String = "",
    onOpenRenderSmoke: () -> Unit,
    onOpenNormalDemo: (() -> Unit)? = null,
    onOpenDrawableExperiments: (() -> Unit)? = null,
) {
    TraceComposableLifecycle("AuroraSuperSmokeScreen", "startupId=$startupId")
    val typography = demoTypography()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText(
                text = stringResource(Res.string.hero_title),
                style = TextStyle(color = DemoColors.textPrimary).merge(typography.hero),
            )
            BasicText(
                text = stringResource(Res.string.mode_super_smoke),
                style = TextStyle(color = DemoColors.textAccent).merge(typography.subtitle),
            )
            BasicText(
                text = startupId,
                style = TextStyle(color = DemoColors.textMuted).merge(typography.caption),
            )
            if (lifecycleSummary.isNotBlank()) {
                BasicText(
                    text = lifecycleSummary,
                    style = TextStyle(color = DemoColors.textSupport).merge(typography.caption),
                )
            }
            SafePlaceholderBadge(modifier = Modifier.size(96.dp))
            DiagnosticButton(
                label = stringResource(Res.string.open_render_smoke),
                background = DemoColors.buttonBlue,
                onClick = {
                    UiTrace.log("AuroraSuperSmokeScreen", "startupId=$startupId Open render smoke tapped")
                    onOpenRenderSmoke()
                },
            )
            onOpenNormalDemo?.let { openNormalDemo ->
                DiagnosticButton(
                    label = stringResource(Res.string.open_normal_demo),
                    background = DemoColors.buttonOrange,
                    onClick = {
                        UiTrace.log("AuroraSuperSmokeScreen", "startupId=$startupId Open normal demo tapped")
                        openNormalDemo()
                    },
                )
            }
            onOpenDrawableExperiments?.let { openDrawableExperiments ->
                DiagnosticButton(
                    label = stringResource(Res.string.open_drawable_experiments),
                    background = DemoColors.buttonPurple,
                    onClick = {
                        UiTrace.log("AuroraSuperSmokeScreen", "startupId=$startupId Open drawable experiments tapped")
                        openDrawableExperiments()
                    },
                )
            }
        }
    }
}

@Composable
fun AuroraRenderSmokeScreen(
    startupId: String,
    lifecycleSummary: String = "",
    modeTitle: String = stringResource(Res.string.mode_render_smoke),
    dashboardTitle: String = stringResource(Res.string.dashboard_smoke_title),
    koinStatusLabel: String = stringResource(Res.string.status_value_not_initialized_smoke),
    backgroundColor: Color = DemoColors.superSmokeBackground,
    page: AuroraDemoRoute = AuroraDemoRoute.Home,
    onNavigate: ((AuroraDemoRoute) -> Unit)? = null,
    onOpenSuperSmoke: (() -> Unit)? = null,
    onOpenNormalDemo: (() -> Unit)? = null,
) {
    TraceComposableLifecycle("AuroraRenderSmokeScreen", "startupId=$startupId")
    val typography = demoTypography()
    val notCheckedLabel = stringResource(Res.string.status_value_not_checked)
    val initializedLabel = stringResource(Res.string.status_value_initialized)
    val notInitializedLabel = stringResource(Res.string.status_value_not_initialized)
    val runningLabel = stringResource(Res.string.status_value_running)
    val loadingLabel = stringResource(Res.string.status_value_loading)
    val savedFetchedPostLabel = stringResource(Res.string.status_value_saved_fetched_post)
    val manualSaveSuccessLabel = stringResource(Res.string.status_value_manual_save_success)
    val readSuccessLabel = stringResource(Res.string.status_value_read_success)
    val readEmptyLabel = stringResource(Res.string.status_value_read_empty)
    val latestNoteSavedLabel = stringResource(Res.string.status_value_latest_note_saved)
    val latestNoteRestoredLabel = stringResource(Res.string.status_value_latest_note_restored)
    val noLatestNoteLabel = stringResource(Res.string.status_value_no_latest_note)
    val noteEmptyLabel = stringResource(Res.string.status_value_note_empty)
    val unknownPlatformLabel = stringResource(Res.string.platform_unknown)
    val waitingDiagnosticsLabel = stringResource(Res.string.platform_waiting_details)
    val diagnosticsProvider = remember { PlatformDiagnosticsProvider() }
    val storage = remember { createPostsStorage() }
    val client = remember { buildHttpClient() }
    val repository = remember { PostsRepository(client, storage) }
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }

    var currentTime by remember { mutableStateOf("--:--:--") }
    var platformName by remember { mutableStateOf(unknownPlatformLabel) }
    var platformDetails by remember { mutableStateOf(listOf(waitingDiagnosticsLabel)) }
    var koinStatus by remember { mutableStateOf(koinStatusLabel) }
    var ktorStatus by remember { mutableStateOf(notCheckedLabel) }
    var roomStatus by remember { mutableStateOf(notCheckedLabel) }
    var lastPost by remember { mutableStateOf<Post?>(null) }
    var latestNote by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    DisposableEffect(client) {
        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId HttpClient created")
        onDispose {
            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId HttpClient disposing")
            client.close()
        }
    }

    LaunchedEffect(Unit) {
        val diagnostics = diagnosticsProvider.snapshot()
        platformName = diagnostics.platform
        platformDetails = diagnostics.details
        koinStatus = if (AuroraFriendlyKoin.isStarted()) initializedLabel else notInitializedLabel
        latestNote = storage.getLatestNote().orEmpty()
        logs.add("[${nowStamp()}] App started")
        logs.add("[${nowStamp()}] $modeTitle booted")
        logs.add("[${nowStamp()}] Startup id: $startupId")
        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId diagnostics loaded platform=$platformName logPath=${UiTrace.currentLogPath()}")
    }

    LaunchedEffect(Unit) {
        currentTimeTickerFlow().collect { value ->
            currentTime = value
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText(
                text = stringResource(Res.string.hero_title),
                style = TextStyle(color = DemoColors.textPrimary).merge(typography.hero),
            )

            BasicText(
                text = modeTitle,
                style = TextStyle(color = DemoColors.textAccent).merge(typography.subtitle.copy(fontSize = 20.sp, lineHeight = 24.sp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SafePlaceholderBadge(modifier = Modifier.size(56.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BasicText(
                        text = dashboardTitle,
                        style = TextStyle(color = DemoColors.textSupport).merge(typography.section.copy(fontSize = 24.sp, lineHeight = 28.sp)),
                    )
                    BasicText(
                        text = stringResource(Res.string.app_subtitle),
                        style = TextStyle(color = DemoColors.textMuted).merge(typography.caption),
                    )
                }
            }

            BasicText(
                text = startupId,
                style = TextStyle(color = DemoColors.textMuted).merge(typography.caption),
            )

            if (lifecycleSummary.isNotBlank()) {
                BasicText(
                    text = lifecycleSummary,
                    style = TextStyle(color = DemoColors.textSupport).merge(typography.caption),
                )
            }

            onNavigate?.let { navigate ->
                NavigationTabs(
                    currentPage = page,
                    onNavigate = navigate,
                )
            }

            if (page == AuroraDemoRoute.Home || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.status_title),
                    background = DemoColors.panelNavy,
                ) {
                    StatusRow(stringResource(Res.string.status_platform), platformName, DemoColors.statusBlue)
                    StatusRow(stringResource(Res.string.status_compose), runningLabel, DemoColors.statusOrange)
                    StatusRow(stringResource(Res.string.status_koin), koinStatus, DemoColors.statusPink)
                    StatusRow(stringResource(Res.string.status_ktor), ktorStatus, DemoColors.statusLime)
                    StatusRow(stringResource(Res.string.status_room), roomStatus, DemoColors.statusCyan)
                    StatusRow(stringResource(Res.string.status_datetime), currentTime, DemoColors.statusYellow)
                }
            }

            if (page == AuroraDemoRoute.Home || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.visual_diagnostics_title),
                    background = DemoColors.panelPurple,
                ) {
                    BasicText(
                        text = stringResource(Res.string.visual_diagnostics_hint),
                        style = TextStyle(color = DemoColors.textPrimary).merge(typography.body),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColorSwatch(Color.Red)
                        ColorSwatch(Color.Green)
                        ColorSwatch(Color.Blue)
                        SafePlaceholderBadge(modifier = Modifier.size(68.dp))
                    }
                }
            }

            if (page == AuroraDemoRoute.Network || page == AuroraDemoRoute.Database || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.actions_title),
                    background = DemoColors.panelGreen,
                ) {
                    ButtonRow(
                        firstLabel = stringResource(Res.string.check_ktor),
                        firstColor = DemoColors.buttonBlue,
                        firstAction = {
                            scope.launch {
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Ktor check started")
                                currentTime = nowStamp()
                                logs.add("[${nowStamp()}] Ktor check started")
                                ktorStatus = loadingLabel
                                runCatching { repository.fetchAndPersistPost(1) }
                                    .onSuccess { result ->
                                        ktorStatus = getString(Res.string.status_value_success)
                                        roomStatus = savedFetchedPostLabel
                                        lastPost = result.cached ?: result.remote
                                        logs.add("[${nowStamp()}] Ktor request success")
                                        logs.add("[${nowStamp()}] Room save success")
                                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Ktor success cached=${result.cached != null}")
                                    }
                                    .onFailure { error ->
                                        ktorStatus = getString(Res.string.status_error_prefix, error.message ?: "unknown")
                                        logs.add("[${nowStamp()}] Ktor request error: ${error.message ?: "unknown"}")
                                        UiTrace.logError("AuroraRenderSmokeScreen", error)
                                    }
                            }
                        },
                        secondLabel = stringResource(Res.string.save_test_post),
                        secondColor = DemoColors.buttonGreen,
                        secondAction = {
                            scope.launch {
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room manual save started")
                                val post = Post(
                                    userId = 42,
                                    id = 777,
                                    title = getString(Res.string.demo_saved_post_title),
                                    body = getString(Res.string.demo_saved_post_body),
                                )
                                currentTime = nowStamp()
                                runCatching { storage.savePost(post) }
                                    .onSuccess {
                                        roomStatus = manualSaveSuccessLabel
                                        lastPost = post
                                        logs.add("[${nowStamp()}] Room manual save success")
                                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room manual save success")
                                    }
                                    .onFailure { error ->
                                        roomStatus = getString(Res.string.status_save_error, error.message ?: "unknown")
                                        logs.add("[${nowStamp()}] Room manual save error: ${error.message ?: "unknown"}")
                                        UiTrace.logError("AuroraRenderSmokeScreen", error)
                                    }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ButtonRow(
                        firstLabel = stringResource(Res.string.read_saved_post),
                        firstColor = DemoColors.buttonPurple,
                        firstAction = {
                            scope.launch {
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read started")
                                currentTime = nowStamp()
                                runCatching { storage.getPost(777) ?: storage.getPost(1) }
                                    .onSuccess { post ->
                                        if (post == null) {
                                            roomStatus = readEmptyLabel
                                            logs.add("[${nowStamp()}] Room read returned empty")
                                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read empty")
                                        } else {
                                            roomStatus = readSuccessLabel
                                            lastPost = post
                                            logs.add("[${nowStamp()}] Room read success")
                                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read success")
                                        }
                                    }
                                    .onFailure { error ->
                                        roomStatus = getString(Res.string.status_read_error, error.message ?: "unknown")
                                        logs.add("[${nowStamp()}] Room read error: ${error.message ?: "unknown"}")
                                        UiTrace.logError("AuroraRenderSmokeScreen", error)
                                    }
                            }
                        },
                        secondLabel = stringResource(Res.string.clear_log),
                        secondColor = DemoColors.buttonRed,
                        secondAction = {
                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId log cleared")
                            logs.clear()
                            currentTime = nowStamp()
                            logs.add("[${nowStamp()}] Log cleared")
                        },
                    )
                    if (page == AuroraDemoRoute.Database || onNavigate == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NotePanel(
                            noteInput = noteInput,
                            latestNote = latestNote,
                            onNoteInputChange = { noteInput = it },
                            onSaveNote = {
                                scope.launch {
                                    val stamp = nowStamp()
                                    if (noteInput.isBlank()) {
                                        roomStatus = noteEmptyLabel
                                        logs.add("[$stamp] Note save skipped: empty")
                                        return@launch
                                    }
                                    runCatching {
                                        storage.saveLatestNote(noteInput)
                                        storage.getLatestNote().orEmpty()
                                    }.onSuccess { saved ->
                                        latestNote = saved
                                        roomStatus = latestNoteSavedLabel
                                        logs.add("[$stamp] Latest note saved")
                                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId latest note saved")
                                    }.onFailure { error ->
                                        roomStatus = getString(Res.string.status_note_save_error, error.message ?: "unknown")
                                        logs.add("[$stamp] Note save error: ${error.message ?: "unknown"}")
                                        UiTrace.logError("AuroraRenderSmokeScreen", error)
                                    }
                                }
                            },
                            onReadNote = {
                                scope.launch {
                                    val stamp = nowStamp()
                                    runCatching { storage.getLatestNote().orEmpty() }
                                        .onSuccess { saved ->
                                            latestNote = saved
                                            roomStatus = if (saved.isBlank()) noLatestNoteLabel else latestNoteRestoredLabel
                                            logs.add("[$stamp] Latest note restored")
                                        }
                                        .onFailure { error ->
                                            roomStatus = getString(Res.string.status_note_read_error, error.message ?: "unknown")
                                            logs.add("[$stamp] Note read error: ${error.message ?: "unknown"}")
                                            UiTrace.logError("AuroraRenderSmokeScreen", error)
                                        }
                                }
                            },
                        )
                    }
                    onOpenNormalDemo?.let { openNormalDemo ->
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagnosticButton(
                            label = stringResource(Res.string.open_normal_demo_app),
                            background = DemoColors.buttonOrange,
                            onClick = {
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Open normal demo tapped")
                                logs.add("[${nowStamp()}] Switching to normal demo app")
                                openNormalDemo()
                            },
                        )
                    }
                    onOpenSuperSmoke?.let { openSuperSmoke ->
                        Spacer(modifier = Modifier.height(12.dp))
                        DiagnosticButton(
                            label = stringResource(Res.string.back_to_super_smoke),
                            background = DemoColors.buttonIndigo,
                            onClick = {
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Back to super smoke tapped")
                                logs.add("[${nowStamp()}] Switching to super smoke")
                                openSuperSmoke()
                            },
                        )
                    }
                }
            }

            if (page == AuroraDemoRoute.Network || page == AuroraDemoRoute.Database || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.post_preview_title),
                    background = DemoColors.panelBlack,
                ) {
                    if (lastPost == null) {
                        BasicText(
                            text = stringResource(Res.string.post_empty),
                            style = TextStyle(color = DemoColors.textPrimary).merge(typography.body),
                        )
                    } else {
                        BasicText(
                            text = lastPost!!.title,
                            style = TextStyle(color = DemoColors.textPrimary).merge(typography.section.copy(fontSize = 20.sp, lineHeight = 24.sp)),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicText(
                            text = lastPost!!.body,
                            style = TextStyle(color = DemoColors.textPrimary).merge(typography.body),
                        )
                    }
                }
            }

            if (page == AuroraDemoRoute.Database || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.platform_diagnostics_title),
                    background = DemoColors.panelNavy,
                ) {
                    platformDetails.forEach { detail ->
                        BasicText(
                            text = "• $detail",
                            style = TextStyle(color = DemoColors.textMuted).merge(typography.caption.copy(fontSize = 15.sp, lineHeight = 18.sp)),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            if (page == AuroraDemoRoute.Home || page == AuroraDemoRoute.Database || onNavigate == null) {
                DiagnosticPanel(
                    title = stringResource(Res.string.runtime_log_title),
                    background = DemoColors.panelBlack,
                ) {
                    if (logs.isEmpty()) {
                        BasicText(
                            text = stringResource(Res.string.log_empty),
                            style = TextStyle(color = DemoColors.logEmpty).merge(typography.caption.copy(fontSize = 15.sp, lineHeight = 18.sp)),
                        )
                    } else {
                        logs.takeLast(12).forEach { entry ->
                            BasicText(
                                text = entry,
                                style = TextStyle(color = DemoColors.logText).merge(typography.caption),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

private enum class DrawableExperimentCase(
    val key: String,
) {
    SvgPainter("svg_painter"),
    XmlMinPainter("xml_min_painter"),
    XmlTwoPathsPainter("xml_two_paths_painter"),
    XmlStrokePainter("xml_stroke_painter"),
    XmlOriginalPainter("xml_original_painter"),
    XmlMinVector("xml_min_vector"),
    XmlTwoPathsVector("xml_two_paths_vector"),
    XmlStrokeVector("xml_stroke_vector"),
    XmlOriginalVector("xml_original_vector"),
    ;

    companion object {
        fun fromKey(value: String?): DrawableExperimentCase? = entries.firstOrNull { it.key == value }
    }
}

@Composable
fun DrawableExperimentScreen(
    startupId: String,
    initialCaseKey: String?,
    onBack: () -> Unit,
) {
    val typography = demoTypography()
    var selectedCase by remember(initialCaseKey) { mutableStateOf(DrawableExperimentCase.fromKey(initialCaseKey)) }

    LaunchedEffect(selectedCase) {
        selectedCase?.let {
            UiTrace.log("DrawableExperimentScreen", "startupId=$startupId loading drawable case=${it.key}")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.superSmokeBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BasicText(
                text = stringResource(Res.string.drawable_experiments_title),
                style = TextStyle(color = DemoColors.textPrimary).merge(typography.hero.copy(fontSize = 28.sp, lineHeight = 32.sp)),
            )
            BasicText(
                text = stringResource(Res.string.drawable_experiments_subtitle),
                style = TextStyle(color = DemoColors.textAccent).merge(typography.subtitle.copy(fontSize = 18.sp, lineHeight = 22.sp)),
            )
            BasicText(
                text = stringResource(Res.string.drawable_experiments_startup, startupId),
                style = TextStyle(color = DemoColors.textMuted).merge(typography.caption),
            )
            BasicText(
                text = stringResource(Res.string.drawable_experiments_mode, initialCaseKey ?: "manual"),
                style = TextStyle(color = DemoColors.textSupport).merge(typography.caption),
            )

            DiagnosticPanel(
                title = stringResource(Res.string.drawable_experiments_cases_title),
                background = DemoColors.panelNavigation,
            ) {
                DrawableExperimentCase.entries.forEach { candidate ->
                    DiagnosticButton(
                        label = candidate.key,
                        background = if (selectedCase == candidate) DemoColors.buttonIndigo else DemoColors.buttonMuted,
                        onClick = { selectedCase = candidate },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                DiagnosticButton(
                    label = stringResource(Res.string.back_to_super_smoke),
                    background = DemoColors.buttonOrange,
                    onClick = onBack,
                )
            }

            DiagnosticPanel(
                title = stringResource(Res.string.drawable_experiments_preview_title),
                background = DemoColors.panelPurple,
            ) {
                BasicText(
                    text = selectedCase?.key ?: stringResource(Res.string.drawable_experiments_no_case),
                    style = TextStyle(color = DemoColors.textPrimary).merge(typography.body),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFF0B1F33), RoundedCornerShape(20.dp))
                        .border(2.dp, DemoColors.statusYellow, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    selectedCase?.let { DangerousDrawablePreview(it) } ?: SafePlaceholderBadge(modifier = Modifier.size(92.dp))
                }
            }
        }
    }
}

@Composable
private fun DangerousDrawablePreview(case: DrawableExperimentCase) {
    when (case) {
        DrawableExperimentCase.SvgPainter -> {
            Image(
                painter = painterResource(Res.drawable.ic_demo_badge),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlMinPainter -> {
            Image(
                painter = painterResource(Res.drawable.ic_vector_min_square),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlTwoPathsPainter -> {
            Image(
                painter = painterResource(Res.drawable.ic_vector_two_paths),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlStrokePainter -> {
            Image(
                painter = painterResource(Res.drawable.ic_vector_stroke_cross),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlOriginalPainter -> {
            Image(
                painter = painterResource(Res.drawable.ic_vector_original_badge),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlMinVector -> {
            Image(
                painter = rememberVectorPainter(vectorResource(Res.drawable.ic_vector_min_square)),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlTwoPathsVector -> {
            Image(
                painter = rememberVectorPainter(vectorResource(Res.drawable.ic_vector_two_paths)),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlStrokeVector -> {
            Image(
                painter = rememberVectorPainter(vectorResource(Res.drawable.ic_vector_stroke_cross)),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }

        DrawableExperimentCase.XmlOriginalVector -> {
            Image(
                painter = rememberVectorPainter(vectorResource(Res.drawable.ic_vector_original_badge)),
                contentDescription = case.key,
                modifier = Modifier.size(92.dp),
            )
        }
    }
}

@Composable
private fun SafePlaceholderBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Green, RoundedCornerShape(18.dp))
            .border(2.dp, Color.White, RoundedCornerShape(18.dp)),
    )
}

@Composable
private fun routeLabel(route: AuroraDemoRoute): String = when (route) {
    AuroraDemoRoute.Home -> appString(AppText.NavHome)
    AuroraDemoRoute.Network -> appString(AppText.NavNetwork)
    AuroraDemoRoute.Database -> appString(AppText.NavDatabase)
}

@Composable
private fun NavigationTabs(
    currentPage: AuroraDemoRoute,
    onNavigate: (AuroraDemoRoute) -> Unit,
) {
    DiagnosticPanel(
        title = stringResource(Res.string.navigation_title),
        background = DemoColors.panelNavigation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuroraDemoRoute.entries.forEach { route ->
                Box(modifier = Modifier.weight(1f)) {
                    DiagnosticButton(
                        label = routeLabel(route),
                        background = if (route == currentPage) DemoColors.buttonIndigo else DemoColors.buttonMuted,
                        onClick = { onNavigate(route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticPanel(
    title: String,
    background: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val typography = demoTypography()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(20.dp))
            .border(2.dp, Color.White, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(
            text = title,
            style = TextStyle(color = DemoColors.textPrimary).merge(typography.section),
        )
        content()
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    accent: Color,
) {
    val typography = demoTypography()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = accent).merge(typography.caption.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = value,
            style = TextStyle(color = DemoColors.textPrimary).merge(typography.body.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 20.sp)),
        )
    }
}

@Composable
private fun ButtonRow(
    firstLabel: String,
    firstColor: Color,
    firstAction: () -> Unit,
    secondLabel: String,
    secondColor: Color,
    secondAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            DiagnosticButton(
                label = firstLabel,
                background = firstColor,
                onClick = firstAction,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            DiagnosticButton(
                label = secondLabel,
                background = secondColor,
                onClick = secondAction,
            )
        }
    }
}

@Composable
private fun DiagnosticButton(
    label: String,
    background: Color,
    onClick: () -> Unit,
) {
    val typography = demoTypography()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .border(2.dp, Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color.White).merge(typography.button),
        )
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .background(color, RoundedCornerShape(16.dp))
            .border(2.dp, Color.White, RoundedCornerShape(16.dp)),
    )
    Spacer(modifier = Modifier.width(0.dp))
}

@Composable
private fun NotePanel(
    noteInput: String,
    latestNote: String,
    onNoteInputChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onReadNote: () -> Unit,
) {
    val typography = demoTypography()
    DiagnosticPanel(
        title = stringResource(Res.string.latest_note_title),
        background = DemoColors.panelBrown,
    ) {
        BasicTextField(
            value = noteInput,
            onValueChange = onNoteInputChange,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .background(DemoColors.inputBackground, RoundedCornerShape(14.dp))
                .border(1.dp, DemoColors.inputBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            decorationBox = { innerTextField ->
                if (noteInput.isBlank()) {
                    BasicText(
                        text = stringResource(Res.string.latest_note_placeholder),
                        style = TextStyle(color = DemoColors.inputPlaceholder).merge(typography.caption.copy(fontSize = 15.sp, lineHeight = 18.sp)),
                    )
                }
                innerTextField()
            },
        )
        ButtonRow(
            firstLabel = stringResource(Res.string.save_latest_text),
            firstColor = DemoColors.buttonGreen,
            firstAction = onSaveNote,
            secondLabel = stringResource(Res.string.read_latest_text),
            secondColor = DemoColors.buttonIndigo,
            secondAction = onReadNote,
        )
        BasicText(
            text = stringResource(Res.string.latest_note_value, latestNote.ifBlank { "-" }),
            style = TextStyle(color = DemoColors.textPrimary).merge(typography.body),
        )
    }
}

private fun nowStamp(): String = formattedCurrentTime()
