## Какие библиотеки используются

В проекте важно различать три уровня зависимостей:

1. полностью кроссплатформенные библиотеки, которые живут в `commonMain`;
2. платформенные runtime-зависимости, которые подключаются только на конкретных target;
3. Aurora-specific build/deploy зависимости, которые нужны не приложению как таковому, а Aurora toolchain.

Ниже это разложено по группам.

## 1. Кроссплатформенные библиотеки

Это зависимости, которые используются из `commonMain` и обеспечивают общий код проекта.

### Compose Multiplatform

Используются:

- `compose.runtime`
- `compose.foundation`
- `compose.material3`
- `compose.ui`
- `compose.components.resources`

Что они дают:

- общий декларативный UI;
- общие `@Composable`-функции;
- layout primitives;
- Material3-компоненты;
- общий слой ресурсов.

Именно благодаря этим зависимостям экраны `Home`, `Network`, `Database` могут жить в `commonMain`.

### Koin

Используются:

- `koin-core`
- `koin-compose`
- `koin-compose-viewmodel`

Что они дают:

- общий DI-граф;
- общий способ регистрации зависимостей;
- общий способ получать `ViewModel` в shared UI;
- единый способ соединить storage, repository и UI.

Для PoC это важно, потому что позволяет проверить: можно ли DI-слой тоже не распиливать на платформы.

### Ktor

Используются:

- `ktor-client-core`
- `ktor-client-content-negotiation`
- `ktor-serialization-kotlinx-json`

Что они дают:

- общий HTTP API;
- общий клиентский pipeline;
- общий JSON parsing;
- единый сетевой слой для всех target.

При этом конкретный network engine уже выбирается платформенно.

### kotlinx.coroutines

Используется:

- `kotlinx-coroutines-core`

Что даёт:

- асинхронные операции;
- `Flow`;
- coroutines для network/storage/UI;
- поток времени, который обновляет экран раз в секунду.

### kotlinx.datetime

Используется:

- `kotlinx-datetime`

Что даёт:

- доступ к текущему времени;
- platform-neutral работу с датой и временем;
- возможность формировать строку времени в `HH:mm:ss`.

### kotlinx.serialization

Используется:

- `kotlinx-serialization-json`

Что даёт:

- сериализацию и десериализацию моделей;
- общий JSON parsing для Ktor response.

### Room KMP

Используются:

- `androidx.room:room-runtime`
- `androidx.sqlite:sqlite-bundled`
- `androidx.room:room-compiler` через KSP

Что даёт:

- единый persistence backend;
- общую схему базы;
- общие DAO;
- общий storage API поверх Room;
- одинаковую модель хранения для Android, iOS, Desktop и Aurora.

Для текущего состояния проекта Room является единым persistence backend вместо SQLDelight.

### AndroidX Lifecycle

Используются:

- `lifecycle-viewmodel`
- `lifecycle-runtime-compose`

Что дают:

- общие `ViewModel`;
- интеграцию lifecycle c Compose;
- удобное управление UI-state в shared-коде.

## 2. Платформозависимые runtime-зависимости

Это уже не общий код, а platform-specific детали, без которых shared abstraction не может реально выполняться.

### Android

Используются:

- `androidx.activity:activity-compose`
- `io.ktor:ktor-client-okhttp`

Роль:

- запуск Compose UI через `ComponentActivity`;
- Android lifecycle entry point;
- Android TLS/network runtime через `OkHttp`;
- Android-specific filesystem/context для Room.

Почему это платформозависимо:

- в `commonMain` нет Android `Context`;
- в `commonMain` нельзя использовать Android Activity APIs;
- Android-specific engine нужен для реального network stack.

### iOS

Используется:

- `io.ktor:ktor-client-darwin`

Роль:

- реальный iOS/macOS Apple networking backend для Ktor.
- iOS-specific filesystem path для Room database file.
- Compose UIKit host app runtime через `iosApp`.

Почему платформозависимо:

- `commonMain` знает только про `ktor-client-core`, но не про конкретный iOS transport.

### Desktop

Используются:

- `compose.desktop.currentOs`
- `io.ktor:ktor-client-java`

Роль:

- desktop runtime для Compose;
- JVM HTTP engine для локального запуска и отладки.
- JVM filesystem path для Room database file.

### Aurora

Используются:

- `io.ktor:ktor-client-curl`
- Aurora-compatible версии BOM/библиотек:
  - `koin-bom:4.2.0-aurora`
  - `ktor-bom:3.4.2-aurora`
  - `kotlinx-datetime:0.7.1-aurora`

Роль:

- реальный HTTP engine для Aurora;
- совместимость зависимостей с Aurora runtime/toolchain;
- работа shared-кода внутри Aurora-target.

Почему это отдельная история:

- Aurora нельзя просто считать "ещё одним Linux" без оговорок;
- под неё нужны совместимые артефакты;
- иногда нужен отдельный BOM или отдельные версии.

## 3. Плагины и build-time инструменты

Это зависимости не рантайма приложения, а сборочной системы.

### Kotlin Multiplatform

Плагин:

- `kotlin("multiplatform")`

Нужен для:

- target-ов;
- sourceSet-ов;
- `expect/actual`;
- shared KMP build graph.

### Compose Multiplatform plugin

Плагин:

- `org.jetbrains.compose`

Нужен для:

- Compose Multiplatform integration;
- Compose resources;
- desktop support;
- Compose compiler integration.

### Kotlin Compose plugin

Плагин:

- `org.jetbrains.kotlin.plugin.compose`

Нужен для:

- корректной компиляции Compose с текущей версией Kotlin.

### Kotlin Serialization plugin

Плагин:

- `org.jetbrains.kotlin.plugin.serialization`

Нужен для:

- генерации сериализации моделей через `@Serializable`.

### KSP

Плагин:

- `com.google.devtools.ksp`

Нужен для:

- генерации кода Room;
- обработки `@Database`, `@Dao`, `@Entity`.

В проекте он подключён отдельно для нужных target-ов, включая Aurora target.

### Room Gradle plugin

Плагин:

- `androidx.room`

Нужен для:

- Room schema management;
- schema export;
- дополнительной Gradle-интеграции Room.

### Aurora Gradle plugins

Используются:

- `ru.auroraos.kmp.aurora-build`
- `ru.auroraos.kmp.aurora-devices`

Нужны для:

- Aurora target wiring;
- sysroot integration;
- packaging;
- RPM build;
- emulator install/run tasks.

Это уже не часть общего KMP runtime, а часть Aurora toolchain.

## Главное различие

Если кратко:

- Compose, Koin, Ktor core, coroutines, datetime, serialization, Room API -> это общий стек;
- `OkHttp`, `Darwin`, `Java`, `Curl`, Android `Activity`, Aurora plugins -> это платформенный glue layer;
- без первого не было бы shared application;
- без второго shared application не смог бы реально исполняться на каждой целевой платформе.

Именно на этой границе и строится архитектура всего PoC.
