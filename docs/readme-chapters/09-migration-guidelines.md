# Ограничения и дальнейшие шаги

## Готовые примеры

- общий Compose UI и Navigation;
- Koin, Room, Ktor, serialization и coroutines;
- Compose Resources для строк, шрифтов, PNG и SVG;
- Aurora RPM pipeline и диагностика;
- минимальный bridge для системной интеграции.

## Не production без доработки

- development RPM: Gradle собирает его без подписи, тестовый экземпляр подписан отдельно сертификатом SDK;
- provider, который отбрасывает трафик;
- `Sandboxing=Disabled` в системном development RPM;
- отсутствие QML-страницы в системных настройках;
- XML VectorDrawable как общий Aurora-ресурс.

## Следующие шаги

1. Согласовать допустимый состав системного пакета и доверенный профиль подписи: сам по себе extended не устраняет выявленные ошибки проверки.
2. Согласовать D-Bus policy и bridge с командой Aurora.
3. Реализовать настоящий VPN provider и обработку ошибок.
4. Добавить QML settings plugin при необходимости.
5. Передать команде Aurora данные о sandbox-rendering проблеме emulator 5.2.
6. Проверить lifecycle на физическом устройстве после подписи.
