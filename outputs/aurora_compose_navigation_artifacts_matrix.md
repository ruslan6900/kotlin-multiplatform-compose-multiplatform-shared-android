# Aurora Compose Navigation Artifacts Matrix

## Checked Locations

- `mavenLocal()`
- Gradle module cache
- local Aurora work directories / mirrored artifact caches

## Navigation Artifact Summary

| Artifact line | Version | Common metadata | linuxX64 | linuxArm64 | Aurora-specific variant | Aurora suitability |
|---|---:|---|---|---|---|---|
| `org.jetbrains.androidx.navigation:navigation-compose` | `2.9.2` | yes | not found for Aurora use case | not found for Aurora use case | no | failed previously |
| `org.jetbrains.androidx.navigation:navigation-compose` | `9999.0.0-SNAPSHOT` | yes | yes | yes | no explicit `auroraX64/auroraArm64` | works via Linux native variant |
| `org.jetbrains.androidx.navigation:navigation-runtime` | `9999.0.0-SNAPSHOT` | yes | yes | yes | no explicit `aurora*` | works transitively |
| `org.jetbrains.androidx.navigation:navigation-common` | `9999.0.0-SNAPSHOT` | yes | yes | yes | no explicit `aurora*` | works transitively |

## Observed Variant Facts

From local `.module` files in `~/.m2/repository`:

### `navigation-compose:9999.0.0-SNAPSHOT`

- `metadataApiElements`
- `linuxX64ApiElements-published`
- `linuxArm64ApiElements-published`
- also Android/JVM/iOS/macOS style variants

### `navigation-runtime:9999.0.0-SNAPSHOT`

- `metadataApiElements`
- `linuxX64ApiElements-published`
- `linuxArm64ApiElements-published`

### `navigation-common:9999.0.0-SNAPSHOT`

- `metadataApiElements`
- `linuxX64ApiElements-published`
- `linuxArm64ApiElements-published`

## Requested vs Selected Variant

Command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:dependencyInsight \
  --dependency org.jetbrains.androidx.navigation:navigation-compose \
  --configuration auroraX64CompileKlibraries
```

Selected by Gradle:

- component: `org.jetbrains.androidx.navigation:navigation-compose:9999.0.0-SNAPSHOT`
- variant: `linuxX64ApiElements-published`

Attributes matched:

- `org.jetbrains.kotlin.platform.type = native`
- `org.jetbrains.kotlin.native.target = linux_x64`
- `org.gradle.jvm.environment = non-jvm`

## Linux/Aurora Compatibility Conclusion

Aurora KMP target does not need a dedicated `auroraX64` or `auroraArm64` variant if:

- the artifact publishes compatible Linux Kotlin/Native variants;
- Gradle can match Aurora target expectations to Linux native target attributes.

In this project the successful mapping was:

- `auroraX64` consumer -> `linuxX64ApiElements-published`

The same packaging pipeline also passed for `auroraArm64`, which strongly indicates the Linux ARM64 native variant chain is compatible too.

## Why The Old Setup Failed

The old assumption was:

- no `aurora*` variant published
- therefore Aurora cannot use Compose Navigation

What was actually true:

- the specific earlier version line did not expose the needed native Linux variant for this target;
- the local snapshot line does expose Linux native variants;
- after switching Aurora to the snapshot line, Compose Navigation resolves and compiles.

## Final Matrix Verdict

| Question | Answer |
|---|---|
| Is explicit `auroraX64` variant required? | No |
| Can Aurora use Linux native navigation variants? | Yes |
| Was the blocker a pure Gradle resolution/artifact publication issue? | Yes |
| Was there evidence of ABI/KLIB incompatibility after switching? | No |
| Did the Aurora full build pipeline pass with navigation enabled? | Yes |
