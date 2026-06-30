# Aurora Sandbox And Runtime Check

Date: 2026-06-22

## Standard launch

Command path:

- Gradle task `:composeApp:runDebugOnEmulator`
- internally runs:
  - `runtime-manager-tool Control startDebug com.example.aurorakmpdemo --output-to-console`

Observed result:

- RPM uploads and installs successfully
- launch fails with:
  - `Did not receive a reply`

## No-sandbox detached workaround

Command:

```bash
runtime-manager-tool Control startDebug com.example.aurorakmpdemo --nosandbox --detach --output-to-console
```

Observed result:

- returns `Reply from Control::StartDebug()`
- returns `Instance id`
- returns `Pid`

Limit:

- detached mode alone did not prove a stable managed running app

## No-sandbox streaming launch

Command:

```bash
runtime-manager-tool Control startDebug com.example.aurorakmpdemo --nosandbox --output-to-console
```

Observed result:

- `Reply from Control::StartDebug()`
- process starts
- `runtime-manager-tool Control getRunningApplications` shows:
  - `Instance Id: com.example.aurorakmpdemo`
  - `State: Active`

This is the strongest proof collected in the PoC that the application can be launched by Aurora RuntimeManager when sandboxing is bypassed.

## Runtime stderr observed

- `xkbcommon: ERROR: couldn't find a Compose file for locale "ru_RU.utf8"`
- `libEGL warning: MESA-LOADER: failed to open zink: /usr/lib64/dri/zink_dri.so ...`

These messages indicate the process enters the graphics/input stack. They do not by themselves prove the reason for the standard launcher timeout.

## Interpretation

Most likely remaining problem area:

- standard launcher/sandbox path
- RuntimeManager reply path under normal launch
- app rendering/output after process start

Less likely problem area:

- RPM installation
- executable deployment
- basic binary startup

## Conclusion

The PoC no longer looks like a simple “app never starts” failure. It is closer to “standard managed launch path times out, while no-sandbox RuntimeManager launch can reach `Active` state”.
