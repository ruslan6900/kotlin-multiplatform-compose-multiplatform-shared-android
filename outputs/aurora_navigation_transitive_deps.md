# Aurora Navigation Transitive Dependencies

## Source of Truth

Primary inspection commands:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:dependencyInsight \
  --dependency org.jetbrains.androidx.navigation:navigation-compose \
  --configuration auroraX64CompileKlibraries

./gradlew --no-daemon -PbuildVariant=aurora :composeApp:dependencies \
  --configuration auroraX64CompileKlibraries
```

Module metadata was also inspected in:

- `~/.m2/repository/org/jetbrains/androidx/navigation/navigation-compose/9999.0.0-SNAPSHOT/*.module`
- `~/.m2/repository/org/jetbrains/androidx/navigation/navigation-runtime/9999.0.0-SNAPSHOT/*.module`
- `~/.m2/repository/org/jetbrains/androidx/navigation/navigation-common/9999.0.0-SNAPSHOT/*.module`

## Direct Aurora Navigation Dependency

Aurora build currently uses:

```kotlin
implementation("org.jetbrains.androidx.navigation:navigation-compose:9999.0.0-SNAPSHOT")
```

Resolved variant for `auroraX64CompileKlibraries`:

- `linuxX64ApiElements-published`

## Key Transitive Dependencies

### Navigation layer

- `org.jetbrains.androidx.navigation:navigation-common:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.navigation:navigation-runtime:9999.0.0-SNAPSHOT`

### Lifecycle layer

- `org.jetbrains.androidx.lifecycle:lifecycle-common:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:9999.0.0-SNAPSHOT`

### SavedState layer

- `org.jetbrains.androidx.savedstate:savedstate:9999.0.0-SNAPSHOT`
- `org.jetbrains.androidx.savedstate:savedstate-compose:9999.0.0-SNAPSHOT`

### Compose layer

- `org.jetbrains.compose.runtime:runtime:9999.0.0-SNAPSHOT`
- `org.jetbrains.compose.runtime:runtime-saveable:9999.0.0-SNAPSHOT`
- `org.jetbrains.compose.foundation:foundation-layout:9999.0.0-SNAPSHOT`
- `org.jetbrains.compose.animation:animation:9999.0.0-SNAPSHOT`
- `org.jetbrains.compose.ui:ui-backhandler:9999.0.0-SNAPSHOT`

## Important Compatibility Observation

The navigation snapshot line is not isolated. It expects a matching ecosystem around it:

- JetBrains AndroidX lifecycle snapshot/native artifacts
- JetBrains AndroidX savedstate snapshot/native artifacts
- Compose native snapshot artifacts

This is why the local snapshot repository matters so much. Pulling only `navigation-compose` without its native-compatible transitive graph would not be enough.

## What Did Not Conflict

During the successful Aurora compile and full build pipeline:

- no KLIB target mismatch was reported;
- no native ABI mismatch was reported;
- no Gradle attribute incompatibility remained after the snapshot line was selected;
- `compileKotlinAuroraX64` passed;
- `compileKotlinAuroraArm64` passed;
- `buildDebugPipeline` passed.

## Known Version Split

Current project state intentionally keeps different navigation coordinates by build variant:

- main build: `org.jetbrains.androidx.navigation:navigation-compose:2.9.2`
- Aurora build: `org.jetbrains.androidx.navigation:navigation-compose:9999.0.0-SNAPSHOT`

That split is acceptable in this PoC because:

- the source-level API used by `NormalDemoContent.kt` is the same;
- Aurora needs the native Linux-published line;
- Android/Desktop/iOS already build successfully with their current line.

## Recommendation

If this approach is moved into a larger product, keep these rules:

1. Treat Aurora navigation as a native Linux variant resolution problem, not as an Android-only dependency problem.
2. Lock the full Aurora navigation/lifecycle/savedstate snapshot family together.
3. Verify `dependencyInsight` after any version bump, not just `compileKotlinAuroraX64`.
4. Keep a documented local or mirrored repository source for the required snapshot-native artifacts.
