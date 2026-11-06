# Обзор

`Aurora KMP Demo` — стенд совместимости Kotlin Multiplatform и Compose Multiplatform с Aurora OS. Его цель — дать проверяемые примеры общего кода и честно зафиксировать системные границы.

## Цели

- разделить UI, навигацию, модели, DI, сеть и persistence между платформами;
- собрать и запустить Aurora RPM;
- проверить ресурсы, Room, Koin, Ktor и Kotlin API;
- показать test VPN: Compose UI → ограниченный bridge → ConnMan/provider.

Не входят в цель реальный защищённый VPN-канал, обход Sailjail и установка unsigned RPM на физическое устройство.
