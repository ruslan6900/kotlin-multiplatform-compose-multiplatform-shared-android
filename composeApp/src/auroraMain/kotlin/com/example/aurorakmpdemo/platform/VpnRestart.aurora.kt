package com.example.aurorakmpdemo.platform

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
private fun settingsPath(): String {
    val home = getenv("HOME")?.toKString() ?: error("Не найден домашний каталог")
    val dirs = listOf("$home/.local", "$home/.local/share", "$home/.local/share/com.example", "$home/.local/share/com.example/aurorakmpdemo")
    dirs.forEach { mkdir(it, 0x1EDu) }
    return "${dirs.last()}/vpn-restart.conf"
}

@OptIn(ExperimentalForeignApi::class)
private fun readSmallFile(path: String): String? {
    val file = fopen(path, "r") ?: return null
    try {
        return memScoped {
            val buffer = allocArray<ByteVar>(512)
            val output = StringBuilder()
            while (fgets(buffer, 512, file) != null) {
                output.append(buffer.toKString())
                check(output.length <= 2048) { "Файл настроек слишком большой" }
            }
            output.toString()
        }
    } finally { fclose(file) }
}

@OptIn(ExperimentalForeignApi::class)
actual fun currentVpnBootId(): Result<String> = runCatching {
    val id = readSmallFile("/proc/sys/kernel/random/boot_id")?.trim()
        ?: error("Не удалось прочитать идентификатор загрузки устройства")
    check(id.matches(Regex("[0-9a-fA-F-]{36}"))) { "Некорректный идентификатор загрузки устройства" }
    id.lowercase()
}

actual fun readVpnRestartSettings(): Result<VpnRestartSettings> = runCatching {
    val text = readSmallFile(settingsPath()) ?: return@runCatching VpnRestartSettings()
    val parts = text.lines()
    check(parts.size >= 3 && parts[0] in setOf("0", "1") && isTestVpnProfileValid(parts[1])) { "Некорректные настройки запуска VPN" }
    val bootId = parts[2]
    check(bootId.isEmpty() || bootId.matches(Regex("[0-9a-f-]{36}"))) { "Некорректный идентификатор сохранённой загрузки" }
    VpnRestartSettings(parts[0] == "1", parts[1], bootId)
}

@OptIn(ExperimentalForeignApi::class)
actual fun saveVpnRestartSettings(settings: VpnRestartSettings): Result<Unit> = runCatching {
    require(isTestVpnProfileValid(settings.profileId)) { "Имя профиля: 1–64 латинских символа, цифры, пробел, точка, дефис или подчёркивание" }
    require(settings.handledBootId.isEmpty() || settings.handledBootId.matches(Regex("[0-9a-f-]{36}")))
    val path = settingsPath()
    val temp = "$path.tmp"
    val file = fopen(temp, "w") ?: error("Не удалось сохранить настройки VPN")
    val written = fprintf(file, "%s\n%s\n%s\n", if (settings.enabled) "1" else "0", settings.profileId, settings.handledBootId)
    val closed = fclose(file)
    check(written > 0 && closed == 0) { "Ошибка записи настроек VPN" }
    check(rename(temp, path) == 0) { "Не удалось применить настройки VPN" }
}
