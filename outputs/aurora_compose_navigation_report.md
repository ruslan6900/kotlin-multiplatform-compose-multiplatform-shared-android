# Aurora Compose Navigation Report

## Goal

Enable real Compose Navigation on Aurora KMP using:

- `rememberNavController()`
- `NavHost(...)`
- `composable(...)`
- `navController.navigate(...)`

without keeping Aurora on fallback screen switching by plain mutable state.

## Workspace

- Project: `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`
- Date: `2026-07-02`

## Baseline Verification

Commands:

```bash
pwd
git status --short
java -version
./gradlew --version
./gradlew --no-daemon -PbuildVariant=main :composeApp:compileDebugKotlinAndroid
./gradlew --no-daemon :composeApp:compileKotlinDesktop
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:compileKotlinAuroraX64 --stacktrace
```

Results:

- Android compile: PASS
- Desktop compile: PASS
- Aurora compile: PASS

## What Was Proven

### 1. Aurora can resolve real Compose Navigation

The key successful command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:dependencyInsight \
  --dependency org.jetbrains.androidx.navigation:navigation-compose \
  --configuration auroraX64CompileKlibraries
```

Observed resolution:

- `org.jetbrains.androidx.navigation:navigation-compose:9999.0.0-SNAPSHOT`
- variant selected by Gradle: `linuxX64ApiElements-published`
- requested native target: `linux_x64`

This proves Aurora is able to consume a Linux Kotlin/Native navigation variant.

### 2. The previous blocker was artifact availability, not NavHost itself

The earlier failure with `org.jetbrains.androidx.navigation:navigation-compose:2.9.2` was caused by the absence of a matching Linux native variant for Aurora resolution.

Meaning:

- the problem was not "Compose Navigation cannot work on Aurora";
- the problem was "the chosen published artifact line does not include the needed native Linux variant".

### 3. Local snapshot artifacts provide the missing native variants

The local artifact line `9999.0.0-SNAPSHOT` contains:

- `metadataApiElements`
- `linuxX64ApiElements-published`
- `linuxArm64ApiElements-published`

for:

- `navigation-compose`
- `navigation-runtime`
- `navigation-common`

### 4. The shared UI now uses a commonMain NavHost

Current navigation implementation is in:

- [NormalDemoContent.kt](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/src/commonMain/kotlin/com/example/aurorakmpdemo/ui/NormalDemoContent.kt)

It now uses:

- `rememberNavController()`
- `NavHost`
- `composable`
- `navController.navigate(...)`

for all `AuroraDemoRoute` entries directly in `commonMain`.

No platform-specific `NormalDemoContent.*.kt` files remain.

## Build Configuration Used

Aurora build file:

- [build.aurora.gradle.kts](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/build.aurora.gradle.kts)

Important detail:

```kotlin
val auroraNavigationVersion = "9999.0.0-SNAPSHOT"
implementation("org.jetbrains.androidx.navigation:navigation-compose:$auroraNavigationVersion")
```

This lets Aurora use the local native snapshot line while main Android/Desktop/iOS build logic can keep using its own dependency graph.

## End-to-End Build Verification

Extra full Aurora packaging check:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
```

Result:

- PASS
- `compileKotlinAuroraX64`: PASS
- `compileKotlinAuroraArm64`: PASS
- `linkDebugExecutableAuroraX64`: PASS
- `linkDebugExecutableAuroraArm64`: PASS
- `buildDebugPipeline`: PASS

RPM artifacts were rebuilt successfully for:

- `x86_64`
- `aarch64`

## Practical Conclusion

Real Compose Navigation on Aurora is feasible in this PoC.

The important nuance is:

- official/public `navigation-compose` version line used earlier was insufficient for Aurora native resolution;
- local snapshot artifacts with Linux native variants are sufficient;
- Aurora can consume Linux native KLIB variants;
- true `NavHost` can live in `commonMain`.

## Remaining Caveats

- The top-level boot selector between `SuperSmoke`, `RenderSmoke`, and `NormalDemo` still exists in [App.kt](/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android/composeApp/src/commonMain/kotlin/com/example/aurorakmpdemo/App.kt). That is a startup/diagnostic mode switch, not the normal page navigation mechanism.
- Reproducibility depends on access to the local snapshot/native Aurora-compatible artifact line (`9999.0.0-SNAPSHOT`) via `mavenLocal()` or the mirrored local Aurora Maven cache.
- This report proves build-time compatibility. Runtime visual behavior on Aurora emulator should still be validated separately after install/run if we want a final UI-runtime certification.

## Final Verdict

- `NavHost` on Aurora: YES
- `rememberNavController()` on Aurora: YES
- `navController.navigate(...)` on Aurora build graph: YES
- commonMain shared navigation implementation: YES
- fallback state navigation required for Home/Network/Database pages: NO
