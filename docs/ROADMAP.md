# Research notes for the next round of Phonix

Written 2026-10-29, updated after the 1.1 bench session on the same day. Each section answers a
question the owner asked, records what is **verified**, what is **folklore**, and what should
**not** be promised. Sources are listed at the end. Where a section says “measured”, it was
executed on the SM-G973U; the current work queue lives in [HANDOFF.md](HANDOFF.md) and the
evidence in [VERIFICATION.md](VERIFICATION.md).

For the current work queue — what is proven, what is unverified and what to do next — read
[HANDOFF.md](HANDOFF.md) first, then [VERIFICATION.md](VERIFICATION.md). This page is the research
behind those items.

## Summary

| Question | Verdict | Effort |
|---|---|---|
| Hide DeX's caption bar by making the display "taller" | **No supported path.** Overscan is gone from Android 11+; `wm size` rescales, it does not crop; apps cannot place their own DeX window at negative y. | — |
| VS Code | **Window exists, server does not.** Microsoft ships an Alpine arm64 CLI with `serve-web`; 1.1 never installed it yet reported “Server Running :8000” (measured). The P0 build now probes `127.0.0.1:8000` and stays red without a listener. The CLI experiment below is still unrun. | 1 day + a device test |
| Update shipped units without Play | **Feasible and supported**: device owner + `PackageInstaller.Session`, self-hosted signed manifest. About is **reachable** since the P0 fix, but the configured endpoint still 404s and the version compare is wrong; fix B8 before building the manifest flow. | 2–3 days |
| Stop customers escaping to the launcher | **Not together with DeX.** Samsung documents DeX as unsupported in lock-task/kiosk mode. In-app PIN is the realistic option. | 1–2 days (PIN) |
| Other distros (Mint, postmarketOS) | **No for both as shipped options**; Debian and Ubuntu are the honest additions. | 3–5 days |
| Local AI | **Implemented and measured 2026-10-29**: Qwen2.5 1.5B Q4_K_M through Alpine’s `llama-server`, 8.2–17.2 tok/s on the S10, CPU only. Thermal soak and 3B comparison remain open. | done; soak next |

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

The experiment to run on the unit (still not run; the 1.1 window only sends `code serve-web`
without installing the CLI, so the measured result is a green badge with
`net::ERR_CONNECTION_REFUSED` and no `code` binary):

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

**P0 status (2026-10-29):** the false green badge is gone. The window now probes loopback and shows
“Not listening on :8000” with the reason when the CLI is absent. It still cannot render an editor
until the experiment above is completed, so “VS Code works” remains unproven.

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

**Measured in 1.1 (2026-10-29):** the configured URL
(`api.github.com/repos/JamesRBerggren/PhonixOS/releases/latest`) returns **404**, the real private
repository also returns 404, and the version check is a lexicographic string compare
(`"1.9" > "1.10"` is true). The About screen that contains the update card was also **unreachable**:
its only entry point (`PhonixStartMenu`) is dead code and the live `Windows11StartMenu` had no
About item. **About is now reachable** through the live Start menu (P0, 2026-10-29), so the
remaining blocker is the wrong repository, the 404 and the lexicographic version compare.

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
source build is needed. The CLI package is `llama.cpp`; the HTTP server is a separate
**`llama-server`** package. The implementation uses `llama-server` on `127.0.0.1:8081`. `ollama` is
also present but is the heavier route.

Realistic sizing with roughly 3–4 GB free while Android and DeX are running:

| Qwen2.5 | Q4_K_M file | Fit |
|---|---:|---|
| 0.5B | 0.40 GB | easy |
| 1.5B | 0.99 GB | comfortable — recommended start |
| 3B | 1.93 GB | the sensible ceiling |
| 7B | 4.68 GB | does not fit |

Speed: measured on the bench S10 on 2026-10-29 with Qwen2.5 1.5B Q4_K_M and `-t 4`: **8.2 tok/s**
for a 185-token grounded answer (9.7 tok/s on an earlier 132-token answer), **17.2 tok/s** for a
7-token answer, with prompt processing around 40–45 tok/s. That is better than the pre-measurement
2–4 tok/s estimate, but still CPU-only; do not extrapolate it to 3B or to a hot, throttled unit.

SD card: fine as *storage* for a model that fits in RAM (llama.cpp memory-maps the GGUF), but it
does not make an over-RAM model practical. A 2 GB model at ~50 MB/s is ~40 s to load; sustained
paging would collapse speed.

NPU: the Hexagon 690 is not a drop-in for GGUF inference. TF Lite's Hexagon delegate supports a
constrained set of quantized ops, and nothing here gives llama.cpp the DSP. Assume CPU.

Thermals: sustained inference on a passively cooled phone throttles; cool-start numbers will not
hold. This is exactly the soak test that item H needs.

**Measured in 1.1 (2026-10-29):** the P0 fix first relabelled the old substring table as the
**Offline Knowledge Base**. The same day, the working tree gained the real engine:
`engine/LocalAiEngine.kt` installs Alpine’s `llama-server`, downloads the pinned
`Qwen2.5-1.5B-Instruct-Q4_K_M.gguf` (986,048,768 bytes, SHA-256
`1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`), verifies it, starts the server,
and only reports READY after `/health`. The measured answers and the streaming `null` parser bug are
in [VERIFICATION.md](VERIFICATION.md) §11.

**Quality is not the same as speed.** The water-filtration test produced a fluent answer that
conflated the notes’ one-minute boil with SODIS’s six hours. The curated notes must remain the
safety reference, and the UI says so. A 3B model may improve prose but costs RAM and speed; it has
not been measured here.

**Recommendation:** the 1.5B opt-in card is now the implemented baseline. Next work is a model
manager (choose 0.5B/1.5B/3B), a thermal soak, and a safe-answer policy for water, medical and
electrical topics. Do **not** claim 7B, desktop-class speed, NPU acceleration, or sustained
performance.

---

## 7. Suggested order of work

1. **Stop the shipping UI from lying — done 2026-10-29.** Phone Screen was removed as a mock and
   then restored as a real read-only capture; the AI window was relabelled and then given a real
   model; VS Code is probe-gated (B3); About is in the live Start menu (B13). The device checks are
   in `VERIFICATION.md` §10 and §11. The remaining work starts at window geometry, and the
   ordered queue is in [HANDOFF.md](HANDOFF.md).
2. **Window and grid geometry.** One unit (dp), container-based clamps, a resize grip, and a
   `normalizeLayout` pass across size/display changes; compute the shortcut grid from the
   container so landscape stops clipping (B4, B5). Persist `DesktopCustomization` next (B6).
3. **Prove the Wi-Fi join path** on a spare unit (the remaining unverified shipping step).
4. **VS Code experiment** (one day) → install the Alpine CLI, probe loopback, then show the
   editor; only then call it a Developer card (ROADMAP §2).
5. **Self-hosted update channel** — make About reachable and fix the version compare first; then
   the signed manifest + `PackageInstaller` flow.
6. **Local AI — done 2026-10-29** with `llama-server`, a pinned 1.5B GGUF and a health probe. The
   follow-up is a thermal soak, a 3B comparison, and safe-answer handling for water/medical topics.
7. **Distro picker: Debian + Ubuntu** behind the existing hub, with the disclosure list above.
8. **Supervisor PIN** (in-app) once the owner decides DeX vs kiosk.
9. **Thermal soak** to replace every performance claim with measurements.

## 8. Still to verify on hardware

- DeX on an actual monitor: caption bar, dock/undock, and whether an open window survives the
  display change. The 1.1 session had no external display, so only `wm size`/`wm density`
  metrics were simulated.
- Process-death/layout persistence: blocked on a device-owner unit because `am kill` is ignored;
  needs the in-app reboot or a reset unit (B6).
- `code serve-web` under proot/musl on this guest (download, start, editor renders in WebView).
- Sustained 1.5B inference: temperature, tok/s after 10–30 minutes, and whether the CPU server
  survives a screen-off soak. The short cool-start number is measured; the sustained number is not.
- A 3B model on the same unit, and whether the 1.5B answer quality is acceptable for safety topics.
- MediaProjection capture while Phonix is on a real DeX display, and whether the default display is
  the phone panel in that configuration.
- Debian/Ubuntu rootfs under proot with the Phonix X11 stack.
- The Wi-Fi DPC save-and-join path (needs a hotspot, not the bench network).
- Sustained thermal behaviour under the above loads.

Already measured on 2026-10-29 (see VERIFICATION.md §3): the three new window types, the phone
portrait/landscape layout, the 1080p mdpi layout, the window drag clamp and the off-screen state
after a metric change, the false VS Code badge, the canned AI reply, the Phone Screen mock, the
unreachable About screen, and the 404 update endpoints. No crash or ANR in the package. **Those
are the 1.1 before-state.** The P0 fix then removed/relabelled the false surfaces (§10), and the
same-day follow-up replaced the AI placeholder with a real 1.5B model and restored Phone Screen
as a real read-only MediaProjection view (§11). Do not use the before-state as a current feature
list.

## Bench checklist for the next session

The live version of this list, with the evidence for each item, is in
[docs/VERIFICATION.md](VERIFICATION.md) §3 and §4. Kept here for the record.

> **Superseded 2026-10-31:** the monitor pass in §12 of VERIFICATION ran this checklist's DeX item
> on a real 1920x1080 display, and the unit now runs 1.5 (`versionCode 6`). The state below is the
> dated 1.1 record.

State of the unit at the end of the 2026-10-29 1.1 session:

- **Phonix 1.1 (`versionCode 2`) installed and device owner**, reached by `adb install -r` with
  the recovered debug key. The current build was installed at **16:58:38** on 2026-10-29; it adds
  the real local model and the real read-only Phone Screen (same versionCode, so use install time).
  See `VERIFICATION.md` §10 and §11. The app data and device owner survived; the Alpine guest
  lives in that data and was not started this session. The phone was left on the launcher with
  `1080x2280` at 420 dpi and rotation 0 restored.
- No monitor attached during the session, so DeX was not exercised; the next deck run needs the
  dock and monitor.
- Play Store is **signed out**, so store installs stop at the sign-in screen.
- Wireless debugging is off; USB tethering enumerated as `R38M200YNRA` with no pairing.

Order for the next session:

1. Connect ADB over USB, then `tools/deck-check.sh baseline`; it records the exact metrics and
   restores them on exit.
2. **DeX on the monitor**: run the same feature matrix in a real workspace. Check the caption
   bar, dock/undock, quarter-window resize, and whether a window opened at 1080p is reachable
   after switching to the phone (the measured 1.1 bug).
3. **Browser shortcut check** (5 min): launch Phonix, close the browser window that opens on the
   desktop, confirm a *Phonix Browser* icon is visible, click it, confirm the window reopens.
4. **Wi-Fi join test** (30 min): needs a hotspot, not the bench network, because joining a new
   network drops wireless ADB. Expect to re-pair afterwards, or do it over USB.
5. **VS Code experiment** (1 h): run the `serve-web` commands in section 2, then open
   `http://127.0.0.1:8000` in the Phonix browser. Record whether the editor renders and what the
   CLI reports for version.
6. **Local AI soak** (1 h): the setup and short-run numbers are done (§11). Re-run the same 1.5B
   question after 10 and 30 minutes of continuous generation, record temperature and tok/s, and
   confirm the server still answers `/health`. That is the number to publish; the cool-start
   number is not.
7. **Debian/Ubuntu probe** (1 h): unpack an arm64 rootfs, `apt update`, start Xvfb, confirm the
   Phonix X11 bridge works outside Alpine.
8. **Thermal soak** while all of the above is installed, with the planned fan housing if it has
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
- Alpine packages (verified via APKINDEX): https://pkgs.alpinelinux.org/package/v3.24/community/aarch64/llama-server and https://pkgs.alpinelinux.org/packages?name=llama.cpp&branch=v3.24&arch=aarch64
- Linux Mint ARM position: https://forums.linuxmint.com/viewtopic.php?t=431231
- postmarketOS device/porting requirements: https://wiki.postmarketos.org/wiki/Porting_to_a_new_device
- Qwen2.5 GGUF sizes: https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF
- Mobile llama.cpp decode rates: https://arxiv.org/html/2410.03613v4
