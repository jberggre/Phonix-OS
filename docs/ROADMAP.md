# Research notes for the next round of Phonix

Written 2026-10-29. Each section answers a question the owner asked, records what is
**verified**, what is **folklore**, and what should **not** be promised. Sources are listed at
the end. Nothing here was tested on the unit unless it says so; the phone was offline for this
work.

For the current work queue — what is proven, what is unverified and what to do next — read
[docs/HANDOFF.md](HANDOFF.md) first. This page is the research behind those items.

## Summary

| Question | Verdict | Effort |
|---|---|---|
| Hide DeX's caption bar by making the display "taller" | **No supported path.** Overscan is gone from Android 11+; `wm size` rescales, it does not crop; apps cannot place their own DeX window at negative y. | — |
| VS Code | **Feasible**: Microsoft ships an Alpine arm64 CLI with `serve-web`. Not yet tested in this guest. | 1 day + a device test |
| Update shipped units without Play | **Feasible and supported**: device owner + `PackageInstaller.Session`, self-hosted signed manifest. | 2–3 days |
| Stop customers escaping to the launcher | **Not together with DeX.** Samsung documents DeX as unsupported in lock-task/kiosk mode. In-app PIN is the realistic option. | 1–2 days (PIN) |
| Other distros (Mint, postmarketOS) | **No for both as shipped options**; Debian and Ubuntu are the honest additions. | 3–5 days |
| Local LLM from an SD card | **Yes, small models only**: Qwen2.5 1.5B/3B Q4. `llama.cpp` is packaged in Alpine 3.24. Expect low single-digit tok/s. | 1–2 days |

---

## 1. The DeX caption bar: can we "make the screen taller"?

The idea was to have DeX lay out a taller logical screen so its ~40 px window header is drawn
outside the visible HDMI area. Research says this does not work, for three separate reasons:

- **`wm overscan` no longer exists.** It was removed from AOSP's WindowManager shell command in
  Android 11, so it is not available on Android 12 / One UI 4.
- **`wm size` rescales, it does not crop.** It sets a *forced display size override*; the
  compositor still fits the logical display into the physical raster. Making the logical display
  taller changes scale/layout, not what is visible at the top edge.
- **An app cannot position its own DeX window off-screen.** `ActivityOptions.setLaunchBounds()` is
  a request to the window manager, subject to clamping, and only on freeform-capable devices. It
  is not a decoration-removal mechanism.

`WindowInsets.Type.captionBar()` is **descriptive**: it reports the header's size so content can
be inset. Android's own desktop-windowing guidance says the system caption bar remains even for
immersive apps, which matches what was measured on this One UI 4 unit.

**One UI 8** does add a user-operated *Immersive* mode (maximise, long-press maximise). It has no
public API, so Phonix cannot invoke it; it is also not present on this hardware.

**Recommendation:** stop treating the bar as removable and design the top ~40 px as reserved
space. If a future unit runs One UI 8, document the manual Immersive gesture as a customer tip.

---

## 2. VS Code

The YouTube videos are almost always Termux → proot Ubuntu/Debian → browser editor. That is not
native VS Code, and it is not what we should ship.

What the research found:

- Microsoft publishes a **separate Alpine arm64 CLI build** (`cli-alpine-arm64`, statically
  linked musl) and maps it to a matching VS Code server target. The generic `cli-linux-arm64`
  build requires glibc ≥ 2.28 and will not run on Alpine without a shim.
- `code serve-web` runs the editor as a local web server; the Phonix browser window is a WebView
  with a URL bar, so `http://127.0.0.1:PORT` is exactly the right client.
- **Alpine 3.24 aarch64 does not package `code-server`, `code-oss` or `vscodium`.** Verified
  against the repository index. `nodejs` (24.22) and `npm` (11.12) exist; `gcompat` exists but is
  not a sound way to run glibc VS Code.
- Microsoft's own support matrix is narrower than "any Alpine arm64": extension native binaries
  can still fail on musl. Do not promise extension parity.

The experiment to run on the unit (not yet run — the phone was offline):

```sh
apk add --no-cache ca-certificates curl tar libgcc libstdc++
mkdir -p ~/.local/bin && cd ~/.local/bin
curl -fL 'https://code.visualstudio.com/sha/download?build=stable&os=cli-alpine-arm64' -o vscode-cli.tar.gz
tar -xzf vscode-cli.tar.gz && chmod +x code
./code --version
./code serve-web --host 127.0.0.1 --port 8000
```

Then open `http://127.0.0.1:8000` in the Phonix browser. Keep the connection token; never bind
to `0.0.0.0` on a customer machine.

**Do not promise:** native VS Code for Android, full Marketplace/extension parity, Docker, or
laptop-class compile times. `vscode.dev` in the browser remains the zero-install fallback, without
a local terminal.

---

## 3. Updating shipped units without Play

Supported, and it does not need Knox:

- As **device owner**, install/update with `PackageInstaller.Session`, writing the APK into a
  session and committing it. Android's dedicated-device cookbook documents installs without user
  interaction for fully managed devices.
- Android 12 adds `SessionParams.setRequireUserAction(USER_ACTION_NOT_REQUIRED)`, but it is
  conditional: the installer must declare `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, and the update
  must be eligible (installer is the recorded installer/update owner, or it is updating itself).
  Always handle `STATUS_PENDING_USER_ACTION`.
- `DevicePolicyManager.installExistingPackage()` is **not** an APK installer; `REQUEST_INSTALL_PACKAGES`
  alone does **not** grant silent install. Both are common folklore.
- Android 14's update-ownership rules matter for any future device, not this S10.

**Recommended architecture:** app checks a signed JSON manifest on the owner's server
(`versionCode`, `url`, `sha256`, `minSdk`), downloads to app-private storage, verifies the signing
certificate matches the installed one, then installs via `PackageInstaller.Session` and reports
the result back. Keep the last-known-good APK so a failed update can be rolled back.

---

## 4. Lockdown vs DeX

Samsung's Knox documentation states plainly: **DeX is not supported on devices running in kiosk
mode or lock task mode.** That kills the idea of a locked-down DeX appliance:

- If the unit must stay in Phonix, use real lock task mode
  (`setLockTaskPackages` + `startLockTask`) and accept **no DeX** — the product would become a
  phone-screen app, which is not what we are selling.
- If the unit must use DeX, customers can reach Samsung's launcher. The honest mitigation is an
  **in-app supervisor PIN** on a settings/maintenance surface, plus making Phonix the preferred
  Home activity (`addPersistentPreferredActivity`) so Home returns to us.
- Screen pinning is not a substitute: the user can leave it.
- `setStatusBarDisabled` has no effect in lock task mode; use `setLockTaskFeatures`.
- Do not rely on `setApplicationHidden` to hide One UI Home: make Phonix Home instead, and keep a
  recoverable maintenance path.

**Decision needed from the owner:** DeX or kiosk, not both. Research supports the in-app PIN for
the DeX product.

---

## 5. Other distributions

The guest is a **proot userland on Android's kernel**; it is not a booted Linux. That limits what
"choose a distro" can mean.

| Candidate | Status | Notes |
|---|---|---|
| Alpine | Shipping | musl, 11 MB base, already integrated |
| Debian | Viable next | Official arm64 minbase rootfs, glibc, apt |
| Ubuntu | Viable next | Official arm64 rootfs, glibc, apt |
| Void | Advanced | aarch64 rootfs, glibc or musl |
| Arch Linux ARM | Advanced | Separate project from Arch; label carefully |
| Fedora | Advanced | AArch64 container base tarball |
| Linux Mint | **No** | No official ARM64 image; Mint forum says no current ARM plans. Cinnamon on top of an Ubuntu arm64 base is not "Mint". |
| postmarketOS | **No** | A device port, not a rootfs; needs an unlocked bootloader. SM-G973U is the US Snapdragon model and is not a supported port (the Exynos `beyond1lte` port is a different phone). |

Disclose for every guest: shared Android kernel; **no systemd/OpenRC as init**; no Docker Engine
or kernel modules; service-starting package scripts may need manual handling; musl vs glibc;
X11 is Phonix's own integration, not part of the distro image.

**Recommendation:** ship Alpine (default) + Debian + Ubuntu, each as a separate rootfs with its
own X11 stack. Keep the others behind "experimental" or out.

---

## 6. Local AI

`llama.cpp` **is packaged in Alpine 3.24 community for aarch64** (verified in the index), so no
source build is needed. `ollama` is also present but is the heavier route.

Realistic sizing with roughly 3–4 GB free while Android and DeX are running:

| Qwen2.5 | Q4_K_M file | Fit |
|---|---:|---|
| 0.5B | 0.40 GB | easy |
| 1.5B | 0.99 GB | comfortable — recommended start |
| 3B | 1.93 GB | the sensible ceiling |
| 7B | 4.68 GB | does not fit |

Speed: there is **no verified S10 Snapdragon benchmark**. Comparable Cortex-A76/A77-class ARM
phones decode at roughly 2–4 tokens/sec in published llama.cpp measurements. Plan for low
single digits, not 20–50.

SD card: fine as *storage* for a model that fits in RAM (llama.cpp memory-maps the GGUF), but it
does not make an over-RAM model practical. A 2 GB model at ~50 MB/s is ~40 s to load; sustained
paging would collapse speed.

NPU: the Hexagon 690 is not a drop-in for GGUF inference. TF Lite's Hexagon delegate supports a
constrained set of quantized ops, and nothing here gives llama.cpp the DSP. Assume CPU.

Thermals: sustained inference on a passively cooled phone throttles; cool-start numbers will not
hold. This is exactly the soak test that item H needs.

**Recommendation:** ship a "Local AI (experimental)" card that installs `llama.cpp`, and let the
customer point it at a GGUF on internal storage or SD. Start with 1.5B Q4_K_M. Do **not** claim
7B, desktop-class speed, NPU acceleration, or sustained performance.

---

## 7. Suggested order of work

1. **Prove the Wi-Fi join path** on a spare unit (only unverified step in the shipping blocker).
2. **VS Code experiment** (one day) → if it works, add a proper "Developer" card that installs the
   Alpine CLI, starts `serve-web` on loopback, and opens the browser at it.
3. **Self-hosted update channel** — a shipped unit that cannot be fixed remotely is a liability.
4. **Distro picker: Debian + Ubuntu** behind the existing hub, with the disclosure list above.
5. **Local AI card** with `llama.cpp` and a 1.5B recommendation.
6. **Supervisor PIN** (in-app) once the owner decides DeX vs kiosk.
7. **Thermal soak** to replace every performance claim with measurements.

## 8. Still to verify on hardware

- `code serve-web` under proot/musl on this guest (download, start, editor renders in WebView).
- Whether `llama.cpp` from Alpine runs under proot and what it actually reports for tok/s.
- Debian/Ubuntu rootfs under proot with the Phonix X11 stack.
- The Wi-Fi DPC save-and-join path (needs a hotspot, not the bench network).
- Sustained thermal behaviour under the above loads.

## Bench checklist for the next session

The live version of this list, with the evidence for each item, is in
[docs/HANDOFF.md](HANDOFF.md) §3 and §6. Kept here for the record.

State of the unit when it was powered off on 2026-10-29:

- APK installed and running as device owner; the build includes the network onboarding screen,
  the Games catalog, the PC Apps tab and the always-present Phonix Browser shortcut.
- Alpine guest with the X11 stack installed (`Xvfb`, `x11vnc`, `websockify`, `IceWM`, `xterm`,
  fonts) and `mpv` installed through the PC Apps tab. These live in app storage and survive an
  APK update, but not an uninstall or a data wipe.
- Play Store is **signed out**, so store installs stop at the sign-in screen.
- Wireless debugging is off again after the power cycle, so ADB needs a fresh pair.

Order for tomorrow:

1. Connect ADB. **USB works and is easier**: with the phone on USB tethering it enumerated as
   `R38M200YNRA` with no pairing. Only fall back to
   Developer options → Wireless debugging → *Pair with code* + `adb pair <ip:port> <code>` if the
   cable is not available.
2. **Browser shortcut check** (5 min): launch Phonix, close the browser window that opens on the
   desktop, confirm a *Phonix Browser* icon is visible at the top left, click it, confirm the
   window reopens. This was compiled and installed but never clicked — the phone went offline
   during that step.
3. **Wi-Fi join test** (30 min): needs a hotspot, not the bench network, because joining a new
   network drops wireless ADB. Expect to re-pair afterwards, or do it over USB.
4. **VS Code experiment** (1 h): run the `serve-web` commands in section 2, then open
   `http://127.0.0.1:8000` in the Phonix browser. Record whether the editor renders and what the
   CLI reports for version.
5. **Local AI measurement** (1 h): `apk add llama.cpp`, fetch Qwen2.5-1.5B Q4_K_M, run
   `llama-cli`, record tokens/sec and temperature after 10 minutes. This is the first honest
   number for the local-AI feature.
6. **Debian/Ubuntu probe** (1 h): unpack an arm64 rootfs, `apt update`, start Xvfb, confirm the
   Phonix X11 bridge works outside Alpine.
7. **Thermal soak** while all of the above is installed, with the planned fan housing if it has
   arrived.

## Support notes (verified on hardware)

### 2026-10-29 — "One UI is crashing" was not a crash

Report: the phone would not open, One UI appeared to be crashing.

What the device shows:

- Boot at 08:37:32. `com.android.systemui` has been up since 08:37:40 and One UI Home
  (`com.sec.android.app.launcher`, pid 2623) since 08:37:42, with **no restart** — a crash loop
  would show a new start time and a fresh pid.
- No `am_crash` or `am_anr` events in the events buffer, and no `system_app_crash` in dropbox for
  the day. The only `system_server_wtf` entries are benign: a JobScheduler bucket mismatch for
  Google Wallet, and ConnectivityService's known network-agent `BUG:` line.
- At 08:40:28 Samsung's Galaxy Finder (uid 10059) sent a `MAIN`/`LAUNCHER` intent to
  `com.facebook.katana/.LoginActivity`. Facebook then opened its own
  `SimpleLoginActivity` → `FbExperimentalLoggedOutBloksActivity`, which is the full-screen Meta
  Terms and Privacy Policy page with a single *Continue* button.

That page is what was covering the screen; it is not a One UI failure and it has no obvious exit
besides Home. Force-stopping Facebook and Galaxy Finder and pressing Home holds the launcher for
45 s+ — nothing relaunches either of them.

**Correction to the first reading of this.** Galaxy Finder did not launch Facebook on its own. The
launcher (uid 10142) started `GalaxyFinderActivity` at 08:40:18, and the Facebook intent came from
Galaxy Finder ten seconds later — the signature of someone opening the app search and tapping a
result. The `galaxyfinder:appservice` churn is the system's `package_changed` broadcasts restarting
a bound service, not a crash loop. There is nothing to disable here; the earlier suggestion to
disable Galaxy Finder was wrong.

Two facts worth keeping for support:

- **The device-owner package cannot be force-stopped.** `am force-stop com.aistudio.phonix.vpxmkr`
  reports success, but ActivityManager logs `Ignoring request to force stop protected package`.
  Recovery is the in-app Reboot/Shutdown control or a real reboot, so those must keep working.
- **The guest shell at boot is the runtime, not a leak.** `LinuxServerService` starts it on
  `BOOT_COMPLETED` and holds a partial wake lock so servers survive. It measured 3.2 MB RSS plus a
  60 KB shell. The real cost is the wake lock, which is deliberate for a 24/7 appliance.

Fixed in this change: `LinuxShellSession.close()` wrote `exit` and then called `Process.destroy()`,
which only sends SIGTERM and returned at once without waiting. It now waits up to two seconds for
the shell to exit and force-kills the tracer if it does not, so a stop cannot leave a guest running
behind a UI that says stopped.

If it recurs, capture evidence before rebooting — logs do not survive a power cycle:

```bash
adb logcat -b events -d | grep -aE 'am_crash|am_anr' | tail -20
adb shell dumpsys dropbox | grep 2026- | grep -ai crash
```

## Sources

- AOSP WindowManager shell command (`wm size` / overscan): https://android.googlesource.com/platform/frameworks/base/+/android11-release/services/core/java/com/android/server/wm/WindowManagerShellCommand.java
- Android immersive mode and caption bar: https://developer.android.com/develop/ui/views/layout/immersive
- Android desktop windowing (header bar): https://android-developers.googleblog.com/2024/09/developer-preview-desktop-windowing-on-android-tablets.html
- One UI 8 DeX Immersive (user gesture only): https://www.androidauthority.com/samsung-dex-one-ui-8-full-screen-3582164/
- Knox: DeX and Knox / kiosk: https://docs.samsungknox.com/dev/knox-sdk/features/mdm-providers/device-management/samsung-dex-and-knox/
- Android lock task mode: https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- Android dedicated-device cookbook (PackageInstaller): https://developer.android.com/work/dpc/dedicated-devices/cookbook
- `setRequireUserAction`: https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams#setRequireUserAction(int)
- VS Code CLI Alpine arm64: https://learn.arm.com/install-guides/vscode-remote/ and https://code.visualstudio.com/docs/remote/linux
- proot-distro: https://github.com/termux/proot-distro
- Alpine packages (verified via APKINDEX): https://pkgs.alpinelinux.org/packages?name=llama.cpp&branch=v3.24&arch=aarch64
- Linux Mint ARM position: https://forums.linuxmint.com/viewtopic.php?t=431231
- postmarketOS device/porting requirements: https://wiki.postmarketos.org/wiki/Porting_to_a_new_device
- Qwen2.5 GGUF sizes: https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF
- Mobile llama.cpp decode rates: https://arxiv.org/html/2410.03613v4
