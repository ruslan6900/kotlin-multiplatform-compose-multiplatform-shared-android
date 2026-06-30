# Aurora Qt Sample Launch Report

Date: 2026-06-26

Sample chosen:

- `BleScanner`
- Local SDK path:
  - `~/Library/Application Support/AuroraOS-SDK-5.2.0.180-BT/Примеры ОС Аврора/BleScanner`
- Public repo:
  - <https://gitlab.com/omprussia/examples/BleScanner>
- Branch observed in README:
  - `example`
- Local clone checked:
  - `~/Documents/Work/Aurora/external-samples/BleScanner-gitlab`
- Commit:
  - `b98ceb2563d9646fdc84979cbefb32e55bffd674`

## Why this sample

- It is a standard Aurora Qt/QML app
- It contains full Aurora-style desktop/spec metadata
- It includes `ExecDBus`
- It is a good launcher/runtime baseline for comparison against Compose/KMP packages

## Build path

The sample is built via Aurora SDK shell tooling:

```bash
sdk-assistant list
mb2 --target <target> build ../<application-path>
```

Source of this command flow:

- `build.sh` in the project

## Desktop metadata

```ini
[Desktop Entry]
Type=Application
X-Nemo-Application-Type=silica-qt5
Icon=ru.auroraos.BleScanner
Exec=/usr/bin/ru.auroraos.BleScanner
Name=BLE Scanner

[X-Application]
Permissions=Bluetooth;
OrganizationName=ru.auroraos
ApplicationName=BleScanner
ExecDBus=/usr/bin/ru.auroraos.BleScanner
```

## Package id / executable / permissions

- Package id: `ru.auroraos.BleScanner`
- Executable path: `/usr/bin/ru.auroraos.BleScanner`
- Runtime permissions: `Bluetooth`

## RPM metadata

Spec file path:

- `rpm/ru.auroraos.BleScanner.spec`

Relevant properties:

- standard Aurora `auroraapp` build requirements
- `%qmake5`
- `%make_build`
- installs desktop file and icons in standard Aurora locations

## Build result in this session

- Full end-to-end build was not completed in this session

Why:

- current host Docker/Aurora runtime state is degraded
- the session focus was to avoid destructive SDK changes and first isolate metadata/runtime differences

## Install result in this session

- not attempted

## Standard launcher run result in this session

- not attempted

## RuntimeManager / mapplauncherd logs

- no fresh launch logs were collected for this sample in this session

## UI visibility

- not checked in this session

## Practical conclusion

Although I did not complete a fresh launch of `BleScanner` in this session, it remains the clearest standard Aurora launcher baseline because it has the full expected desktop metadata, especially `ExecDBus`, and a conventional Qt/QML RPM/spec flow.
