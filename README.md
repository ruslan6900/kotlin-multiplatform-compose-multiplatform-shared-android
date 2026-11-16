# Aurora KMP Demo

Демонстрационный проект Kotlin Multiplatform и Compose Multiplatform для Android, iOS, Desktop и Aurora OS. Это воспроизводимый стенд: он показывает общий код, границы платформ и Aurora-системные интеграции. Проект не является production VPN-клиентом.

Текущий релиз: **0.0.4**.

## Что есть

- общий Compose UI и Compose Navigation;
- Compose Resources: строки, шрифты, PNG и SVG;
- Koin, Ktor, coroutines, serialization, datetime и Kotlin stdlib;
- Room KMP: entity, DAO, миграции, repository, Flow и повторное чтение данных;
- экраны БД, ресурсов, библиотечных проверок, файлов и VPN;
- диагностика сети и системного времени;
- локальные и периодические уведомления, разовые и периодические фоновые задачи;
- экспериментальные системные диалоги и сценарий VPN после перезагрузки;
- UI-диагностика Aurora и сохраняемый журнал проверок;
- test split-tunnel VPN через ConnMan.

## Текущее состояние

| Возможность | Состояние |
| --- | --- |
| Общие UI и логика | находятся в `commonMain` |
| Room, Koin, Ktor и сериализация | проверяются в приложении |
| RPM для Aurora `aarch64` | собирается |
| Один development RPM с test VPN и системными диалогами | собран и установлен на тестовом устройстве с обходом проверки состава |
| Test VPN | создаёт профиль, поднимает `vpn0`, не меняет DNS и default route |
| Тестовая подпись RPM | подписывается отдельно сертификатом regular из подготовленного SDK-набора |
| Страница VPN в системных настройках | не реализована |
| Sandbox-рендер Compose в Aurora 5.2 emulator | требует исправления/подтверждения от команды платформы |

Test VPN обслуживает только `198.51.100.0/24` и намеренно отбрасывает кадры. Это проверка ConnMan lifecycle, а не внешний VPN-канал.

## Aurora: быстрый старт

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew -PbuildVariant=aurora :composeApp:buildSystemVpnBundleAuroraArm64
```

Объединённый development RPM:

```text
composeApp/build/system-rpm/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.4-1.aarch64.rpm
```

Подпись и установка этого системного development RPM требуют отдельных действий и привилегий; подробности и ограничения приведены в [инструкции по сборке](docs/readme-chapters/05-aurora-specific.md).

## Документация

1. [Обзор](docs/readme-chapters/01-overview.md)
2. [Структура](docs/readme-chapters/02-project-structure.md)
3. [Библиотеки](docs/readme-chapters/03-libraries.md)
4. [Границы платформ](docs/readme-chapters/04-platform-boundaries.md)
5. [Aurora и test VPN](docs/readme-chapters/05-aurora-specific.md)
6. [UI и ресурсы](docs/readme-chapters/06-how-compose-runs-on-aurora.md)
7. [Архитектура и логи](docs/readme-chapters/07-architecture.md)
8. [Сборка и запуск](docs/readme-chapters/08-run-configurations-and-artifacts.md)
9. [Ограничения и дальнейшие шаги](docs/readme-chapters/09-migration-guidelines.md)
10. [Заметка по Aurora 5.2](docs/aurora-5.2-sandbox-ui-and-vpn-investigation.md)
11. [Нативная `.so` и Kotlin/Native Interop](docs/aurora-native-so-interop.md)
12. [Проверки сети и времени](docs/aurora-network-and-time.md)
13. [Уведомления и фоновые задачи](docs/aurora-system-events.md)
14. [Системные диалоги](docs/aurora-system-dialogs.md)
15. [VPN после перезагрузки](docs/aurora-vpn-after-reboot.md)

`outputs/` содержит архивные результаты запусков, а не текущие инструкции.

## Подпись и секреты

В репозитории нет сертификатов и закрытых ключей. Сборка создаёт неподписанный RPM; пакет на тестовом устройстве был отдельно подписан сертификатом regular из подготовленного SDK-набора. Подпись не заменяет проверку состава: системный development RPM штатный APM отклонил, а проверка extended также выявила ошибки. Тестовая установка выполнена с разовым обходом проверки состава и отключённым Sailjail. Это не готовая схема промышленной поставки.
