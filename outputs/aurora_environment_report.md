# Aurora Environment Report

Date: 2026-06-22  
Workspace: `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`

> Note:
> This report includes earlier exploration phases of the PoC.
> Some later sections still mention the old SQLDelight-based Aurora experiments and pre-iOS-Room state.
> The current codebase has since moved to Room on Android, iOS, Desktop, and Aurora.

## Workspace Migration

- Old project path:
  - `/Users/ruslaneremeev/Documents/Codex/2026-06-19/kotlin-multiplatform-compose-multiplatform-shared-android`
- New project path:
  - `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`
- `~/Documents/Work/Aurora` status:
  - directory existed after `mkdir -p` and is now the canonical root for Aurora PoC work
- Migration status:
  - project was moved into the new workspace path
  - no conflicting project directory existed at the target path before migration
- Commands that confirmed the project works from the new location:
  - `cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android && pwd`
  - `cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android && ls -la`
  - `cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android && ls -la composeApp`
  - `cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android && ls -la outputs || true`
  - `./gradlew --no-daemon -PbuildVariant=main :composeApp:assembleDebug`
  - `./gradlew --no-daemon -PbuildVariant=aurora :composeApp:tasks`

## Goal

Continue the Kotlin Multiplatform / Compose Multiplatform PoC and push Aurora OS support as far as possible on macOS Apple Silicon, including official Aurora SDK/tooling hookup, build verification, and readiness for emulator deployment.

## Host Environment

- macOS `15.7.7` (`24G720`)
- Architecture: `arm64` (Apple Silicon / M1-compatible)
- Launcher Java: `Oracle GraalVM 21.0.7`
- Gradle wrapper JDK selection:
  - `Java 17` for `-PbuildVariant=main`
  - `Java 21` for `-PbuildVariant=aurora`
- Gradle: `8.14.3`
- Android SDK: `/Users/ruslaneremeev/Android/Sdk`
- Docker CLI: installed
- Docker daemon: available and working

## Official Aurora Sources Used

- Aurora SDK BT 5.2.0.180 release notes: <https://developer.auroraos.ru/release_notes/sdk_bt_5.2.0.180>
- Aurora SDK BT archive downloads: <https://developer.auroraos.ru/downloads/archive/sdk_bt>
- Aurora SDK setup docs: <https://developer.auroraos.ru/doc/5.2.0/sdk/app_development/setup>
- Aurora CLI / emulator SSH docs: <https://developer.auroraos.ru/doc/sdk/app_development/build/build_engine/cli_emulator_device>
- Aurora Q&A: <https://developer.auroraos.ru/qa>

Key facts verified from official Aurora docs:

- Aurora SDK BT `5.2.0.180` is the relevant SDK release.
- macOS ARM64 / Apple M-series is supported.
- QEMU is used for emulation in current SDK generation.
- Emulator SSH path is expected through `~/AuroraOS/vmshare/ssh/private_keys/sdk`.

## What Was Connected

### Aurora Maven

- Local Aurora Maven mirror/checkout is present at:
  - `work/aurora-maven-partial`
- Aurora artifacts were synced into local Maven cache:
  - `~/.m2/repository`
- This allowed Gradle to resolve Aurora-specific artifacts and plugins.

### Aurora Build Tools Plugins

- Official Aurora Gradle plugins were published locally from:
  - `work/aurora-build-tools`
- Available/working plugin IDs:
  - `ru.auroraos.kmp.aurora-build`
  - `ru.auroraos.kmp.aurora-devices`

### Project Configuration

- `settings.gradle.kts` now conditionally adds Aurora Maven only for `-PbuildVariant=aurora`.
- The repo keeps split variants:
  - `main` -> `composeApp/build.gradle.kts`
  - `aurora` -> `composeApp/build.aurora.gradle.kts`

This isolation is important: upstream Android/iOS/Desktop build flow remains separate from Aurora-specific dependency versions.

## Code Fixes Made During This Run

- Fixed a shared-layer regression where `commonMain` depended on Aurora SQLDelight generated types.
- Moved `SqlDelightPostsStorage` out of `commonMain` into `auroraMain`.
- `commonMain` now stays platform-neutral again, and Android build works after the fix.
- Removed hardcoded `org.gradle.java.home` from `gradle.properties`.
- Patched `gradlew` to auto-select:
  - `Liberica 17` for Android/main
  - `GraalVM 21` for Aurora
- Added Aurora-only render smoke boot mode through `shouldUseAuroraRenderSmokeTest()`.
- Added `AuroraRenderSmokeScreen`, which bypasses:
  - `KoinApplication`
  - `MaterialTheme`
  - `Surface`
  - `Scaffold`
  - navigation
  - Compose resources on the first screen
- The Aurora smoke screen still exposes diagnostic buttons for:
  - Ktor
  - SQLDelight
  - log clearing
  - switching back to the normal demo app

This change directly supports the target architecture of:

- `commonMain`
- `androidMain`
- `iosMain`
- `auroraMain`

with Aurora-specific storage implementation only in `auroraMain`.

## Aurora SDK Installer Status

Official SDK installer downloaded:

- File: `work/sdk-installer/AuroraSDK-5.2.0.180-BT-release-mac-arm64-online.dmg`
- Official online ARM64 macOS installer MD5 from Aurora downloads page:
  - `ab38253df5c153cd2e9cf8b0680f8a80`
- Local MD5:
  - `ab38253df5c153cd2e9cf8b0680f8a80`

Status:

- DMG mounted successfully.
- Installer binary is accessible.
- `7z` was installed locally to inspect archives.
- Full Aurora SDK install is now completed in:
  - `~/AuroraOS`
- Installed SDK content now includes:
  - `~/AuroraOS/sdk`
  - `~/AuroraOS/emulator`
  - `~/AuroraOS/vmshare/ssh/private_keys/sdk`
  - Aurora Docker build-tools image on local Docker host

## Docker Status

Docker is now working.

Current status:

- `docker info` succeeds
- local Aurora image is present:
  - `aurora-build-tools:5.2.0.180`

This removed the original `initSysroot` blocker.

## Build Verification

### 1. Android / main variant

Command:

```bash
./gradlew --no-daemon -PbuildVariant=main :composeApp:assembleDebug
```

Result:

- `BUILD SUCCESSFUL`

Notes:

- Build was restored after moving Aurora SQLDelight code out of `commonMain`.
- There is a deprecation warning in Koin Compose API usage:
  - `KoinApplication(...)` old lambda-based overload is deprecated

### 2. Aurora task discovery

Command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:tasks
```

Result:

- `BUILD SUCCESSFUL`
- Aurora tasks are registered and visible.

Important available tasks:

- `initSysroot`
- `buildDebugPackageAuroraArm64`
- `buildDebugPackageAuroraX64`
- `buildDebugPipeline`
- `buildReleasePackageAuroraArm64`
- `buildReleasePackageAuroraX64`
- `installDebugToEmulator`
- `runDebugOnEmulator`

This confirms the official Aurora Gradle plugins are integrated correctly enough for Gradle task wiring.

### 3. Aurora package pipeline

Command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
```

Result:

- `BUILD SUCCESSFUL`

Artifacts produced:

- `composeApp/build/rpm/debug/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.1-1.aarch64.rpm`
- `composeApp/build/rpm/debug/x86_64/RPMS/x86_64/com.example.aurorakmpdemo-0.0.1-1.x86_64.rpm`

Important verified stages:

- `:composeApp:compileKotlinAuroraArm64`
- `:composeApp:compileKotlinAuroraX64`
- `:composeApp:linkDebugExecutableAuroraArm64`
- `:composeApp:linkDebugExecutableAuroraX64`
- `:composeApp:buildDebugPackageAuroraArm64`
- `:composeApp:buildDebugPackageAuroraX64`
- RPM validation succeeded for both architectures
- RPM signing succeeded using `~/AuroraOS/package-signing`

### 4. Aurora sysroot init

Command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:initSysroot
```

Result:

- `BUILD SUCCESSFUL`

Observed output included:

- `Copying sysroot for aarch64`
- `Copying sysroot for x86_64`
- `Sysroot copied successfully`

This confirms:

- Docker works
- Aurora build image works
- Aurora SDK/sysroot integration path is valid on this machine

### 5. Resolved Aurora compile blockers

The earlier blocker was incomplete/unusable native Maven artifacts from `aurora-maven`.

Observed pattern:

- many `.klib` files in local Aurora Maven mirror are Git LFS pointer files, not real binaries
- example pointer content:

```text
version https://git-lfs.github.com/spec/v1
oid sha256:...
size ...
```

Examples encountered:

- `media.kamel:kamel-image-default-linuxarm64`
- `ru.auroraos.kmp:ak-path-info-linuxarm64`
- `org.jetbrains.compose.material3:material3-linuxarm64`
- `org.jetbrains.compose.foundation:foundation-linuxarm64`
- `org.jetbrains.compose.runtime:runtime-linuxarm64`
- `org.jetbrains.compose.ui:ui-linuxarm64`

Important findings:

- after `hub.mos.ru` access was restored, targeted `git lfs pull` worked for the missing native artifacts
- rehydrated artifact groups included:
  - Compose native (`runtime`, `ui`, `foundation`, `material3`, `components-resources`)
  - Ktor native (`ktor-client-core`, `ktor-http`, `ktor-network`, `ktor-serialization-kotlinx-json`)
  - Koin native
  - Skiko native (`skiko-linuxarm64`, `skiko-linuxx64`)
- these hydrated artifacts were synced into `~/.m2/repository` and Gradle cache entries were refreshed
- after that, Aurora native compilation succeeded

### 6. Temporary code adjustments made to push Aurora further

To reduce dependency on broken Aurora artifacts:

- removed optional Aurora `Kamel` dependency from `build.aurora.gradle.kts`
- introduced `expect/actual` string provider
- kept Compose resources usage on `main` platforms
- switched Aurora string resolution to hardcoded fallback strings
- disabled Aurora KInterop dependencies in the Aurora build variant for now
- replaced Aurora diagnostics with temporary stub diagnostics

These changes allowed the Aurora build to move to completion while keeping the PoC small and stable.

### 7. Aurora packaging fixes made after compile succeeded

- Aurora `Main.kt` entrypoint was adapted to Aurora Compose API:
  - changed from desktop-style `Window(...)` wrapper to plain `application { App() }`
- Aurora RPM validation required explicit third-party library allowance:
  - added `libs3rdParty.set(listOf("maliit-glib"))`
- this resolved the RPM content validation error for `libmaliit-glib.so.0`

### 8. Emulator deployment status

Commands attempted:

```bash
./gradlew -PbuildVariant=aurora :composeApp:installDebugToEmulator
'/Users/ruslaneremeev/AuroraOS/Qt Creator.app/Contents/MacOS/sfdk' emulator start --headless
```

Observed result:

- `sfdk emulator list` sees installed emulator `AuroraOS-5.2.0.180`
- `sfdk emulator status` briefly reports `выполнение: да`
- a `qemu-system-aarch64` process starts on macOS with `-accel hvf`
- port `2223` is exposed transiently, matching Aurora emulator SSH forwarding
- shortly after start, emulator is no longer running
- `installDebugToEmulator` fails with:
  - `ssh: connect to host 127.0.0.1 port 2223: Connection refused`

Practical conclusion:

- Aurora build/deploy tooling is configured correctly enough to target the emulator
- the remaining blocker is emulator runtime stability/boot completion on this host, not Kotlin/Gradle packaging

### 9. Emulator control findings after migration

New findings from the migrated workspace:

- `sfdk` user config needed a valid:
  - `~/.config/AuroraOS-SDK-5.2.0.180-BT/libsfdk/buildengines.xml`
- the SDK bundle also lacked a system-wide:
  - `~/AuroraOS/Qt Creator.app/Contents/Resources/AuroraOS-SDK-5.2.0.180-BT/libsfdk/buildengines.xml`
- after adding the missing system-wide file, the fatal `system-wide build engine configuration` warning stopped appearing
- however, `sfdk emulator start --headless` still did not reliably bring `qemu` up on this machine
- for continued testing, Aurora emulator was started directly with bundled:
  - `~/AuroraOS/share/qemu/bin/qemu-system-aarch64`

This means emulator lifecycle control through `sfdk` is still unreliable here, while the actual Aurora QEMU image itself is runnable.

### 10. Control experiment with official sample

To distinguish project-specific issues from platform/runtime issues, the local Aurora sample project was built and tested:

- sample path:
  - `work/student-box-cmp`
- sample build:
  - `./gradlew --no-daemon -p work/student-box-cmp -PbuildVariant=aurora buildDebugPipeline`
  - result: `BUILD SUCCESSFUL`
- sample run:
  - `./gradlew --no-daemon -p work/student-box-cmp -PbuildVariant=aurora runDebugOnEmulator`
  - result: same failure as the PoC

Observed sample runtime error:

- `App starting failed`
- `Did not receive a reply`

This is a strong signal that the remaining failure is not caused only by the PoC application code or manifest. It also affects the official local Aurora Compose sample on this host/emulator/runtime combination.

### 11. Partial RuntimeManager workaround

Further manual testing showed:

- standard launcher path:
  - `runtime-manager-tool Control startDebug <appId> --output-to-console`
  - often fails or gets stuck in launcher state
- workaround path:
  - `runtime-manager-tool Control startDebug com.example.aurorakmpdemo --nosandbox --detach --output-to-console`
  - returns `Reply from Control::StartDebug()`
  - returns `Instance id` and `Pid`

Project change added:

- custom task:
  - `:composeApp:runDebugOnEmulatorNoSandbox`

Behavior of the workaround task:

- installs the debug RPM successfully
- calls `RuntimeManager` with `--nosandbox --detach`
- receives a successful `StartDebug` reply

Limitation:

- Aurora `RuntimeManager` still does not report the application as a normal running managed app afterwards
- detached process lifetime appears unstable / short-lived
- so this is a useful PoC workaround, but not a full proof of clean Aurora lifecycle integration

### 12. Streaming launch proved the app reaches managed `Active` state

Additional task added:

- `:composeApp:runDebugOnEmulatorNoSandboxStreaming`

Behavior:

- installs the debug RPM
- launches via:
  - `runtime-manager-tool Control startDebug com.example.aurorakmpdemo --nosandbox --output-to-console`
- does not use `--detach`
- streams stdout/stderr into:
  - `outputs/aurora_runtime_streaming.log`

Observed result:

- `Reply from Control::StartDebug()`
- `Pid1887`
- `runtime-manager-tool Control getRunningApplications` reported:
  - `Running applications: 1`
  - `Instance Id: com.example.aurorakmpdemo`
  - `PID: 1887`
  - `State: Active`

This is stronger than the earlier detached workaround:

- the app is not only assigned a PID
- it is also visible to Aurora `RuntimeManager` as an active managed application

Visible runtime stderr from the streaming launch:

- `xkbcommon: ERROR: couldn't find a Compose file for locale "ru_RU.utf8"`
- `libEGL warning: MESA-LOADER: failed to open zink: /usr/lib64/dri/zink_dri.so ...`

These messages did not prove an application crash by themselves, but they show the process enters the UI/runtime stack.

### 13. UI visibility is still unproven

Captured artifact:

- `outputs/aurora_emulator_app_screen.png`

Current result:

- VNC screenshot from the emulator is fully black

Interpretation:

- the application can reach a managed `Active` state
- but visual output is still not confirmed
- the remaining uncertainty is now narrower:
  - rendering path
  - VNC/screen capture path
  - or a UI startup problem that happens after launcher registration

### 14. RPM / launcher metadata comparison narrowed one likely difference

Compose-generated Aurora packages for both:

- this PoC
- `work/student-box-cmp`

share the same important characteristics:

- standard `runDebugOnEmulator` fails with:
  - `Did not receive a reply`
- generated `.desktop` files do not contain:
  - `ExecDBus=...`

By comparison, the ordinary Aurora Qt sample:

- `~/Library/Application Support/AuroraOS-SDK-5.2.0.180-BT/Примеры ОС Аврора/BleScanner`

has a `.desktop` file that includes:

- `ExecDBus=/usr/bin/ru.auroraos.BleScanner`

This does not prove causality yet, but it is the clearest metadata-level difference found so far between:

- normal Qt/Aurora sample packaging
- Compose/Aurora plugin packaging

### 15. Local plugin override experiment exposed a toolchain conflict

To test the `ExecDBus` hypothesis directly, the local Aurora build plugin source was patched to emit:

- `ExecDBus=/usr/bin/<appId>`

Then the plugin was republished to `mavenLocal`.

Important findings:

- the project resolves Aurora plugins from `work/aurora-maven-partial` before `mavenLocal`
- after temporarily moving `mavenLocal()` ahead of the Aurora mirror, Aurora plugin resolution switched to the locally republished plugin
- but that local plugin build required at least JVM `21`
- the project Aurora build currently runs on Gradle JVM `17`

Observed failure:

- `Could not resolve ru.auroraos.kmp.aurora-build:aurora-build:0.0.1`
- `Dependency requires at least JVM runtime version 21. This build uses a Java 17 JVM.`

Practical result:

- the `ExecDBus` hypothesis is still not fully validated experimentally
- but the PoC now has a documented secondary blocker:
  - local Aurora plugin overrides are not straightforward because of JVM compatibility mismatch between the locally built plugins and the working project toolchain

### 16. Another Aurora pipeline edge case was found

Command:

```bash
./gradlew --no-daemon -PbuildVariant=aurora clean :composeApp:buildDebugPipeline
```

Observed result:

- `initSysroot` ran first
- then `clean` removed `composeApp/build/sysroot`
- later native link failed because the sysroot symlink had already been deleted

Meaning:

- the combined `clean :composeApp:buildDebugPipeline` ordering is unsafe for this setup
- the stable sequence is:
  - `:composeApp:initSysroot`
  - `:composeApp:buildDebugPipeline`

## What Works

- KMP/Compose project structure with:
  - `commonMain`
  - `androidMain`
  - `iosMain`
  - `auroraMain`
- Android main variant build
- Aurora variant dependency resolution
- Aurora Gradle plugin integration
- Aurora build and deploy tasks registration
- Aurora-specific source set segregation
- Aurora Maven / official artifact resolution path
- SQLDelight-based Aurora storage wiring at source level
- Official Aurora SDK installer download and checksum verification
- Full Aurora SDK installation to `~/AuroraOS`
- Docker-based Aurora sysroot initialization
- Aurora native Compose compilation for `auroraArm64` and `auroraX64`
- Aurora RPM build, validation, and signing for `aarch64` and `x86_64`
- Official local Aurora sample reproduces the same launcher/runtime failure
- Patched PoC build can generate `ExecDBus` in `.desktop` metadata
- Custom workaround task `runDebugOnEmulatorNoSandbox` reaches `StartDebug` successfully
- Custom workaround task `runDebugOnEmulatorNoSandboxStreaming` proves the app reaches RuntimeManager state `Active`
- Aurora render smoke mode builds and launches successfully
- Aurora render smoke mode reaches `RuntimeManager` state `Active` with:
  - `Instance Id: com.example.aurorakmpdemo`
  - `PID: 1292`

## What Does Not Work Yet

- Stable Aurora emulator boot on this macOS/M1 host
- Clean Aurora launcher / RuntimeManager lifecycle integration for Compose apps
- Reliable app persistence after launcher handoff
- Automated pixel-perfect screenshot confirmation of the guest UI
- Standard `runDebugOnEmulator` still fails even after RPM installation
- Standard `runDebugOnEmulator` still fails even when `ExecDBus` is present

## Dependency / Version Tensions

Observed mismatch area:

- Main project path uses newer Kotlin/Compose ecosystem versions.
- Aurora artifact bundle is tied to Aurora-published versions, for example:
  - Compose plugin `0.0.4-aurora`
  - Koin `4.2.0-aurora`
  - Ktor `3.4.2-aurora`
  - kotlinx-datetime `0.7.1-aurora`
  - SQLDelight `2.3.2-aurora`
- Aurora Maven README indicates its tested toolchain includes:
  - Kotlin `2.2.0`
  - Gradle `8.14.4`
  - Aurora SDK `5.2.0.180`

Practical conclusion:

- A single unified dependency graph for Android/iOS/Aurora is risky.
- Split build variants are the safer integration strategy.
- Aurora git-based Maven mirror is workable only after full Git LFS object access is available.
- Local override/testing of Aurora Gradle plugins requires Java `21`.
- Android/main remains more stable on Java `17`.
- This project now handles that split explicitly in `gradlew`.

## Migration Guidance For Main Project

Recommended changes if this PoC is applied to the real project:

1. Keep Aurora in a dedicated build variant or dedicated Gradle module path.
2. Keep all Aurora-only dependencies in `auroraMain`.
3. Do not let `commonMain` reference Aurora-generated DB classes or Aurora-specific libraries directly.
4. Use `expect/actual` seams for:
   - `HttpClient` engine
   - local storage
   - diagnostics/platform integrations
5. Treat Aurora dependency versions as a separate compatibility matrix from Android/iOS.

## Can Room Stay on Android/iOS While Aurora Uses SQLDelight?

Short answer:

- Yes, this is viable.

Recommended shape:

- Keep a shared `PostsStorage` interface in `commonMain`.
- Android/iOS can continue with Room or temporary in-memory implementations.
- Aurora can use SQLDelight only in `auroraMain`.

Important constraint:

- The shared layer must depend only on abstractions, not on Room entities/DAOs or SQLDelight generated database types.

This run already validated that this separation is workable, and it also exposed why keeping SQLDelight-generated types in `commonMain` is a mistake.

## What Still Needs To Happen For Real Aurora Emulator Launch

1. Stabilize emulator boot on macOS/M1 so that SSH on `127.0.0.1:2223` stays available.
2. Verify whether Aurora emulator requires additional host-side launch mode, GUI mode, or SDK environment initialization beyond `sfdk emulator start --headless`.
3. Once the emulator stays up, retry:

```bash
./gradlew -PbuildVariant=aurora :composeApp:initSysroot
./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulator
```

4. Emulator prerequisites are already present locally, including:

```bash
~/AuroraOS/vmshare/ssh/private_keys/sdk
```

## Final Conclusion

The PoC is in a strong state:

- Android build works.
- Aurora Gradle integration works.
- Aurora sysroot bootstrap works.
- Aurora native compilation works.
- Aurora packaging pipeline works.
- Aurora RPMs are built, validated, and signed for `aarch64` and `x86_64`.
- Emulator deployment can be continued through direct QEMU startup when `sfdk` start is unreliable.
- Standard `runDebugOnEmulator` launcher handoff fails even for the official local Aurora sample.
- Aurora-specific render smoke mode now launches as an `Active` application and is ready for direct visual inspection on the emulator.
- Manual visual inspection now confirms:
  - `super-smoke` renders correctly on first launch
  - the larger dashboard renders correctly on first launch
  - Ktor and SQLDelight actions work from the visible Aurora UI
- Host-side screenshot capture of the QEMU window is unreliable here, and guest-side screenshot DBus is permission-blocked for `defaultuser`.
- The remaining blocker is therefore narrowed to repeated-launch Compose rendering stability on the emulator, plus possible Compose-on-Aurora lifecycle or graphics-path incompatibility.

Main technical conclusion:

- A shared `commonMain` + `androidMain` + `iosMain` + `auroraMain` architecture is realistic.
- Aurora should be isolated through source-set and build-variant boundaries.
- Using SQLDelight only for Aurora while keeping Android/iOS on other storage implementations is feasible and does not require breaking the shared stack.
- For a real Aurora port, access to the full Aurora Maven artifact set is mandatory; once `hub.mos.ru` LFS access works, the Gradle/Kotlin side becomes viable.
- The standard launcher failure is now narrowed further:
  - it reproduces on the official local Compose sample
  - it also reproduces on the patched PoC after `ExecDBus` is added
  - the app can still be driven into managed `Active` state through `RuntimeManager --nosandbox`
  - the unpatched PoC fails one step earlier with `HandlerNotFound`, which strongly suggests `ExecDBus` is required
  - the PoC now also has a first-frame diagnostic path that removes `MaterialTheme`, `Scaffold`, navigation, and Koin from the initial render path
  - first launch visual rendering is now confirmed for both super-smoke and dashboard screens
  - but repeated launch can still degrade to only the root background, so the remaining gap is around repeated-launch Compose subtree rendering on Aurora
