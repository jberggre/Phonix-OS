# Phonix OS

> **Shipping candidate, 2026-10-31:** `1.5 (versionCode 6)`. It builds, its **194** unit tests pass (27 suites, 0 failures),
> and it is **installed on the bench S10 over the device-owner build** (`adb install -r` → Success;
> device owner, the bundled Alpine rescue tree and an empty crash buffer all verified by
> `tools/verify-shipping.sh`). On that unit it renders on a real 1920x1080 HDMI monitor through DeX
> and runs a genuine Debian 12 guest under proot. The checksum-gated downloader has since been run
> on that unit through the production `switchDistro` — Kali, Debian, Arch, alpine→debian and Ubuntu
> (§25). Still unexercised on hardware: the debloat walkthrough, the Start-menu view modes, and a
> switch with the radio enabled but the mirror unreachable. Read [HANDOFF.md](docs/HANDOFF.md) and
> [VERIFICATION.md §13](docs/VERIFICATION.md).

Phonix is an Android app that turns a phone into a desktop mini-PC. It is a Kotlin/Jetpack
Compose shell with its own window manager, a proot Alpine guest, and Samsung DeX / connected-display
support. The target is recycled or screenless phones in a cyberdeck-style enclosure.

**Read [docs/HANDOFF.md](docs/HANDOFF.md) first, then [docs/VERIFICATION.md](docs/VERIFICATION.md).**
The handoff is the work queue for a new session; verification is the evidence for every claim.
This README is only the entry point.

**Status (2026-10-31):** this tree ships on-demand distribution installs that are verified rather
than simulated, a debloat walkthrough, Start-menu view modes, and a three-tier offline library
that reports its real document count. The previous session's first attempt at multi-distro
fabricated a rootfs — it copied Alpine's `/bin` and wrote a fake `os-release` claiming Debian — and
that code is gone; see
[VERIFICATION §12](docs/VERIFICATION.md#12-multi-distro-made-honest--2026-10-31-source-verification).
The update path is now a verifiable feed instead of a browser hand-off; §13 records it.

**Measured 1.1 record (2026-10-29, 16:58:38 build):** the P0 honesty fixes remain, and two
features that were only placeholders now do real work:

- **Local AI** — a real Qwen2.5 1.5B Instruct Q4_K_M model runs through `llama-server` inside the
  Alpine guest. The model is downloaded outside the APK, SHA-256 verified, and only marked READY
  after `/health` answers. Measured on the S10: **8.2–17.2 tok/s**, CPU only, no cloud call.
- **Phone Screen** — restored as a real, read-only `MediaProjection` view. It shows the actual
  default display (verified first frame 540×1140) and does not pretend to inject touches. On
  2026-11-01 the capture was aimed at the built-in panel by display: with the desktop on the HDMI
  monitor it had been mirroring the monitor (`panel 1080x2280 -> capturing 540x1140` now, see
  [VERIFICATION §14](docs/VERIFICATION.md)).

The **installed** APK on the bench unit is `1.5 (versionCode 6)`, installed 2026-10-31 over the
device-owner build from this tree (APK 30,013,418 bytes). See
[VERIFICATION.md](docs/VERIFICATION.md) §11 for the measured 1.1 record, §12 for the distro
round, §13 for the update path, and [HANDOFF.md](docs/HANDOFF.md) for what comes next.

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
- **Phonix 1.5 (`versionCode 6`)**, device owner
  `com.example.receiver.PhonixDeviceAdminReceiver`, installed 2026-10-31 over the previous build.
- **Local model installed on the unit:** Qwen2.5 1.5B Instruct Q4_K_M, 986,048,768 bytes, SHA-256
  `1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`, under the guest’s
  `/root/models`. `llama-server` is installed from Alpine community. The server is stopped at the
  end of the session; start it from the Local AI window.
- **1080×2280** override on a 1440×3040 panel at **420 dpi**, rotation 0. The S10's
  `wm density reset` jumps to **560 dpi**, not 420; use the harness restore path.
- adb is in `.tools/platform-tools/adb.exe`; USB tethering enumerates without pairing. Wireless
  debugging does not survive a reboot.
- **A real 1920x1080 HDMI monitor is attached** (DeX display id 2), so desktop rendering on DeX is
  measured. Dock/undock cycles, the caption bar and a 24 h plugged-in soak are still owed.
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

Window geometry (B4), layout persistence (B6), the landscape grid (B5) and the high-density host
chrome are implemented and were measured on the S10 before this round. What is **not** ready:- **1.5 is on hardware**, installed over the device-owner build on 2026-10-31 with the acceptance
  script passing (version, crash buffer, guest trees, device owner).
- **The update channel is a feed, not a store.** A build points at a feed with
  `-PphonixUpdateFeed=https://…/update.json`; About offers only a release whose `versionCode` is
  strictly newer, whose APK matches the published SHA-256, and whose signing certificate matches
  this install. With no feed configured the build says it has checked nothing. Phonix does not yet
  publish such a feed, so a deployed unit cannot be updated until one is hosted.
- **A downloaded distro has not been booted by this round's downloader.** The Debian tree on the
  unit is real and boots, but it predates the SHA-256 gate; a run of that path is owed.
- **Real DeX is measured** on the HDMI monitor, but dock/undock cycles and the caption bar are not.

The 1.5B model is usable but small: its first water answer was fluent and grounded yet conflated
boiling time with SODIS, so safety-critical answers still need the curated notes.

## Licence

Phonix OS is **Apache-2.0** - see [LICENSE](LICENSE). Use it, modify it, ship it, sell it, keep your
changes closed; keep the notices and you are compliant.

The Alpine userspace bundled as `app/src/main/assets/alpine-rootfs.tgz` is **not** covered by that
licence. The packages inside carry their own, recorded in
[docs/THIRD-PARTY-NOTICES.md](docs/THIRD-PARTY-NOTICES.md); distros downloaded at runtime stay under
their own upstream terms. `debug.keystore` is not in the repository and never has been - builds from
a fresh clone are signed with your own key, which means they cannot take over an existing install.
