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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.aurorakmpdemo.data.PostsStorage
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
