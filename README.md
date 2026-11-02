# Phonix OS

> **On the bench unit, 2026-10-31:** `1.5 (versionCode 6)`. It builds, its **47 unit tests** pass
> (`assembleDebug testDebugUnitTest`, re-run from scratch with `--rerun-tasks`), and
> `tools/verify-shipping.sh` installed it over the device-owner build (`adb install -r`, Success):
> version, crash buffer, bundled Alpine rescue tree and device owner all passed.
>
> **DeX is measured, but on the 1.3 (`versionCode 4`) build.** That pass found a real ES-G34C5
> 1920x1080 HDMI monitor attached and captured Phonix rendering on Samsung's `Desktop` display,
> running a genuine Debian 12 rootfs under proot. 1.5's device evidence is install-level only; no
> monitor pass is recorded for it yet. Read [HANDOFF.md](docs/HANDOFF.md) and
> [VERIFICATION.md §12](docs/VERIFICATION.md).

Phonix is an Android app that turns a phone into a desktop mini-PC. It is a Kotlin/Jetpack
Compose shell with its own window manager, a proot guest, and Samsung DeX / connected-display
support. The target is recycled or screenless phones in a cyberdeck-style enclosure.

**Read [docs/HANDOFF.md](docs/HANDOFF.md) first, then [docs/VERIFICATION.md](docs/VERIFICATION.md).**
The handoff is the work queue for a new session; verification is the evidence for every claim.
This README is only the entry point.

**Status (1.5, source):** `model/PhonixApps.kt` is the shipped-app catalogue, so removing a desktop
icon or unpinning a taskbar entry can no longer make an app unreachable — Start and All apps read
the catalogue, and all three surfaces resolve icons through `AppIconGlyphs`. The AI window has
75–200% zoom. Linux package commands ask the active distro for its manager (`apk` / `apt-get` /
`pacman`) instead of assuming Alpine. The local-AI runtime owns a private bundled Alpine guest, so
switching the desktop's distro no longer prunes the server or the model.

**Earlier rounds, for orientation:** 1.3 replaced a fabricated distro installer with a verified one
(the rejected code copied Alpine's `/bin` and wrote a fake `os-release` claiming Debian — it is
gone; see
[VERIFICATION §12](docs/VERIFICATION.md#12-multi-distro-made-honest--2026-10-31-source-verification)).
1.4 made the default desktop flat shortcuts with tiles opt-in under Personalize › Shortcut Style,
and stopped the Linux card, terminal banner and shell agent hardcoding "Alpine" — they now name the
installed guest (`Debian 12 (Bookworm) · 12.15` on the monitor).

**Measured feature record:** Local AI is a real Qwen2.5 1.5B Instruct Q4_K_M model served by
`llama-server`, SHA-256 checked and marked READY only after `/health` answers — **8.2–17.2 tok/s**
on the S10, CPU only, no cloud call (measured on 1.1 in the Alpine guest). Phone Screen is a real,
read-only `MediaProjection` view (first frame 540×1140) and does not pretend to inject touches.

## Start here

| Document | What it answers |
|---|---|
| [docs/HANDOFF.md](docs/HANDOFF.md) | Current state and the ordered work queue for the next session |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) | Current unit state, measured bugs (B1–B13), feature matrix, P0 checks, local-AI/mirror record (§11), distro and monitor passes (§12) |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Research behind DeX, VS Code, updates, distros, local AI, kiosk; next work order |
| [docs/PROVISIONING.md](docs/PROVISIONING.md) | Toolchain, signing key, build/install recipe, per-unit record |
| [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) | DeX, Pixel desktop mode, Motorola, LG; what is verified per vendor |
| [docs/LINUX.md](docs/LINUX.md) | The proot guest, X11 bridge, servers |
| [docs/PRIVACY.md](docs/PRIVACY.md) | Private DNS, Pi-hole guidance, decommission/wipe |
| [tools/deck-check.sh](tools/deck-check.sh) | Device baseline, metric matrix, crash scan, death/soak checks |

## Bench unit

- **SM-G973U Galaxy S10**, Android 12 / One UI 4.1 (API 31), serial `R38M200YNRA`.
- **Phonix 1.5 (`versionCode 6`)**, device owner
  `com.example.receiver.PhonixDeviceAdminReceiver`, installed **2026-10-31** over the previous
  device-owner build with the recovered `debug.keystore`.
- **Monitor attached:** ES-G34C5 HDMI at 1920x1080 (`type EXTERNAL`, `FLAG_PRESENTATION`, display
  id 7), plus Samsung's virtual `Desktop` display (id 2, 160 dpi). Phonix targets `Desktop` by
  name. Measured on the 1.3 build: wallpaper, top bar, OS Card / Vitals, taskbar and badge all
  render there.
- **Active guest:** a genuine Debian 12 (bookworm) rootfs under proot — real `os-release`, `apt`
  and `var/lib/dpkg/status`. That tree predates the 1.3 installer (it was not produced by it).
- **Alpine rescue tree:** ships in the APK and is never pruned by `pruneOtherDistros`. It was
  absent from the unit at the 1.3 pass; a first Alpine launch re-extracts it from the APK.
- **Local model:** Qwen2.5 1.5B Instruct Q4_K_M, 986,048,768 bytes, SHA-256
  `1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`, pinned and verified before
  READY. In 1.5 it lives in the private bundled Alpine guest, so a desktop distro switch does not
  remove it. The server is stopped at the end of a session; start it from the Local AI window.
- **1080×2280** override on a 1440×3040 panel at **420 dpi**, rotation 0. The S10's
  `wm density reset` jumps to **560 dpi**, not 420; use the harness restore path.
- adb is in `.tools/platform-tools/adb.exe`; USB tethering enumerates without pairing. Wireless
  debugging does not survive a reboot, so re-pair before post-boot checks.
- The package is **device owner**. `adb uninstall` and `am force-stop` are not recovery options;
  see PROVISIONING before removing or replacing it.

## Build and install

There is no Gradle wrapper JAR. Use the local toolchain (JDK 21, Gradle 9.3.1, SDK 36) and the
existing gitignored `debug.keystore`:

```bash
export JAVA_HOME="$HOME/android-toolchain/jdk"
export ANDROID_HOME="$HOME/android-toolchain/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
"$HOME/android-toolchain/gradle-9.3.1/bin/gradle" \
  --offline --no-daemon assembleDebug testDebugUnitTest

export PATH="$PWD/.tools/platform-tools:$PATH"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point `sdk.dir` at the SDK with forward slashes. A clean offline build takes
about 7 minutes; an incremental Kotlin compile about 3. The APK is
`app/build/outputs/apk/debug/app-debug.apk`.

Prefer the stable `~/android-toolchain` copy: the older `%LOCALAPPDATA%\Temp\phonix-toolchain` path
can be removed by cleanup.

**Do not lose `debug.keystore`.** It is gitignored and is the only key that can update the
device-owner install. A matching copy has been recovered from a prior worktree before; the
SHA-256 and recovery path are in [docs/PROVISIONING.md](docs/PROVISIONING.md). Back it up outside
the repository.

## Before changing device state

```bash
tools/deck-check.sh baseline     # records size, density, rotation, owner
tools/deck-check.sh crashes      # crash/ANR evidence
tools/deck-check.sh matrix       # simulated pocket/deck metrics, restores on exit

tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk
                                 # non-destructive acceptance: version, crash buffer,
                                 # guest trees, device owner; never uninstalls
```

The harness restores the exact recorded metrics, including the S10's 420 dpi base. Do not leave
`wm size` or `wm density` modified.

## Local AI and Phone Screen

- **Local AI** is opt-in and on demand. It downloads about **1.0 GB** into its private Alpine guest
  and needs about **1.3 GB RAM** while loaded. Setup detects the guest's package manager, installs
  `llama-server` (accepting the result only when `command -v llama-server` finds a binary),
  downloads the model from Hugging Face, checks it against the pinned SHA-256, and starts it on
  `127.0.0.1:8081`. “READY” is only shown after `GET /health` returns 200. The UI exposes measured
  tok/s and a Stop control. It does **not** autostart at boot.
- **Phone Screen** is a real capture, not a mock. Android’s screen-capture consent dialog is
  mandatory; once granted, the window shows the default display read-only. It cannot tap or type.
  The consent is per session and does not survive a reboot. See
  [PROVISIONING.md](docs/PROVISIONING.md) for the network-ADB approval path on a headless unit.

## Current boundary

Window geometry (B4), layout persistence (B6), the landscape grid (B5) and the high-density host
chrome are implemented and were measured on the S10 in earlier rounds. What is **not** ready:

- **The downloader has never installed a distro on the phone.** The SHA-256 gate, byte progress and
  failure path are unit-tested only; a real install prunes the working Debian tree, so it needs the
  owner's go-ahead. The Debian tree on the unit predates this installer, so no guest produced by
  the checksum-gated path has ever booted.
- **Unexercised on hardware:** the debloat hide walkthrough, the Start-menu view modes (at 640 dpi
  the pinned row and all-apps list still clip), taps through Start/All apps and the AI zoom on the
  DeX display (`uiautomator` only sees the phone display), and the 1 GB model download from an
  in-app tap.
- **No update channel.** The updater reports `ERROR` with “no public update feed is configured”
  instead of claiming the unit is current, but there is still no signed manifest to check against,
  so a deployed unit cannot be updated remotely.
- **The Pixel 9 Pro Fold is pre-flighted, not verified** — the physical posture/display/window
  matrix is still owed, and it must not be enrolled as device owner.

The 1.5B model is usable but small: its first water answer was fluent and grounded yet conflated
boiling time with SODIS, so safety-critical answers still need the curated notes.
