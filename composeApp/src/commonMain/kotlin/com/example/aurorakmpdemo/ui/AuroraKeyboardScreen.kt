package com.example.aurorakmpdemo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

// На остальных платформах резервное значение отсутствует; используется системный IME inset.
val LocalAuroraKeyboardHeightPx = staticCompositionLocalOf { 0 }

@Composable
fun AuroraKeyboardScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var usePadding by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current
    val backgroundInteraction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val imeBottom = imeInsets.getBottom(density)
    val nativePx = LocalAuroraKeyboardHeightPx.current
    val extraPx = (nativePx - imeBottom).coerceAtLeast(0)
    LaunchedEffect(imeBottom, nativePx, usePadding) {
        UiTrace.log("KeyboardScreen", "Отступ включён=$usePadding, imePx=$imeBottom, MaliitPx=$nativePx, дополнительныйPx=$extraPx, density=${density.density}")
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Сохраняем половину первоначальной высоты экрана при появлении клавиатуры.
        val fieldHeight = maxHeight * 0.5f
        val keyboardModifier = if (usePadding) {
            Modifier.windowInsetsPadding(imeInsets).padding(bottom = with(density) { extraPx.toDp() })
        } else Modifier
        Box(Modifier.fillMaxSize().then(keyboardModifier).padding(16.dp)) {
            // Отдельный слой под содержимым не перехватывает нажатия на поле и кнопки.
            Box(Modifier.matchParentSize().clickable(
                interactionSource = backgroundInteraction,
                indication = null,
            ) {
                UiTrace.log("KeyboardScreen", "Нажатие вне поля: снимаем фокус")
                focusManager.clearFocus()
            })
            Column(Modifier.align(Alignment.TopStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack) { Text("← На главную") }
                Text("Проверка клавиатуры")
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = usePadding, onCheckedChange = { usePadding = it })
                    Text("  Отступ клавиатуры")
                }
                Text("IME: $imeBottom px; Maliit: $nativePx px")
                Text("Нажмите вне поля, чтобы закрыть клавиатуру. Переключите отступ для сравнения.")
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Введите текст") },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(fieldHeight)
                    .onFocusChanged { UiTrace.log("KeyboardScreen", "Фокус поля: ${it.isFocused}") }
                    .onGloballyPositioned { coordinates ->
                        UiTrace.log("KeyboardScreen", "Поле: размер=${coordinates.size}, позиция=${coordinates.positionInWindow()}, отступ=$usePadding")
                    },
            )
        }
    }
}
