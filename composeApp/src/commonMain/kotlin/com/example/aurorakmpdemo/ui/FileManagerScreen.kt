package com.example.aurorakmpdemo.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aurorakmpdemo.platform.FileManagerEntry
import com.example.aurorakmpdemo.platform.copyFileManagerEntry
import com.example.aurorakmpdemo.platform.createFileManagerFile
import com.example.aurorakmpdemo.platform.createFileManagerDirectory
import com.example.aurorakmpdemo.platform.deleteFileManagerEntry
import com.example.aurorakmpdemo.platform.fileManagerParent
import com.example.aurorakmpdemo.platform.fileManagerRoot
import com.example.aurorakmpdemo.platform.listFileManagerEntries
import com.example.aurorakmpdemo.platform.moveFileManagerEntry
import com.example.aurorakmpdemo.ui.theme.DemoColors

private enum class PendingFileOperation { Copy, Move }

/** A simple device file browser. It deliberately uses no Compose resources on Aurora. */
@Composable
fun FileManagerScreen(onBack: (() -> Unit)? = null) {
    val root = remember { fileManagerRoot() }
    var currentPath by remember { mutableStateOf(root) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Готово") }
    var selected by remember { mutableStateOf<FileManagerEntry?>(null) }
    var pendingOperation by remember { mutableStateOf<PendingFileOperation?>(null) }
    var transferSource by remember { mutableStateOf<FileManagerEntry?>(null) }
    var newItemName by remember { mutableStateOf("Новый файл") }
    var confirmDelete by remember { mutableStateOf(false) }
    val entries = remember(currentPath, refreshKey) {
        runCatching { listFileManagerEntries(currentPath) }.getOrDefault(emptyList())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoColors.normalDemoBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BasicText(
            text = "Файловый менеджер",
            style = TextStyle(color = DemoColors.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold),
        )
        onBack?.let { back ->
            FileManagerButton("← На главную", onClick = back)
        }
        BasicText(
            text = "Просматривайте папки устройства, создавайте файлы и папки, копируйте, перемещайте и удаляйте выбранный объект.",
            style = TextStyle(color = DemoColors.textMuted, fontSize = 15.sp),
        )
        FileManagerPanel(title = "Текущая папка") {
            BasicText(currentPath, style = TextStyle(color = DemoColors.textPrimary, fontSize = 14.sp))
            BasicText("$status · элементов: ${entries.size}", style = TextStyle(color = DemoColors.textMuted, fontSize = 13.sp))
            selected?.let { BasicText("Выбрано: ${it.name}", style = TextStyle(color = DemoColors.textAccent, fontSize = 13.sp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FileManagerButton("← Вверх", enabled = fileManagerParent(currentPath) != null) {
                fileManagerParent(currentPath)?.let {
                    currentPath = it
                    selected = null
                    status = "Открыта родительская папка"
                }
            }
            FileManagerButton("Обновить") {
                refreshKey += 1
                status = "Список обновлён"
            }
        }
        FileManagerPanel(title = "Создание") {
            BasicText("Имя файла или папки", style = TextStyle(color = DemoColors.textMuted, fontSize = 13.sp))
            BasicTextField(
                value = newItemName,
                onValueChange = { newItemName = it },
                textStyle = TextStyle(color = DemoColors.textPrimary, fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DemoColors.normalDemoBackground, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FileManagerButton("Создать файл", enabled = newItemName.isNotBlank()) {
                    createFileManagerFile(currentPath, newItemName.trim())
                        .onSuccess { status = "Создан файл «${it.name}»"; refreshKey += 1 }
                        .onFailure { status = "Не удалось создать файл: ${it.message ?: "неизвестная ошибка"}" }
                }
                FileManagerButton("Создать папку", enabled = newItemName.isNotBlank()) {
                    createFileManagerDirectory(currentPath, newItemName.trim())
                        .onSuccess { status = "Создана папка «${it.name}»"; refreshKey += 1 }
                        .onFailure { status = "Не удалось создать папку: ${it.message ?: "неизвестная ошибка"}" }
                }
            }
        }
        selected?.let { item ->
            FileManagerPanel(title = "Действия: ${item.name}") {
                if (item.isDirectory) {
                    FileManagerButton("Открыть папку") {
                        currentPath = item.path
                        selected = null
                        pendingOperation = null
                        status = "Открыта папка «${item.name}»"
                    }
                }
                if (pendingOperation == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FileManagerButton("Копировать") { transferSource = item; pendingOperation = PendingFileOperation.Copy; status = "Выберите папку назначения" }
                        FileManagerButton("Переместить") { transferSource = item; pendingOperation = PendingFileOperation.Move; status = "Выберите папку назначения" }
                    }
                    FileManagerButton(if (confirmDelete) "Подтвердить удаление" else "Удалить") {
                        if (!confirmDelete) {
                            confirmDelete = true
                            status = "Нажмите «Подтвердить удаление», чтобы удалить ${item.name}"
                        } else {
                            deleteFileManagerEntry(item.path)
                                .onSuccess { status = "Удалено: ${item.name}"; selected = null; confirmDelete = false; refreshKey += 1 }
                                .onFailure { status = "Ошибка удаления: ${it.message ?: "неизвестная ошибка"}"; confirmDelete = false }
                        }
                    }
                }
            }
        }
        pendingOperation?.let { operation ->
            transferSource?.let { source ->
                val operationName = if (operation == PendingFileOperation.Copy) "Копирование" else "Перемещение"
                FileManagerPanel(title = "$operationName: ${source.name}") {
                    BasicText("Откройте папку назначения и нажмите «Вставить сюда».", style = TextStyle(color = DemoColors.textAccent, fontSize = 14.sp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FileManagerButton("Вставить сюда") {
                            val result = if (operation == PendingFileOperation.Copy) copyFileManagerEntry(source.path, currentPath)
                            else moveFileManagerEntry(source.path, currentPath)
                            result.onSuccess {
                                status = "${if (operation == PendingFileOperation.Copy) "Скопирован" else "Перемещён"}: ${it.name}"
                                selected = null; transferSource = null; pendingOperation = null; refreshKey += 1
                            }.onFailure { status = "Ошибка: ${it.message ?: "неизвестная ошибка"}" }
                        }
                        FileManagerButton("Отменить") { transferSource = null; pendingOperation = null; status = "Операция отменена" }
                    }
                }
            }
        }
        FileManagerPanel(title = "Содержимое") {
            if (entries.isEmpty()) {
                BasicText("Папка пуста", style = TextStyle(color = DemoColors.textMuted, fontSize = 15.sp))
            } else {
                entries.forEach { entry ->
                    FileManagerEntryRow(entry, entry.path == selected?.path) {
                        if (pendingOperation != null && entry.isDirectory) {
                            currentPath = entry.path
                            selected = null
                            status = "Открыта папка назначения: ${entry.name}"
                        } else {
                            selected = entry
                            confirmDelete = false
                            status = "Выбран ${if (entry.isDirectory) "каталог" else "файл"}: ${entry.name}"
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileManagerPanel(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DemoColors.panelNavy, RoundedCornerShape(18.dp))
            .border(1.dp, DemoColors.textPrimary, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicText(title, style = TextStyle(color = DemoColors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        content()
    }
}

@Composable
private fun FileManagerButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (enabled) DemoColors.buttonIndigo else DemoColors.buttonMuted, RoundedCornerShape(10.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicText(label, style = TextStyle(color = DemoColors.textPrimary, fontSize = 14.sp))
    }
}

@Composable
private fun FileManagerEntryRow(entry: FileManagerEntry, selected: Boolean, onClick: () -> Unit) {
    val type = if (entry.isDirectory) "ПАПКА" else "ФАЙЛ"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) DemoColors.buttonMuted else DemoColors.panelNavy, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(if (entry.isDirectory) "▸" else "·", style = TextStyle(color = DemoColors.textAccent, fontSize = 20.sp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            BasicText(entry.name, style = TextStyle(color = DemoColors.textPrimary, fontSize = 16.sp))
            BasicText(type, style = TextStyle(color = DemoColors.textMuted, fontSize = 12.sp))
        }
        if (!entry.isDirectory) BasicText(formatSize(entry.sizeBytes), style = TextStyle(color = DemoColors.textMuted, fontSize = 13.sp))
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1024 * 1024 -> "${bytes / 1024} КБ"
    else -> "${bytes / (1024 * 1024)} МБ"
}
