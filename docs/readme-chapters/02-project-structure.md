# Структура проекта

```text
composeApp/
  src/commonMain/      общий UI, navigation, data, DI и Room
  src/androidMain/     Android actual-реализации
  src/iosMain/         iOS actual-реализации
  src/desktopMain/     Desktop actual-реализации
  src/auroraMain/      Aurora entry point, D-Bus, диагностика и платформенные ресурсы
    native/            C interop, задачи, VPN и системные диалоги
    packaging/         иконки и скрипты упаковки RPM
  schemas/             общие схемы Room для всех платформ
  composeResources/    общие ресурсы
docs/                  актуальная документация
outputs/               архивные результаты запусков
```

`commonMain` не содержит системные Aurora-пути и привилегии. Они остаются в `auroraMain` и нативной части, поэтому не переносятся искусственно на другие платформы.
