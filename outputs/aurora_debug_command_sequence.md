# Aurora Debug Command Sequence

Date: 2026-06-26

Workspace:

- `/Users/ruslaneremeev/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android`

## 1. Rebuild and cold-start the app

Use this when you want the most reliable visible launch:

```bash
cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

Expected result:

- app is reinstalled
- stale emulator-side process is killed first
- runtime stdout is streamed to `outputs/aurora_runtime_streaming.log`
- first visible launch usually renders correctly

## 2. Tail UI diagnostics live

Open a second terminal and run:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'tail -f ~/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log'
```

Use this while:

- opening the app
- switching from super-smoke to render-smoke
- swiping the app away in Aurora OS
- reopening it from the launcher or app switcher

## 3. Check whether the process is still alive

Run this after close, after reopen, and after a failed red-only resume:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'ps aux | grep -i aurorakmpdemo | grep -v grep'
```

Use this to answer:

- is there still an old `/usr/bin/com.example.aurorakmpdemo` process
- did a fresh process appear after relaunch
- is `firejail` still holding an older instance

## 4. Dump the full current UI log

Run this after reproducing the problem:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'cat ~/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log'
```

This is the best command for post-mortem analysis.

## 5. Kill the running app manually

Use this if the app seems stuck between launches:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'ps aux | grep -E "(/usr/bin/com.example.aurorakmpdemo|private-bin=com.example.aurorakmpdemo)" | grep -v grep | awk '"'"'{print $2}'"'"' | xargs -r kill -9'
```

Then verify:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'ps aux | grep -i aurorakmpdemo | grep -v grep'
```

## 6. What to do in each scenario

If you want to confirm that the build itself is healthy:

```bash
cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:buildDebugPipeline
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```

If the app shows only the red background after reopen:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'cat ~/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log'
```

Then immediately also run:

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'ps aux | grep -i aurorakmpdemo | grep -v grep'
```

If you want a clean retry without rebooting the emulator:

```bash
cd ~/Documents/Work/Aurora/kotlin-multiplatform-compose-multiplatform-shared-android
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:killAuroraDemoOnEmulator
./gradlew --no-daemon -PbuildVariant=aurora :composeApp:runDebugOnEmulatorNoSandboxStreaming --stacktrace
```
