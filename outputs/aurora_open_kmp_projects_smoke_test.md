# Aurora Open KMP Projects Smoke Test

Date: 2026-06-26

Workspace for external checks:

- `~/Documents/Work/Aurora/external-samples`

Main PoC kept separate:

- `~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`

## Search summary

Public projects/examples confirmed during this run:

1. `student-box-cmp`
   - Link: <https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box-cmp>
   - Tree/tag: <https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box-cmp/-/tree/aurora-0.0.4>
   - Search evidence: [StudentBox CMP tree](https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box-cmp/-/tree/aurora-0.0.4), [project page](https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box-cmp)

2. `student-box`
   - Link: <https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box>
   - Tree/tag: <https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box/-/tree/aurora-0.0.4>
   - Search evidence: [StudentBox project page](https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box), [README search result](https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box/-/blob/master/README.md)

3. `BleScanner`
   - Link: <https://gitlab.com/omprussia/examples/BleScanner>
   - Search evidence: [GitLab project page](https://gitlab.com/omprussia/examples/BleScanner)

I did not find a broader public set of Aurora Compose/KMP repos beyond these official examples during this run. The public search signal was dominated by Aurora official examples and docs.

## Environment notes that affected all smoke tests

- Docker daemon is available on this host
- Aurora emulator SSH is available on:
  - `127.0.0.1:2223`
- This made it possible to complete fresh Compose RPM build/install/run smoke checks
- No dangerous system changes were made

## Project 1: student-box-cmp

- Name: `student-box-cmp`
- Repo: `https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box-cmp.git`
- Branch/tag checked out: `aurora-0.0.4`
- Commit: `747a1cfacb78deacbd37e46637809df7bec00d88`
- Technologies:
  - Kotlin Multiplatform
  - Compose Multiplatform
  - SQLDelight
  - Koin
  - Aurora Build / Aurora Devices plugins
- Compose Multiplatform: yes
- `auroraMain`: yes
- Aurora Gradle plugins: yes
- Build/install/run tasks: yes
  - `./gradlew tasks`
  - `./gradlew -PbuildVariant=aurora :composeApp:tasks`
  - `./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline`
  - `runAppAuroraEmulator`
  - `runDebugOnEmulator`
- `.desktop`: generated during build
- `ExecDBus`: yes in generated `.desktop` in this environment

Commands run:

```bash
./gradlew --no-daemon tasks
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:tasks
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
```

Results:

- `./gradlew tasks`: `BUILD SUCCESSFUL` after adding local `local.properties`
- `./gradlew -PbuildVariant=aurora :composeApp:tasks`: `BUILD SUCCESSFUL`
- `./gradlew -PbuildVariant=aurora :composeApp:buildDebugPipeline`: `BUILD SUCCESSFUL`
- `./gradlew -PbuildVariant=aurora :composeApp:runDebugOnEmulator`:
  - uploads RPM
  - installs RPM
  - then fails at launch step

Observed runtime/build errors:

- standard launch fails with:
  - `Error code: Failed`
  - `Error message: Did not receive a reply. Possible causes include: the remote application did not send a reply, the message bus security policy blocked the reply, the reply timeout expired, or the network connection was broken.`

UI visible:

- no

What differs from our PoC:

- its `settings.gradle.kts` resolves `mavenLocal()` before remote repos
- it does not pin Gradle daemon JDK to Java 17
- generated `.desktop` in this run contains `ExecDBus=/usr/bin/ru.auroraos.box.cmp.cmp`

What can be concluded:

- the open official Compose sample builds successfully in this environment
- Aurora task wiring is valid
- even with generated `ExecDBus`, standard launcher flow still fails on this emulator/runtime path
- `ExecDBus` alone is not sufficient to make Compose app launch succeed normally

## Project 2: student-box

- Name: `student-box`
- Repo: `https://hub.mos.ru/auroraos/kotlin-multiplatform/examples/student-box.git`
- Branch/tag checked out: `aurora-0.0.4`
- Commit: `e880cd809289d1894ec7ddb9b4b01adb842eabe6`
- Technologies:
  - Kotlin Multiplatform shared library
  - QtBindings
  - SQLDelight
  - Aurora native Qt app
- Compose Multiplatform: no for Aurora app path
- `auroraMain`: not in Compose style; Aurora app is separate Qt/qmake project
- Aurora Gradle plugins: yes in shared module
- Build/install/run tasks:
  - shared Gradle tasks exist
  - Aurora app build is qmake/Qt-based, not Compose Gradle `composeApp`
- `.desktop`: yes
- `ExecDBus`: yes

Commands run:

```bash
cd apps/shared && ./gradlew --no-daemon tasks
cd apps/shared && ./gradlew --no-daemon build
```

Results:

- `./gradlew tasks`: `BUILD SUCCESSFUL`
- `./gradlew build`:
  - progressed through Android and KMP shared stages
  - failed on native Linux/Aurora shared library link because sysroot files/libraries were missing

Observed errors:

- missing `crti.o`
- missing `crtbeginS.o`
- unable to find `-lsqlite3`, `-lresolv`, `-lm`, `-lpthread`, `-lstdc++`, `-lc`, etc.

Install result:

- not attempted in this session

Standard run result:

- not attempted in this session

UI visible:

- not checked in this session

What differs from our PoC:

- not a Compose Aurora UI path
- Aurora app uses Qt/qmake
- desktop metadata already contains `ExecDBus`
- app delivery model is “KMP shared library + QtBindings + native Aurora app”

What can be concluded:

- this is a strong control sample for non-Compose Aurora packaging
- it supports the idea that Compose Aurora and Qt/QtBindings Aurora follow different runtime metadata paths

## Project 3: BleScanner

- Name: `BleScanner`
- Repo: `https://gitlab.com/omprussia/examples/BleScanner.git`
- Branch checked out: `example`
- Commit: `b98ceb2563d9646fdc84979cbefb32e55bffd674`
- Technologies:
  - C++
  - QML
  - Aurora app packaging
  - `mb2`
- Compose Multiplatform: no
- `auroraMain`: no
- Aurora Gradle plugins: no
- Build/install/run tasks: shell/qmake/RPM flow via `build.sh`
- `.desktop`: yes
- `ExecDBus`: yes

Commands inspected:

- `build.sh`
- desktop/spec metadata

Build command shape from project:

```bash
sdk-assistant list
mb2 --target <target> build ../<app-path>
```

Build result in this session:

- not executed end-to-end

Reason:

- this path requires Aurora SDK CLI toolchain invocation around `mb2` / `sdk-assistant`
- I limited this run to non-destructive smoke checks because Docker/host Aurora runtime was already degraded

Install result:

- not attempted in this session

Standard run result:

- not attempted in this session

UI visible:

- not checked in this session

What differs from our PoC:

- completely non-KMP, non-Compose
- standard Aurora desktop metadata with `ExecDBus`
- useful as a clean Qt/QML launcher baseline

## Overall conclusion

- I did not find evidence in this session that any open Aurora Compose/KMP project launches cleanly in the same host/runtime conditions.
- The official public Aurora examples split into two distinct families:
  - Compose/KMP via `student-box-cmp`
  - QtBindings/Qt/QML via `student-box` and `BleScanner`
- The strongest new result is stronger than a metadata-only hypothesis:
  - `student-box-cmp` builds successfully
  - `student-box-cmp` generates `ExecDBus`
  - `student-box-cmp` still fails standard launch with the same DBus timeout
- This shifts the most likely remaining problem area to:
  - SDK / emulator / RuntimeManager / launcher / mapplauncherd / host environment
  - and possibly Compose-on-Aurora lifecycle/runtime behavior itself
