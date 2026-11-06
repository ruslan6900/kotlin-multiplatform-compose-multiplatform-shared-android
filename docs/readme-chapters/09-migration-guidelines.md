# Ограничения и дальнейшие шаги

## Готовые примеры

- общий Compose UI и Navigation;
- Koin, Room, Ktor, serialization и coroutines;
- Compose Resources для строк, шрифтов, PNG и SVG;
- Aurora RPM pipeline и диагностика;
- минимальный bridge для системной интеграции.

## Не production без доработки

- unsigned development RPM;
- provider, который отбрасывает трафик;
- `--nosandbox`;
- отсутствие QML-страницы в системных настройках;
- XML VectorDrawable как общий Aurora-ресурс.

## Следующие шаги

1. Настроить доверенную system/extended подпись.
2. Согласовать D-Bus policy и bridge с командой Aurora.
3. Реализовать настоящий VPN provider и обработку ошибок.
4. Добавить QML settings plugin при необходимости.
5. Передать команде Aurora данные о sandbox-rendering проблеме emulator 5.2.
6. Проверить lifecycle на физическом устройстве после подписи.
