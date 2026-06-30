# Aurora ExecDBus Experiment

Date: 2026-06-26

## Goal

Check whether `ExecDBus` can be added to Compose-generated Aurora package metadata safely, and whether that is enough to restore standard Aurora launcher behavior for Compose apps.

## What was changed

Earlier local Aurora build plugin source was patched to add:

```ini
ExecDBus=/usr/bin/<appId>
```

Change location:

- local Aurora build plugin source:
  - `work/aurora-build-tools/aurora-build/src/main/kotlin/ru/auroraos/kmp/build/plugin/tasks/buildRpm/Executor.kt`

## Before

Current PoC generated desktop file:

```ini
[Desktop Entry]
Type=Application
Name=Aurora KMP Demo
Comment=Aurora Kotlin Multiplatform compatibility check.
Icon=com.example.aurorakmpdemo
Exec=/usr/bin/com.example.aurorakmpdemo
X-Nemo-Application-Type=silica-qt5

[X-Application]
Permissions=Internet;UserDirs
OrganizationName=com.example
ApplicationName=aurorakmpdemo
```

Meaning:

- `ExecDBus` absent
- standard `runDebugOnEmulator` failed with:
  - `Error code: HandlerNotFound`
  - `Error message: Cannot find application com.example.aurorakmpdemo`

## After

Fresh generated desktop file for the patched main PoC:

```ini
[Desktop Entry]
Type=Application
Name=Aurora KMP Demo
Comment=Aurora Kotlin Multiplatform compatibility check.
Icon=com.example.aurorakmpdemo
Exec=/usr/bin/com.example.aurorakmpdemo
X-Nemo-Application-Type=silica-qt5

[X-Application]
Permissions=Internet;UserDirs
OrganizationName=com.example
ApplicationName=aurorakmpdemo
ExecDBus=/usr/bin/com.example.aurorakmpdemo
```

Meaning:

- `ExecDBus` present in the PoC package after forcing the patched plugin path

Fresh standard launch result for the patched PoC:

- RPM upload: success
- RPM install: success
- launch: failed with
  - `Error code: Failed`
  - `Error message: Did not receive a reply. Possible causes include: the remote application did not send a reply, the message bus security policy blocked the reply, the reply timeout expired, or the network connection was broken.`

Fresh generated desktop file for external `student-box-cmp` in this environment:

```ini
[Desktop Entry]
Type=Application
Name=Student Box CMP
Comment=Example application Compose Multiplatform.
Icon=ru.auroraos.box.cmp.cmp
Exec=/usr/bin/ru.auroraos.box.cmp.cmp
X-Nemo-Application-Type=silica-qt5

[X-Application]
Permissions=DeviceInfo
OrganizationName=ru.auroraos.box.cmp
ApplicationName=cmp
ExecDBus=/usr/bin/ru.auroraos.box.cmp.cmp
```

Meaning:

- `ExecDBus` present
- standard `runDebugOnEmulator` still fails with the same `Did not receive a reply` error

## Why the result differs between projects

### Main PoC

- `settings.gradle.kts` for Aurora variant checks remote Aurora repo before `mavenLocal()`
- `gradle.properties` pins:
  - `org.gradle.java.home=/Users/ruslaneremeev/Library/Java/JavaVirtualMachines/liberica-17.0.6`
- result before the repo/JDK override:
  - PoC build kept using the unpatched remote Aurora plugin
  - generated desktop metadata stayed without `ExecDBus`

After:

- moving `mavenLocal()` before Aurora remote repos in `settings.gradle.kts`
- forcing Java 21 for the Aurora Gradle invocation

result:

- the PoC consumed the patched local plugin
- generated desktop metadata included `ExecDBus`

### External `student-box-cmp`

- `settings.gradle.kts` lists `mavenLocal()` before any Aurora remote mirror
- `gradle.properties` does not pin daemon JDK
- `./gradlew --version` shows daemon JVM on Java 21
- result:
  - external project can consume the locally patched plugin
  - generated desktop metadata includes `ExecDBus`

## RuntimeManager response

Fresh launch results were obtained in this session.

Observed transition:

- without `ExecDBus` in the PoC desktop file:
  - launcher failed with `HandlerNotFound`
- with `ExecDBus` in the PoC desktop file:
  - launcher progressed further
  - package was found, uploaded, installed
  - then failed with `Did not receive a reply`

Cross-check:

- external official `student-box-cmp` also has `ExecDBus`
- it also fails standard launch with `Did not receive a reply`

## Conclusion

The `ExecDBus` experiment produced two concrete results:

- yes, Compose-generated Aurora package metadata can be changed to include `ExecDBus`
- yes, project-level repository priority and daemon JDK selection directly affect whether that patched plugin is actually used

And one equally important negative result:

- no, adding `ExecDBus` is not sufficient to make standard Compose launcher flow work on this emulator/runtime path

Updated conclusion:

- `ExecDBus` is necessary to avoid the `HandlerNotFound` failure mode
- but the remaining blocker is deeper, because both the patched PoC and official `student-box-cmp` still fail with the same DBus timeout
