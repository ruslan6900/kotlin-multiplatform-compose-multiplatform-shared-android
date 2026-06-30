# Aurora KMP Demo

PoC-проект для проверки, насколько далеко можно увести общий стек `Kotlin Multiplatform + Compose Multiplatform` в сторону `Android + iOS + Desktop + Aurora OS`, сохраняя максимум логики, UI и persistence в `commonMain`.

Проект не является production-образцом. Это исследовательский стенд, в котором проверяются:

- общий Compose UI;
- общий DI через Koin;
- общий сетевой слой через Ktor;
- общий persistence-слой через Room;
- граница между `commonMain` и платформенными `actual`-реализациями;
- особенности сборки и запуска на Aurora OS;
- практическая переносимость shared-стека на Android, iOS, Desktop и Aurora.

## Current Status

Сейчас в проекте подтверждено следующее:

- `Android` собирается и использует Room;
- `iOS` собирается, запускается в симуляторе и использует Room;
- `Desktop` собирается и использует Room;
- `Aurora` собирается, упаковывается в RPM, ставится в emulator, запускается и показывает красный diagnostic smoke dashboard;
- общий Compose UI, Koin, Ktor, `Flow`-время и Room уже используются как реальный shared stack, а не как формальная демонстрация.

Из известных ограничений:

- на Aurora повторный запуск всё ещё может иногда приводить к состоянию, где остаётся только красный root background без дочернего Compose subtree;
- некоторые старые отчёты в `outputs/` сохраняют исторический контекст ранних SQLDelight-экспериментов, но актуальное состояние проекта уже `Room-only`.

## Chapters

Ниже собраны главы документации. `README.md` здесь выступает как оглавление, а подробности вынесены в отдельные файлы.

1. [Overview](docs/readme-chapters/01-overview.md)  
   Что это за PoC, какие у него цели, что уже работает и почему проект разделён на `main` и `aurora`.

2. [Project Structure](docs/readme-chapters/02-project-structure.md)  
   Как разложены `commonMain`, `androidMain`, `iosMain`, `desktopMain`, `auroraMain`, и что именно лежит в каждом source set и build-пайплайне.

3. [Libraries](docs/readme-chapters/03-libraries.md)  
   Какие библиотеки используются, что является кроссплатформенным стеком, а что остаётся platform runtime или Aurora toolchain зависимостью.

4. [Platform Boundaries](docs/readme-chapters/04-platform-boundaries.md)  
   Где проходит практическая граница между общим кодом и платформенными `actual`-реализациями: entry point, HTTP engine, filesystem, database path, build/deploy.

5. [Aurora-Specific Layer](docs/readme-chapters/05-aurora-specific.md)  
   Что пришлось писать отдельно для Aurora и почему это нельзя честно унести в `commonMain`.

6. [How Compose Runs On Aurora](docs/readme-chapters/06-how-compose-runs-on-aurora.md)  
   Объяснение того, как shared Compose UI вообще доходит до реального окна Aurora runtime и почему это работает.

7. [Architecture](docs/readme-chapters/07-architecture.md)  
   Многослойная архитектура PoC: platform/runtime layer, bootstrap, presentation, data, infrastructure, а также фактические потоки данных.

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

## Useful Paths

- project root: `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`
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
