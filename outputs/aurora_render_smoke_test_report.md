# Aurora Render Smoke Test Report

Date: 2026-06-26

Workspace:

- `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`

## Before changes

Baseline visual result reported by the user on Aurora emulator:

- `Aurora KMP Demo`: white screen
- `Student Box CMP`: violet / purple square

System-level state before render-focused changes was already positive:

- Aurora RPM built successfully
- RPM installed successfully
- app launch reached `RuntimeManager`
- Compose app window was created

## What was added

An Aurora-specific boot mode was introduced through:

- `shouldUseAuroraRenderSmokeTest()` expect/actual

Current behavior:

- `auroraMain`: starts in render smoke mode by default
- Android/Desktop/iOS: continue to boot the normal app flow

New screen:

- `AuroraRenderSmokeScreen`

Design goals:

- avoid `KoinApplication`
- avoid `MaterialTheme`
- avoid `Surface`
- avoid `Scaffold`
- avoid navigation on first render path
- use explicit `fillMaxSize()`
- use explicit strong background colors
- use large visible text
- use custom foundation-based buttons
- keep Ktor and Room actions available for diagnostics

## Smoke screen contents

The new Aurora diagnostic screen includes:

- large `HELLO AURORA` title
- yellow subtitle `Compose render smoke test`
- dashboard title `Aurora KMP Demo Dashboard`
- status panel:
  - Platform
  - Compose
  - Koin
  - Ktor
  - Room
  - Datetime
- visual diagnostics panel:
  - red block
  - green block
  - blue block
  - yellow block
- action buttons:
  - `Check Ktor`
  - `Save test post`
  - `Read saved post`
  - `Clear log`
  - `Open normal demo app`
- post preview panel
- platform diagnostics panel
- runtime log panel

## Build and run results

Confirmed commands:

```bash
./gradlew --no-daemon -PbuildVariant=main :composeApp:assembleDebug
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

Results:

- Android/main build: `BUILD SUCCESSFUL`
- Aurora build pipeline: `BUILD SUCCESSFUL`
- Aurora streaming launch:
  - package uploaded
  - package uninstalled/reinstalled
  - `Reply from Control::StartDebug()`
  - `Instance id: com.example.aurorakmpdemo`
  - `Pid1292`

RuntimeManager state after launch:

- `Instance Id: com.example.aurorakmpdemo`
- `PID: 1292`
- `State: Active`

## Visual confirmation status

What is confirmed by system state:

- the new smoke-test build is the installed/running package
- the smoke-test process is alive
- the process reached `Active`

What is **not** fully confirmed by machine-captured pixels:

- a host-side macOS screenshot of the QEMU window saved to:
  - `outputs/aurora_render_smoke_screen.png`
  - but it captured a black QEMU surface rather than usable guest UI pixels
- a guest-side DBus screenshot attempt was discovered but failed with:
  - `org.freedesktop.DBus.Error.AccessDenied: PID ... is not in privileged group`

Therefore:

- visual confirmation of the exact rendered smoke-screen contents required the emulator window to be checked by eye
- that manual confirmation has now been obtained
- CLI-side evidence confirms the same build is what was running

## Manual visual result from emulator window

The user confirmed the following screenshots on Aurora emulator:

### First launch: super-smoke mode

Visible:

- dark red fullscreen background
- `HELLO AURORA`
- `Super smoke mode`
- green rounded square
- `Open render smoke`
- `Open normal demo`

### First launch: render-smoke mode

Visible:

- dashboard header
- status cards
- color diagnostics row
- action buttons
- post preview
- platform diagnostics
- runtime log

### Functional checks confirmed visually

Visible and working from screenshots:

- `Check Ktor`
  - status changed to `Success`
- `Save test post`
  - status changed to `Manual save success`
- `Read saved post`
  - status changed to `Read success`
- Room-backed post preview updated correctly
- runtime log panel updated correctly

This is the strongest confirmation so far that:

- Compose UI can render a substantial visible screen on Aurora
- custom colors, text, borders, panels, and buttons render correctly
- Ktor action works from the Aurora UI
- Room save/read actions work from the Aurora UI

## What works for sure in Compose on Aurora

At minimum, the following now works end-to-end:

- Compose app package build
- Compose app RPM install
- Compose app launch
- Compose app reaches `RuntimeManager` state `Active`
- custom Aurora-only boot path selection
- foundation-based root layout compiles and launches
- Ktor + Room diagnostics are wired into the Aurora screen code path
- visible dashboard rendering works on first launch
- button-triggered UI updates work on first launch
- Ktor request succeeds on Aurora from the visible UI
- Room save/read succeeds on Aurora from the visible UI

## What fails on repeated launch

After closing the app and launching it again in the same emulator session, the user observed:

- only the red background remains visible
- `HELLO AURORA` disappears
- the green square disappears
- buttons disappear
- all child content disappears

This happened even in `super-smoke` mode.

Interpretation:

- root `Box(...background(...))` still renders
- the child subtree does not render reliably after repeated launch
- the failure is therefore deeper than:
  - `Koin`
  - `MaterialTheme`
  - `Surface`
  - `Scaffold`
  - navigation
  - Ktor
  - Room

The strongest remaining suspects are now:

- Compose native rendering/recomposition lifecycle on Aurora
- text/layout subtree initialization on repeated launch

## Room-only status

- SQLDelight has now been removed from the PoC.
- Active local storage backend is Room on Android, iOS, Desktop, and Aurora.
- Aurora smoke dashboard now includes a Room-backed latest-note block.
- Database screen now includes:
  - text input
  - save latest text
  - restore latest text

## Persistence status

- Confirmed by implementation:
  - latest note is persisted to Room
  - latest note should survive ordinary app restart
- Current Aurora storage path:
  - `~/Documents/aurora-kmp-demo/posts-room.db`
- Reinstall survival expectation:
  - likely survives reinstall because storage was moved outside the app package working directory
  - still requires manual emulator confirmation after uninstall/reinstall cycle
- native scene/window lifecycle after process restart

## Latest log-backed interpretation

The most recent user-captured terminal session adds an important clarification:

- the failing repeated-launch case is not a silent app crash
- it is not a missing `ON_START` / `ON_RESUME`
- it is not a failure to reach the smoke-screen composable path on first launch

Specifically, the Aurora-side file log showed:

- `AuroraSuperSmokeScreen enter`
- repeated `rendering subtree generation=2`
- repeated `heartbeat`
- `visible=true`

Even in the session that later degraded to the red-only screen.

Practical interpretation:

- the red root background is still being drawn
- the staged container believes content is present
- the child visual subtree is the part that stops appearing reliably

That narrows the remaining issue to visual restoration / repaint rather than to business logic or dependency initialization.

## Component-level render isolation

Current Aurora smoke path intentionally bypasses these potentially problematic layers:

- `MaterialTheme`
- `Surface`
- `Scaffold`
- bottom navigation
- Koin startup
- `koinViewModel`
- Compose resources on the first screen

This means:

- if the emulator still shows a blank/white/black screen now, the problem is below those layers
- if the emulator now shows the new diagnostic dashboard, then at least one of the bypassed layers is implicated in the original white-screen path

## Koin / Ktor / Room status

- `Koin`:
  - intentionally bypassed in smoke mode
  - status shown as `Not initialized in smoke mode`
- `Ktor`:
  - wired through `PostsRepository` and `buildHttpClient()`
  - button exists in the Aurora smoke UI
  - visual success still needs manual check on emulator screen
- `Room`:
  - active persistence backend for the current PoC
  - save/read actions are wired into the Aurora smoke UI
  - latest-note flow is also used on Android, iOS, and Desktop targets

## Additional runtime notes

Streaming launch log saved in:

- `outputs/aurora_runtime_streaming.log`

Current captured runtime output is minimal:

- `Reply from Control::StartDebug()`
- `Pid1292`
- `xkbcommon: ERROR: couldn't find a Compose file for locale "ru_RU.utf8"`

Journal extraction attempt saved in:

- `outputs/aurora_render_smoke_journal.log`

But the command did not have sufficient permissions and returned:

- `Failed to search journal ACL: Operation not supported`
- `No journal files were opened due to insufficient permissions.`

## Conclusion

- The PoC now has a dedicated Aurora render smoke mode.
- The smoke mode is intentionally much simpler than the original app path and should be visually obvious if Aurora Compose is really painting content.
- Build and runtime state are both healthy enough to test this screen on the emulator.
- Manual screenshot evidence now confirms that the dashboard renders and functions correctly on first launch.
- The main unresolved bug is narrower:
  - repeated app launch can leave only the root background visible, with the whole child Compose subtree missing
- That points to a repeated-launch Compose/Aurora runtime issue rather than a simple app-structure or dependency-integration issue.
