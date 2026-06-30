## Run-конфигурации Android Studio и артефакты сборки

В этом PoC важен не только исходный код, но и удобство повседневного запуска.

Именно поэтому в проект были добавлены shared run-конфигурации для Android Studio, чтобы Aurora-сценарии можно было запускать не только руками из терминала, но и напрямую из IDE.

Это особенно полезно для Aurora, потому что её workflow заметно отличается от привычного Android-сценария:

- сначала нужно собрать Aurora-пайплайн;
- затем упаковать приложение в RPM;
- затем установить его в emulator;
- затем выполнить запуск;
- иногда дополнительно убить старый процесс и прогнать сценарий заново.

## 1. Где лежат run-конфигурации

Конфигурации сохранены в проекте по пути:

[`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/.idea/runConfigurations`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/.idea/runConfigurations)

Сейчас там находятся:

- `Aurora Build Debug`
- `Aurora Install Debug To Emulator`
- `Aurora Kill App On Emulator`
- `Aurora Run Debug On Emulator`
- `Aurora Rebuild + Run Cleanly`

Эти XML-файлы лежат в проекте как shared configuration files, а значит могут быть закоммичены в Git и доступны другим разработчикам после clone/pull.

Важно:

- шарятся именно файлы из `.idea/runConfigurations`;
- локальный `.idea/workspace.xml` в это не входит;
- поэтому персональные IDE-состояния не смешиваются с общими проектными конфигами.

## 2. Что делает каждая Aurora-конфигурация

### `Aurora Build Debug`

Эта конфигурация запускает:

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline
```

Назначение:

- собрать Aurora-вариант проекта;
- подготовить RPM и сопутствующие build artifacts;
- не выполнять автоматический запуск приложения.

Когда использовать:

- если нужно просто проверить сборку;
- если нужно обновить RPM-пакет;
- если перед запуском хочется посмотреть, что именно собралось.

### `Aurora Install Debug To Emulator`

Эта конфигурация запускает:

```bash
./gradlew -PbuildVariant=aurora :composeApp:installDebugToEmulator
```

Назначение:

- установить уже собранный debug-пакет в Aurora emulator.

Когда использовать:

- если RPM уже собран;
- если нужно отдельно проверить стадию установки;
- если хочется разделить build и install на два отдельных шага.

### `Aurora Kill App On Emulator`

Эта конфигурация запускает:

```bash
./gradlew -PbuildVariant=aurora :composeApp:killAuroraDemoOnEmulator
```

Назначение:

- завершить процесс приложения в эмуляторе.

Когда использовать:

- если приложение зависло;
- если нужно проверить повторный запуск;
- если хочется исключить сценарий, где старый процесс ещё жив;
- если идёт диагностика проблем жизненного цикла на Aurora.

### `Aurora Run Debug On Emulator`

Эта конфигурация запускает before-run сборку, а затем:

```bash
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming
```

Что делает по сути:

1. сначала запускает `:composeApp:buildDebugPipeline`;
2. затем выполняет запуск на эмуляторе;
3. стримит вывод запуска.

Когда использовать:

- это основной рабочий сценарий из IDE;
- если нужно быстро пересобрать и сразу увидеть результат;
- если хочется запускать Aurora примерно так же удобно, как обычный Run в Android Studio.

### `Aurora Rebuild + Run Cleanly`

Эта конфигурация похожа на предыдущую, но использует `--rerun-tasks`.

Фактически она делает:

1. `:composeApp:buildDebugPipeline` с `--rerun-tasks`;
2. затем `:composeApp:runDebugOnEmulatorNoSandboxStreaming` тоже с `--rerun-tasks`.

Назначение:

- принудительно прогнать build/run pipeline заново;
- уменьшить вероятность того, что IDE/Gradle переиспользуют неожиданный кэш;
- получить более "чистый" повторный сценарий запуска.

Когда использовать:

- если поведение приложения после обычного Run выглядит подозрительно;
- если нужно сравнить "обычный повторный запуск" и "запуск после полного rebuild";
- если идёт диагностика нестабильного UI на Aurora.

Важно:

- для этого проекта ранее уже находился edge case, где `clean` мог ломать sysroot;
- поэтому конфигурация использует `--rerun-tasks`, а не aggressive `clean`.

## 3. Какие команды соответствуют конфигурациям

Если запускать не из IDE, а из терминала, то рабочие команды такие:

### Aurora build

```bash
./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline
```

### Aurora install

```bash
./gradlew -PbuildVariant=aurora :composeApp:installDebugToEmulator
```

### Aurora run

```bash
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

### Aurora kill app

```bash
./gradlew -PbuildVariant=aurora :composeApp:killAuroraDemoOnEmulator
```

### Aurora rebuild + rerun

```bash
./gradlew -PbuildVariant=aurora --rerun-tasks :composeApp:buildDebugPipeline
./gradlew -PbuildVariant=aurora --rerun-tasks :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

### Android debug build

```bash
./gradlew -PbuildVariant=main :composeApp:assembleDebug
```

### iOS simulator build

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.5' \
  build
```

### iOS simulator install + run

```bash
xcrun simctl install 93D328E6-4DF9-4F12-ABD6-49BA0BA737ED \
'/Users/ruslaneremeev/Library/Developer/Xcode/DerivedData/iosApp-ahtvkemskgwwiagaaeblvzgdmeks/Build/Products/Debug-iphonesimulator/Aurora KMP Demo.app'

xcrun simctl launch 93D328E6-4DF9-4F12-ABD6-49BA0BA737ED com.example.aurorakmpdemo.ios
```

## 4. Куда сохраняются собранные артефакты

### Android APK

Android debug APK лежит по пути:

[`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/outputs/apk/debug/composeApp-debug.apk`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/outputs/apk/debug/composeApp-debug.apk)

Также встречается intermediate APK:

[`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/intermediates/apk/debug/composeApp-debug.apk`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/intermediates/apk/debug/composeApp-debug.apk)

Практически ориентироваться лучше на `build/outputs/apk/...`.

### Aurora RPM

Основной x86_64 RPM для эмулятора лежит по пути:

[`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/RPMS/x86_64/com.example.aurorakmpdemo-0.0.1-1.x86_64.rpm`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/RPMS/x86_64/com.example.aurorakmpdemo-0.0.1-1.x86_64.rpm)

Сопутствующие RPM рядом:

- `debuginfo`
- `debugsource`

Есть и `src.rpm`, и `aarch64` вариант для других сценариев:

- [`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/SRPMS/com.example.aurorakmpdemo-0.0.1-1.src.rpm`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/SRPMS/com.example.aurorakmpdemo-0.0.1-1.src.rpm)
- [`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.1-1.aarch64.rpm`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.1-1.aarch64.rpm)

Если цель - запуск именно в текущем Aurora emulator на Mac, обычно смотреть нужно прежде всего на `x86_64` debug RPM.

### iOS app bundle

iOS host app после `xcodebuild` лежит по пути:

[`/Users/ruslaneremeev/Library/Developer/Xcode/DerivedData/iosApp-ahtvkemskgwwiagaaeblvzgdmeks/Build/Products/Debug-iphonesimulator/Aurora KMP Demo.app`](/Users/ruslaneremeev/Library/Developer/Xcode/DerivedData/iosApp-ahtvkemskgwwiagaaeblvzgdmeks/Build/Products/Debug-iphonesimulator/Aurora%20KMP%20Demo.app)

## 5. Какие служебные Aurora-артефакты тоже появляются

Кроме RPM, в build-дереве появляются:

- `.desktop` файл приложения;
- `spec` файл RPM;
- иконки;
- bundled resources;
- вспомогательные списки файлов RPM package.

Например:

- [`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/applications/com.example.aurorakmpdemo.desktop`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/applications/com.example.aurorakmpdemo.desktop)
- [`/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/com.example.aurorakmpdemo.spec`](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build/rpm/debug/x86_64/com.example.aurorakmpdemo.spec)

Это полезно, если нужно:

- проверить launcher metadata;
- посмотреть package identity;
- сравнить packaging с другими Aurora sample project;
- исследовать проблемы установки или запуска.

## 6. Какой сценарий запуска использовать каждый день

Для обычной работы из Android Studio наиболее удобный порядок такой:

1. `Aurora Run Debug On Emulator` - основной ежедневный запуск;
2. `Aurora Rebuild + Run Cleanly` - если нужно перепроверить запуск после rebuild;
3. `Aurora Kill App On Emulator` - если есть подозрение на зависший старый процесс;
4. `Aurora Install Debug To Emulator` - если нужно отдельно проверить установку;
5. `Aurora Build Debug` - если нужен только build без запуска.

Если работать из терминала, основной практический сценарий такой:

```bash
cd /Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android
./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

Именно этот сценарий в ходе PoC чаще всего давал наиболее предсказуемый результат.

Важно:

- в Android Studio shared Gradle run-конфигурации для этого проекта не используют `--no-daemon`;
- раньше этот флаг там приводил к ошибке `Unknown command-line option '--no-daemon'`;
- поэтому для IDE-конфигов был оставлен только `-PbuildVariant=...`, а `--rerun-tasks` сохранён только там, где он действительно нужен.

## 7. Можно ли управлять Aurora emulator из Android Studio как Android AVD

На текущем состоянии проекта и toolchain ответ практический такой:

- Aurora run-конфигурации можно запускать прямо из Android Studio;
- но сам Aurora emulator не становится обычным Android Device Manager-устройством.

Причина в том, что это не Android AVD и не ADB-устройство в стандартном смысле.

Запуск и взаимодействие здесь идут через Aurora SDK/tooling, а не через стандартный Android Emulator integration path.

Поэтому Android Studio в этом PoC используется как удобная IDE и launcher Gradle-конфигураций, но не как нативный менеджер Aurora emulator.

## 8. Что важно закоммитить в Git

Если нужно, чтобы run-конфигурации были доступны всей команде, в репозиторий имеет смысл коммитить:

- `.idea/runConfigurations/*.xml`

При этом обычно не нужно коммитить:

- `.idea/workspace.xml`
- локальные кэши;
- `build/`
- `.gradle/`

Для этого проекта run-конфиги уже оформлены именно в том виде, который подходит для командной работы: общие XML-файлы отдельно, а личное IDE-состояние отдельно.
