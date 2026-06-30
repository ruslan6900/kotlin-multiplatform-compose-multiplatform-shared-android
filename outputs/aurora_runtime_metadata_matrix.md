# Aurora Runtime Metadata Matrix

Date: 2026-06-26

Compared applications:

1. PoC: `com.example.aurorakmpdemo`
2. Official Compose sample: `ru.auroraos.box.cmp.cmp`
3. Official QtBindings sample: `ru.auroraos.studentbox`
4. Official Qt/QML sample: `ru.auroraos.BleScanner`

## Matrix

| Item | PoC | student-box-cmp | student-box | BleScanner |
|---|---|---|---|---|
| Stack | KMP + Compose | KMP + Compose | KMP + QtBindings + Qt app | Qt/QML |
| Aurora Gradle plugin | yes | yes | shared module only | no |
| `.desktop` present | yes | yes | yes | yes |
| `Exec=` | yes | yes | yes | yes |
| `ExecDBus=` | yes in patched PoC build | yes in current external build | yes | yes |
| `X-Nemo-Application-Type=silica-qt5` | yes | yes | yes | yes |
| `[X-Application]` section | yes | yes | yes | yes |
| Package/app id | `com.example.aurorakmpdemo` | `ru.auroraos.box.cmp.cmp` | `ru.auroraos.studentbox` | `ru.auroraos.BleScanner` |
| Binary path | `/usr/bin/com.example.aurorakmpdemo` | `/usr/bin/ru.auroraos.box.cmp.cmp` | `/usr/bin/ru.auroraos.studentbox` | `/usr/bin/ru.auroraos.BleScanner` |
| Install path style | Aurora RPM standard | Aurora RPM standard | Aurora RPM standard | Aurora RPM standard |
| Icons path style | standard hicolor | standard hicolor | standard hicolor | standard hicolor |
| RPM scripts | none significant | none significant | simple qmake spec | standard qmake/spec |
| Runtime permissions | `Internet;UserDirs` | `DeviceInfo` | `DeviceInfo;UserDirs;Internet` | `Bluetooth` |
| Standard launch known result | with no `ExecDBus`: `HandlerNotFound`; with `ExecDBus`: `Did not receive a reply` | `Did not receive a reply` | not re-run in this session | not re-run in this session |
| Workaround run known result | yes, reaches `Active` with `--nosandbox` | previously same failure on standard run; no fresh workaround run in this session | n/a in this session | n/a in this session |

## Important observations

### 1. Compose family versus Qt family

The two Compose packages:

- PoC
- `student-box-cmp`

share the same general Aurora package structure and launcher registration style.

The Qt family:

- `student-box`
- `BleScanner`

uses the conventional Aurora Qt/QML or QtBindings route and consistently includes `ExecDBus`.

### 2. The biggest suspicious difference

The most suspicious difference originally was:

- `ExecDBus`

It is present in:

- `student-box`
- `BleScanner`
- regenerated external `student-box-cmp`
- patched PoC build

This run showed a more precise conclusion:

- missing `ExecDBus` explains the `HandlerNotFound` variant for the unpatched PoC
- but adding `ExecDBus` does not fix the standard Compose launcher flow by itself

### 3. Why `student-box-cmp` now shows `ExecDBus`

This run exposed a concrete reason:

- external `student-box-cmp` resolves `mavenLocal()` before remote repos
- it does not force Gradle daemon JDK to Java 17
- therefore it can consume the locally patched Aurora build plugin on Java 21
- generated desktop file now includes:
  - `ExecDBus=/usr/bin/ru.auroraos.box.cmp.cmp`

By contrast, the main PoC originally had:

- Aurora remote repo precedence before `mavenLocal()` for the Aurora variant
- `org.gradle.java.home=/Users/ruslaneremeev/Library/Java/JavaVirtualMachines/liberica-17.0.6`

After switching repo precedence and forcing Java 21 for the Aurora build, the PoC also generated:

- `ExecDBus=/usr/bin/com.example.aurorakmpdemo`

## Conclusion

- Compose/KMP RPM and working Qt-style Aurora RPM still differ in runtime behavior, but `ExecDBus` is no longer enough to explain the gap on its own.
- A second major factor remains plugin resolution:
  - if the project consumes the patched local Aurora build plugin on Java 21, Compose package metadata changes
  - if it consumes the unpatched remote plugin on Java 17, the metadata stays in the older form
- New strongest conclusion:
  - unpatched Compose package without `ExecDBus` fails with `HandlerNotFound`
  - patched Compose packages with `ExecDBus` still fail with `Did not receive a reply`
  - therefore the remaining blocker is beyond only `.desktop` metadata
