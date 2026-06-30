## Структура проекта

Проект использует стандартное KMP-разделение исходников:

- `commonMain`
- `androidMain`
- `iosMain`
- `desktopMain`
- `auroraMain`

Также проект разделён по build-вариантам:

- `main` -> обычный KMP/Android/Desktop/iOS сценарий
- `aurora` -> Aurora-specific build pipeline

## Что лежит в исходниках

### `commonMain`

Это основной shared-слой проекта. Здесь лежит почти вся прикладная логика:

- общий Compose UI;
- экраны `Home`, `Network`, `Database`;
- общий persistence API;
- общий Room schema/DAO/database contract;
- общий сетевой слой Ktor;
- общий DI-модуль Koin;
- общие `ViewModel`;
- общие модели данных;
- flow для времени;
- общая Aurora diagnostic screen логика.

Именно `commonMain` отвечает за то, что приложение в основном выглядит и ведёт себя одинаково между платформами.

### `androidMain`

В Android-слое лежат только platform-specific детали:

- Android `MainActivity`;
- инициализация `AndroidAppContext`;
- Android-specific `actual` для platform API;
- Android Room database path;
- Android Ktor engine (`OkHttp`).

То есть `androidMain` не должен содержать бизнес-логику приложения. Он только подключает shared-код к Android runtime.

### `iosMain`

В iOS-слое сейчас остались:

- `actual`-реализации platform API;
- iOS-specific Ktor engine (`Darwin`);
- iOS-specific Room database path;
- `ComposeUIViewController` entry point для shared `App()`;
- iOS host app integration через `iosApp/`.

После последних правок iOS-слой уже не является purely minimal:

- на iOS тоже используется Room, а не `in-memory` storage;
- красный smoke dashboard запускается тем же shared UI-путём;
- для стабильного старта Compose UIKit понадобились корректные `Info.plist` keys, включая `CADisableMinimumFrameDurationOnPhone`.

### `desktopMain`

`desktopMain` оставлен как дополнительный JVM-target для локальной отладки.

Он полезен потому что:

- позволяет быстро проверить shared Compose UI без Android emulator;
- удобен для проверки layout/state;
- помогает локально тестировать общую KMP-логику;
- теперь тоже использует файловый Room backend, а не `in-memory`.

Для Aurora этот target не обязателен, но как инструмент разработки он очень удобен.

### `auroraMain`

`auroraMain` содержит всё, что нельзя или не стоит уносить в `commonMain`.

В частности:

- Aurora-specific `actual`-реализации;
- Aurora-specific Room database path;
- Aurora-specific build/deploy/run integration через Aurora plugins;
- Aurora-specific environment diagnostics;
- Aurora-specific Ktor engine (`Curl`);
- Aurora-specific packaging/deployment semantics.

Именно `auroraMain` соединяет общий KMP-код с рантаймом Aurora OS.

## Что лежит в Gradle

### Основной build script

Файл:

- `composeApp/build.gradle.kts`

Используется для:

- Android
- Desktop
- iOS

То есть это обычный `main`-вариант проекта.

### Aurora build script

Файл:

- `composeApp/build.aurora.gradle.kts`

Используется для:

- `auroraArm64`
- `auroraX64`
- Aurora RPM packaging
- install/run tasks для emulator

Это отдельный build pipeline, потому что Aurora требует свои plugins, targets и packaging flow.

### Root project scripts

В корне находятся:

- `build.gradle.kts`
- `settings.gradle.kts`

Они отвечают за:

- переключение между `main` и `aurora`;
- общие helper tasks;
- подключение Aurora Maven при `-PbuildVariant=aurora`.

## Дополнительные директории

### `outputs/`

Здесь лежат диагностические и исследовательские отчёты PoC:

- environment reports;
- render smoke reports;
- runtime сравнения;
- вспомогательные заметки по запуску.

Это не production-артефакты, а документация по исследованию.

### `work/`

Здесь лежат локальные рабочие материалы, связанные с Aurora SDK/tooling, например:

- aurora docs;
- aurora build tools;
- локальные mirrors/checkout материалов Aurora;
- внешние sample-проекты.

Эта директория нужна для инженерной работы над PoC, но не является частью прикладной архитектуры самого приложения.

### `.idea/runConfigurations/`

Здесь лежат shared run-конфигурации Android Studio для Aurora.

Они добавлены специально, чтобы Aurora pipeline можно было запускать из IDE, а не только руками через терминал.

## Ключевая идея структуры

Вся структура проекта подчинена одной цели:

- максимум UI и логики остаётся в `commonMain`;
- platform-specific код держится как можно тоньше;
- Aurora интегрируется через отдельный platform layer и отдельный build pipeline, а не через разрастание `commonMain` условными `if (aurora)`.

Это позволяет использовать проект как реалистичный шаблон для оценки переноса более крупного KMP-приложения на Aurora OS.
