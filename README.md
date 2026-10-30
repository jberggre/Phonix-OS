# Phonix OS

Phonix is an Android app that turns a phone into a desktop mini-PC. It is a Kotlin/Jetpack
Compose shell with its own window manager, a proot Alpine guest, and Samsung DeX / connected-display
support. The target is recycled or screenless phones in a cyberdeck-style enclosure.

**Read [docs/HANDOFF.md](docs/HANDOFF.md) first, then [docs/VERIFICATION.md](docs/VERIFICATION.md).**
The handoff is the work queue for a new session; verification is the evidence for every claim.
This README is only the entry point.

**Status (2026-10-29, 16:58:38 build):** the P0 honesty fixes remain, and two features that were
only placeholders now do real work:

- **Local AI** — a real Qwen2.5 1.5B Instruct Q4_K_M model runs through `llama-server` inside the
  Alpine guest. The model is downloaded outside the APK, SHA-256 verified, and only marked READY
  after `/health` answers. Measured on the S10: **8.2–17.2 tok/s**, CPU only, no cloud call.
- **Phone Screen** — restored as a real, read-only `MediaProjection` view. It shows the actual
  default display (verified first frame 540×1140) and does not pretend to inject touches.

The source is committed and pushed on `main`. The APK still reports `1.1 (versionCode 2)`;
identify this build by its **2026-10-29 16:58:38** install time or SHA-256
`a7516b6cef12cbbd80378d510ff031c2a04c9a3b8248c0f5e3bddba5c74a7674`. See
[VERIFICATION.md](docs/VERIFICATION.md) §11 for the measured record and
[HANDOFF.md](docs/HANDOFF.md) for what comes next.

## Start here

| Document | What it answers |
|---|---|
| [docs/HANDOFF.md](docs/HANDOFF.md) | Current state and the ordered work queue for the next session |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) | Current unit state, measured bugs (B1–B13), feature matrix, P0 checks, local-AI/mirror record (§11) |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Research behind DeX, VS Code, updates, distros, local AI, kiosk; next work order |
| [docs/PROVISIONING.md](docs/PROVISIONING.md) | Toolchain, signing key, build/install recipe, per-unit record |
| [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) | DeX, Pixel desktop mode, Motorola, LG; what is verified per vendor |
| [docs/LINUX.md](docs/LINUX.md) | The proot/Alpine guest, X11 bridge, servers |
| [docs/PRIVACY.md](docs/PRIVACY.md) | Private DNS, Pi-hole guidance, decommission/wipe |
| [tools/deck-check.sh](tools/deck-check.sh) | Device baseline, metric matrix, crash scan, death/soak checks |

## Bench unit

- **SM-G973U Galaxy S10**, Android 12 / One UI 4.1 (API 31), serial `R38M200YNRA`.
- **Phonix 1.1 (`versionCode 2`)**, device owner
  `com.example.receiver.PhonixDeviceAdminReceiver`. The current APK was installed **2026-10-29
  16:58:38**; the version string alone does not distinguish it from earlier 1.1 builds.
- **Local model installed on the unit:** Qwen2.5 1.5B Instruct Q4_K_M, 986,048,768 bytes, SHA-256
  `1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`, under the guest’s
  `/root/models`. `llama-server` is installed from Alpine community. The server is stopped at the
  end of the session; start it from the Local AI window.
- **1080×2280** override on a 1440×3040 panel at **420 dpi**, rotation 0. The S10's
  `wm density reset` jumps to **560 dpi**, not 420; use the harness restore path.
- adb is in `.tools/platform-tools/adb.exe`; USB tethering enumerates without pairing. Wireless
  debugging does not survive a reboot.
- **No external monitor is attached.** Real DeX, dock/undock and caption-bar behaviour still need
  hardware; `wm size`/`wm density` only simulate the metrics.
- The package is **device owner**. `adb uninstall` and `am force-stop` are not recovery options;
  see PROVISIONING before removing or replacing it.

## Build and install

There is no Gradle wrapper JAR. Use the local toolchain and the existing gitignored
`debug.keystore`:

```bash
export JAVA_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/jdk-21.0.12.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
"$LOCALAPPDATA/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle" \
  --offline --no-daemon assembleDebug

export PATH="$PWD/.tools/platform-tools:$PATH"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

A clean offline build takes about 7 minutes; an incremental Kotlin compile is about 3 minutes.
The APK is `app/build/outputs/apk/debug/app-debug.apk`.

**Do not lose `debug.keystore`.** It is gitignored and is the only key that can update the
device-owner install. A matching copy has been recovered from a prior worktree before; the
SHA-256 and recovery path are in [docs/PROVISIONING.md](docs/PROVISIONING.md). Back it up outside
the repository.

## Before changing device state

```bash
tools/deck-check.sh baseline     # records size, density, rotation, owner
tools/deck-check.sh crashes      # crash/ANR evidence
tools/deck-check.sh matrix       # simulated pocket/deck metrics, restores on exit
```

The harness restores the exact recorded metrics, including the S10's 420 dpi base. Do not leave
`wm size` or `wm density` modified.

## Local AI and Phone Screen

- **Local AI** is opt-in and on demand. It downloads about **1.0 GB** into the guest and needs about
  **1.3 GB RAM** while loaded. The first launch installs `llama-server` with `apk add`; the model is
  then downloaded from Hugging Face, checked against the pinned SHA-256, and started on
  `127.0.0.1:8081`. “READY” is only shown after `GET /health` returns 200. The UI exposes measured
  tok/s and a Stop control. It does **not** autostart at boot.
- **Phone Screen** is a real capture, not a mock. Android’s screen-capture consent dialog is
  mandatory; once granted, the window shows the default display read-only. It cannot tap or type.
  The consent is per session and does not survive a reboot. See
  [PROVISIONING.md](docs/PROVISIONING.md) for the network-ADB approval path on a headless unit.

## Current boundary

The P0 truthfulness fixes and the two real features above are done. The next app-side blockers are
window geometry/normalisation (B4), the landscape shortcut grid (B5), and the updater’s URL/version
compare/install path (B8). The 1.5B model is usable but small: its first water answer was fluent and
grounded yet conflated boiling time with SODIS, so safety-critical answers still need the curated
notes. No external monitor was attached, so real DeX capture/positioning remains unverified.
