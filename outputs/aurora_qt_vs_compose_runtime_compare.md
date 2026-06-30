# Aurora Qt Vs Compose Runtime Compare

Date: 2026-06-22

## Compared artifacts

- Compose PoC:
  - `com.example.aurorakmpdemo`
- Official local Compose sample:
  - `ru.auroraos.box.cmp.cmp`
- Ordinary Aurora Qt sample:
  - `~/Library/Application Support/AuroraOS-SDK-5.2.0.180-BT/Примеры ОС Аврора/BleScanner`

## What Compose PoC and Compose sample have in common

- Standard `runDebugOnEmulator` fails the same way
- `.desktop` generated from Aurora Compose plugin does not contain `ExecDBus`
- RPM packaging structure is nearly identical
- Both are packaged as `silica-qt5`

## What the Qt sample has that Compose packages do not

From `BleScanner` desktop file:

```ini
[X-Application]
Permissions=Bluetooth;
OrganizationName=ru.auroraos
ApplicationName=BleScanner
ExecDBus=/usr/bin/ru.auroraos.BleScanner
```

Important difference:

- Qt sample explicitly declares `ExecDBus`
- Compose-generated packages currently do not

## Why this matters

The current blocker is:

- `App starting failed`
- `Did not receive a reply`

That error is consistent with a launcher / bus / RuntimeManager handoff problem. `ExecDBus` is therefore a meaningful metadata difference, not just cosmetic noise.

## Attempted validation

The local Aurora build plugin source was patched to emit `ExecDBus`, then republished to `mavenLocal`.

Two blockers prevented a clean end-to-end validation:

- the project resolves Aurora plugins from `work/aurora-maven-partial` before `mavenLocal`
- when repository priority was temporarily changed, the locally republished plugin required Java 21 while the stable project build uses Java 17

So the `ExecDBus` hypothesis remains plausible but not yet experimentally confirmed.

## Additional runtime signal

Even without standard launch success:

- `runDebugOnEmulatorNoSandboxStreaming` can start the PoC
- RuntimeManager reports the app in `Active` state

This suggests the binary itself is viable enough to start, and the gap is in normal Aurora application launch semantics rather than simple executable incompatibility.

## Conclusion

Current evidence points toward a Compose-on-Aurora launcher integration gap, or at least a packaging/runtime metadata gap, rather than a failure in the shared KMP business code.
