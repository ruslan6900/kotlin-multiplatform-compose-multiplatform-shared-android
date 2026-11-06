package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurorakmpdemo.data.Post
import com.example.aurorakmpdemo.data.KotlinLibraryChecks
import com.example.aurorakmpdemo.data.LibraryCheckEvent
import com.example.aurorakmpdemo.data.LibraryChecksRunner
import com.example.aurorakmpdemo.data.LibraryChecksViewModel
import com.example.aurorakmpdemo.data.PostsStorage
import com.example.aurorakmpdemo.data.RoomVerificationRepository
import com.example.aurorakmpdemo.data.VpnConnectionState
import com.example.aurorakmpdemo.data.VpnController
import com.example.aurorakmpdemo.data.VpnLogEntry
import com.example.aurorakmpdemo.db.PostsDao
import com.example.aurorakmpdemo.db.PostsRoomDatabase
import com.example.aurorakmpdemo.di.AuroraFriendlyKoin
import com.example.aurorakmpdemo.resources.Res
import com.example.aurorakmpdemo.resources.ic_demo_badge
import com.example.aurorakmpdemo.resources.ic_raster_png_test
import com.example.aurorakmpdemo.resources.ic_vector_min_square
import com.example.aurorakmpdemo.resources.ic_vector_original_badge
import com.example.aurorakmpdemo.resources.ic_vector_stroke_cross
import com.example.aurorakmpdemo.resources.ic_vector_two_paths
import com.example.aurorakmpdemo.ui.theme.DemoColors
import com.example.aurorakmpdemo.ui.theme.demoFontFamily
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.mp.KoinPlatform

@Composable
fun AuroraDatabaseScreen(onBack: () -> Unit) {
    val storage = remember { KoinPlatform.getKoin().get<PostsStorage>() }
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("Проверка Room на Aurora") }
    var status by remember { mutableStateOf("Проверка сохранённых данных…") }
    var savedPost by remember { mutableStateOf<Post?>(null) }
    var savedNote by remember { mutableStateOf<String?>(null) }

    fun readPersistedData() {
        scope.launch {
            runCatching {
                storage.getPost(777) to storage.getLatestNote()
            }.onSuccess { (post, latestNote) ->
                savedPost = post
                savedNote = latestNote
                status = if (post != null || latestNote != null) "Данные прочитаны из Room: пережили перезапуск приложения" else "В Room пока нет тестовых данных"
                UiTrace.log("AuroraDatabase", "Room read post=${post != null} note=${latestNote != null}")
            }.onFailure { error ->
                status = "Ошибка чтения Room: ${error.message ?: "неизвестная ошибка"}"
                UiTrace.logError("AuroraDatabase", error)
            }
        }
    }

    LaunchedEffect(Unit) { readPersistedData() }
    AuroraDiagnosticsLayout("База данных", onBack) {
        AuroraDiagnosticsPanel("Состояние") {
            BasicText("Koin: ${if (AuroraFriendlyKoin.isStarted()) "инициализирован" else "не инициализирован"}", style = diagnosticsBodyStyle())
            BasicText("Room: $status", style = diagnosticsMutedStyle())
        }
        AuroraDiagnosticsPanel("Добавить запись") {
            BasicText("Текст заметки", style = diagnosticsMutedStyle())
            BasicTextField(
                value = note,
                onValueChange = { note = it },
                textStyle = diagnosticsBodyStyle(),
                modifier = Modifier.fillMaxWidth().background(DemoColors.normalDemoBackground, RoundedCornerShape(8.dp)).padding(10.dp),
            )
            AuroraDiagnosticsButton("Сохранить запись Room", enabled = note.isNotBlank()) {
                scope.launch {
                    val post = Post(userId = 42, id = 777, title = note.trim(), body = "Создано в Aurora Database screen")
                    runCatching {
                        storage.savePost(post)
                        storage.saveLatestNote(note.trim())
                        storage.getPost(777) to storage.getLatestNote()
                    }.onSuccess { (reloadedPost, reloadedNote) ->
                        savedPost = reloadedPost; savedNote = reloadedNote
                        status = "Запись сохранена и немедленно прочитана из Room"
                        UiTrace.log("AuroraDatabase", "Room save/read success")
                    }.onFailure { error ->
                        status = "Ошибка сохранения Room: ${error.message ?: "неизвестная ошибка"}"
                        UiTrace.logError("AuroraDatabase", error)
                    }
                }
            }
            AuroraDiagnosticsButton("Прочитать после перезапуска") { readPersistedData() }
        }
        AuroraDiagnosticsPanel("Последнее прочитанное") {
            BasicText("Post #${savedPost?.id ?: "—"}: ${savedPost?.title ?: "нет записи"}", style = diagnosticsBodyStyle())
            BasicText("Заметка: ${savedNote ?: "нет записи"}", style = diagnosticsMutedStyle())
        }
    }
}

@Composable
fun AuroraCompatibilityScreen(onBack: () -> Unit) {
    val database: PostsRoomDatabase = koinInject()
    val dao: PostsDao = koinInject()
    val repository: RoomVerificationRepository = koinInject()
    val runner: LibraryChecksRunner = koinInject()
    val viewModel: LibraryChecksViewModel = koinViewModel()
    val lastUiEvent by viewModel.lastUiEvent.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Нажмите «Запустить все проверки»") }
    var persistedValue by remember { mutableStateOf("—") }
    var checks by remember { mutableStateOf(KotlinLibraryChecks.run()) }
    var runLog by remember { mutableStateOf<List<LibraryCheckEvent>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    fun readPersistedValue() {
        scope.launch {
            runCatching { repository.read() }
                .onSuccess { result ->
                    persistedValue = result.note ?: "нет сохранённой записи"
                    status = if (result.isPersisted) "Room прочитал запись, сохранённую ранее" else "База пуста — это нормально при первом запуске"
                    UiTrace.log("Compatibility", "Room persisted=${result.isPersisted}")
                }
                .onFailure { error ->
                    status = "Ошибка чтения: ${error.message ?: error::class.simpleName}"
                    UiTrace.logError("Compatibility", error)
                }
        }
    }

    LaunchedEffect(Unit) { readPersistedValue() }
    AuroraDiagnosticsLayout("Проверка библиотек", onBack) {
        AuroraDiagnosticsPanel("Граф зависимостей") {
            BasicText("Koin: ${if (AuroraFriendlyKoin.isStarted()) "запущен" else "не запущен"}", style = diagnosticsBodyStyle())
            BasicText("AppDatabase: singleton внедрён через koinInject", style = diagnosticsBodyStyle())
            BasicText("DAO: ${if (dao === database.postsDao()) "получен из AppDatabase" else "разрешён Koin"}", style = diagnosticsBodyStyle())
            BasicText("Repository: RoomVerificationRepository зарегистрирован", style = diagnosticsBodyStyle())
            BasicText("Koin ViewModel + lifecycle StateFlow: $lastUiEvent", style = diagnosticsMutedStyle())
        }
        AuroraDiagnosticsPanel("Room: запись → чтение → перезапуск") {
            BasicText(status, style = diagnosticsMutedStyle())
            BasicText("Последняя заметка: $persistedValue", style = diagnosticsBodyStyle())
            AuroraDiagnosticsButton("Запустить проверку") {
                scope.launch {
                    runCatching { repository.saveAndRead("Проверка Koin + Room") }
                        .onSuccess { result ->
                            persistedValue = result.note ?: "—"
                            status = if (result.isPersisted) "Запись сохранена и тут же прочитана через DAO" else "Запись не найдена"
                            UiTrace.log("Compatibility", "Room write/read success timestamp=${result.timestamp}")
                        }
                        .onFailure { error ->
                            status = "Ошибка записи: ${error.message ?: error::class.simpleName}"
                            UiTrace.logError("Compatibility", error)
                        }
                }
            }
            AuroraDiagnosticsButton("Прочитать сохранённое") { readPersistedValue() }
        }
        AuroraDiagnosticsPanel("Полная последовательная проверка") {
            BasicText("Операции идут строго одна за другой. Каждый результат сохраняется в журнал ниже и в ui-diagnostics.log.", style = diagnosticsMutedStyle())
            BasicText("Последний прогон: ${UiTrace.libraryChecksLogPath()}", style = diagnosticsMutedStyle())
            AuroraDiagnosticsButton("Запустить все проверки", enabled = !isRunning) {
                scope.launch {
                    isRunning = true
                    status = "Выполняются проверки библиотек…"
                    runLog = emptyList()
                    UiTrace.beginLibraryChecksLog()
                    UiTrace.appendLibraryChecks("START sequential library verification")
                    viewModel.recordUiEvent("Проверка запущена")
                    runner.run { event ->
                        runLog = runLog + event
                        viewModel.recordUiEvent("${event.area}: ${if (event.success) "OK" else "ERROR"}")
                        val level = if (event.success) "OK" else "ERROR"
                        UiTrace.log("LibraryChecks", "$level ${event.area}: ${event.detail}")
                        UiTrace.appendLibraryChecks("$level ${event.area}: ${event.detail}")
                    }
                    checks = KotlinLibraryChecks.run()
                    status = if (runLog.all { it.success }) "Все проверки завершились успешно" else "Есть ошибки — смотрите журнал ниже"
                    viewModel.recordUiEvent(status)
                    UiTrace.appendLibraryChecks("FINISH status=$status")
                    isRunning = false
                }
            }
        }
        AuroraDiagnosticsPanel("Журнал операций") {
            if (runLog.isEmpty()) {
                BasicText("Журнал появится после запуска проверки.", style = diagnosticsMutedStyle())
            } else {
                runLog.forEachIndexed { index, event ->
                    val mark = if (event.success) "✓" else "✕"
                    BasicText("${index + 1}. $mark ${event.area}: ${event.detail}", style = if (event.success) diagnosticsMutedStyle() else diagnosticsBodyStyle().copy(color = Color(0xFFFF8A80)))
                }
            }
        }
        AuroraDiagnosticsPanel("Короткая Kotlin-проверка") {
            checks.forEach { BasicText(it, style = diagnosticsMutedStyle()) }
        }
    }
}

@Composable
fun AuroraVpnSimulationScreen(onBack: () -> Unit) {
    val controller: VpnController = koinInject()
    val connectionState by controller.connectionState.collectAsState()
    val logs by controller.logs.collectAsState()
    val scope = rememberCoroutineScope()
    var profileId by remember { mutableStateOf("aurora-kmp-test-profile") }
    var status by remember { mutableStateOf("Готово создать и подключить тестовый профиль") }
    var persistedLogSize by remember { mutableStateOf(0) }

    LaunchedEffect(logs) {
        if (logs.size < persistedLogSize) persistedLogSize = 0
        logs.drop(persistedLogSize).forEach { entry ->
            UiTrace.log("VpnSimulation", "${entry.level} ${entry.component}: ${entry.message}")
        }
        persistedLogSize = logs.size
    }

    fun launchOperation(name: String, operation: suspend () -> Result<Unit>) {
        scope.launch {
            status = "$name…"
            operation()
                .onSuccess { status = "$name: успешно" }
                .onFailure { status = "$name: ${it.message ?: it::class.simpleName}" }
        }
    }

    AuroraDiagnosticsLayout("VPN-плагин: ConnMan", onBack) {
        AuroraDiagnosticsPanel("Режим работы") {
            BasicText("Статус: ${connectionState.label}", style = diagnosticsBodyStyle())
            BasicText(status, style = diagnosticsMutedStyle())
            BasicText("Aurora: профиль создаётся через ConnMan/D-Bus, затем состояние подключения подтверждается опросом ConnMan.", style = diagnosticsMutedStyle())
            BasicText("Тестовый провайдер использует только подсеть 198.51.100.0/24 и не меняет DNS или маршрут по умолчанию.", style = diagnosticsMutedStyle())
        }
        AuroraDiagnosticsPanel("Профиль и управление") {
            BasicText("Идентификатор профиля", style = diagnosticsMutedStyle())
            BasicTextField(
                value = profileId,
                onValueChange = { profileId = it },
                textStyle = diagnosticsBodyStyle(),
                modifier = Modifier.fillMaxWidth().background(DemoColors.normalDemoBackground, RoundedCornerShape(8.dp)).padding(10.dp),
            )
            AuroraDiagnosticsButton(
                "Создать и включить тестовый VPN",
                enabled = connectionState == VpnConnectionState.Disconnected && profileId.isNotBlank(),
            ) {
                launchOperation("Подключение") { controller.connect(profileId.trim()) }
            }
            AuroraDiagnosticsButton(
                "Выключить тестовый VPN",
                enabled = connectionState == VpnConnectionState.Connected,
            ) {
                launchOperation("Отключение") { controller.disconnect() }
            }
            AuroraDiagnosticsButton("Очистить журнал") {
                controller.clearLogs()
                status = "Журнал очищен"
            }
        }
        AuroraDiagnosticsPanel("Подробный журнал (${logs.size}/120)") {
            if (logs.isEmpty()) {
                BasicText("Записей пока нет.", style = diagnosticsMutedStyle())
            } else {
                logs.forEachIndexed { index, entry ->
                    VpnLogLine(index + 1, entry)
                }
            }
        }
    }
}

@Composable
private fun VpnLogLine(index: Int, entry: VpnLogEntry) {
    val color = when (entry.level.name) {
        "Error" -> Color(0xFFFF8A80)
        "Warn" -> Color(0xFFFFD180)
        else -> DemoColors.textMuted
    }
    BasicText(
        "$index. [${entry.timestamp.substringAfter('T').substringBefore('Z')}] ${entry.level} ${entry.component}: ${entry.message}",
        style = diagnosticsMutedStyle().copy(color = color),
    )
}

private enum class AuroraImageCase(val label: String) {
    Png("PNG (растровое)"), Svg("SVG"), XmlMin("XML Vector: minimal"), XmlPaths("XML Vector: paths"), XmlStroke("XML Vector: stroke"), XmlOriginal("XML Vector: original"),
}

@Composable
fun AuroraResourcesScreen(
    onBack: () -> Unit,
    onOpenImages: () -> Unit,
    onOpenFonts: () -> Unit,
) {
    AuroraDiagnosticsLayout("Ресурсы", onBack) {
        AuroraDiagnosticsPanel("Общая CMP-база") {
            BasicText("Ресурсы лежат в commonMain/composeResources. Gradle создаёт Res.* биндинги и упаковывает их для Android, iOS и Aurora.", style = diagnosticsMutedStyle())
        }
        AuroraDiagnosticsPanel("Проверка") {
            AuroraDiagnosticsButton("Изображения: PNG, SVG и XML Vector") {
                UiTrace.log("AuroraResources", "open images screen")
                onOpenImages()
            }
            AuroraDiagnosticsButton("Шрифты: Golos + Material 3") {
                UiTrace.log("AuroraResources", "open fonts screen")
                onOpenFonts()
            }
        }
    }
}

@Composable
fun AuroraFontsScreen(onBack: () -> Unit) {
    val golos = demoFontFamily()
    LaunchedEffect(Unit) {
        UiTrace.log("AuroraFonts", "Golos font resources requested; MaterialTheme typography is active")
    }
    AuroraDiagnosticsLayout("Шрифты", onBack) {
        AuroraDiagnosticsPanel("Golos из composeResources/font") {
            BasicText("Golos Bold — заголовок", style = MaterialTheme.typography.titleLarge.copy(fontFamily = golos, fontSize = 28.sp, fontWeight = FontWeight.Bold))
            BasicText("Golos Medium — основной текст", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = golos, fontSize = 18.sp, fontWeight = FontWeight.Medium))
            BasicText("Golos Regular — подпись", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = golos, fontSize = 15.sp, fontWeight = FontWeight.Normal))
        }
        AuroraDiagnosticsPanel("Material 3") {
            BasicText("Typography titleLarge", style = MaterialTheme.typography.titleLarge)
            BasicText("Typography bodyLarge", style = MaterialTheme.typography.bodyLarge)
            BasicText("Typography bodyMedium", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AuroraImagesScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<AuroraImageCase?>(null) }
    AuroraDiagnosticsLayout("Изображения", onBack) {
        AuroraDiagnosticsPanel("Предпросмотр") {
            if (selected == null) {
                BasicText("Выберите формат ниже — выбранный ресурс сразу появится здесь.", style = diagnosticsMutedStyle())
            } else {
                BasicText("Выбран: ${selected!!.label}", style = diagnosticsBodyStyle())
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFF0B1F33), RoundedCornerShape(18.dp)).border(2.dp, DemoColors.statusYellow, RoundedCornerShape(18.dp)).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(painter = auroraImagePainter(selected!!), contentDescription = selected!!.label, modifier = Modifier.size(128.dp))
                }
                LaunchedEffect(selected) {
                    UiTrace.log("AuroraXml", "preview composition applied case=${selected!!.name}")
                }
            }
        }
        AuroraDiagnosticsPanel("Проверка отображения") {
            BasicText("Нажмите формат: выбор фиксируется в журнале, а результат виден выше без прокрутки.", style = diagnosticsMutedStyle())
            AuroraImageCase.entries.forEach { imageCase ->
                AuroraDiagnosticsButton(imageCase.label, selected != imageCase) {
                    UiTrace.log("AuroraXml", "button clicked case=${imageCase.name}")
                    selected = imageCase
                }
            }
        }
    }
}

@Composable
private fun auroraImagePainter(imageCase: AuroraImageCase): Painter {
    val resourceName = when (imageCase) {
        AuroraImageCase.Png -> "ic_raster_png_test.png"
        AuroraImageCase.Svg -> "ic_demo_badge.svg"
        AuroraImageCase.XmlMin -> "ic_vector_min_square.xml"
        AuroraImageCase.XmlPaths -> "ic_vector_two_paths.xml"
        AuroraImageCase.XmlStroke -> "ic_vector_stroke_cross.xml"
        AuroraImageCase.XmlOriginal -> "ic_vector_original_badge.xml"
    }
    UiTrace.log("AuroraXml", "painterResource begin case=${imageCase.name} resource=$resourceName")
    val painter = when (imageCase) {
        AuroraImageCase.Png -> painterResource(Res.drawable.ic_raster_png_test)
        AuroraImageCase.Svg -> painterResource(Res.drawable.ic_demo_badge)
        AuroraImageCase.XmlMin -> painterResource(Res.drawable.ic_vector_min_square)
        AuroraImageCase.XmlPaths -> painterResource(Res.drawable.ic_vector_two_paths)
        AuroraImageCase.XmlStroke -> painterResource(Res.drawable.ic_vector_stroke_cross)
        AuroraImageCase.XmlOriginal -> painterResource(Res.drawable.ic_vector_original_badge)
    }
    UiTrace.log("AuroraXml", "painterResource success case=${imageCase.name} resource=$resourceName")
    return painter
}

@Composable
private fun AuroraDiagnosticsLayout(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DemoColors.normalDemoBackground).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(title, style = TextStyle(DemoColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold))
        AuroraDiagnosticsButton("← На главную", onClick = onBack)
        content()
    }
}

@Composable
private fun AuroraDiagnosticsPanel(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(DemoColors.panelNavy, RoundedCornerShape(18.dp)).border(1.dp, DemoColors.textPrimary, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicText(title, style = TextStyle(DemoColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        content()
    }
}

@Composable
private fun AuroraDiagnosticsButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (enabled) DemoColors.buttonIndigo else DemoColors.buttonMuted, RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { BasicText(label, style = diagnosticsBodyStyle()) }
}

private fun diagnosticsBodyStyle() = TextStyle(color = DemoColors.textPrimary, fontSize = 15.sp)
private fun diagnosticsMutedStyle() = TextStyle(color = DemoColors.textMuted, fontSize = 14.sp)
