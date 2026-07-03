# Release 0.0.2 Packages

Дата проверки: `2026-07-03`

## Commit baseline

- base commit before final release commit: `66ccd93`

## Что изменилось в версии 0.0.2

- добавлен общий слой ресурсов через `Compose Resources`;
- строки и шрифты вынесены в общий `commonMain` resource-layer;
- добавлен и зафиксирован общий navigation flow через `Compose Navigation`;
- сохранён Aurora-friendly diagnostic bootstrap:
  - `Super Smoke`
  - `Render Smoke`
  - `Normal Demo`
  - `Drawable Experiment Lab`
- версия проекта поднята до `0.0.2`;
- для Aurora проведены отдельные повторные проверки проблемы `XML VectorDrawable`;
- для run-конфигураций Android Studio зафиксировано новое именование:
  - `Aurora | ...`
  - `Desktop | Run`
  - локально в IDE:
    - `Android | Run`
    - `IOS | Run`

## Состояние сборок по платформам

### Android

Статус:

- сборка успешна

Команда:

```bash
./gradlew --no-daemon -PbuildVariant=main :composeApp:assembleDebug
```

Артефакт:

- `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### Desktop

Статус:

- desktop jar собран
- native distributable через `jpackage` пока не считается успешно закрытым

Команды:

```bash
./gradlew --no-daemon -PbuildVariant=main :composeApp:packageDistributionForCurrentOS
./gradlew --no-daemon -Dorg.gradle.java.home="$JAVA_HOME_21" :composeApp:createDistributable
```

Что получилось:

- собран desktop jar:
  - `composeApp/build/libs/composeApp-desktop-0.0.2.jar`

Что не получилось:

- `createDistributable` падает на `jpackage`
- причина:
  - `The first number in an app-version cannot be zero or negative`
- это означает, что `0.0.2` как desktop native package version не принимается текущим `jpackage`

Практический вывод:

- desktop runtime и desktop jar собираются;
- для полноценного native desktop package нужно отдельно согласовать package version для `jpackage`.

### iOS

Статус:

- framework сборка успешна
- simulator app build успешен

Команды:

```bash
./gradlew --no-daemon :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro,OS=18.5' \
  build
```

Артефакты:

- framework:
  - `composeApp/build/bin/iosSimulatorArm64/debugFramework/ComposeApp.framework`
- simulator app:
  - `/Users/ruslaneremeev/Library/Developer/Xcode/DerivedData/iosApp-ahtvkemskgwwiagaaeblvzgdmeks/Build/Products/Debug-iphonesimulator/Aurora KMP Demo.app`

### Aurora

Статус:

- RPM для `aarch64` собран
- RPM для `x86_64` собран
- обе сборки имеют версию `0.0.2-1`

Команда:

```bash
./gradlew --no-daemon -PbuildVariant=aurora \
  :composeApp:buildDebugPackageAuroraArm64 \
  :composeApp:buildDebugPackageAuroraX64
```

Артефакты:

- `composeApp/build/rpm/debug/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.2-1.aarch64.rpm`
- `composeApp/build/rpm/debug/x86_64/RPMS/x86_64/com.example.aurorakmpdemo-0.0.2-1.x86_64.rpm`
- `composeApp/build/rpm/debug/aarch64/SRPMS/com.example.aurorakmpdemo-0.0.2-1.src.rpm`
- `composeApp/build/rpm/debug/x86_64/SRPMS/com.example.aurorakmpdemo-0.0.2-1.src.rpm`

Дополнительно:

- `debuginfo` и `debugsource` RPM тоже собираются для обеих архитектур.

## Известные ограничения, сохраняющиеся в 0.0.2

- Aurora:
  - `XML VectorDrawable` пока остаётся проблемным в diagnostic flow;
- Android:
  - `SVG` пока не считается беспроблемным общим drawable-форматом в текущем PoC;
- Desktop native packaging:
  - `jpackage` не принимает `0.0.2` как valid app-version для native distributable;
- Aurora repeated launch:
  - всё ещё остаётся исторически подтверждённый риск повторного старта только с красным root background.
