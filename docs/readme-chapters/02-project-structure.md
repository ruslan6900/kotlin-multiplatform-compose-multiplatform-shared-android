# Структура проекта

```text
composeApp/
  src/commonMain/      общий UI, navigation, data, DI и Room
  src/androidMain/     Android actual-реализации
  src/iosMain/         iOS actual-реализации
  src/desktopMain/     Desktop actual-реализации
  src/auroraMain/      Aurora entry point, D-Bus controller, диагностика
  native-vpn/          test ConnMan plugin, provider и bridge
  system-rpm/          spec и скрипт объединённого development RPM
  composeResources/    общие ресурсы
docs/                  актуальная документация
outputs/               архивные результаты запусков
```

`commonMain` не содержит системные Aurora-пути и привилегии. Они остаются в `auroraMain` и нативной части, поэтому не переносятся искусственно на другие платформы.
