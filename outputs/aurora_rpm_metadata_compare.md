# Aurora RPM Metadata Compare

Date: 2026-06-22

## Compared packages

- PoC:
  - `composeApp/build/rpm/debug/aarch64/RPMS/aarch64/com.example.aurorakmpdemo-0.0.1-1.aarch64.rpm`
- Official local Compose sample:
  - `work/student-box-cmp/composeApp/build/rpm/debug/aarch64/RPMS/aarch64/ru.auroraos.box.cmp.cmp-0.0.1-1.aarch64.rpm`

## Common properties

- Both are `aarch64` RPMs
- Both have version `0.0.1-1`
- Both package a single executable under `/usr/bin/<appId>`
- Both install icons under `/usr/share/icons/hicolor/.../apps`
- Both install a desktop file under `/usr/share/applications/<appId>.desktop`
- Both install resources under `/usr/share/<appId>/resources`
- Both package bundled `maliit-glib` shared libraries
- Both have no `%post` / `%pre` / `%postun` script bodies in extracted RPM scripts

## Desktop files

PoC:

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

Sample:

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
```

## Key similarities

- Same `.desktop` shape
- Same `X-Nemo-Application-Type=silica-qt5`
- Same lack of `ExecDBus`
- Same packaging strategy in `.spec`
- Same launcher failure under standard `runDebugOnEmulator`

## Key differences

- App ID differs
- Permissions differ:
  - PoC: `Internet;UserDirs`
  - sample: `DeviceInfo`
- Resource payload and binary size differ slightly

## RPM metadata summary

PoC:

- Name: `com.example.aurorakmpdemo`
- Summary: `Aurora Kotlin Multiplatform compatibility check.`
- Build host: `d9747030bfc8`

Sample:

- Name: `ru.auroraos.box.cmp.cmp`
- Summary: `Example application Compose Multiplatform.`
- Build host: `5ea394edd280`

## Spec comparison

The generated `.spec` files are structurally identical:

- create `/usr/bin`
- create `/usr/share/icons`
- create `/usr/share/applications`
- create `/usr/share/<appId>/lib`
- create `/usr/share/<appId>/resources`
- install executable to `/usr/bin/<appId>`
- copy `maliit-glib` into app-local lib directory
- copy `icons`, `applications`, and `resources`

No systemd units or service registration were found in either package.

## Conclusion

The PoC RPM is not an outlier. It matches the official local Compose sample in all launcher-relevant metadata that was inspected. The standard `runDebugOnEmulator` failure therefore looks platform/tooling-related, not specific to PoC business logic.
