# Portable Setup Verification

Date: 2026-07-03

## Goal

Проверить, что новый разработчик не упирается в скрытые абсолютные пути внутри живой конфигурации проекта и может понять prerequisites через явные команды проверки.

## What Was Cleaned Up

- В `README.md` добавлен раздел `First-Run Verification`.
- В корневой `build.gradle.kts` добавлены задачи:
  - `doctorMainEnvironment`
  - `doctorAuroraEnvironment`
  - `verifyMainTargets`
  - `verifyAuroraTarget`
- В `local.properties.example` убраны user-specific примеры и заменены на generic absolute placeholders.
- В `docs/readme-chapters/08-run-configurations-and-artifacts.md` абсолютные пути заменены на portable примеры и относительные build-артефакты.

## Absolute Path Scan

Проверка по живым файлам проекта:

```bash
rg -n '/Users/|/Applications/|ruslaneremeev|Documents/Work|Codex/2026-06-19|Android/Sdk' README.md docs .idea build.gradle.kts settings.gradle.kts composeApp local.properties.example --glob '!outputs/**' --glob '!**/build/**'
```

Результат:

- совпадений в активной конфигурации проекта не найдено;
- абсолютные пути остались только в исторических отчётах внутри `outputs/`, что не влияет на сборку и запуск после clone.

## Verification Commands

### Main targets

```bash
./gradlew --no-daemon doctorMainEnvironment
./gradlew --no-daemon --no-configuration-cache -PbuildVariant=main verifyMainTargets
```

Подтверждено:

- Android target собирается;
- Desktop target компилируется;
- iOS simulator framework собирается;
- Android SDK определяется без хардкода в проектных файлах.

Фактически прогнаны:

- `:composeApp:assembleDebug`
- `:composeApp:compileKotlinDesktop`
- `:composeApp:linkDebugFrameworkIosSimulatorArm64`

Итог:

- `BUILD SUCCESSFUL`

### Aurora target

```bash
./gradlew --no-daemon -PbuildVariant=aurora doctorAuroraEnvironment
./gradlew --no-daemon --no-configuration-cache -PbuildVariant=aurora verifyAuroraTarget
```

Подтверждено:

- Aurora Maven mirror в `work/aurora-maven-partial` найден;
- Aurora SSH key найден;
- Aurora debug pipeline собирается;
- x86_64 и aarch64 RPM пакеты формируются и подписываются.

Фактически прогнаны:

- `:composeApp:buildDebugPipeline`

Итог:

- `BUILD SUCCESSFUL`

## What Is Portable Now

- Android / Desktop / iOS зависимости подтягиваются обычным Gradle resolution flow.
- Android SDK path больше не зашит в проект.
- Android Studio run-конфиги используют `$PROJECT_DIR$`, а не абсолютные host-пути.
- Aurora SSH host/port/user/key переопределяются через `-P` свойства или env vars.
- JDK selection вынесен в `JAVA_HOME`, `MAIN_JAVA_HOME`, `AURORA_JAVA_HOME`.

## What Still Requires External Tooling

- Для iOS нужен установленный Xcode CLI toolchain.
- Для Aurora нужен установленный Aurora SDK / emulator runtime.
- Для Aurora всё ещё нужен Aurora-specific Maven mirror:
  - `work/aurora-maven-partial`

Это уже не hidden dependency внутри кода, а явный prerequisite toolchain.

## Practical Conclusion

Для `Android + Desktop + iOS` проект сейчас достаточно portable для clone-and-build сценария при наличии стандартного toolchain.

Для `Aurora` проект portable только при условии, что на машине есть подготовленный Aurora SDK и доступен локальный Aurora Maven mirror. Это ограничение связано не с hardcoded путями в коде PoC, а с самим состоянием Aurora toolchain и зависимостей.
