# Aurora KMP Compose Check Report

## What works now

- Shared KMP structure is in place:
  - `commonMain`
  - `androidMain`
  - `iosMain`
  - `desktopMain`
  - `auroraMain`
- Shared Compose Multiplatform UI works across the active targets.
- Shared DI is wired through Koin.
- Shared network layer is wired through Ktor with platform engines.
- Shared time updates are driven from a `Flow` and rendered as `HH:mm:ss`.
- Room is the active persistence backend on:
  - Android
  - iOS
  - Desktop
  - Aurora
- Aurora build pipeline works:
  - RPM is produced
  - RPM installs into Aurora emulator
  - app launches in Aurora emulator
- iOS host app is created and builds successfully through `iosApp`.
- iOS simulator launch works after required Compose UIKit `Info.plist` fixes.
- Desktop build works with the same Aurora-style smoke UI path.

## Current persistence status

- Android uses Room with Android app storage path.
- iOS uses Room with a sandboxed path under:
  - `~/Library/Application Support/aurora-kmp-demo/posts-room.db`
- Desktop uses Room with:
  - `~/.aurora-kmp-demo/posts-room.db`
- Aurora uses Room with:
  - `~/Documents/aurora-kmp-demo/posts-room.db`

Expected behavior:

- ordinary app restart should preserve saved note/post data on all four targets above
- app reinstall does **not** have the same meaning on every platform
- on iOS, reinstall is expected to remove the sandbox, so Room data is not expected to survive reinstall

## Aurora UI/runtime result

- Aurora no longer stops at “does not launch”.
- Compose content is visibly rendered on Aurora emulator.
- A red high-contrast smoke dashboard is available and visible.
- Ktor / Room / note persistence actions are exposed through the Aurora smoke UI.

Known Aurora limitation still under investigation:

- repeated launch can sometimes leave only the red root background visible while the child Compose subtree disappears
- this is narrower than a full app-start failure and looks like a repeated-launch render/runtime issue

## iOS-specific notes

- iOS now also starts with the red Aurora-style smoke dashboard.
- Compose UIKit required `Info.plist` fixes:
  - supported interface orientations
  - `CADisableMinimumFrameDurationOnPhone = true`
- Before that, the app was aborting inside `androidx.compose.ui.uikit.PlistSanityCheck`.

## Migration conclusion

- Shared Compose UI is realistic.
- Shared Koin is realistic.
- Shared Ktor is realistic.
- Shared Room is now realistic for this PoC across Android, iOS, Desktop, and Aurora.
- The main platform-specific work remains:
  - entry points
  - HTTP engine selection
  - filesystem/database path selection
  - build/deploy/install/run tooling
  - Aurora runtime stabilization
