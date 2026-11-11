# Provisioning a unit, and the numbers worth keeping

Three things live here: the current build/sign/install recipe (so a new session can put a build
on the unit without rediscovering it), the per-unit record we should keep (for QA and for honest
marketing numbers), and the design for a USB provisioning stick that can do this without a PC.
Current measured results and the work queue are in [VERIFICATION.md](VERIFICATION.md).

## Current build, signing and install (2026-10-29)

**On the unit now:** Phonix **1.5**, `versionCode 6`, installed on 2026-10-31 with
`tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk` (`adb install -r`
reported `Success`) over the device-owner build. Device owner is still
`com.example.receiver.PhonixDeviceAdminReceiver`, the crash buffer had no entry for the package,
and the APK was signed with the shared `debug.keystore` (file SHA-256 `7670a511…`), so the update
went in place and kept app data. The unit then held two rootfs trees, `alpine` (bundled rescue
image) and `debian` (the real Debian 12 guest from the earlier round). 1.2 was installed in place
on 2026-10-29; **1.3 and 1.4 never were**.

**Toolchain on this bench machine.** The 2026-10-31 builds used the stable copy at
`~/android-toolchain` (`jdk`, `gradle-9.3.1`, `sdk`); `%LOCALAPPDATA%/Temp/phonix-toolchain` is the
older location and can be cleaned up:

| Piece | Path |
|---|---|
| JDK 21 | `%LOCALAPPDATA%/Temp/phonix-toolchain/jdk-21.0.12.1+1` |
| Gradle 9.3.1 | `%LOCALAPPDATA%/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle` |
| Android SDK 36.1.0 | `%LOCALAPPDATA%/Temp/phonix-toolchain/sdk` |
| adb | `.tools/platform-tools/adb.exe` in the working checkout (or `sdk/platform-tools`) |

There is no Gradle wrapper JAR in the repository, so use the installed Gradle above rather than
`./gradlew`:

```bash
export JAVA_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/jdk-21.0.12.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
"$LOCALAPPDATA/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle" --offline --no-daemon assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A clean offline build took 7m17s (39 tasks) from a warm Gradle cache. The P0 follow-up was
incremental: `:app:compileDebugKotlin` 2m57s, then `assembleDebug` 1m42s. The current AI/mirror
build took `:app:compileDebugKotlin` 1m52s and `assembleDebug` 4m21s (final rebuild 3m43s); the APK is 31,685,535
bytes at `app/build/outputs/apk/debug/app-debug.apk`. `:app:testDebugUnitTest` passed when run
online (4m27s).

**The signing key is not in the repository and must not be lost.** `app/build.gradle.kts` uses
`debugConfig` with `storeFile = file("${rootDir}/debug.keystore")`; `debug.keystore` is
gitignored. The current app is signed by a debug certificate with SHA-256
`7d2b6c3d85f5519e5628a922bbebf1bb3981dd2400a7037fe83938b1cec0b5da`. A matching copy was
recovered from a prior Freebuff worktree at
`~/.config/freebuff-desktop/local-projects/*/threads/*/debug.keystore`. Copy it to the worktree
root before building, and keep a backup outside the repository.

Why it matters: the package is **device owner**. A new APK signed with a different key fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, and the old package cannot be uninstalled while it is device
owner. If the key is lost, the only ways out are the in-app decommission/reset (which wipes the
guest) or a factory reset. Never run `adb uninstall com.aistudio.phonix.vpxmkr` on a provisioned
unit.

Verify after installing:

```bash
adb shell dumpsys package com.aistudio.phonix.vpxmkr | grep -E 'versionName|versionCode'
adb shell dumpsys device_policy | grep 'admin='
```

Expected: `versionName=1.1`, `versionCode=2`, and the Phonix admin still listed. For the current
build, the installed package's `lastUpdateTime` is `2026-10-29 16:58:38`; use that to distinguish
it from the earlier 1.1 APKs.

## Per-unit record

Keep one row per phone. These are the fields that matter, where they come from, and why:

| Field | Source | Why it matters |
|---|---|---|
| Model / codename | `adb shell getprop ro.product.model` | Compatibility claim on the site |
| Android version + SDK | `getprop ro.build.version.release` / `.sdk` | DeX behaviour differs by One UI version |
| Serial | `adb shell getprop ro.serialno` | Warranty and RMA |
| Battery health / cycle count | Samsung Members or `dumpsys battery` | Sets the price tier and the battery guidance |
| Free storage before / after install | `df -h /data` | "How much space does Phonix use?" |
| APK install time | stopwatch around `adb install` | Fulfilment throughput |
| Guest unpack time | first launch to "Root filesystem ready" in the terminal | First-boot expectation on the site |
| X11 stack install time + size | first "Open Linux Desktop" to connected; `du -sh /usr` in guest | The biggest one-time wait a customer sees |
| Measured disk throughput | the Benchmark window (`dd` inside the guest) | A real performance number, not a synthetic score |
| Idle and load temperature | `/sys/class/thermal` via the Thermals screen | Thermal claim per cooling build |
| RAM used idle / with desktop open | `free -m` in the guest | Which phones are comfortable |
| Local model health + tok/s | `curl http://127.0.0.1:8081/health`, then one chat | The only honest local-AI number |
| Local model size + hash | `sha256sum` under `files/linux/alpine/root/models` | Detects a corrupt/prefilled model |
| Phone Screen consent + first frame | UI consent flow; `logcat -s PhonixScreenMirror:I` | Proves capture, not a painted mock |
| DeX display mode + resolution | `dumpsys display` (`Desktop` virtual display) | Proof the monitor path works |
| DeX wallpaper set | visual check | It is manual per unit (see `LINUX.md`) |
| Debloat commands run | the list copied by the wizard | What was removed, for support |
| Test results | browser, terminal, server, Linux window, reboot | Pass/fail per unit |

Store it as CSV keyed by serial. A simple sheet is enough until volume justifies a database.

### Baseline measured on the S10 (SM-G973U, Android 12, 2026-10-29)

Current bench unit: serial **R38M200YNRA**, app **1.5 (versionCode 6)** as of 2026-10-31, device
owner, phone
panel `1080x2280` at 420 dpi (physical `1440x3040`), rotation locked portrait. The readings
below are from this unit; the 1.1 bench session did not re-measure disk/RAM or attach a monitor.
These are real readings, not estimates:

- RAM: 7,461 MB total; 4,148 MB used with the desktop, browser and Linux window open.
- Storage: 109 GB `/data`, 12 GB used, 96 GB free.
- Guest rootfs plus the X11 desktop stack: 447 MB in `/usr` after installing Xvfb, x11vnc,
  noVNC, IceWM, xterm, fonts and x11-utils.
- `dd` inside the guest: ~600–690 MB/s write, 1.2–1.7 GB/s read for a 64 MB file (page cache;
  treat it as an upper bound, not disk endurance).
- Shell loop (2M awk iterations, via `time`): ~1.5 s.
- DeX virtual display: 3440×1440 at 160 dpi, `type=VIRTUAL`, `FLAG_DESKTOP_DISPLAY`;
  presented to a 1920×1080 HDMI output (that is the resolution `screencap -d <desktop id>`
  returns, and the size apps should fill).
- Local model: Qwen2.5 1.5B Q4_K_M, 986,048,768 bytes, SHA-256 matches; `/health` `{"status":"ok"}`;
  measured **8.2 tok/s** for a 185-token water answer and **17.2 tok/s** for a 7-token answer, with
  prompt processing around 40–45 tok/s. An earlier 132-token water answer measured 9.7 tok/s before
  the safety prompt was strengthened. CPU only; no GPU/NPU path. App process PSS while the
  model was loaded was ~252 MB, but the model pages are shared/mapped; do not publish that as the
  model’s RAM cost. Budget ~1.3 GB.
- Phone Screen: MediaProjection first frame **540×1140**, foreground notification 4711; Stop leaves
  `dumpsys media_projection` at `null`. Read-only; no touch injection.
- Idle guest RAM is small; the desktop itself (Xvfb + IceWM + xterm) is the bulk.

Re-run these after changing the rootfs or the X11 package list; the numbers above are the
reference for the current build.

## Local AI and Phone Screen provisioning

### Model download (normal path)

Open **Local AI → Set up local AI** on Wi-Fi. The app installs Alpine’s `llama-server` with
`apk add --no-cache llama-server`, downloads the pinned GGUF from Hugging Face, verifies SHA-256,
and starts it on `127.0.0.1:8081`. The measured bench network did 391 of 940 MB in 25 s.

Pinned artifact for manual verification:

```text
file:   Qwen2.5-1.5B-Instruct-Q4_K_M.gguf
bytes:  986048768
sha256: 1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370
url:    https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf
```

On-device paths (app-private; `run-as` works because this is a debug build):

```text
files/linux/alpine/root/models/qwen2.5-1.5b-instruct-q4_k_m.gguf
files/linux/alpine/root/models/llama-server.pid
files/linux/alpine/root/models/llama-server.log
```

Check it without the UI:

```bash
adb shell curl -s http://127.0.0.1:8081/health
adb shell run-as com.aistudio.phonix.vpxmkr \
  sha256sum files/linux/alpine/root/models/qwen2.5-1.5b-instruct-q4_k_m.gguf
adb shell run-as com.aistudio.phonix.vpxmkr \
  tail -20 files/linux/alpine/root/models/llama-server.log
```

Expected health body: `{"status":"ok"}`. The server is **not** started at boot; tap Start in the
window, and use Stop when finished. A first answer takes roughly 4–10 s of prompt processing; the
cool-start decode measured 8.2–17.2 tok/s. Treat those as short-run numbers, not a thermal claim.

If the download is done on a PC instead, a debug install can be prefilled with `run-as`:

```bash
adb push Qwen2.5-1.5B-Instruct-Q4_K_M.gguf /data/local/tmp/model.gguf
adb shell run-as com.aistudio.phonix.vpxmkr mkdir -p files/linux/alpine/root/models
adb shell run-as com.aistudio.phonix.vpxmkr \
  cp /data/local/tmp/model.gguf files/linux/alpine/root/models/qwen2.5-1.5b-instruct-q4_k_m.gguf
```

The app verifies every model it downloads itself. A manually prefilled file is accepted on size alone, so run `sha256sum` yourself before copying it; a wrong size is rejected, but a same-size wrong file is not rehashed.

### Phone Screen consent

Phone Screen uses `MediaProjection`, so Android’s system dialog is mandatory. On a headless or
shattered unit, accept it once through whichever surface is usable:

```bash
# DeX with a mouse: click Start now on the system dialog.
# Or, over network ADB, find and tap the button:
adb shell uiautomator dump /sdcard/ui.xml
adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep 'Start now'
adb shell input tap <x> <y>
```

Consent is per capture session; it does not survive Stop or reboot. ADB is a setup channel, not a
bypass. The window is read-only and cannot inject taps. Closing the window stops the foreground
service (notification id 4711).

## First boot after a factory reset: the one click we cannot script

A reset unit has no Phonix, no USB debugging and DeX auto-start **off** (it is off by default).
Connect HDMI and the monitor shows Samsung's "Start" screen. Nothing of ours can appear
there: an app cannot draw on that display until DeX is already running, so no amount of
Phonix code removes this first click. It has to be clicked once, with a mouse, per unit.

That makes the click a build step, not a customer step: do it **before the phone goes into
the shell**, because once the panel is gone the only input left is the USB mouse the unit ships
with. A keyboard works too (Tab, Enter).

After the one click, in this order:

1. Finish the setup wizard (skip the Samsung account and the restore prompt).
   - Inside Phonix's own **Setup Wizard**, Step 3 now includes the **Linux Distro Switcher** (`Alpine 3.24` bundled offline default, or on-demand HTTPS rootfs download for `Debian 12`, `Ubuntu 24.04 LTS`, `Arch Linux ARM`, or `Kali Linux`). For offline bench provisioning, keep **Alpine 3.24** selected so rootfs extraction completes in ~2 seconds without network traffic.
   - Any time after setup, technicians or users can open the **Help & Docs** tab from the top navigation pill (`Phonix Mini PC` → `Help & Docs`) or the Start menu to inspect live runtime status and run diagnostic terminal commands.
2. Turn on DeX auto-start, so the prompt never returns:
   Settings → Advanced features → Samsung DeX → *Auto start when HDMI is connected*
   (One UI 5 and newer: Settings → Connected devices → Samsung DeX).
3. Developer options → *Stay awake* on, *USB debugging* on; plug into the technician PC.
4. Run `provision.sh` — installs the APK, makes Phonix device owner, grants the secure-settings
   permission, then the unit is a sealed appliance again.

Support script for exactly this situation: if a customer reports a black monitor after a reset,
ask them to plug a USB mouse into the hub. That is the whole fix.

Note the one normal-looking oddity: while DeX is active the phone's own panel goes dark. That is
Samsung saving power, not a fault. If the panel stays dark after a power-button press *and* a
mouse click on the monitor does nothing, then the panel or its flex is the fault, and the unit
needs the mouse to be usable at all.

## Network onboarding (first run, and after a customer moves house)

A unit only knows the Wi-Fi it was provisioned on, and a factory reset erases it. Phonix now has
its own network screen so the customer does not need a phone screen to fix that:

- The app opens **Connect Phonix** by itself on a unit that has never been online. After that it
  is one click away: the collapsed top notch shows `ONLINE` or `OFFLINE` with a Wi-Fi icon, and
  clicking it opens the screen again.
- Typing the SSID and password saves a **real Android network** through
  `WifiManager.addNetwork`. Android 10 restricted Wi-Fi configuration to system apps and DPCs;
  `provision.sh` makes Phonix the device owner, so the restriction does not apply. The saved
  network belongs to the platform, survives reboots and is used by every app, not just Phonix.
- Success is reported from `ConnectivityManager`'s **validated** state, not from the write. If
  the password is wrong the app says so instead of showing a green tick.
- **Wired Ethernet is a first-class path.** Plug an Ethernet adapter into the USB-C hub and the
  screen says *Wired connected* and never asks for Wi-Fi. This is the simplest option for the
  housing build.
- **Android's own picker is always offered**, because it lists nearby networks instead of asking
  the customer to type a name. Wi-Fi scanning requires location permission (and location
  services) that Phonix deliberately does not turn on, so the in-app form takes the SSID by hand
  and the system screen is the list-view alternative.

Why not `WifiNetworkSpecifier`: it creates an **app-scoped** connection that the platform tears
 down when the requesting process dies, and it asks for approval on screen each time. On an
 appliance that reboots unattended and must serve SSH/Syncthing for days, that would drop the
 machine's network whenever the app is killed. `WifiNetworkSuggestion` persists, but Android 12
 does not auto-approve suggestions for a device owner, so it still needs a user tap on a
 notification that may not land on the monitor.

**What has been verified on hardware (2026-10-29):** the screen renders and reads live state on
DeX (`screenshots/wifi-02-setup.png`), the device owner silently granted the SSID readback
permission, and the Android-settings button is wired. The *save-and-join* path has **not** been
exercised end to end, because joining a different network would drop the bench's wireless ADB
mid-test. Test it on a spare unit with a known hotspot before promising it.

## Bench traps found on the first real unit

- **A preload can hold an account that blocks device owner.** After the factory reset this
  Cricket S10 still had a `NewsBreak` account (type `Newsbreak`), created by the preloaded
  `com.particlenews.newsbreak` app. `dpm set-device-owner` refuses while any account exists, so
  the run has to remove the app (`pm uninstall --user 0 com.particlenews.newsbreak`) and wait a
  second before claiming owner. `provision.sh` reports the count; do this by hand first.
- **Wireless debugging does not survive a reboot.** Samsung turns it off, so a bench that pairs
  over Wi-Fi and then reboots to test the kiosk loses contact mid-run. Provision over USB when
  the unit will be rebooted, or expect to re-pair before the post-boot checks. USB tethering
  enumerated as `R38M200YNRA` with no pairing on this unit. Network ADB is still the right
  initial-setup channel for a headless unit: `adb pair`, `adb connect`, install, then
  `input tap` the system consent button if needed.
- **MediaProjection consent cannot be granted by device owner.** Do not look for an `appops`
  shortcut in production. The system dialog is per capture session; a reboot loses it. Approved
  once through DeX, a mouse or network ADB `input tap`.
- **The package to install is `llama-server`, not `llama.cpp`.** `llama.cpp` is the CLI package.
  The server binds `127.0.0.1:8081`; the base image does not have it until `apk add`. The model is
  ~941 MiB, so leave at least 1.2 GB free before setup.
- **`wm density reset` is wrong on this S10.** It sets the display to **560 dpi** (the panel's
  density), not the 420 dpi base the phone ships with. After a density test, restore with
  `wm density 420`, not `wm density reset`; `tools/deck-check.sh` now snapshots and restores the
  recorded value. `wm size` was `1080x2280` override on `1440x3040` physical; restore that
  override explicitly rather than calling `wm size reset` if you want the bench state back.
- **A device-owner package cannot be killed for a persistence test.** `am kill` is ignored and
  `adb shell kill -9` is denied (`Operation not permitted`). Process-death and layout-persistence
  tests need the in-app Reboot control or a factory-reset unit, not `am kill`.
- **The DeX workspace is not the HDMI display.** DeX renders its desktop on a virtual display
  named `Desktop` and presents that onto the HDMI output. A window launched on the physical HDMI
  display (it does appear in `DISPLAY_CATEGORY_PRESENTATION`) is composed *under* DeX's
  presentation: the app looks healthy in `dumpsys` and the monitor still shows Samsung's
  launcher. This build targets the `Desktop` display by name. If a unit shows the launcher
  instead of Phonix, check `dumpsys SurfaceFlinger` — the HDMI display's layer list either ends
  with the Phonix window or with `desktoplauncher`.

## The provisioning stick

A stick that does the work when plugged into a phone is a good idea, but the constraint is
ADB authorization, not hardware:

- **ADB over USB needs the phone to trust the host.** On a freshly reset phone, `adb` shows
  "unauthorized" until someone taps *Allow USB debugging*; the key is then stored in
  `/data/misc/adb/adb_keys`, which an app cannot write.
- **A Rubber Ducky cannot do it alone.** It is a keyboard: it can navigate menus and enable
  Developer options with keystrokes, but it cannot install an APK or run `adb` — there is no
  host on the other end of the cable.
- **A small Linux board can.** A Pi Zero 2 W (or CM4 carrier) in a USB-stick shape runs `adb`
  and the install script itself. That is the design that actually works.

### Recommended stick

1. Pi Zero 2 W + USB-A male plug + 16 GB card, powered from the phone's USB port (or a hub).
2. Boots a minimal Linux with `adb` and the Phonix APK on the card.
3. Script: wait for the device, `adb install -r phonix.apk`, grant the notification permission,
   launch the app once, open the Linux window once (to unpack the guest), then run the selected
   `pm uninstall --user 0` lines, write the unit's metrics row, and print a PASS/FAIL summary.
4. If the phone is not yet authorized, the stick cannot tap the dialog. Options: keep one
   technician tap per unit (fine at low volume), pre-authorize the stick's key on units you
   control, or use Knox/MDM enrolment for fleet volume.

### Do not promise

- "Fully automatic on any phone" — the RSA prompt stands in the way.
- Root, unlocked bootloader or custom ROM behaviour — none of that is required for Phonix, and
  none of it is something the stick can do over ADB.
- Debloating on the stick without ADB authorization; the app itself can only copy commands.

### Metrics the stick should emit (for the site and for support)

`model, android, serial, install_seconds, first_boot_seconds, x11_seconds, guest_mb, dd_write,
dd_read, loop_ms, temp_idle_c, temp_load_c, ram_idle_mb, dex_resolution, tests_passed,
tests_failed, debloat_list`

With a hundred rows of that, the compatibility table and the performance claims on the website
can be built from data instead of adjectives.
