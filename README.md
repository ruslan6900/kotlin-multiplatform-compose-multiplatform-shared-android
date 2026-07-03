# Aurora KMP Demo

PoC-проект для проверки, насколько далеко можно увести общий стек `Kotlin Multiplatform + Compose Multiplatform` в сторону `Android + iOS + Desktop + Aurora OS`, сохраняя максимум логики, UI и persistence в `commonMain`.

Проект не является production-образцом. Это исследовательский стенд, в котором проверяются:

- общий Compose UI;
- общий слой ресурсов через Compose Resources;
- общий Compose Navigation graph;
- общий DI через Koin;
- общий сетевой слой через Ktor;
- общий persistence-слой через Room;
- граница между `commonMain` и платформенными `actual`-реализациями;
- особенности сборки и запуска на Aurora OS;
- практическая переносимость shared-стека на Android, iOS, Desktop и Aurora.

Текущая фиксируемая версия проекта: `0.0.2`.

## Current Status

Сейчас в проекте подтверждено следующее:

- `Android` собирается и использует Room;
- `iOS` собирается, запускается в симуляторе и использует Room;
- `Desktop` собирается и использует Room;
- `Aurora` собирается, упаковывается в RPM, ставится в emulator, запускается и показывает красный diagnostic smoke dashboard;
- общий Compose UI, Compose Resources, Compose Navigation, Koin, Ktor, `Flow`-время и Room уже используются как реальный shared stack, а не как формальная демонстрация.

Из известных ограничений:

- на Aurora повторный запуск всё ещё может иногда приводить к состоянию, где остаётся только красный root background без дочернего Compose subtree;
- в текущем PoC `SVG` пока не считается беспроблемным общим drawable-форматом для Android;
- в текущем PoC `XML VectorDrawable` пока не считается беспроблемным общим drawable-форматом для Aurora;
- некоторые старые отчёты в `outputs/` сохраняют исторический контекст ранних SQLDelight-экспериментов, но актуальное состояние проекта уже `Room-only`.

## Chapters

Ниже собраны главы документации. `README.md` здесь выступает как оглавление, а подробности вынесены в отдельные файлы.

1. [Overview](docs/readme-chapters/01-overview.md)  
   Что это за PoC, какие у него цели, что уже работает и почему проект разделён на `main` и `aurora`.

2. [Project Structure](docs/readme-chapters/02-project-structure.md)  
   Как разложены `commonMain`, `androidMain`, `iosMain`, `desktopMain`, `auroraMain`, и что именно лежит в каждом source set и build-пайплайне.

3. [Libraries](docs/readme-chapters/03-libraries.md)  
   Какие библиотеки используются, что является кроссплатформенным стеком, как устроены общие ресурсы, и что остаётся platform runtime или Aurora toolchain зависимостью.

4. [Platform Boundaries](docs/readme-chapters/04-platform-boundaries.md)  
   Где проходит практическая граница между общим кодом и платформенными `actual`-реализациями: entry point, HTTP engine, filesystem, database path, build/deploy.

5. [Aurora-Specific Layer](docs/readme-chapters/05-aurora-specific.md)  
   Что пришлось писать отдельно для Aurora и почему это нельзя честно унести в `commonMain`.

6. [How Compose Runs On Aurora](docs/readme-chapters/06-how-compose-runs-on-aurora.md)  
   Объяснение того, как shared Compose UI и shared Compose Navigation вообще доходят до реального окна Aurora runtime и почему это работает.

7. [Architecture](docs/readme-chapters/07-architecture.md)  
   Многослойная архитектура PoC: platform/runtime layer, bootstrap, presentation, общий resource layer, navigation graphs, data, infrastructure, а также фактические потоки данных.

8. [Run Configurations And Build Artifacts](docs/readme-chapters/08-run-configurations-and-artifacts.md)  
   Какие есть shared run-конфигурации Android Studio, какие команды использовать для Android/Aurora/iOS, и где лежат собранные артефакты.

9. [Migration Guidelines](docs/readme-chapters/09-migration-guidelines.md)  
   Как переносить эти наработки в основной проект поэтапно, не ломая рабочий Android-код и рано проверяя Aurora target.

## Key Reports

Актуальные сводные и диагностические отчёты:

- [Current Summary Report](outputs/report.md)
- [Aurora Render Smoke Test Report](outputs/aurora_render_smoke_test_report.md)
- [Aurora Environment Report](outputs/aurora_environment_report.md)
- [Aurora Open KMP Projects Smoke Test](outputs/aurora_open_kmp_projects_smoke_test.md)
- [Portable Setup Verification](outputs/portable_setup_verification.md)
- [Release 0.0.2](outputs/release_0.0.2.md)
- [Release 0.0.2 Packages](outputs/release_0.0.2_packages.md)
- [Resource Format Known Issues](outputs/resource_format_known_issues.md)

## Portable Local Setup

Проект больше не требует хардкода путей к вашей машине в `gradlew`, Gradle wrapper или Aurora SSH-настройках.

Что теперь используется:

- Android SDK:
  - сначала `local.properties -> sdk.dir`
  - затем `ANDROID_SDK_ROOT`
  - затем `ANDROID_HOME`
  - затем `adb` / `emulator` из `PATH`
- JDK:
  - для Aurora можно задать `AURORA_JAVA_HOME`
  - для main/Android можно задать `MAIN_JAVA_HOME`
  - если они не заданы, используется обычный `JAVA_HOME` или `java` из `PATH`
- Aurora emulator:
  - по умолчанию используются
    - host: `127.0.0.1`
    - port: `2223`
    - user: `defaultuser`
    - key: `$HOME/AuroraOS/vmshare/ssh/private_keys/sdk`
  - но всё это можно переопределить через `-P` свойства или переменные окружения

Поддерживаемые Aurora overrides:

- `-Paurora.host=...` или `AURORA_HOST`
- `-Paurora.port=...` или `AURORA_PORT`
- `-Paurora.user=...` или `AURORA_USER`
- `-Paurora.sshKey=...` или `AURORA_SSH_KEY`

Поддерживаемые Android overrides:

- `sdk.dir` в `local.properties`
- `adb.path` в `local.properties`
- `emulator.path` в `local.properties`
- `android.avd` в `local.properties`
- `ANDROID_AVD` через environment

## First-Run Verification

Если человек только что клонировал проект, рекомендованный порядок такой:

### 1. Проверить main toolchain

```bash
./gradlew doctorMainEnvironment
./gradlew -PbuildVariant=main verifyMainTargets
```

Что это проверяет:

- Android SDK и доступность `adb` / `emulator`;
- наличие `xcodebuild` / `xcrun` для iOS simulator;
- сборку `Android + Desktop + iOS Simulator` без ручной правки проекта.

### 2. Проверить Aurora toolchain

```bash
./gradlew -PbuildVariant=aurora doctorAuroraEnvironment
./gradlew -PbuildVariant=aurora verifyAuroraTarget
```

Что это проверяет:

- Aurora SSH-конфигурацию;
- наличие локального Aurora Maven mirror в `work/aurora-maven-partial`;
- сборку Aurora debug pipeline и RPM.

### 3. При необходимости запустить приложения

```bash
./gradlew -PbuildVariant=main :composeApp:assembleDebug
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming
```

Замечание:

- Android, Desktop и iOS должны подтягивать зависимости из обычных репозиториев Gradle;
- Aurora по-прежнему зависит от локально подготовленного Aurora toolchain и Aurora-specific Maven artifacts, но теперь это явно диагностируется и не зашито в персональные абсолютные пути.

## Useful Paths

- project root: current repository root
- Aurora run configs: `.idea/runConfigurations/`
- documentation chapters: `docs/readme-chapters/`
- reports: `outputs/`

## Practical Takeaway

На текущем этапе этот PoC уже подтверждает, что:

- общий Compose UI между Android, iOS, Desktop и Aurora реалистичен;
- общий Koin-слой реалистичен;
- общий Ktor-слой реалистичен;
- общий Room-based persistence тоже реалистичен;
- основная оставшаяся сложность находится не в shared-коде как таковом, а в platform runtime, filesystem paths, packaging, emulator behavior и Aurora-specific lifecycle/runtime особенностях.
