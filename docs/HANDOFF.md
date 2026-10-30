# Phonix OS — handoff for the next session

Written 2026-10-29, after the round that replaced the local-AI placeholder and restored
Phone Screen as a real view. This page is the **work queue**. Read
[VERIFICATION.md](VERIFICATION.md) for the evidence behind every claim, [ROADMAP.md](ROADMAP.md)
for the research and sources, and [PROVISIONING.md](PROVISIONING.md) before touching the unit.

## Where the tree stands

- `main` is committed and pushed at the build described here. The APK was built from this source.
- **Version:** `1.1 (versionCode 2)`. The version was deliberately not bumped during the bench
  round, so identify this build by its **2026-10-29 16:58:38** install time or SHA-256
  `a7516b6cef12cbbd80378d510ff031c2a04c9a3b8248c0f5e3bddba5c74a7674`, not by “1.1” alone.
- **Unit:** SM-G973U Galaxy S10, Android 12 / One UI 4.1 (API 31), serial `R38M200YNRA`,
  device owner, `1080x2280` override on a 1440x3040 panel at 420 dpi, rotation 0.
- **No external monitor is attached.** DeX was simulated with `wm size`/`wm density`; real DeX,
  dock/undock and caption-bar behaviour remain unverified.
- **Left running:** nothing. `llama-server` is stopped and Phone Screen is OFF. The desktop is
  open; the package is device owner.

## What this round actually added

| Surface | State | Evidence |
|---|---|---|
| Local AI | Real Qwen2.5 1.5B Instruct Q4_K_M through Alpine's `llama-server`; model downloaded outside the APK, SHA-256 checked, READY only after `/health` 200 | VERIFICATION §11; measured 8.2–17.2 tok/s on CPU |
| Phone Screen | Real read-only `MediaProjection` capture; Android consent is mandatory; first frame measured 540x1140 | VERIFICATION §11 |
| VS Code | Badge is gated on a real `Socket.connect(127.0.0.1:8000)` probe; the success path is still unverified because Alpine has no `code` binary | VERIFICATION §10 |
| About / updates | **About Phonix OS** is in the live Start menu | VERIFICATION §10 |
| Docs | README, verification, roadmap, provisioning, compatibility, Linux, privacy updated; old deleted handoff replaced by this page | this commit |

The earlier P0 removal of the painted Phone Screen and the “Cyberdeck AI” label is history, not
current behaviour. The mock must not return; the current window shows real frames or says OFF.

## Next work, in order

1. **Window geometry and normalisation (B4).** This blocks the deck. Drag currently clamps at
   about 850x750 px, there is no resize grip, and an open window keeps stale coordinates when the
   display changes, leaving its close button off-screen. Unify units, clamp to the container,
   add a resize affordance, and normalise every window on a size/density change.
   *Done when:* a window can be dragged and resized to every edge at 1920x1080 in both 160 dpi
   and 640 dpi, and survives a live metric change with its caption reachable.
2. **Persist the desktop (B6).** Shortcut positions and window layout are not saved, so a reboot
   or dock cycle loses the setup. Persist per size class so one deck layout can be restored.
   *Done when:* set a layout, force-stop/reopen, and get the same layout back.
3. **Landscape icon grid (B5).** The grid is sized for a tall phone; at 2280x1080 the third row is
   clipped by the taskbar. Compute rows/columns from the container.
4. **Real DeX pass.** With a monitor: capture, window placement, caption bar, dock/undock, and a
   24 h plugged-in soak. Do not claim DeX is verified until this is done.
5. **Updater (B8/B13).** About is reachable, but the check still targets the wrong repository,
   compares versions as strings, and returns 404. Fix the repository, use an integer version
   compare, and require a signed manifest before offering an install.
6. **Decide Phone Screen input.** The picture is real; tapping is not. If the owner wants control,
   add it through an explicit AccessibilityService opt-in with its own disclosure. Do not bring
   back app-UID `input` injection — Android rejects it.
7. **Local-model quality and thermals.** Run a 30-minute plugged-in generation soak and record
   tok/s, temperature and `/health` stability. The 1.5B model is useful but conflated a one-minute
   boil with SODIS once; the curated notes remain the safety reference. Evaluate a larger model
   only after measuring RAM and heat.
8. **Module registry and three deck layouts (cyberdeck parity).** Declare module capabilities,
   save layouts per deck, add a keyboard command palette, and delete the dead `PhonixStartMenu`
   in the same pass.
9. **Untested paths.** OOBE (the unit is past it and a reset is destructive), game install URLs,
   VS Code's success path, and Phone Screen on a real monitor. Use a spare unit for OOBE.

## Do not

- Do not try to remove DeX's caption bar or combine kiosk with DeX; Samsung documents that these
  do not combine, and there is no public API for the caption bar.
- Do not inject touches through the app UID or `input` without an accessibility service.
- Do not wipe, uninstall or `am force-stop` the device-owner install. Read
  [PROVISIONING.md](PROVISIONING.md) first.
- Do not lose `debug.keystore`. It is gitignored and is the only key that can update this install.
- Do not present a simulated `wm size` run as a DeX test.

## Build, install, check

```bash
export JAVA_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/jdk-21.0.12.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
"$LOCALAPPDATA/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle" \
  --offline --no-daemon assembleDebug

export PATH="$PWD/.tools/platform-tools:$PATH"
adb install -r app/build/outputs/apk/debug/app-debug.apk

tools/deck-check.sh baseline
tools/deck-check.sh crashes
tools/deck-check.sh matrix     # restores the recorded 1080x2280 / 420 dpi
```

A clean offline build is about 7 minutes; an incremental Kotlin compile about 3. The test task
was last run green in this round (`:app:testDebugUnitTest`, 2m06s offline).

## Open questions for the owner

1. **Phone Screen input:** is read-only enough, or is an accessibility opt-in wanted?
2. **Update channel:** GitHub Releases, or a Phonix-owned signed manifest? The updater cannot be
   finished without choosing one.
3. **Next hardware:** this unit is API 31, so the API 36/37 large-screen restrictions do not bite
   here. A newer unit would exercise them; decide before designing the next layout rules.
4. **Model size:** stay at 1.5B for RAM/heat, or test 3B on this S10?
5. **Sealing:** supervisor PIN in-app while keeping DeX, or a stricter kiosk on hardware that
   supports it?

## Document map

| Document | Use |
|---|---|
| [VERIFICATION.md](VERIFICATION.md) | Measured state, B1–B13, feature matrix, bench records |
| [ROADMAP.md](ROADMAP.md) | DeX/Pixel/Motorola research, VS Code, updates, distros, local AI, kiosk |
| [PROVISIONING.md](PROVISIONING.md) | Toolchain, signing key, install and recovery, per-unit record |
| [COMPATIBILITY.md](COMPATIBILITY.md) | Vendor/desktop-mode differences |
| [LINUX.md](LINUX.md) | proot/Alpine guest, X11 bridge, servers |
| [PRIVACY.md](PRIVACY.md) | Private DNS, Pi-hole, decommission |
| [tools/deck-check.sh](../tools/deck-check.sh) | Device baseline, metric matrix, crash/death/soak checks |
