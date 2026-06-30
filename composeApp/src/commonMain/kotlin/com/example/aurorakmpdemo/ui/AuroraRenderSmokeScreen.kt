package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aurorakmpdemo.di.AuroraFriendlyKoin
import com.example.aurorakmpdemo.data.Post
import com.example.aurorakmpdemo.data.PostsRepository
import com.example.aurorakmpdemo.data.buildHttpClient
import com.example.aurorakmpdemo.data.currentTimeTickerFlow
import com.example.aurorakmpdemo.data.formattedCurrentTime
import com.example.aurorakmpdemo.platform.PlatformDiagnosticsProvider
import com.example.aurorakmpdemo.platform.createPostsStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Clock

enum class AuroraBootMode {
    SuperSmoke,
    RenderSmoke,
    NormalDemo,
}

@Composable
fun AuroraStagedBootContainer(
    startupId: String,
    stageLabel: String,
    contentDelayMs: Long = 100L,
    content: @Composable () -> Unit,
) {
    TraceComposableLifecycle("AuroraStagedBootContainer", "startupId=$startupId stage=$stageLabel")
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
            .background(Color(0xFF5A0014)),
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
                    text = "Starting Aurora UI...",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
                BasicText(
                    text = stageLabel,
                    style = TextStyle(
                        color = Color(0xFFFFE066),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                BasicText(
                    text = startupId,
                    style = TextStyle(
                        color = Color(0xFFD0EBFF),
                        fontSize = 14.sp,
                    ),
                )
                BasicText(
                    text = "start=$startCount resume=$resumeCount event=$lastLifecycleEvent gen=$subtreeGeneration",
                    style = TextStyle(
                        color = Color(0xFFB2F2BB),
                        fontSize = 14.sp,
                    ),
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
) {
    TraceComposableLifecycle("AuroraSuperSmokeScreen", "startupId=$startupId")
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText(
                text = "HELLO AURORA",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                ),
            )
            BasicText(
                text = "Super smoke mode",
                style = TextStyle(
                    color = Color.Yellow,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = startupId,
                style = TextStyle(
                    color = Color(0xFFD0EBFF),
                    fontSize = 14.sp,
                ),
            )
            if (lifecycleSummary.isNotBlank()) {
                BasicText(
                    text = lifecycleSummary,
                    style = TextStyle(
                        color = Color(0xFFB2F2BB),
                        fontSize = 14.sp,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color.Green, RoundedCornerShape(18.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(18.dp)),
            )
            DiagnosticButton(
                label = "Open render smoke",
                background = Color(0xFF1971C2),
                onClick = {
                    UiTrace.log("AuroraSuperSmokeScreen", "startupId=$startupId Open render smoke tapped")
                    onOpenRenderSmoke()
                },
            )
            onOpenNormalDemo?.let { openNormalDemo ->
                DiagnosticButton(
                    label = "Open normal demo",
                    background = Color(0xFFF76707),
                    onClick = {
                        UiTrace.log("AuroraSuperSmokeScreen", "startupId=$startupId Open normal demo tapped")
                        openNormalDemo()
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
    modeTitle: String = "Compose render smoke test",
    dashboardTitle: String = "Aurora KMP Demo Dashboard",
    koinStatusLabel: String = "Not initialized in smoke mode",
    onOpenSuperSmoke: (() -> Unit)? = null,
    onOpenNormalDemo: (() -> Unit)? = null,
) {
    TraceComposableLifecycle("AuroraRenderSmokeScreen", "startupId=$startupId")
    val diagnosticsProvider = remember { PlatformDiagnosticsProvider() }
    val storage = remember { createPostsStorage() }
    val client = remember { buildHttpClient() }
    val repository = remember { PostsRepository(client, storage) }
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }

    var currentTime by remember { mutableStateOf("--:--:--") }
    var platformName by remember { mutableStateOf("Unknown") }
    var platformDetails by remember { mutableStateOf(listOf("Waiting for diagnostics...")) }
    var koinStatus by remember { mutableStateOf(koinStatusLabel) }
    var ktorStatus by remember { mutableStateOf("Not checked") }
    var sqlStatus by remember { mutableStateOf("Not checked") }
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
        koinStatus = if (AuroraFriendlyKoin.isStarted()) "Initialized" else "Not initialized"
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            BasicText(
                text = "HELLO AURORA",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                ),
            )

            BasicText(
                text = modeTitle,
                style = TextStyle(
                    color = Color(0xFFFFE066),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            BasicText(
                text = dashboardTitle,
                style = TextStyle(
                    color = Color(0xFF8CE99A),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )

            BasicText(
                text = startupId,
                style = TextStyle(
                    color = Color(0xFFD0EBFF),
                    fontSize = 14.sp,
                ),
            )
            if (lifecycleSummary.isNotBlank()) {
                BasicText(
                    text = lifecycleSummary,
                    style = TextStyle(
                        color = Color(0xFFB2F2BB),
                        fontSize = 14.sp,
                    ),
                )
            }

            DiagnosticPanel(
                title = "Status",
                background = Color(0xFF101F3C),
            ) {
                StatusRow(label = "Platform", value = platformName, accent = Color(0xFF4DABF7))
                StatusRow(label = "Compose", value = "running", accent = Color(0xFFFF922B))
                StatusRow(label = "Koin", value = koinStatus, accent = Color(0xFFE599F7))
                StatusRow(label = "Ktor", value = ktorStatus, accent = Color(0xFF94D82D))
                StatusRow(label = "Room", value = sqlStatus, accent = Color(0xFF66D9E8))
                StatusRow(label = "Datetime", value = currentTime, accent = Color(0xFFFFE066))
            }

            DiagnosticPanel(
                title = "Visual Diagnostics",
                background = Color(0xFF2B0A3D),
            ) {
                BasicText(
                    text = "Large white text, yellow subtitle, and four color blocks should be visible.",
                    style = TextStyle(
                        color = Color(0xFFFFF3BF),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSwatch(Color.Red)
                    ColorSwatch(Color.Green)
                    ColorSwatch(Color.Blue)
                    ColorSwatch(Color.Yellow)
                }
            }

            DiagnosticPanel(
                title = "Actions",
                background = Color(0xFF0B3D2E),
            ) {
                ButtonRow(
                    firstLabel = "Check Ktor",
                    firstColor = Color(0xFF1971C2),
                    firstAction = {
                        scope.launch {
                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Ktor check started")
                            currentTime = nowStamp()
                            logs.add("[${nowStamp()}] Ktor check started")
                            ktorStatus = "Loading"
                            runCatching { repository.fetchAndPersistPost(1) }
                                .onSuccess { result ->
                                    ktorStatus = "Success"
                                    sqlStatus = "Saved fetched post"
                                    lastPost = result.cached ?: result.remote
                                    logs.add("[${nowStamp()}] Ktor request success")
                                    logs.add("[${nowStamp()}] Room save success")
                                    UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Ktor success cached=${result.cached != null}")
                                }
                                .onFailure { error ->
                                    ktorStatus = "Error: ${error.message ?: "unknown"}"
                                    logs.add("[${nowStamp()}] Ktor request error: ${error.message ?: "unknown"}")
                                    UiTrace.logError("AuroraRenderSmokeScreen", error)
                                }
                        }
                    },
                    secondLabel = "Save test post",
                    secondColor = Color(0xFF2B8A3E),
                    secondAction = {
                        scope.launch {
                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room manual save started")
                            val post = Post(
                                userId = 42,
                                id = 777,
                                title = "Aurora smoke saved post",
                                body = "If you can read this, Room storage survived the render smoke test.",
                            )
                            currentTime = nowStamp()
                            runCatching { storage.savePost(post) }
                                .onSuccess {
                                    sqlStatus = "Manual save success"
                                    lastPost = post
                                    logs.add("[${nowStamp()}] Room manual save success")
                                    UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room manual save success")
                                }
                                .onFailure { error ->
                                    sqlStatus = "Save error: ${error.message ?: "unknown"}"
                                    logs.add("[${nowStamp()}] Room manual save error: ${error.message ?: "unknown"}")
                                    UiTrace.logError("AuroraRenderSmokeScreen", error)
                                }
                        }
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                ButtonRow(
                    firstLabel = "Read saved post",
                    firstColor = Color(0xFFAE3EC9),
                    firstAction = {
                        scope.launch {
                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read started")
                            currentTime = nowStamp()
                            runCatching { storage.getPost(777) ?: storage.getPost(1) }
                                .onSuccess { post ->
                                    if (post == null) {
                                        sqlStatus = "Read returned empty"
                                        logs.add("[${nowStamp()}] Room read returned empty")
                                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read empty")
                                    } else {
                                        sqlStatus = "Read success"
                                        lastPost = post
                                        logs.add("[${nowStamp()}] Room read success")
                                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Room read success")
                                    }
                                }
                                .onFailure { error ->
                                    sqlStatus = "Read error: ${error.message ?: "unknown"}"
                                    logs.add("[${nowStamp()}] Room read error: ${error.message ?: "unknown"}")
                                    UiTrace.logError("AuroraRenderSmokeScreen", error)
                                }
                        }
                    },
                    secondLabel = "Clear log",
                    secondColor = Color(0xFFC92A2A),
                    secondAction = {
                        UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId log cleared")
                        logs.clear()
                        currentTime = nowStamp()
                        logs.add("[${nowStamp()}] Log cleared")
                    },
                )
                Spacer(modifier = Modifier.height(12.dp))
                NotePanel(
                    noteInput = noteInput,
                    latestNote = latestNote,
                    onNoteInputChange = { noteInput = it },
                    onSaveNote = {
                        scope.launch {
                            val stamp = nowStamp()
                            if (noteInput.isBlank()) {
                                sqlStatus = "Note is empty"
                                logs.add("[$stamp] Note save skipped: empty")
                                return@launch
                            }
                            runCatching {
                                storage.saveLatestNote(noteInput)
                                storage.getLatestNote().orEmpty()
                            }.onSuccess { saved ->
                                latestNote = saved
                                sqlStatus = "Latest note saved"
                                logs.add("[$stamp] Latest note saved")
                                UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId latest note saved")
                            }.onFailure { error ->
                                sqlStatus = "Note save error: ${error.message ?: "unknown"}"
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
                                    sqlStatus = if (saved.isBlank()) "No latest note" else "Latest note restored"
                                    logs.add("[$stamp] Latest note restored")
                                }
                                .onFailure { error ->
                                    sqlStatus = "Note read error: ${error.message ?: "unknown"}"
                                    logs.add("[$stamp] Note read error: ${error.message ?: "unknown"}")
                                    UiTrace.logError("AuroraRenderSmokeScreen", error)
                                }
                        }
                    },
                )
                onOpenNormalDemo?.let { openNormalDemo ->
                    Spacer(modifier = Modifier.height(12.dp))
                    DiagnosticButton(
                        label = "Open normal demo app",
                        background = Color(0xFFF76707),
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
                        label = "Back to super smoke",
                        background = Color(0xFF5C7CFA),
                        onClick = {
                            UiTrace.log("AuroraRenderSmokeScreen", "startupId=$startupId Back to super smoke tapped")
                            logs.add("[${nowStamp()}] Switching to super smoke")
                            openSuperSmoke()
                        },
                    )
                }
            }

            DiagnosticPanel(
                title = "Post Preview",
                background = Color(0xFF1F1F1F),
            ) {
                if (lastPost == null) {
                    BasicText(
                        text = "No post loaded yet.",
                        style = TextStyle(color = Color.White, fontSize = 16.sp),
                    )
                } else {
                    BasicText(
                        text = lastPost!!.title,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicText(
                        text = lastPost!!.body,
                        style = TextStyle(color = Color(0xFFFFF3BF), fontSize = 16.sp),
                    )
                }
            }

            DiagnosticPanel(
                title = "Platform Diagnostics",
                background = Color(0xFF14213D),
            ) {
                platformDetails.forEach { detail ->
                    BasicText(
                        text = "• $detail",
                        style = TextStyle(color = Color(0xFFD0EBFF), fontSize = 15.sp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            DiagnosticPanel(
                title = "Runtime Log",
                background = Color.Black,
            ) {
                if (logs.isEmpty()) {
                    BasicText(
                        text = "Log is empty.",
                        style = TextStyle(color = Color(0xFFCED4DA), fontSize = 15.sp),
                    )
                } else {
                    logs.takeLast(12).forEach { entry ->
                        BasicText(
                            text = entry,
                            style = TextStyle(color = Color(0xFF69DB7C), fontSize = 14.sp),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
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
            style = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
            .border(1.dp, accent, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = value,
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
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
            style = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
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
    DiagnosticPanel(
        title = "Latest Note",
        background = Color(0xFF3A1C0D),
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
                .background(Color(0xFF5C2D12), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFFFC078), RoundedCornerShape(14.dp))
                .padding(14.dp),
            decorationBox = { innerTextField ->
                if (noteInput.isBlank()) {
                    BasicText(
                        text = "Type text and save it to Room",
                        style = TextStyle(color = Color(0xFFFFE8CC), fontSize = 15.sp),
                    )
                }
                innerTextField()
            },
        )
        ButtonRow(
            firstLabel = "Save latest text",
            firstColor = Color(0xFF2B8A3E),
            firstAction = onSaveNote,
            secondLabel = "Read latest text",
            secondColor = Color(0xFF5C7CFA),
            secondAction = onReadNote,
        )
        BasicText(
            text = "Latest saved text: ${latestNote.ifBlank { "-" }}",
            style = TextStyle(color = Color.White, fontSize = 16.sp),
        )
    }
}

private fun nowStamp(): String = formattedCurrentTime()
