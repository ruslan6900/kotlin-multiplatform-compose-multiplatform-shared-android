# Aurora Repeated Launch Minimal Test

Date: 2026-06-26

## What was tested

The PoC was instrumented with:

- cold-start `startupId`
- staged boot logging
- lifecycle logging
- subtree generation logging
- composable enter/dispose logging
- Aurora-side file logging at:
  - `/home/defaultuser/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log`

## Confirmed from the latest logs

### Cold start path

The app logs show a healthy first launch:

- `App startupId=launch-... bootMode=SuperSmoke`
- `AuroraStagedBootContainer enter`
- `Lifecycle event=ON_CREATE`
- `Lifecycle event=ON_START`
- `Lifecycle ON_START generation=1`
- `showContent=true`
- `rendering super-smoke`
- `AuroraSuperSmokeScreen enter`
- `Lifecycle event=ON_RESUME`
- `Lifecycle ON_RESUME generation=2`
- subtree is recreated and continues rendering

This confirms:

- the process starts correctly
- the lifecycle events arrive
- the subtree generation logic executes
- the Compose subtree is created successfully on first launch

### During runtime

The log continues to emit:

- `heartbeat`
- `rendering subtree generation=2`

This means:

- the app keeps recomposing at the top staged container level
- the process remains alive while visible

### On close/background

Observed lifecycle events:

- `ON_PAUSE`
- `ON_STOP`

So the app does receive lifecycle transitions when moved away from the foreground.

### Important evidence from the latest user-captured session

The latest attached terminal logs show this sequence:

- first launch created `startupId=launch-1782492512579`
- the app reached:
  - `ON_CREATE`
  - `ON_START generation=1`
  - `showContent=true`
  - `AuroraSuperSmokeScreen enter`
  - `ON_RESUME generation=2`
- after that, the Aurora-side file log continued to emit:
  - `rendering subtree generation=2`
  - `heartbeat=20`, `heartbeat=25`, `heartbeat=30`, `heartbeat=35`, `heartbeat=40`
  - all with `visible=true`
- when the app was moved away from foreground, the log then recorded:
  - `ON_PAUSE`
  - `ON_STOP`

This is the clearest evidence so far that:

- Compose root composition is still alive
- the staged boot container still thinks child content is visible
- repeated visual failure is not caused by missing initial composition
- repeated visual failure is not caused by missing heartbeat / total app freeze

## Process observations

Observed states from `ps`:

1. During launch:
   - `runtime-manager-tool Control startDebug ...`
   - `/usr/bin/com.example.aurorakmpdemo`

2. After one close check:
   - firejail and app process were still visible briefly

3. After a later check:
   - only the log tail process remained

Interpretation:

- the app process does not appear to be permanently immortal
- shutdown/teardown may be asynchronous
- the bug is not yet proven to be caused only by a permanently stuck old process
- however, one user check did show a new `firejail` + `/usr/bin/com.example.aurorakmpdemo` process after relaunch, so a half-dead previous instance is still worth keeping in mind as a secondary suspect

## Important current conclusion

The logs strongly confirm:

- first-launch initialization is good
- lifecycle events do arrive
- the staged boot container keeps rendering
- the remaining red-only screen after later reactivation is not explained by “nothing initialized”

That shifts the suspicion further toward:

- Compose subtree visual restoration after resume/reactivation
- or Aurora graphics/compositor state for the child Compose content
- or a Skia / Skiko / scene invalidation problem where only the root clear/background survives

## Current recommended diagnostic commands

### Read the Aurora UI log

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'cat ~/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log'
```

### Tail the Aurora UI log

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'tail -f ~/.local/share/com.example/aurorakmpdemo/ui-diagnostics.log'
```

### Check current processes

```bash
ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null \
  -i "$HOME/AuroraOS/vmshare/ssh/private_keys/sdk" \
  -p 2223 defaultuser@127.0.0.1 \
  'ps aux | grep -i aurorakmpdemo | grep -v grep'
```

## Workaround status

- `key(startupId)`: did not solve the issue
- delayed content: did not solve the issue
- staged boot: improves observability, but did not solve the issue
- lifecycle-aware subtree generation on `ON_START`/`ON_RESUME`: logging confirms that it executes, but it still did not prevent the red-only screen
- cold start via Gradle remains the most reliable visible demo path
