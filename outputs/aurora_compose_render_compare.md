# Aurora Compose Render Compare

Date: 2026-06-26

Compared projects:

1. Main PoC: `com.example.aurorakmpdemo`
2. Official sample: `ru.auroraos.box.cmp.cmp` (`student-box-cmp`)

## Visual baseline reported by the user

- Main PoC before render-smoke changes:
  - white screen
- `student-box-cmp`:
  - violet / purple square

## Root app startup comparison

### Main PoC after this run

- Aurora app defaults into a dedicated render smoke screen
- this path bypasses:
  - `KoinApplication`
  - `MaterialTheme`
  - `Surface`
  - `Scaffold`
  - navigation
  - Compose resources on first render
- first screen is built mostly from:
  - `Box`
  - `Column`
  - `BasicText`
  - `Spacer`
  - custom clickable boxes
  - explicit backgrounds and borders

### `student-box-cmp`

- root still starts with:
  - `KoinApplication`
  - navigation host
- screen layout still relies on:
  - `Scaffold`
  - `TopAppBar`
  - `MaterialTheme.colorScheme`
  - resource strings
  - nested `Box`/`Column`
  - scrollable content

## Why the purple square in `student-box-cmp` is plausible

The official sample uses several Material-themed colored containers.

Most relevant observed code:

- `AppScaffold.kt`
  - `TopAppBar` uses `MaterialTheme.colorScheme.secondaryContainer`
- `HomeBody.kt`
  - top section uses `.background(MaterialTheme.colorScheme.secondaryContainer)`
  - content section uses `.background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))`

Interpretation:

- if only part of the screen is rendering correctly, the purple square the user sees may be one of these Material background containers or a partially rendered scaffold/content block
- this fits the hypothesis that Compose is not completely dead, but the final layout/theme/render pipeline is incomplete or clipped

## Why the new PoC path is different

The new PoC smoke screen deliberately removes the likely suspects one by one:

- no Material color scheme needed
- no `Scaffold`
- no `TopAppBar`
- no navigation host
- no Koin startup for the first frame
- no Compose resource lookup for the first frame

So the smoke screen is a cleaner answer to:

- can Aurora Compose draw a full-screen background?
- can it draw large text?
- can it draw simple colored blocks?
- can it draw basic clickable controls?

## Current diagnostic conclusion

- `student-box-cmp` proving a purple shape is visible suggests the Compose render path reaches at least partial painting.
- The original PoC white screen suggested either:
  - a higher-level UI layer hid everything
  - or the content rendered with colors/theme/layout that looked blank
- The new PoC smoke mode is the right next step because it strips those layers away while keeping the app runnable on Aurora.

## What this means for further debugging

If the new smoke screen is visible:

- the next suspect set is:
  - `MaterialTheme`
  - `Surface`
  - `Scaffold`
  - navigation
  - Koin initialization on first render

If the new smoke screen is still blank:

- the problem is deeper than those layers and closer to:
  - Compose native rendering
  - Skia/Skiko/Aurora graphics path
  - Wayland/compositor interaction
  - visibility/clipping of the root surface
