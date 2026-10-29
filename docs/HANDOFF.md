# Phonix OS — working handoff

Written 2026-10-29, end of the round that added network onboarding, the app/game catalogs, the
desktop browser shortcut and the container stop fix. This page is the *work queue*: what is
proven, what is only written, and what has to happen next. `docs/ROADMAP.md` holds the research
behind the bigger questions (VS Code, distros, local AI, updates, kiosk) and its sources.

Read the last section before writing any customer-facing sentence.

---

## 1. Repo and unit

| | |
|---|---|
| Branch | `main` — last code commit `2acbe22`, then this handoff; pushed to `origin` |
| Build | `gradle --offline assembleDebug` (JDK 21 + Gradle 9.3.1 + SDK in `%LOCALAPPDATA%/Temp/phonix-toolchain`) |
| Package | `com.aistudio.phonix.vpxmkr`, `versionCode 1`, `versionName "1.0"` |
| Install | `adb install -r app/build/outputs/apk/debug/app-debug.apk` — in place, keeps device owner and guest |
| Unit | SM-G973U `R38M200YNRA`, Android 12 / One UI 4 |
| ADB | **USB works** (USB tethering on). Wireless debugging resets on every reboot |
| Play Store | **No Google account registered** (`dumpsys account` shows none) — store installs stop at the sign-in screen |
| Guest | Alpine 3.24.3 in `files/linux/alpine`, 559 MB with the X11 stack installed |
| State at handoff | Plugged in over USB, no monitor attached, Phonix running with the guest up (one app process plus one `libproot`) |

Three commits this round:

- `2f47952` — network onboarding screen, PC Apps catalog, games/emulators catalog, always-present
  Phonix Browser desktop shortcut, research roadmap.
- `2acbe22` — `LinuxShellSession.close()` now actually waits for the shell and kills the tracer.
- Earlier: `4e9d2db` — the DeX caption bar is not suppressible; stop testing it.

---

## 2. Proven on hardware

Only list here what was seen on the unit, not what compiled.

Screenshots live in `screenshots/` in the working checkout. That folder is gitignored, so they
are not in the repo — copy any that matter before switching machines.

- **Provisioning** — device owner, `WRITE_SECURE_SETTINGS`, bloat removed, DeX auto-start, keyguard
  off, rotation locked. `provision.sh --serial <ip:port> --apply --apk …`.
- **Zero-click cold boot to the desktop** — verified on the monitor; the phone never shows an app.
- **Display routing** — the app resolves Samsung's `…category.DESKTOP` display and composes there;
  the HDMI display alone is invisible under DeX.
- **Dark window theme**, Privacy Hub Private DNS write path, cooling governor.
- **Network setup screen** — rendered on the monitor with the live link state (`screenshots/wifi-02-setup.png`).
- **Alpine guest** — `uname -a` reports the S10 kernel, `Alpine Linux 3.24.3`, real `root@alpine`.
- **X11 desktop** — `screenshots/linux-02-desktop.png` shows IceWM, the terminal and the live
  `apk add` of `xvfb x11vnc novnc icewm xterm` finishing at `root@alpine:~#`. All are present now:
  `Xvfb`, `x11vnc`, `websockify`, `icewm`, `xterm`, `mpv`.
- **Games catalog** — `screenshots/games-01.png`; every store link was checked when written.
- **PC Apps tab** — `screenshots/pcapps-05.png`, and `mpv` was installed through it for real.
- **Device-owner force-stop immunity** — `am force-stop` on Phonix logs
  `Ignoring request to force stop protected package`. The in-app Reboot/Shutdown controls are the
  only recovery path; they must keep working.

Not executed, by design: decommission/erase. It would wipe the only unit.

---

## 3. Written but never exercised

Each of these is one bench action away from being provable. Do them before touching anything new.

1. **Phonix Browser desktop shortcut** (5 min). Launch Phonix with a monitor attached, close the
   browser window that auto-opens, confirm a *Phonix Browser* icon sits top-left of the desktop, click
   it, confirm the window reopens. Blocked earlier only because the unit was powered off.
2. **Wi-Fi join** (30 min). `NetworkSetupScreen` + `NetworkOnboarding.connectWifi()` write a real
   persistent network through the DPC (`addNetwork`) and then wait for Android to report *validated*
   before claiming success. Only the "no network on first run" prompt has been seen; the save-and-join
   path has not, because joining a new SSID drops wireless ADB — **use a hotspot and stay on USB**.
3. **Container stop** (5 min). `2acbe22` compiled, installed and the guest restarted cleanly, but the
   stop path was never clicked: the Reboot/Shutdown row sits below the fold at phone width.
   From the monitor, hit Shutdown, then confirm `ps -A | grep -E 'libproot'` is empty and a second
   Start does not create two shells.
4. **PC Apps from a cold guest** (20 min). The tab lists real Alpine 3.24 aarch64 packages with real
   compressed sizes, but only `mpv` was installed through it. Install a second entry and confirm the
   window shows the package manager's own output.
5. **Games catalog** (30 min). Entries are links and package-manager checks, nothing is bundled. With
   no Google account, every store link dead-ends. Sign in during pre-config, or add a direct-download
   path (see §5).

---

## 4. What still blocks shipping a unit

### A. Wi-Fi onboarding — code complete, needs the one test above
The screen is reachable from the header whenever the unit is offline, prompts itself once on a
fresh unit with no link, and falls back to Android's Wi-Fi settings when the DPC write is refused.
The alternative, an Ethernet USB-C hub, is already supported by the same code path
(`TRANSPORT_ETHERNET` is reported) and is the zero-typing option for a pre-configured unit.

### B. First Linux desktop on a reset unit needs the internet
The X11 stack is **not in the APK**. On first open, `LinuxDesktop` runs
`apk add xvfb x11vnc novnc icewm xterm font-misc-misc xsetroot`, roughly 450 MB of packages, and
survives only as long as the app's data does. Consequences:

- A customer with no network gets a blank desktop and a failed install.
- A factory reset throws away 559 MB of work.

Options, cheapest first: (1) do the install during pre-config and state in the docs that the first
desktop needs a connection; (2) bundle a second asset tarball with the X11 stack so a reset unit
installs offline (bigger APK, no network dependency). Decide before the clean-unit rehearsal —
option 2 changes the APK, and the rehearsal should test what ships.

### C. Full clean-unit rehearsal
reset → mouse click past DeX Start → `provision.sh` → first run → desktop → Linux → servers →
decommission. Every slice is proven; the sequence is not. This is the only way to know what a
customer sees, and it is destructive — **ask the owner first**, and only after B is decided.

### D. Update path and version stamping
`versionCode` is still `1`, and nothing in the UI shows a version. Needed: bump per release, show
the build in About/support, and implement the self-hosted signed manifest + `PackageInstaller.Session`
design in `docs/ROADMAP.md` §3. A shipped unit that cannot be fixed remotely is a liability.

### E. Server Center catalog
Only claim what answers on this unit: nginx `8080`, OpenSSH `2222`, Syncthing, Mosquitto `1883`,
Uptime Kuma `3001`. Ports below 1024 cannot be bound (`ip_unprivileged_port_start=1024`, measured),
so Pi-hole and any local DNS/DoT server stay out of the product.

### F. DeX home finishing
Wallpaper automation, a working Phonix app shortcut on the DeX desktop, homescreen debloat.

### G. Lockdown decision
Customers can escape into DeX's launcher. Samsung documents DeX as unsupported in lock-task/kiosk
mode, so the honest option is an in-app supervisor PIN plus Phonix as preferred Home. Needs an
owner decision, not more research (`docs/ROADMAP.md` §4).

### H. Thermal soak and the gaming story
24/7 at load, with the fan housing, before any performance claim. Gaming is streaming, Android
titles and emulators — not native Windows games.

### I. Docs
`docs/COMPATIBILITY.md` already corrects the old "does not need DeX-specific APIs" claim; keep it
that way. `docs/LINUX.md` and `docs/PRIVACY.md` carry the honest limits (no root, no ports <1024,
no x86, no GPU).

---

## 5. Known defects and gaps

- **Power controls clip at phone width.** `PhonixDesktopScreen`'s monitor panel puts Reboot/Shutdown
  below the visible area; taps land outside the app. Fine on a monitor, bad exactly when something
  is wrong. Fix: move them into the top bar menu or make the panel scroll.
- **Play is signed out.** The games and emulator catalogs link to the Play Store, so on this unit
  they are dead ends. Either sign in during pre-config (whose account? licensing?) or add F-Droid /
  direct APK links for the open-source titles, which is the better fit for the product pitch.
- **`versionCode 1`** — see D.
- **First desktop needs network** — see B.
- **No eGPU.** The S10 has no Thunderbolt/USB4 controller; its USB-C is USB 3.1 Gen 1 with DP alt
  mode. An eGPU enclosure cannot be attached to this generation. Confirm against Samsung's spec
  before any copy mentions graphics upgrades, and do not repeat the idea in marketing.
- **Caption bar** — ~40 px of DeX chrome stays. Reserve the space; do not promise fullscreen.
- **`am force-stop` cannot recover Phonix** — device-owner protection. Test the in-app controls.

---

## 6. Bench session, copy-paste

```bash
export MSYS_NO_PATHCONV=1
ADB="$LOCALAPPDATA/Temp/android-platform-tools/platform-tools/adb.exe"
"$ADB" devices                       # expect R38M200YNRA  device
"$ADB" shell ps -A | grep -E 'aistudio|libproot'

# build + install in place
export JAVA_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/jdk-21.0.12.1+1"
export ANDROID_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/sdk"
"$LOCALAPPDATA/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle" --offline assembleDebug
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk

# what the monitor actually shows (not what the app thinks)
"$ADB" exec-out screencap -d 2 -p > screenshots/desk.png
```

If Gradle hangs, it is the daemon, not the build: `gradle --stop`, then retry. Two builds earlier
this round timed out on a wedged daemon and ran in 46 s afterwards.

---

## 7. Do not promise

- Fullscreen DeX, or no caption bar.
- Pi-hole, local DNS, or anything else on a port below 1024.
- Root, kernel modules, Docker, or a booted Linux — the guest is a proot userland on Android's kernel.
- x86-64 Windows `.exe` support. The guest is aarch64; there is no emulation layer.
- Mint or postmarketOS as guest options. Mint has no ARM64 image; postmarketOS is a device port and
  this bootloader is locked.
- 7B local models, desktop-class inference speed, or NPU acceleration.
- Native VS Code on Android, or extension parity under musl (see `docs/ROADMAP.md` §2).
- eGPU support.
- Decommission/erase has never been run on hardware. Say so if it comes up.

Every number in customer copy should trace to a measurement in this repo. That rule came from
finding invented benchmark scores and dead distro links in the UI, and it is not negotiable.
