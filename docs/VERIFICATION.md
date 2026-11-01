# Deck and pocket verification — 1.1 on the unit

Written 2026-10-29, first as a static review of `c42622a`, then updated after a bench session
with the phone attached over USB. **1.1 was built and installed during that session**, so the
new features in this document were executed on the SM-G973U, not just read.

Each finding is marked **[measured]** (seen on the unit) or **[static]** (code reading only).
Screenshots and UI dumps for the measured items are in `artifacts/deck/` in the working
checkout; that folder is gitignored. The APK is
`app/build/outputs/apk/debug/app-debug.apk`; the replaced 1.0 APK is preserved at
`artifacts/deck/installed-1.0.apk`.

**P0 follow-up, 2026-10-29:** the four honesty/reachability fixes in §7 items 1–4 were implemented
and verified in a second install of the same `1.1 (versionCode 2)` debug build. Phone Screen was
removed, the AI window was relabelled **Offline Knowledge Base**, VS Code stayed red until a real
loopback probe succeeded, and **About Phonix OS** entered the live Start menu. §10 is that record.

**§11 supersedes the removal/relabel decisions:** on the same day, the working tree gained a real
local Qwen2.5 1.5B llama.cpp model and restored Phone Screen as a real **read-only
MediaProjection** view. The APK was installed at **16:58:38**. The old “removed / relabelled” text
below is kept as history; it is not the current feature state. Input injection remains
unimplemented, and Android’s capture consent remains mandatory.

**Unit, confirmed by `getprop`/`dumpsys`:** SM-G973U (S10), Android 12 (API 31), One UI 4.1
(`40100`), `1080x2280` override on a `1440x3040` panel at 420 dpi, rotation locked portrait,
device owner `com.example.receiver.PhonixDeviceAdminReceiver`. **No external display was
attached in this session, so DeX itself was not exercised**; the 1080p workspace was simulated
with `wm size`/`wm density` and is labelled as such below.

## New chat quick start

Read [HANDOFF.md](HANDOFF.md) first for the queue; this page holds the evidence behind it.
[ROADMAP.md](ROADMAP.md) holds the research behind the open decisions,
[PROVISIONING.md](PROVISIONING.md) the build/signing record, and [LINUX.md](LINUX.md) the guest.

- **Unit:** serial `R38M200YNRA`, SM-G973U, Android 12 / One UI 4.1, **Phonix 1.5
  (`versionCode 6`) installed as device owner on 2026-10-31**, 1080x2280 at 420 dpi, rotation 0.
  The install was `adb install -r` over the device-owner build; it reported `Success` and left the
  device owner intact with a clean crash buffer (`tools/verify-shipping.sh`, exit 0). The 1.1
  records below describe the 2026-10-29 session and are dated history, not current state. That
  session left the model server and mirror stopped.
- **adb:** `.tools/platform-tools/adb.exe` in this worktree (`tools/deck-check.sh` finds it).
  USB tethering enumerated without pairing; wireless debugging resets on reboot.
- **Build:** JDK 21 + Gradle 9.3.1 + SDK 36.1.0 are under
  `%LOCALAPPDATA%/Temp/phonix-toolchain`. The offline build that produced 1.1 took 7m17s:

  ```bash
  export JAVA_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/jdk-21.0.12.1+1"
  export ANDROID_HOME="$LOCALAPPDATA/Temp/phonix-toolchain/sdk"
  "$LOCALAPPDATA/Temp/phonix-toolchain/gradle-9.3.1/bin/gradle" --offline --no-daemon assembleDebug
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

- **Signing:** the app uses `debugConfig` → `rootDir/debug.keystore`, which is gitignored and
  **not in this repository**. On this bench machine a matching copy exists in a prior Freebuff
  worktree under `~/.config/freebuff-desktop/local-projects/*/threads/*/debug.keystore` (SHA-256
  `7d2b6c3d85f5519e5628a922bbebf1bb3981dd2400a7037fe83938b1cec0b5da`). Without that key, an
  APK cannot update the device-owner install and the package cannot be uninstalled. Copy it to
  the worktree root before building, and back it up; see §9.
- **Harness:** `tools/deck-check.sh baseline|matrix|crashes|death|soak`. It restores the exact
  recorded display metrics; note that on this S10 `wm density reset` sets **560 dpi**, not the
  420 dpi base.
- **Current status:** the four P0 items are done and device-verified (§10). The same day’s
  follow-up (§11) replaced the two stopgaps with real features: **Local AI** (Qwen2.5 1.5B through
  `llama-server`, health-gated, 8.2–17.2 tok/s) and **Phone Screen** (real read-only
  MediaProjection capture). VS Code is still probe-gated, About is live. The remaining blockers are
  window units/normalisation (B4), the landscape grid (B5), input injection, and the updater (B8);
  §7 has the order.

---

## 0. Result at a glance

The table records the measured 1.1 build. The Phone Screen, AI, VS Code and updater rows describe
that build *before* the P0 follow-up; §10 records what changed.

| | |
|---|---|
| Build | `gradle --offline assembleDebug`, JDK 21 + Gradle 9.3.1 + SDK 36.1.0, 7m17s, 39 tasks |
| Signing | Same debug certificate as the installed 1.0 (SHA-256 `7d2b6c3d…b5da`), recovered from a prior worktree, so `adb install -r` succeeded and **device owner survived** |
| Version on unit | `versionName 1.1`, `versionCode 2` |
| Crashes/ANRs | None; `logcat -b crash` empty for the package after the whole session |
| New modules | Cyberdeck AI works but is a keyword table labelled as a model; VS Code shows a false “running” badge; Phone Screen is a painted mock whose taps cannot be delivered |
| Layout | Phone portrait usable but labels truncate; phone landscape clips the third row under the taskbar; simulated 1920×1080 mdpi renders all icons |
| Windows | Drag is clamped at x=850 on a 1920px workspace; after changing display metrics, an open window stays off-screen with its close button unreachable |
| Updater | Endpoint returns 404 for both the wrong and the real repository; **the About/Software Updates screen is unreachable from the live UI** because its only entry point is dead code |

---

## 1. What changed in 1.1

`c42622a feat: update desktop layout and expand features` is the only code commit since the
previous handoff (`2ea4af2`, documentation only). 17 files, +1636/−667.

| Area | Change | What the bench found |
|---|---|---|
| Version | `versionCode 1→2`, `versionName 1.0→1.1` | Installed; About fallback still hardcodes “1.1 (build 2)”. |
| Desktop layout | Default shortcuts moved to the bottom-left quadrant, positions recomputed at draw time | Portrait now clears the widget column; landscape still loses the third row (B5). |
| New window types | `PHONE_MIRROR`, `VS_CODE`, `AI_ASSISTANT` added to `WindowType`, taskbar, app picker and content dispatch | All three open; Phone Screen and VS Code are not what their labels say (B1, B3). |
| AI assistant | `model/AiChatMessage.kt` (canned text) + `CyberdeckAiWindowContent` | Replies are substring lookups; header claims Qwen2.5/llama.cpp (B2). |
| VS Code | `VsCodeWindowContent`, `startVsCodeServer()` | Badge says running while the WebView reports `ERR_CONNECTION_REFUSED` (B3). |
| Phone mirror | `PhoneMirrorWindowContent`, `sendPhoneTouch/Key` | Painted mock, fake 12:00/LTE, input never reaches the OS (B1). |
| Updater | `model/AppUpdateInfo.kt`, `checkAppUpdates()`, update card | Not reachable from the live menu (B13); endpoint 404s (B8). |
| OOBE | `CYBERDECK_DEV` profile, `PreferredBrowser`, generated shortcuts | Not re-run: the unit is already past OOBE and clearing a device-owner install is destructive. |
| Games | `GameEntry.directUrl` / `fdroidUrl` surfaced as install URLs | Not exercised; catalogue still links out, installs nothing. |
| Docs | **`docs/HANDOFF.md` deleted** in 1.1 | Replaced by a new handoff in the follow-up commit; links now point at [HANDOFF.md](HANDOFF.md). The 1.1 file remains readable at `git show c42622a^:docs/HANDOFF.md`. |

No `targetSdk` bump happened in this commit. `targetSdk` is 36, but the unit runs API 31, so the
API 36/37 large-screen restrictions do not apply to this phone. They matter for the next unit.

---

## 2. Bugs

Severities are for the product, not for the diff. “Both” means pocket and deck. Line numbers and
class names are for the measured 1.1 build (`c42622a`) before the P0 follow-up; code removed by
the fix is named for the historical record.

### B1 — The Phone Screen window is a drawing, and its input cannot work (High, both) **[measured]**

**Status 2026-10-29: superseded by §11.** The painted mock and its dead input path were removed in
the P0 round (§10); §11 restores `PHONE_MIRROR` as a real read-only MediaProjection capture. There
is still no touch injection. The paragraph below records the removal that existed between the two
builds.

- `PhonixDesktopScreen.kt:2437` (`PhoneMirrorWindowContent`) paints a phone with a literal
  `"12:00"`, `"LTE"`, `"100%"` and the caption `"Active Display 0 Surface"` (`:2520`, `:2526`).
  No `MediaProjection`, `screencap`, `ImageReader` or capture path exists in the app.
- Measured: the window opened with **12:00** while the real status bar showed **1:58 PM**. Tapping
  the fake panel updated only a local label, `Tap: 1034,1093`; no phone UI moved.
- `PhonixViewModel.kt:2006` (`sendPhoneTouch`) and `:2018` (`sendPhoneKey`) shell out to
  `input -d 0 …` as the app UID, with every exception swallowed.
- Measured as the app UID (`run-as com.aistudio.phonix.vpxmkr …`):
  - `input -d 0 tap …` printed the `input` usage text and injected nothing.
  - `input keyevent KEYCODE_HOME` returned success but the resumed activity did not change.
  - Control as the shell UID: the same HOME key immediately returned to the launcher. The
    method works; the caller's UID does not.
- Target coordinates are hardcoded to `1080×2280` (`:2006-2012`), this panel's override size.
- **Fix:** either cut the window, or build it honestly: `MediaProjection` consent (plus a
  `mediaProjection` foreground-service type at API 34+) for the picture and an
  `AccessibilityService.dispatchGesture` for input. A window labelled “rescue” that cannot
  deliver a tap is worse than no window. Also note that on the phone panel the app itself is
  the topmost window at the tapped coordinates, so injected taps would re-enter Phonix unless
  it is running on the other display.

### B2 — “Cyberdeck AI” is a keyword table wearing a model's name (High, both) **[measured]**

**Status 2026-10-29: superseded by §11.** The relabel was the P0 stopgap; §11 adds a real
Qwen2.5 1.5B model behind llama-server, with a health probe and measured tok/s. The curated notes
remain the fallback and the safety reference.

- `model/AiChatMessage.kt` is `DefaultAiKnowledge.matchResponse()`: substring matching that
  returns fixed strings. The window header says `"Qwen2.5 / Cyberdeck AI"` and
  `"Dual Mode: Built-in Knowledge + Alpine llama.cpp"` (`PhonixDesktopScreen.kt:2713-2714`),
  and the welcome message says the assistant “runs 100% offline” on the board.
- Measured: tapping **Water Filtration** returned a fixed `[OFFLINE SURVIVAL: WATER
  PURIFICATION]` answer (boiling, bio-sand layers, SODIS). The send button works; `sendAiMessage`
  only sleeps 350 ms (`PhonixViewModel.kt:2115`). No GGUF, no `llama-cli`, no token counter.
- **Fix:** relabel now (“Offline Knowledge Base — rule-based”), keep the chips and the
  suggested-command buttons (they are the useful part), and only add `llama.cpp` behind a real
  probe: `apk info -e llama.cpp`, start `llama-cli` with a GGUF path, stream tokens, show the
  model file and measured tok/s. ROADMAP §6 has the sizing.

### B3 — “Server Running :8000” is asserted, never checked (High, both) **[measured]**

**Status 2026-10-29: fixed by probe.** `startVsCodeServer()` now waits up to 12 s for a TCP
connection to `127.0.0.1:8000` and leaves the badge red with a reason when nothing answers. It
never sets `vsCodeServerRunning` from the command alone, and the WebView reloads only after a
successful probe. Measured on the unit with the container running: “Waiting for :8000...”, then
“Not listening on :8000” and “No listener ... the VS Code CLI is missing or failed.” See §10.

- `startVsCodeServer()` (`PhonixViewModel.kt:2162-2164`) sets `vsCodeServerRunning = true`
  before sending `code serve-web …`, and never reads the command's output.
- Measured: after tapping **Start Server**, the window showed **“Server Running :8000”** next
  to a WebView displaying **`net::ERR_CONNECTION_REFUSED`** for `http://127.0.0.1:8000/`.
  `curl` from the device returned `rc=7` (connection refused) and `pidof libproot` was empty;
  Alpine 3.24's base does not contain `code` (ROADMAP §2 records the CLI as untried).
- **Fix:** add an install step with visible output, probe loopback with a short connect timeout
  before flipping the badge, add Stop, and keep `--without-connection-token` only because the
  listener is loopback (say so in the caption). `network_security_config.xml` already permits
  cleartext to `127.0.0.1`, so the WebView is not the blocker.

### B4 — Windows cannot be resized, movement clamps at 850×750 px, and positions are not normalized when the display changes (High, deck) **[measured]**

- `DesktopWindowFrame` (`PhonixDesktopScreen.kt:1110-1145`) has drag and three buttons, no
  resize grip. `PhonixViewModel.resizeWindow()` exists at `:728` and **is never called**.
- `moveWindow()` (`:713-726`) clamps `posX` to `850f` and `posY` to `750f` in px. Measured on a
  simulated 1920×1080 mdpi workspace: dragging the window from x≈300 to x=1800 stopped it at
  **x≈850**, leaving the right third of the workspace unreachable.
- Measured state bug: after switching the workspace from 1920×1080 back to the phone's
  1080×2280, the window stayed at x≈850. Its close/minimise buttons were off-screen and could
  not be tapped; it had to be reached by switching the workspace back to 1920 mdpi. Nothing
  re-clamps window positions on a configuration change.
- Units are mixed: positions are px (`IntOffset(window.posX.toInt())`), sizes are dp
  (`.size(width = window.width.dp)`), defaults are bare numbers (`24f`, `360f`, `640f`). At
  420 dpi a “640 wide” window is 1680 px, wider than the panel.
- **Fix:** one unit (store dp, convert once at the layout), clamp against the measured container
  from `BoxWithConstraints`, add a 24 dp corner grip wired to `resizeWindow`, and run a pure
  `normalizeLayout(state, container)` on every size-class/display change (same function needed
  for dock/undock, §5). Accept: quarter-window resize at 1920×1080 and 411×869 dp, and a window
  opened at either size is fully reachable after switching to the other.

### B5 — The icon grid is sized for a tall phone; landscape clips a row (Medium-High, pocket) **[measured]**

- `PhonixDesktopScreen.kt:262-270`: `bottomQuadrantBaseY = screenHeight − 64.dp − 360.dp`,
  coerced to at least `140`, rows at `+88.dp`. On the 411 dp-tall landscape phone the workspace
  is ~347 dp, the base clamps to 140, and the third row sits at ~404 dp.
- Measured: portrait 1080×2280 shows all four rows, but labels truncate (`Phonix Term…`,
  `Photos & Me…`, `System Mon…`). Landscape 2280×1080 clips the third row behind the taskbar;
  only two rows are fully reachable.
- The earlier 1.0 overlap between the widget column and the third icon column is gone in 1.1:
  icons now start below the widgets. Do not “fix” that part again.
- Simulated 1920×1080 at 160 dpi (deck metrics) shows all 16 icons and the centered taskbar.
- **Fix:** compute columns and rows from the measured container and `IconSizeMode`
  (`floor((width − margins) / (icon + gap))`), page or scroll the overflow, and drop the widget
  column below the icons when the window is narrower than ~840 dp. Accept: every default
  shortcut visible and tappable at 411×869, 869×411 and 1920×1080.

### B6 — The deck layout is not persisted, so a reboot or dock cycle loses it (Medium, both) **[static]**

- `SharedPreferences` is written only for `oobe_completed`, `network_onboarded` and the
  wallpaper (`.kt:278, 310, 323, 1617, 1792`). `DesktopCustomization` — wallpaper, accent, icon
  size, taskbar, shortcut positions, widget visibility — and the window list exist only in
  `_uiState`. DataStore is still commented out at `app/build.gradle.kts:108`.
- Not measured this session: the package is device owner, `am kill` is ignored and shell
  `kill -9` is denied, so the process cannot be reaped without the in-app reboot or a reset.
  The code path is unambiguous: nothing writes those fields.
- This is also a prerequisite for “saved per-deck layouts” (§6).
- **Fix:** serialize `DesktopCustomization` (Moshi is already present) keyed by size class;
  restore in `init` before first composition; debounce writes. Keep `windows` session-scoped
  first, then restore the last layout per size class with the registry. Accept: arrange →
  reboot → identical layout; dock → undock → identical layout.

### B7 — Immersive mode is unconditional, but it does not take effect (Medium, pocket) **[measured, corrected]**

- `MainActivity.kt:71` calls `setImmersiveDesktopMode(true)` on every launch; the function
  (`:246`) sets `setDecorFitsSystemWindows(false)` and hides status + navigation bars.
- Measured on the phone: **the system status bar is still visible** (battery, Wi-Fi, clock) on
  both 1.0 and 1.1. The call runs in `onCreate` before `setContent`, and the hide is not
  re-applied after the window attaches, so the policy never lands. The predicted "no bars"
  symptom was wrong; the intent is still unconditional and still wrong for the pocket.
- **Fix:** apply the insets controller after content/attach, and derive the policy from the
  window: hide bars only on a non-default display, or when the window is ≥840 dp with a pointer;
  show them in pocket mode. Then verify both modes.

### B8 — The updater checks the wrong repository, compares versions as strings, and its screen is unreachable (Medium, both) **[measured + static]**

- `checkAppUpdates()` (`PhonixViewModel.kt:2030-2088`) calls
  `https://api.github.com/repos/JamesRBerggren/PhonixOS/releases/latest`. The remote is
  `jberggre/Phonix-OS`, and it is **private**. Measured from the device: both URLs return
  **HTTP 404** unauthenticated.
- `tagName.replace("v","") > "1.1"` is lexicographic (`:2047`): `"1.9" > "1.10"` is true, which
  is wrong; `"1.2" > "1.1"` passes by luck.
- The failure path sets `UpdateCheckState.UP_TO_DATE` with a note, so the card can claim the
  unit is current after a 404.
- `downloadAndInstallUpdate()` (`:2091-2100`) opens a browser, no signature check, contrary to
  ROADMAP §3.
- See B13: the screen that contains this card is not reachable.
- **Fix:** signed manifest on the owner's host (ROADMAP §3), integer `versionCode` from
  `PackageManager`, explicit `OFFLINE`/`UNKNOWN` state, `PackageInstaller` only after the signer
  matches, and an entry point that actually exists.

### B9 — Failures are swallowed in the newest paths (Low-Medium, both) **[static]**

`sendPhoneTouch`, `sendPhoneKey` and `downloadAndInstallUpdate` ended in
`catch (_: Exception) {}` (`.kt:2013`, `:2022`, `:2091-2100`), and `startVsCodeServer` had no
error path at all (B3). The first two were deleted with the mock; VS Code now reports a specific
probe failure (§10). `downloadAndInstallUpdate` still swallows errors, and the updater remains
unfixed (B8). The app already has a terminal that shows real command output; use it.

### B10 — Documentation links broke in 1.1 (Low, repo) **[fixed]**

`docs/HANDOFF.md` was deleted in 1.1 while `ROADMAP.md` still linked to it twice. The follow-up
commit restores a handoff and points the links at it; the 1.1 file remains readable at
`git show c42622a^:docs/HANDOFF.md`.

### B11 — Keep these as they are (no action)

- `AndroidManifest.xml:69-70` already has `resizeableActivity="true"`, no `screenOrientation`,
  no `min/maxAspectRatio`, and `configChanges` covering size/density/uiMode. That is the correct
  side of the API 36/37 rules.
- Every desktop window is a Compose box inside one activity, so Samsung's five-app DeX cap
  never applies. That is a real architectural advantage; keep it and make it deliberate (§6).
- `DesktopAutoLaunch` resolving Samsung's `…category.DESKTOP` display is discovery, not a
  behaviour branch. It stays.

### B12 — “Display mode” is cosmetic, not a display fact (Low, both) **[static]**

`MonitorDisplayMode` (`PHONE_PORTRAIT` / `FHD 16:9` / `ULTRAWIDE 21:9`) only changes the
right-hand widget column's max width (`PhonixDesktopScreen.kt:200-204`). It does not change the
layout, and on the phone it can claim “External Monitor (16:9)”. Derive it from the window or
rename it so it stops reading as a display fact.

### B13 — About / Software Updates is unreachable (High for the update feature) **[measured + static]**

**Status 2026-10-29: fixed for reachability.** **About Phonix OS** is now an item in the live
`Windows11StartMenu` and opens the About window; the Software Updates card is reachable by
scrolling it. The endpoint and version-compare bugs in B8 are unchanged and still block a real
update check.

- `WindowType.ABOUT` is dispatched at `PhonixDesktopScreen.kt:1258` and offered only by
  `PhonixStartMenu` (`:1837`), which **is never called** (one definition, zero call sites).
- The live start menu is `Windows11StartMenu` (`:379`); it contains **no About entry** (search
  for “About” in that file returns nothing). Measured: opening the live menu showed App Store,
  All apps, Pinned and the app grid, with no About.
- Consequence: the entire About screen, including the Software Updates card, cannot be opened
  in 1.1. The new update feature is dead UI regardless of the endpoint bug.
- **Fix:** add About to the live menu (and/or the OS Card), and delete the dead `PhonixStartMenu`
  or wire it up. This should land before any endpoint work, or the fix cannot be reached.

### Measured corrections to the earlier review

- B7's predicted symptom (“no status bar in pocket”) was wrong; the bar is visible because the
  hide does not take effect. The policy is still unconditional.
- B5's overlap claim came from 1.0; 1.1's portrait layout no longer overlaps the widget column.
  The landscape clipping and the label truncation are real.
- B8 is worse than “wrong URL”: the screen that shows it is unreachable (B13).
- New measured state bug: window positions are not re-clamped after a display-size change (B4).

---

## 3. Feature → verification matrix

Run this on the unit. IDs are stable; `tools/deck-check.sh` automates the device-level parts.
“Result” is what the 2026-10-29 session actually saw.

| ID | Feature | Steps | Pass criterion | Result 2026-10-29 |
|---|---|---|---|---|
| F1 | Window types open, focus, minimise, maximise, close | Open Browser, Terminal, VS Code, Offline Knowledge, About; use the three title-bar buttons; drag by the header | Window fully inside workspace; controls reachable; no crash | **Pass at open**; close became unreachable after a metric change (B4) |
| F2 | VS Code server | VS Code → Start Server; wait 30 s | Badge green only if `127.0.0.1:8000` answers; editor renders; Stop works | **Fail** — green badge with `ERR_CONNECTION_REFUSED` and `curl` rc=7 (B3) |
| F3 | Phone Screen | Open, consent, view live frames, Stop, close | Actual display frames; LIVE/OFF honest; Stop ends projection; input is explicitly absent | **Pass (read-only)** in §11 — first frame 540×1140, foreground service 4711, Stop returns OFF. No tap test because injection is not implemented. |
| F4 | Local AI | Open, Set up/Start, ask, wait for stream, Stop | Model installed/verified; `/health` READY; streamed text; measured tok/s; notes fallback | **Pass** in §11 — water answers 9.7 then 8.2 tok/s, 2+2 answer 17.2 tok/s; `null` prefix bug found and fixed. |
| F5 | Game install URLs | App Store → Games → install an open-source title | Store page opens; “Installed” only from `pm` | **Not run** this session |
| F6 | Cyberdeck OOBE profile + browser choice | Reset/first run; choose the profile; open a web shortcut | Expected shortcuts; chosen browser opens | **Not run** — unit already past OOBE; reset is destructive |
| F7 | Update check | About → Check for Updates (with network and offline) | Correct repo/version; offline says offline; install is signed | **Fail** in 1.1 — About unreachable (B13); configured URL 404 measured (B8). **About is reachable after P0; check still fails on B8.** |
| F8 | Default shortcut layout | Fresh desktop; portrait and landscape | Every icon visible above the taskbar | **Fail in landscape** — third row clipped; portrait labels truncate (B5) |
| F9 | State across process death / dock cycle | Arrange → kill → relaunch; dock → undock | Layout identical; no window lost | **Not run** — device-owner package cannot be killed; code has no persistence (B6) |
| F10 | Immersive policy | Compare bars on phone and DeX | Pocket: bars visible. Deck: hidden with caption bar reserved | **Fail on phone** — status bar visible despite the hide call; DeX not run (B7) |
| F11 | Linux desktop + container stop | Open/close; stop the container | Session survives closing; stop really stops the guest | **Not run** this session |
| F12 | Network onboarding | Render on DeX; join a hotspot; wrong password | Real SSID state; failure reported | **Not run** this session |
| F13 | Wallpaper / device owner | Launch, relaunch | Wallpaper applied once; admin intact | **Pass** — admin intact after the 1.1 install |
| F14 | Window normalisation on size change | Open a window at 1920×1080, switch to 1080×2280 | Window stays fully reachable | **Fail** — stayed at x≈850, close off-screen (B4) |

F2, F3, F4 and the reachability half of F7 were re-run after the P0 fixes; §10 has the post-fix
results. The table above keeps the 1.1 before-state for comparison.

---

## 4. Bench harness

`tools/deck-check.sh` automates the device-level parts: baseline capture, the pocket/deck metric
matrix with a guaranteed restore, crash-buffer scan, process-death check and an optional soak
recorder. It refuses to run without `adb` and prints the install command instead. On this machine
it now falls back to the local `.tools/platform-tools` copy, and it disables Git Bash path
conversion so `adb shell cat /proc/uptime` works.

```bash
tools/deck-check.sh baseline
tools/deck-check.sh matrix
tools/deck-check.sh crashes
tools/deck-check.sh death
tools/deck-check.sh soak --hours 24
tools/deck-check.sh reset
```

Two corrections came out of the bench run and are now in the script:

- **Samsung density:** `wm density reset` on this S10 jumps to **560 dpi** (the panel's density),
  not the 420 dpi base. The script snapshots the physical density and restores it with
  `wm density 420`; the pocket profiles restore the recorded metrics instead of calling `reset`.
  The same quirk bit this session: after `wm density reset` the UI briefly rendered at 560 dpi
  and had to be corrected by hand.
- **Device-owner processes cannot be reaped:** `am kill` is ignored and shell `kill -9` is denied.
  The `death` command now says so instead of implying a failed test.

Two things the script deliberately does **not** do:

- **No `monkey`.** This app's own buttons can run debloat commands and reboot the unit; random
  tapping is destructive here, not a crash hunt. Run it by hand only on a unit you will reset.
- **No claim that `wm size` is DeX.** `wm size 1920x1080` + `wm density 160` reproduces the
  *metrics* Phonix sees on a 1080p workspace at mdpi; it does not create a second display, enter
  freeform, or exercise DeX's caption bar or task switcher. Real deck acceptance still needs the
  dock and monitor.

Always record before you change, and always restore: the script snapshots `wm size`,
`wm density`, `display_density_forced` and rotation, and restores them from an `EXIT` trap,
including on Ctrl-C.

---

## 5. Usage-mode parity: one capability layer, two policies

The goal is that the pocket and the deck run the same code and differ only in policy. No
`Build.MANUFACTURER` checks, no “is this a Samsung” branch.

**Signals** (all from the current window/activity, recomputed on configuration change):

| Signal | Source | Used for |
|---|---|---|
| Width class | window dp width: `< 600` compact, `600–839` medium, `≥ 840` expanded | Pane count, icon grid, widget column |
| Height class | window dp height `< 480` short | Icon rows, whether widgets can sit beside icons |
| Pointer | `InputManager.inputDeviceIds` → any `SOURCE_MOUSE`/`SOURCE_TOUCHPAD` | Hover, right-click, drag precision |
| Keyboard | any `SOURCE_KEYBOARD` | Accelerators, command palette default-open |
| External display | `activity.display.displayId != Display.DEFAULT_DISPLAY` | Immersive policy, caption-bar inset |
| Caption bar | `WindowInsets.Type.captionBar()` | Reserve top strip on the deck |
| Desk hint | `uiMode == UI_MODE_TYPE_DESK` | Weak corroboration only, never the sole trigger |

**Policies.**

| | Pocket (compact, touch) | Deck (medium/expanded, pointer) |
|---|---|---|
| Shell | Single pane + taskbar switcher; one window visible at a time | Tiled panes, 2–4 visible |
| Windows | Open maximised; “minimise” returns to the launcher; no free drag | Free drag, quarter snapping, resize grips |
| Context menu | Long-press (already implemented in `DraggableDesktopShortcut`) | Right-click (already implemented) + Shift+F10 |
| Target size | ≥ 48 dp, 8 dp gaps | 32–40 dp chrome is acceptable |
| Hover | Never required; every hover affordance has a tap path | Hover states, tooltips |
| System bars | Visible | Hidden, caption bar reserved |
| Keyboard | Soft keyboard, `adjustResize` | Ctrl+K palette, Alt+Tab cycle, Ctrl+W close |

**Dock/undock and size changes.** Keep window models and the active module in the ViewModel
(they already survive configuration changes because the manifest declares the configs). On a
size-class change, run one pure `normalizeLayout(state, container)` that clamps positions/sizes
into the new bounds and collapses panes in compact; do not rebuild state from defaults. The
measured B4 failure — a window left at x≈850 with its close button off-screen after switching
from 1920 to 1080 — is exactly what this function must prevent. Input focus belongs in the model,
not a composable-local `remember`.

**Why not `screenOrientation` or resize flags:** already correct — see B11. The failure here is
layout policy and the missing normalisation pass, not manifest policy.

---

## 6. Cyberdeck parity: make the module surface deliberate

The app is already a modular shell: `WindowType` is the module list and the Compose boxes are the
tiling. What is missing is the declaration, the entry points and the persistence.

1. **Module registry.** Replace the separate `when (type)` blocks (`DesktopWindowFrame`,
   `Windows11Taskbar`, `DesktopAppPickerDialog`) with one list:

   ```kotlin
   data class DeckModule(
       val id: String, val title: String, val icon: ImageVector,
       val factory: @Composable (PhonixUiState, PhonixViewModel) -> Unit,
       val minWidthDp: Int = 320, val needs: Set<Need> = emptySet() // POINTER, KEYBOARD, GUEST, NETWORK
   )
   ```

   A module then appears in the picker, taskbar and desktop without touching three files. B13 is
   the cautionary tale: `About` exists as a module but its only menu is dead code, so the feature
   is invisible. Add a registry test that every module is reachable from at least one live entry
   point.

2. **Named deck layouts.** A layout is `(name, sizeClass, list of (moduleId, tile), pins)`. One
   definition must produce the same arrangement at 1920×1080@160 and, collapsed, at 411×869 dp.
   Ship three: *Field* (terminal + vitals), *Dev* (editor + terminal + browser), *Server* (guest
   console + services + vitals). Persist via B6.

3. **In-app tiling, on purpose.** Samsung caps DeX at five simultaneous apps and minimises the
   oldest. That is a reason to keep the deck inside one window, and to say so in the product
   copy. Add quarter snapping and a keyboard resize so four panes are usable on 1080p without OS
   window management.

4. **Keyboard-first.** Ctrl+K command palette over the existing action layer (`openWindow`,
   `executeTerminalCommand`, connect Wi-Fi, start/stop guest). Every palette entry is one of those
   calls; nothing new is needed underneath.

5. **Files.** Use `ACTION_OPEN_DOCUMENT_TREE` (SAF) for customer storage. Do not add
   `MANAGE_EXTERNAL_STORAGE`: it is Play-restricted and this app is sideloaded, so it would buy
   nothing and cost the option of a store listing later.

6. **Peripherals and thermals.** Surface `UsbManager` device-attach events in a Devices pane (the
   hub's HID already works through DeX). Use `DeviceTelemetry`'s thermal zones to degrade
   deliberately: drop background refresh first, show a throttling badge, never lie about clocks.

---

## 7. Fix order

P0 is “the feature is not what the label says”; P1 is “the deck breaks”; P2 is polish. Each item
names the matrix row that closes it.

| # | Fix | Why first | Closes |
|---|---|---|---|
| 1 | Phone Screen: real MediaProjection picture; accessibility input only if wanted later | The most obviously false surface; read-only capture is now done (§11), input is not | F3 |
| 2 | Real local model behind a health probe and measured tok/s | Replaces the keyword table (§11) | F4 |
| 3 | Probe VS Code before the badge; install step + Stop + output | One click produces a lie today | F2 |
| 4 | Add About to the live menu (done); delete the dead `PhonixStartMenu` in the registry pass | The update feature was unreachable; fixing the endpoint first would be invisible | F7, B13 |
| 5 | Unify window units, clamp to container, add resize grip, normalise on size change | Blocks every deck acceptance test; measured off-screen window | F1, F9, F14 |
| 6 | Persist `DesktopCustomization` + per-size-class layout | Without it a deck cannot be set up once | F9 |
| 7 | Apply immersive after attach and derive it from display/pointer/size | Pocket bars currently contradict the code's intent | F10 |
| 8 | Compute the icon grid from the container | Landscape phone loses a row | F8 |
| 9 | Updater: signed manifest, integer version, explicit offline | A unit that cannot be fixed remotely is a liability | F7 |
| 10 | Surface command failures in the terminal | Turns silent failures into readable support tickets | F2, F3 |
| 11 | Module registry + three deck layouts | The “modular cyberdeck” claim | §6 |

Items 1–3 and the live About entry in item 4 landed on 2026-10-29 and were verified on the unit
(§10). Item 1’s picture and item 2’s model were then replaced by real implementations in §11. The
dead `PhonixStartMenu` is still present and should be deleted in the registry pass. The remaining
order is unchanged, starting with window geometry (item 5).

**Do not fix:** the DeX caption bar (no API — measured), kiosk + DeX (Samsung documents they do
not combine), `input`-based injection without accessibility, or the manifest's resize/orientation
settings (already correct).

---

## 8. Open questions for the owner

1. **Phone Screen: build it or cut it?** Resolved by building the picture on 2026-10-29 (§11):
   real MediaProjection consent and a read-only window. Input still needs an AccessibilityService
   and a separate opt-in; the painted mock must not return.
2. **Local AI: relabel only, or commit to the llama.cpp path?** Resolved on 2026-10-29: real
   Qwen2.5 1.5B Q4_K_M through `llama-server`, measured at 8.2–17.2 tok/s. The remaining question
   is model quality and thermal soak, not whether the path exists.
3. **DeX or kiosk?** ROADMAP §4 recommends the in-app supervisor PIN and keeps DeX. The decision
   changes nothing in this document, but it changes what “sealed appliance” means.
4. **OOBE and update path next?** OOBE was not exercised on this unit. About is reachable now, but
   the updater still needs B8's correct repository, integer version compare and signed manifest
   before a check can succeed.

---

## 9. Bench session record — 2026-10-29

Commands were run from this worktree with `adb` from `.tools/platform-tools`, a device owner
S10 on USB tethering, and the phone unlocked.

**Build and install**

- Toolchain: `%LOCALAPPDATA%/Temp/phonix-toolchain` — JDK 21.0.12.1+1, Gradle 9.3.1, SDK 36.1.0.
- `gradle --offline --no-daemon assembleDebug`: **BUILD SUCCESSFUL in 7m 17s**, 39 tasks, APK
  30,944,622 bytes.
- The installed 1.0 was signed by `CN=Android Debug` with SHA-256
  `7d2b6c3d85f5519e5628a922bbebf1bb3981dd2400a7037fe83938b1cec0b5da`. The matching
  `debug.keystore` was recovered from a prior Freebuff worktree; the new APK was checked with
  `apksigner` and matches.
- `adb install -r app-debug.apk`: **Success**. Package now `versionCode 2`, `versionName 1.1`;
  device-owner admin unchanged.

**Measured**

- AI window: canned Water Filtration answer; header `Qwen2.5 / Cyberdeck AI`; send button works.
- VS Code: green `Server Running :8000`; WebView `net::ERR_CONNECTION_REFUSED`; `curl` rc=7;
  no `libproot` running.
- Phone Screen: fake 12:00/LTE/100%; real status bar 1:58 PM; tap label `Tap: 1034,1093`;
  app-UID HOME ignored, shell-UID HOME works.
- Layout: portrait 1080×2280 all rows present with truncated labels; landscape 2280×1080 third
  row clipped by the taskbar; simulated 1920×1080 mdpi all 16 icons visible.
- Window: drag stopped at x≈850 on a 1920px workspace; after switching to 1080×2280 it stayed
  there and its close button was off-screen; recovered by switching back to 1920 mdpi.
- Live start menu has no About entry; `PhonixStartMenu` has zero call sites; both GitHub URLs
  return 404 from the device.
- `logcat -b crash` empty for the package; no ANR.

**Restored**

- `wm size 1080x2280` (the original override), `wm density 420` (Samsung's `wm density reset`
  had set 560), `user_rotation 0`, `accelerometer_rotation 0`. Final `wm size`/`density` match
  the session baseline; device-owner admin intact.

**Not run, and why**

- DeX on a monitor, dock/undock, caption-bar behaviour: no external display attached.
- OOBE Cyberdeck profile and preferred-browser choice: the unit is past OOBE; resetting the only
  device-owner install is destructive.
- Process-death persistence: `am kill` is ignored and shell `kill -9` is denied for the device
  owner; B6 is therefore static.
- 24 h soak, monkey, container stop, Wi-Fi join, games install URL, browser preference.
- No release build or signature rotation; debug only.

**Changes made in this session:** all six files in `docs/` — this file (measured review and
quick start), `ROADMAP.md` (verdicts and work order), `PROVISIONING.md` (toolchain, signing,
install and Samsung traps), and current-status notes in `LINUX.md`, `PRIVACY.md` and
`COMPATIBILITY.md`; plus `tools/deck-check.sh` (Samsung density restore, local adb fallback,
Git Bash path conversion, device-owner death note) and `.gitignore` (`.tools/`, `artifacts/`).
The tracked AI Studio plan under `.aistudio/` now carries a historical-plan banner so it is not
mistaken for current behaviour. The round is committed and pushed to `main`; see
[HANDOFF.md](HANDOFF.md) for the queue that starts after it.

---

## 10. P0 follow-up — implemented and verified 2026-10-29

This is the post-review fix round. The APK was built from the working tree that is now committed,
so it still reports `1.1 (versionCode 2)`; identify it by the 15:39:37 install time, not by the
version string.

### What changed

- **Phone Screen removed.** `PHONE_MIRROR` is gone from `WindowType`; the default shortcut, taskbar
  mapping, content dispatch and `PhoneMirrorWindowContent` are gone; `sendPhoneTouch`/`sendPhoneKey`
  and `phoneMirrorLastTap` are gone. The OOBE rescue walkthrough was removed too, so the wizard is
  now six steps (0–5) and deploys on the last one. The old default grid slot now opens **About
  Phonix OS**.
- **AI relabelled.** Window and shortcut titles now say **Offline Knowledge** / **Offline Knowledge
  Base**. The header says “Rule-based notes - no language model is loaded”; the offline badge says
  “LOCAL NOTES”; the llama.cpp action says “Install llama.cpp”. Welcome and fallback copy no longer
  claim Qwen2.5 or a running model.
- **VS Code is probe-gated.** `startVsCodeServer()` keeps `vsCodeServerRunning = false` until a
  `Socket.connect(127.0.0.1:8000)` succeeds (350 ms per attempt, 12 s overall), checks for an
  already-listening server first, and reports a specific failure when the container is stopped or
  no listener appears. The WebView reloads only after success. Header states: “Not listening on
  :8000”, “Waiting for :8000...”, “Listening on 127.0.0.1:8000”.
- **About is live.** **About Phonix OS** was added to `Windows11StartMenu` with the version/update
  card entry point. The dead `PhonixStartMenu` was left in place this round; it still has zero call
  sites and should be deleted when the module registry lands.

### Checks on the unit

| Check | Result |
|---|---|
| Build | `:app:compileDebugKotlin` and `assembleDebug` succeeded; final APK 31,658,001 bytes |
| Install | `adb install -r` succeeded; `versionCode 2`; device owner unchanged |
| Phone Screen removed | Desktop has no Phone Screen shortcut or label; About now occupies its grid slot. No `Phone Screen` text in the live Start menu or desktop dump |
| About entry | Start menu → search “about” → **About Phonix OS** → window opens. Scrolling inside the window shows the **Software Updates** card (“Manual Only”, repository row) |
| AI labels | Window shows **Offline Knowledge Base** / “Rule-based notes - no language model is loaded” / “LOCAL NOTES”; no Qwen2.5, “Cyberdeck AI”, “Dual Mode” or “100% OFFLINE” text |
| VS Code initial | Badge **Not listening on :8000**; status “Server not started” |
| VS Code start | Tap **Start & Probe** → **Waiting for :8000...**; after 12 s → **Not listening on :8000** and “No listener ... CLI is missing or failed.” No green badge |
| Crashes | `tools/deck-check.sh crashes`: no crash/ANR evidence for the package |
| Final state | Desktop, Start menu closed, keyboard hidden; `1080x2280` override, 420 dpi, rotation 0/0; device owner intact |

One test swipe intended to scroll the About window landed on the desktop and moved the Linux
System shortcut. It was restored with **Personalize → Reset Shortcuts to Default Grid**; the final
dump matches the pre-test default positions. This is a reminder that the About window is only
~340 dp tall, so its content must be scrolled inside the window, not below it.

### Evidence (gitignored `artifacts/deck/`)

- `p0-desktop.xml` / `p0-final.xml`: desktop without Phone Screen, About shortcut in place.
- `p0-start-about.xml`: filtered Start menu with the live About item.
- `p0-about-open.xml`, `p0-about-updates.xml`: About window and the Software Updates card.
- `p0-ai.xml`: new knowledge-base labels and welcome copy.
- `p0-vscode.xml`, `p0-vscode-probing.xml`, `p0-vscode-failed.xml`: red badge, waiting state and
  measured failure.

### Not verified

- The **success** path of the VS Code probe (a real CLI listening on 8000), Stop, and editor
  rendering. The base Alpine image has no `code` binary; ROADMAP §2's CLI experiment is still the
  prerequisite.
- A real update check. About is reachable, but B8's wrong URL/404 and lexicographic compare remain.
- DeX on a monitor, dock/undock and window geometry; no monitor was attached.
- The renumbered six-step OOBE after removing the rescue walkthrough. The unit is past OOBE and a
  reset is destructive; compile and UI inspection are the only checks so far.

---

## 11. Local AI and real Phone Screen — implemented and measured 2026-10-29

This round replaced the two P0 stopgaps with working features. The APK is still
`1.1 (versionCode 2)`; identify it by the **16:58:38** install time. It is 31,685,535 bytes
(SHA-256 `a7516b6cef12cbbd80378d510ff031c2a04c9a3b8248c0f5e3bddba5c74a7674`) and was built from
the working tree that is now committed and pushed on `main`.

### Why Phone Screen came back

The P0 removal was correct: the old window painted a fake phone. The owner needs a real view
because the bench S10’s panel is shattered and entombed inside the mini-PC shell. The window is
now a real capture, not a mock, and it is deliberately **read-only**. It does not inject touches;
that would require a separate AccessibilityService opt-in and is not implemented.

### Local AI: what is actually running

- **Engine:** `engine/LocalAiEngine.kt`; process state in `PhonixViewModel`; UI in
  `OfflineKnowledgeWindowContent`.
- **Guest runtime:** Alpine community package **`llama-server`** (not `llama-cli`), launched as
  `llama-server -m qwen2.5-1.5b-instruct-q4_k_m.gguf --host 127.0.0.1 --port 8081 -c 4096 -t 4`.
- **Model:** Qwen2.5 1.5B Instruct Q4_K_M from
  `bartowski/Qwen2.5-1.5B-Instruct-GGUF`; **986,048,768 bytes**; SHA-256
  `1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`.
- **Storage:** guest path `/root/models/qwen2.5-1.5b-instruct-q4_k_m.gguf`, app-private under
  `files/linux/alpine/root/models/`. The model is not in the APK.
- **Ready gate:** `/health` must return 200. A launch command alone never sets READY.
- **Stop:** kills the pid file; `/health` becomes unreachable and the badge returns to STOPPED.
- **Fallback:** if the model is stopped, erroring or still setting up, the existing curated notes
  answer with a visible “answer comes from the built-in notes” prefix.

**Measured setup:** tapping **Set up local AI** installed `llama-server`, downloaded the model
(391 of 940 MB after 25 s), verified the SHA-256, and reached `{"status":"ok"}` from
`curl http://127.0.0.1:8081/health`. The on-device `sha256sum` matched the pinned hash.

**Measured inference (S10, CPU, no cloud):**

| Prompt | Prompt tokens / decode | Result |
|---|---|---|
| “water filtration” (first answer) | 421 prompt, 132 decoded | **9.7 tok/s**, prompt eval 42.18 tok/s, total 23.8 s |
| “what is two plus two” | 169 prompt, 7 decoded | **17.2 tok/s**, answer “Two plus two is four.” |
| “water filtration” (after safety prompt) | 484 prompt, 185 decoded | **8.2 tok/s**, prompt eval 40.08 tok/s, total 34.5 s; final answer recommended a one-minute boil |

The first streamed answer exposed a parser bug: the role delta’s `"content": null` was converted
by `JSONObject.optString` into the literal word `null`, so the answer began “nullFor water…”. The
parser now treats `content: null` as empty. The next answer was clean, with no `null` prefix.

The first water answer also conflated the notes’ one-minute boil with SODIS’s six hours. The system
prompt now requires safety-critical topics to copy the supplied notes exactly and never combine
steps. Re-running the same question produced a one-minute boil and no six-hour instruction. The
model still invented an irrelevant sentence about a “safe, readable zone”, so it remains a
fallback to the curated notes, not their replacement.

### Phone Screen: what is actually running

- `service/ScreenMirror.kt`: a `mediaProjection` foreground service plus process-wide frame state.
- The UI uses `MediaProjectionManager.createScreenCaptureIntent()`; Android’s system consent dialog
  is mandatory. The measured dialog was “Start recording or casting with Phonix OS?”.
- Capture is **540×1140** at 300 ms intervals, drawn through the real `ImageReader` frame; no fake
  clock, carrier, battery or touch label exists.
- Measured: foreground notification id **4711**, `first captured frame 540x1140`, UI **LIVE**;
  Stop drove the UI back to **OFF** and `dumpsys media_projection` returned `null`.
- Closing the window also stops the service; a capture is never left running invisibly.

**Hard limits, stated in the UI:** read-only; no tap/type injection; consent per session and lost on
reboot; the default display is what gets captured. On the phone’s own panel the window necessarily
contains itself. On DeX it should show the phone display, but that was not tested because no
external monitor was attached.

### Verification and device state

- `:app:assembleDebug` succeeded; APK 31,685,535 bytes; `adb install -r` at **16:58:38** succeeded.
- `:app:testDebugUnitTest` **passed** (4m27s online; final rerun 2m06s offline). The first
  `--offline` attempt failed only because the test artifacts were not cached; that was not a code
  failure.
- No package crash or ANR records after the session (`logcat -b crash` empty for the package).
- Final metrics restored: `1080x2280` override on `1440x3040`, physical density 420, rotation 0/0.
  Device owner unchanged. The model server was stopped and the mirror was OFF at the end.
- Not run: real DeX/monitor capture, touch injection, 24 h thermal soak, process-death recovery,
  reboot re-consent, and a second model size. The 1.5B model’s first water answer conflated boiling
  time with SODIS, so the UI must keep saying the curated notes are the safety reference.

### High-density host chrome (2026-10-30)

- **Expanded top bar:** at `1920x1080`/640 dpi the old header put the identity block and actions
  on one `SpaceBetween` row; the actions, including the only collapse button, were laid out past
  the right edge. The header now pins collapse, ellipsizes the subtitle, and scrolls the action
  row. Measured `Hide` bounds: `[1772,301][1844,373]`; Debloat/STOPPED at y=483..589.
- **Collapsed notch:** the pill was 270 dp wide and covered most of a full-height window's title
  bar. Below 600 dp it is now a ~90 dp `Phonix` pill with a compact network button, and it expands
  on tap/click rather than on pointer entry. A title drag from `(1500,280)` to `(1100,280)` moved
  the Terminal window and left the bar collapsed.
- **Taskbar:** below 600 dp the 120 dp brand block is hidden and the app strip scrolls. At
  `1080x2280`/420 dpi the first frame showed three app entries; a swipe from x=550 to x=120 at
  y=2039 exposed Terminal and Settings. The clock still wraps its AM/PM suffix at this width.
- `assembleDebug` + `testDebugUnitTest` green, **38 tests, 0 failures**; APK 29,682,604 bytes,
  installed over the device-owner build. Final device state: `1080x2280` override on `1440x3040`,
  physical density 420, user rotation 0, accelerometer on; guest stopped; crash buffer empty for
  the package; device owner still
  `com.aistudio.phonix.vpxmkr/com.example.receiver.PhonixDeviceAdminReceiver`.
- Remaining: the 640 dpi Start menu still clips its pinned row and all-apps list. This is host
  chrome, not the desktop window geometry.

### Network debugging as the initial setup channel

Network ADB is useful for provisioning, not for bypassing consent:

```bash
adb pair <phone-ip>:<pair-port>       # from Android’s Wireless debugging screen
adb connect <phone-ip>:<debug-port>
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Accept the system capture dialog from a PC when the panel is unusable:
adb shell uiautomator dump /sdcard/ui.xml
adb shell input tap <x> <y>            # “Start now” in the consent dialog
```

A pre-downloaded GGUF can be pushed into the debug install with `run-as`; the app’s own download is
faster on the bench network and verifies the hash itself. ADB cannot remove the MediaProjection
consent requirement, and it cannot make the appliance self-provisioning.

---

## 12. Multi-distro made honest — 2026-10-31 (source verification)

This is the **source** record for the 1.3 candidate. It was produced in a cloud Linux VM
(JDK 21, Gradle 9.3.1, Android SDK 36, 2 vCPU) with **no adb and no USB path to the bench unit**,
so nothing here is a device measurement. The 1.1/1.2 sections above remain the last measured
hardware record.

### What was wrong in `9df837b`

- `LinuxContainer.switchDistro` fabricated a distribution. For any non-bundled entry it created
  `bin/`, `etc/`, `usr/bin/`… and copied **Alpine's** `bin/` into it, then wrote an `etc/os-release`
  reading `NAME="Debian 12 (Bookworm)"`. The marker file claimed the distro was installed. The
  guest would have run Alpine busybox under a Debian label.
- It deleted the previous rootfs **before** installing, and set `active_distro_id` **before** the
  install succeeded. A cancel, a full disk or a bad download left the user with a broken guest.
- `DistroCatalog` pointed at `…/default/rootfs.tar.xz`, which does not exist. Measured from the VM:

  | URL | Result |
  |---|---|
  | `…/debian/bookworm/arm64/default/rootfs.tar.xz` | **404** |
  | `…/debian/bookworm/arm64/default/` | 200 (listing of dated builds) |
  | `…/<dated>/rootfs.tar.xz` + `SHA256SUMS` | 200 |

  The real images are date-stamped directories with a published `SHA256SUMS` beside
  `rootfs.tar.xz`, which is exactly what the new installer reads.
- Knowledge-tier copy promised 24/65/130 documents at “~800 KB / 2.8 MB / 7.5 MB”. `KnowledgeLibrary`
  contains **25** documents; no library byte size is measured anywhere.

### Measured image facts (from the VM, 2026-10-31)

| Distro | Newest arm64 build | `rootfs.tar.xz` | SHA-256 (first 12) |
|---|---|---|---|
| Debian 12 bookworm | `20261029_05:24` | 94,998,816 B (95 MB) | `18f38dd5bab2` |
| Ubuntu 24.04 noble | `20261018_13:35` | 127,665,700 B (128 MB) | `e87ce7681d6a` |
| Arch Linux current | `20261028_04:18` | 178,268,808 B (178 MB) | `b1cbf58d2636` |
| Kali current | `20261029_17:14` | 77,652,060 B (78 MB) | `f3bdbcfdd336` |

The catalogue now uses these measured download sizes; installed sizes stay marked `≈`.

### What the code does now

- `engine/DistroInstaller.kt`: resolve newest dated build → check free space → stream download with
  byte progress → require the published SHA-256 → extract `rootfs.tar.xz` into `<id>.partial` →
  write `resolv.conf`/`hosts` → rename into place. Any failure aborts and deletes only the staging
  tree. No checksum entry means **no install**, not an unverified install.
- `LinuxContainer.switchDistro` is `suspend`, activates the preference only after a complete rootfs
  exists, and prunes other trees only after success. Alpine (bundled in the APK) is never pruned.
- `LinuxRuntime.switchDistro` returns success/failure and restarts the previously active distro when
  a switch fails.
- `DistroHubScreen` surfaces the failure verbatim and states that nothing was deleted.
- `KnowledgeLibrary.countFor(tier)` replaces the invented sizes; both the OOBE and the assistant
  window print real per-tier counts.

### Checks run

- `:app:assembleDebug` — **BUILD SUCCESSFUL** (JDK 21 / Gradle 9.3.1 / SDK 36, cloud VM).
- `:app:testDebugUnitTest` — **46 tests, 0 failures**, including the new `DistroInstallerTest`
  (newest-build selection ignores listing order, colon encoding, checksum parsing and rejection,
  every streamable distro has an https index, Alpine stays the fallback, tier counts are monotonic
  and cover the whole library).
- APK produced at `app/build/outputs/apk/debug/app-debug.apk` from a **throwaway** debug keystore
  generated in the VM. It is a build artefact only: it cannot update the device-owner install.

### Not verified, and why it matters

- **No install and no launch on the S10.** No device is reachable from this VM.
- **No downloaded distro has ever booted.** The archive is verified and extracted, but proot booting
  `bash`/`apt` from a Debian/Ubuntu/Arch/Kali tree is untested. Extraction is the documented
  `TarExtractor` ustar reader plus an XZ stream; both are new to this path.
- **Disk and time cost on the phone** are unknown. The smallest image is 95 MB and needs roughly
  twice that free to download and unpack; `requireFreeSpace` enforces a margin but the estimate is
  unmeasured on the unit.
- **Debloat walkthrough** is unchanged from the previous round except in wiring; hiding a package
  as device owner was already verified in the 1.2 round, uninstalling through the native dialog was
  not.
- The updater remains **honest but unusable**: no feed → `ERROR` with “no public update feed is
  configured”. There is still no signed manifest (B8), so a deployed unit cannot be updated.


### Device pass — 2026-10-31, S10 with a real HDMI monitor

The cloud VM could not reach the unit, but the session later ran on the owner's laptop on the same
LAN, over wireless debugging (paired with a code, then connected by mDNS as
`adb-R38M200YNRA-CQKrqB._adb-tls-connect._tcp`). That changed "unverified" into measured:

- **1.3 is installed on the unit.** `adb install -r` reported **Success** over the device-owner
  install; `dumpsys package` reads `versionCode=4 versionName=1.3`. The APK was signed with the
  recovered `debug.keystore`, whose certificate SHA-256 is
  `7D:2B:6C:3D:85:F5:51:9E:56:28:A9:22:BB:EB:F1:BB:39:81:DD:24:00:A7:03:7F:E8:39:38:B1:CE:C0:B5:DA`
  — the documented device-owner identity. (The hash in PROVISIONING/HANDOFF is the **certificate**
  fingerprint, not the keystore file hash; every local copy of the file hashes to `7670a511…` and
  is nevertheless the right key.)
- **Device owner survived:** `dpm list-owners` → `User 0: admin=…PhonixDeviceAdminReceiver,
  DeviceOwner,Affiliated`.
- **Crash buffer clean:** no `com.aistudio.phonix.vpxmkr` entry in `logcat -b crash` after launch.
- **Real DeX, finally measured.** The unit has an **ES-G34C5 HDMI monitor at 1920x1080** attached
  (`type EXTERNAL, FLAG_PRESENTATION`, display id 7) plus Samsung's virtual `Desktop` display
  (id 2, 160 dpi). Phonix is resumed on display 2 and its desktop renders there: wallpaper, top bar
  (`Phonix Mini PC`, `AC`), `+ OS Card` / `+ Vitals`, taskbar with File Explorer, Terminal, Phonix
  Browser and Settings, and the `Phonix OS` badge. Captures are in `artifacts/deck/` (gitignored).
- **A real Debian userspace boots under proot on the S10.** The running process's command line is
  `/data/app/…/lib/arm64/libproot.so --link2symlink -0 -r /data/user/0/com.aistudio.phonix.vpxmkr/files/linux/debian … /bin/sh`,
  and that tree carries a genuine `PRETTY_NAME="Debian GNU/Linux 12 (bookworm)"`, `apt`, and a
  `var/lib/dpkg/status` database. So the deck is running a Debian guest, not Alpine under a Debian
  label — the failure mode the 9df837b code would have produced.
- **Correction to this section's earlier claim:** the `debian` tree on the unit is genuine. Its
  `.phonix-installed` marker uses the old `debian-12.7-aarch64` format, but the content is a real
  Debian rootfs, so the tree predates (or bypassed) the fabricated code path. It was **not**
  produced by this round's installer.
- **The Alpine rescue image is currently absent from the unit**, because the earlier switch pruned
  it. The first Alpine launch will re-extract it from the APK. The new `pruneOtherDistros` never
  deletes `alpine`.
- `tools/verify-shipping.sh` was corrected during this pass: API 31 prints `DeviceOwner` with no
  space, so the original owner check reported a false negative. It also now prints each rootfs
  tree's `PRETTY_NAME`.

Still owed: an install **initiated by 1.3's own downloader** (the SHA-256 gate, progress UI and
failure path have not run end to end on the phone), the debloat hide walkthrough, and the
Start-menu view modes.


### Merge with the faulted session — 2026-10-31

A second session (`worktrees/926fecd3`) had solved this same problem **better and on hardware**, but
its work was uncommitted and died with the session. It was recovered and adopted:

- **Resolution** uses the mirror's authoritative `meta/1.0/index-system` (`model/DistroIndex.kt`)
  instead of scraping the HTML directory listing. One request, and it picks
  `arm64/default/<newest date>` without hardcoding an archive URL that rots.
- **Unpacking happens inside the running guest** with busybox `unxz | tar`, so no XZ dependency is
  needed in the APK. A rootfs is promoted to `files/linux/<id>` only if it contains a shell and its
  own `/etc/os-release`.
- **It had already run on the S10**: Debian 12 downloaded at exactly **94,998,816 bytes**, unpacked,
  promoted, marked installed, and `apt --version` answered from Debian's own binary. That is the
  source of the genuine `debian` tree measured in the device pass above.
- **Three faults it found on hardware are in the tree:** the blocking installer ran on the main
  thread (`NetworkOnMainThreadException`) and now runs on `Dispatchers.IO`; Debian ships
  `/etc/resolv.conf` as a symlink into `/run`, so the file is unlinked before being written; and
  `refreshStorage` measured a hardcoded `linux/alpine`, so it now measures whichever distro is
  active without following symlinks.

Added on top of the recovered work: a **SHA-256 gate**. The downloader now reads the `SHA256SUMS`
published beside each `rootfs.tar.xz` and refuses to install on a missing entry or a mismatch, with
`DistroIndex.checksumFor` / `sha256Hex` covered by a unit test. The earlier `org.tukaani:xz`
dependency and my HTML index scraper were removed, since the adopted path needs neither.

Verification of the merged tree: `assembleDebug` + `testDebugUnitTest` **green, 45 tests, 0
failures**; APK 28,918,475 bytes. Still owed on hardware: a run of the **checksum-gated** path
(a real install prunes the working Debian tree, so it needs the owner's go-ahead), the debloat
hide walkthrough, and the Start-menu view modes.


### Plan audit follow-up — 2026-10-31

The implementation plan was checked line by line against the tree. What was missing, and how it was
closed:

- **Icon tiles were structurally dead.** `showIconFrames` defaulted to `false`, and
  `DraggableDesktopShortcut` skipped the tile whenever designed artwork existed, so "Icon Tiles: On"
  changed nothing on the default desktop. Frames now default on, a glass tile with a 0.30-alpha
  white border is drawn behind artwork too (artwork inset to 88%), and the preference key moved to
  `showIconFramesV2` so a `false` saved under the old default does not stick forever.
- **Alignment grid contrast** now draws a dark halo under each dot (was a single 0.65-alpha dot).
- **Debloat walkthrough** gained the row app icon, a `StorageStatsManager` data+cache figure (null
  without Usage Access, and then simply not shown), and a per-row **Hide** button that only appears
  on a device-owner install. The banner previously promised 1-click hiding the dialog could not do.
- **Start menu** now has the plan's seven categories instead of five, and the storage view has a
  **Debloat** action, not only App Info.
- **Installer** resumes: the `.part` file is kept and fetched with `Range`/`206`, verified by
  checksum before promotion.
- **Offline library** grew from 24 to **54 documents** (Medium 32, Large 54, measured by
  `KnowledgeLibrary.docCount`). This is still below the plan's 65 / 130, and the UI reports the real
  measured counts rather than the plan's targets.

Verification: `assembleDebug` + `testDebugUnitTest` **green, 45 tests**, APK 29,950,886 bytes. The
frame default is asserted in `ExampleRobolectricTest`; the visual result is **not** screenshotted on
the device yet.


### Device pass on the HDMI monitor — 2026-10-31

Installed over the previous build (`adb install -r`, signature matched, versionCode still 4) and
captured display 2 at 1920x1080 with `screencap`. Measured on screen:

- **Icon tiles render.** File Explorer, Terminal, Phonix Browser and Settings all draw the rounded
  tile with its translucent border, with the artwork inset inside it. This is the first build where
  the default desktop shows frames at all; the border is deliberately subtle, so on the dark
  wallpaper the tile reads as a faint outline rather than a strong card. **Judgement call, not a
  defect**: the fill is 0.07 alpha and the border 0.30.
- **The Linux card named the wrong guest.** It read `Alpine Linux 12.15` while the active rootfs was
  Debian (`rootfs trees: debian tmp`). `DesktopQuickDistroWidget` hardcoded the name and fell back to
  the Alpine catalog version; it now reads `uiState.currentDistro` and appends the version measured
  from the guest, rendering `Debian 12 (Bookworm) · 12.15`. [measured, second capture]
- The same hardcoding was in the terminal welcome banner and the shell agent's system prompt
  (`apk add` offered to a Debian guest); both now use the active distro's name and package manager.

Remaining, honestly: the terminal banner and agent prompt fixes are compiled but **not yet exercised**
against a running guest, and the tiles have not been checked on a light wallpaper.


### Debian desktop on the monitor — 2026-10-31 [measured]

The first attempt to boot the guest in a window never left "Starting X11 on :1…". Two separate
causes, both real:

1. **`LinuxDesktop.kt` ran Alpine's installer inside Debian.** The bootstrap ended its package
   step with `apk add --no-cache xvfb x11vnc novnc icewm xterm ...`, which fails with
   "apk: not found" in a Debian rootfs, so Xvfb never started. Its "already running" checks also
   used `pidof`, which a minimal rootfs need not provide.
2. **The window loaded the viewer page once and never again.** `webView.loadUrl` ran 300 ms after
   the window opened, while a first run is still installing packages, so the load failed with
   `net::ERR_CONNECTION_REFUSED`; a page that never loaded cannot report itself connected, and the
   splash stayed up forever. The periodic "repair" re-sent the start script but never the page.

Fixes: package probe and install now follow the active distro (`apk` / `dpkg` / `pacman`), process
detection reads `/proc/*/comm` instead of `pidof`, and the window reloads the page every 8 s until a
framebuffer is connected, logging WebView failures to the `PhonixVnc` tag.

Measured after the fix, on the S10 with the 1920x1080 HDMI monitor:

- `apt-get` observed running inside the Debian guest (the distro-aware path is exercised, not just
  compiled), then 6080 and 5901 listening, then established connections on both.
- logcat: `PhonixVnc: [phonix] connected 1920x922`.
- The capture shows the IceWM desktop inside the "Linux Desktop (X11)" window: wallpaper, the
  taskbar with four workspace buttons, and an xterm running `sh` at a `#` prompt.

Not claimed: the in-app terminal's `root@debian` prompt was not separately photographed, and the
desktop has not been exercised beyond opening a shell.


### Local AI after the switch to Debian — 2026-10-31 [measured]

Local AI was not deleted. `LocalAiEngine.kt` (524 lines, committed in `726ff84`), the
`WindowType.AI_ASSISTANT` window and the `sc_ai` desktop shortcut are all still in the tree, and
the phone's saved prefs still carried a `win_ai_assistant_97e3` layout from an earlier session.
Two things made it look absent:

1. **No start-menu entry.** `Windows11StartMenu` listed ten apps and never included the assistant.
   It ships as a taskbar pin (`sc_ai` in `getDefaultTaskbarPins()`), and the phone's saved
   `taskbarPins` still list it - the purple glyph at the right of the bar is Local AI, captured on
   the monitor - but a user looking in Start found nothing, and the icons carry no labels.
2. **The installer ran Alpine's `apk add llama-server` unconditionally**, in whatever guest was
   active. Debian has been the active guest since the switch, so setup died on its first step with
   `apk: not found` and never reached the model download.

Package availability, read from the guests themselves rather than assumed:

- Debian 12 bookworm: `grep -ah "Package: .*llama" var/lib/apt/lists/*_Packages` returns **nothing**.
  There is no llama.cpp or llama-server for that release, so `apt-get` cannot fix it.
- Alpine 3.24, fetched from `dl-cdn.alpinelinux.org/alpine/v3.24/*/aarch64/APKINDEX.tar.gz`:
  `community` lists `llama-server`, `llama-server-openrc`, `llama.cpp`, `llama.cpp-cpu`.

Fix: the model runtime now owns a **private bundled Alpine guest** (`LinuxContainer(context,
distroOverride = "alpine")`), so the desktop's distro switch neither deletes the server nor prunes
the 1 GB model. The installer discovers the manager (`apk`/`apt-get`/`pacman`), tries
`llama-server` then `llama.cpp`, and accepts the result only when `command -v llama-server` finds a
binary. The missing menu entry is restored, and the build is `1.5 (build 6)`.

Not claimed: the 1 GB model download and an in-app "Set up local AI" tap were **not** exercised in
this round — the guest package lists and the Alpine index were measured instead. `uiautomator`
dumps only the phone display, not the 1920x1080 DeX display, so the menu entry itself is verified
by code and build, not by an automated tap.

Correction: an earlier draft of this section claimed the saved four-item *desktop* shortcut list had
dropped Local AI. It had not - the desktop list is four items by design and AI is a taskbar pin.
A one-time backfill of desktop shortcuts was written on that wrong premise and then reverted.

## App identity, AI zoom and distro-aware commands (1.5)

- **[static]** `model/PhonixApps.kt` is the shipped-app list. `Windows11StartMenu` builds its Start
  entries and the new "Phonix OS apps" row in All apps from it, so removing a desktop icon
  (`removeShortcut`) and dropping a bar pin (`unpinFromTaskbar`) cannot remove an app from the
  product: both still only filter their own layout list. `AppCatalogTest` asserts unique ids and
  that Local AI is always present.
- **[static]** Icons now resolve through `AppIconGlyphs` on all three surfaces. The taskbar kept a
  second `when (type)` table of Material vectors that disagreed with the desktop artwork; that table
  is gone, and Start tiles render the same drawable art as the desktop instead of a tinted glyph.
- **[static]** The AI window has zoom: `A-` / percentage / `A+` in its header, 75-200%, the
  percentage resets to 100%. It scales the content (`graphicsLayer` on a `1/zoom`-sized layout), so
  text, controls and chat bubbles grow together and the message list still scrolls.
- **[static]** `LinuxDistro.installCommand()` / `installedQuery()` pick `apk`, `apt-get` or `pacman`;
  the Linux-app install, its probe, the Server Hub install/probe and the guest MOTD use them. This
  is the same class of fault that left `apk add llama-server` failing on Debian.
- **[measured]** `assembleDebug testDebugUnitTest` BUILD SUCCESSFUL, installed on SM-G973U, process
  starts with no `AndroidRuntime` error.
- **Not run:** tapping through Start/All apps and the zoom control on the DeX display. `uiautomator`
  only sees the phone display, and taps on the bar's start button did not open the menu, so the
  layout is `[static]` verified, not photographed.

## 13. Verifiable update path — 2026-10-31

### The defect this replaces

- `checkAppUpdates()` requested `api.github.com/repos/jberggre/Phonix-OS/releases/latest`. That
  repository is private, so the only observable result was a 404 (measured earlier for both the
  wrong and the real repository). A build that never receives a document must not render "up to
  date"; with no feed configured it now says so.
- Ordering ran on `versionName` text, so a future "1.10" would sort below "1.9". [static]
- `downloadAndInstallUpdate()` opened the release page in a browser: no digest, no signer check, no
  installer session, so the "update" was whatever the page served.

### What the tree does now [static]

| Gate | Code | Failure behaviour |
|---|---|---|
| Feed shape | `model/UpdateFeed.parse` | null on a missing digest, non-https `apkUrl`, zero `versionCode`, non-JSON |
| Ordering | `UpdateFeed.isNewer(manifest, currentVersionCode)` | offers only a strictly newer `versionCode` |
| Transfer | `engine/UpdateInstaller.download` | writes `.part`; a truncated download is rejected |
| Integrity | SHA-256 of the file vs the feed's published digest | aborts, deletes the partial file, names both prefixes |
| Signer | archive certificate vs installed certificate | refuses before the installer opens |
| Consent | `PackageInstaller` session, default user action | Android's own dialog; no silent install |

`BuildConfig.UPDATE_FEED_URL` comes from `-PphonixUpdateFeed=…` or `PHONIX_UPDATE_FEED` and defaults
to empty, which About reports as "not configured in this build". It never prints a claim that the
unit is current unless the feed actually said so.

### Checks

- `:app:assembleDebug` — **BUILD SUCCESSFUL** (JDK 21 / Gradle 9.3.1 / SDK 36).
- `:app:testDebugUnitTest` — **54 tests, 0 failures**, including the new `UpdateFeedTest` (7 tests).
- [measured] `tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk --serial
  R38M200YNRA`: `Success`, `1.5 (versionCode 6)`, crash buffer 0 entries, `DeviceOwner` intact,
  `rootfs trees: alpine debian tmp`, Alpine rescue present. Exit 0.
- [measured] post-install launch: `pidof` 3387, no `FATAL`/`AndroidRuntime` error for the package.
- **Not run:** a live feed check, download and install. No feed is hosted, so those [static] paths
  have not been measured on the device. The signer comparison and the installer hand-off are the
  parts worth exercising first.

### Known limits

- The feed is JSON over HTTPS with a published digest. It is **not** a signed manifest: whoever can
  serve the feed can offer any APK — but that APK still has to carry this app's signing certificate,
  which `signersMatch` checks and Android enforces. Signing the feed itself would remove the
  trust-the-host property and is the next step if the owner wants it.
- A download needs a connection and about 30 MB free while the old build is still installed. Nothing
  prunes the cache except a successful install.

## 14. Phone Screen targets the phone panel — 2026-11-01 (measured, S10 + DeX)

### The defect

With Phonix running fullscreen on the 1920x1080 HDMI desktop (display 2) and the phone panel on
display 0:

- The capture service measured `WindowManager.defaultDisplay` from its own context and got the
  **desktop**: 1920x1080 at 160 dpi.
- It built a **540x303 (16:9)** virtual display — `dumpsys display` showed
  `phonix-phone-screen ... 540 x 303` — and logged `capturing at 540x303`.
- The frames were the desktop: the window showed Phonix's own network-onboarding card while a
  simultaneous `screencap -d 0` showed the phone's Gemini dialog on a black panel.

So the one surface that exists to show the cracked phone screen showed the monitor instead.

### What changed

| Change | File |
|---|---|
| Panel metrics come from `DisplayManager.getDisplay(Display.DEFAULT_DISPLAY)`; the surface keeps the panel's aspect ratio | `service/ScreenMirror.kt` |
| The projection is requested from a **display-0 context** (`Context.createDisplayContext(panel)`) | `service/ScreenMirror.kt` |
| API 34+ asks the system for `MediaProjectionConfig.createConfigForDefaultDisplay()` | `ui/components/PhoneMirrorWindow.kt` |
| Copy says it captures the panel, not this desktop; the display-0 recursion card is unchanged | `ui/components/PhoneMirrorWindow.kt` |
| `ScreenMirror.captureSize()` is pure and unit-tested | `ScreenMirrorGeometryTest` |

One UI 4.1 binds a MediaProjection capture to the display of the requesting context, so the
display-0 context is the only app-level lever on API 31; `MediaProjectionConfig`, which states the
target display directly, exists only from API 34.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL, 58 tests, 0 failures**. New
  `ScreenMirrorGeometryTest` (4): 1080x2280 → 540x1140, 1920x1080 → 540x303 and deliberately *not*
  equal to the panel size, landscape 2280x1080 → 540x255, and degenerate input cannot produce a
  zero-height surface.
- [measured] `adb install -r` over the device-owner build: `Success`; device owner intact.
- [measured] log after consent: `panel 1080x2280 @420dpi -> capturing 540x1140`, then
  `first captured frame 540x1140`; `dumpsys display` reports the virtual display at 540x1140.
- [measured] the window frame shows the phone panel (Gemini dialog, status bar 58%), matching a
  simultaneous `screencap -d 0`; no part of the desktop is in the picture.
- [measured] closing the window stopped the session: `dumpsys media_projection` → `null`, no
  `ScreenMirrorService`, crash buffer empty.

### Not run, and why it matters

- **The stock consent window ignored injected taps** on this DeX display in most attempts, while the
  same `input -d 2 tap` works on Phonix's own windows. The run above was granted with
  `appops set … PROJECT_MEDIA allow`, which was then reset to `default`, so the shipped gate is
  still Android's per-session dialog and a real pointer is needed to answer it.
- Not run: capture after moving the desktop onto display 0 (the recursion guard is unchanged),
  rotation during a live capture, and the API 34 config path (the unit is API 31).
- Start → All apps laid out only 11 of the catalog's entries on the same display; fixed in §15.

## 15. Start menu: every app reachable (2026-11-01, S10 over wireless adb + HDMI monitor)

[measured] With Phonix fullscreen on the HDMI desktop, Start → All apps laid out **11 of 21** Phonix
cells: the "Phonix OS apps" grid was a `LazyVerticalGrid` capped at 132 dp with
`userScrollEnabled = false`. The Pinned view had the same shape at 240 dp (`GridCells.Adaptive`) and
its third row was cut through the icons. Both sit inside a `Column` that already scrolls vertically,
so the caps hid entries without giving anything else room.

Fixed in `ui/components/Windows11StartMenu.kt`: both surfaces are plain `Row`s of
`START_GRID_COLUMNS = 5` weighted cells (`chunked`, with a trailing `Spacer(weight(1f))` so a short
row keeps its columns) inside the menu's own scroller. The pinned tile moved into `StartTile`, so the
row layout has one definition of a pin. The All-apps lists for Android apps (compact, storage weight,
categories) were left alone: they scroll by themselves and carry no fixed cap.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, 58 tests, 0 failures. The change is
  layout-only and no test covers Compose geometry, so the evidence below is the measurement.
- [measured] `adb install -r` → `Success`; `versionCode=6`; crash buffer empty after the taps and
  the drag.
- [measured] All apps on the desktop: all **21** Phonix cells are drawn (App Store … "Photos &
  Media" as the fifth row), against 11 before. `artifacts/session/start-fix-allapps.png`.
- [measured] Pinned view: four whole rows, no tile sliced (row 3 was cut before), and one drag
  (`input -d 2 swipe 960 950 960 560 400`) brings the 21st pin and the footer into view.
  `artifacts/session/start-fix-pinned2.png`, `artifacts/session/start-fix-scrolled.png`.
- [measured] Same build, same session: Phone Screen showed the phone's own home screen (status bar
  1:19 / 58%, phone wallpaper and dock) while Phonix owned the desktop.

Not run: the Android-app tabs (compact / storage weight / categories) were not clicked through after
the change, and there was no rotation or 320 dp narrow-width check of the new rows.

## 16. Taskbar docking: bottom, left, right, top (2026-11-01, S10 + HDMI monitor)

The bar could only sit at the bottom. `TaskbarPosition` had two values (`BOTTOM`, `TOP`), nothing
called `updateTaskbarPosition`, and `PhonixDesktopScreen` hard-placed the bar at
`Alignment.BottomCenter` with a fixed 50 dp inset, so on a monitor the bar permanently cost 50 dp of
height and there was no way to move it.

### What changed

| Change | File |
|---|---|
| `TaskbarPosition` gains `LEFT`/`RIGHT`, a `title`, `isVertical`; new `TaskbarMenu` menu data | `model/DesktopCustomization.kt` |
| `openTaskbarContextMenu(position)` | `viewmodel/PhonixViewModel.kt` |
| Vertical bar layout (56 dp column: start, search, scrolling app strip, stacked tray, show-desktop) and the dock gesture on the bar | `ui/components/Windows11Taskbar.kt` |
| Dock menu with Bottom / Left / Right / Top and a tick on the current edge | `ui/components/DesktopContextMenu.kt` |
| Work area, icon grid, window layer and Start menu follow the bar's edge | `ui/components/PhonixDesktopScreen.kt` |

The menu is placed by alignment, on the opposite side of the bar's edge, rather than at the pointer
offset, so it can never open underneath the bar or off-screen. The gesture is a secondary-button
press with a mouse and a long press in the empty bar area for touch. The bar's own dimensions
(`TASKBAR_HEIGHT_DP`, `TASKBAR_WIDTH_DP`) are single constants that the work-area inset reads, so a
window can never be dragged under the bar or lose its resize grip to it.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, 58 tests, 0 failures.
- [measured] Long-press on the bar opens the dock menu on the DeX display, at the bar's bottom edge
  (`artifacts/session/dock-1-menu.png`) and, after docking to the top, just below the top bar
  (`dock-6-menu-top.png`, tick on "Top").
- [measured] All four edges applied in one session on 1920x1080: bottom, **left** (`dock-2-left.png`
  — vertical bar, desktop icons shifted right of it), **top** (`dock-4-top.png` — bar across the
  top, icons and widgets below it), **right** (`dock-7-right.png` — vertical bar, Vitals widget
  pulled left of it).
- [measured] Start opens beside the right-docked bar instead of under it (`dock-8-start-right.png`).
- [measured] A Terminal window opened inside the inset work area, clear of the right bar, and the
  guest answered `root@debian:~#` (`dock-9-window-right.png`).
- [measured] The choice persists: `shared_prefs/phonix_desktop_prefs.xml` holds
  `"taskbarPosition":"RIGHT"` in `desktop_state_v1` after the change.
- [measured] Crash buffer empty (`logcat -b crash` → 0 entries for the package).

### Not run

- **Mouse right-click is unmeasured.** `adb shell input` only injects the primary button, so the
  secondary-button path (`PointerType.Mouse && buttons.isSecondaryPressed`, the same mechanism the
  desktop icons use) was exercised only by long-press. It needs a real mouse on the DeX display.
- Not run: dragging/resizing a window against a left or top bar, and the phone-panel portrait layout
  with a vertical bar (a 56 dp bar on a 360 dp-wide phone leaves 300 dp of desktop).

## 17. Start menu height follows the screen (2026-11-01, S10 + HDMI monitor)

The panel was capped at 560 dp on every display. On the 1080p monitor that left the Android-app list
with a single clipped row under the 21-cell Phonix grid, even though the desktop is about 940 dp
tall. `Windows11StartMenu` now caps at `min(720 dp, 82% of the screen height)`: a phone still gets a
floating card, a monitor gets the height it actually has.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, 58 tests, 0 failures.
- [measured] Pinned view on the desktop draws five whole rows of pins and the store banner
  (`artifacts/session/start-h-1.png`), where the third row used to be sliced.
- [measured] All apps draws all 21 Phonix cells, the "Android apps" header, the three view-mode chips
  and the category chips, with real Android rows beneath them — Category Tabs selected, Air Zone and
  Browser listed (`artifacts/session/start-h-2.png`).
- [measured] Crash buffer empty.

Not run: the phone-panel (portrait, non-DeX) layout at the new cap, and a 320 dp-wide window.

## 18. The top notch clears a top-docked taskbar (2026-11-01, S10 + HDMI monitor)

`AutoHidingTopBar` (the centred "Phonix Mini PC / AC / WIFI ONLINE" pill, and the full telemetry
panel it expands into) is drawn over the desktop as an overlay at `Alignment.TopCenter`, and the
taskbar is drawn after it. While the bar could only sit at the bottom this never mattered; once it
docks to the top edge it covered the pill outright.

`AutoHidingTopBar` now takes a `topInset: Dp`, and `MainActivity` passes the taskbar height when
`customization.taskbarPosition == TaskbarPosition.TOP` and `0.dp` otherwise. The inset is applied
after `statusBarsPadding()` in both states, so the collapsed pill hangs off the bar's bottom edge and
the expanded panel opens underneath it.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL**, 58 tests, 0 failures.
- [measured] With the bar docked to the top on the 1920x1080 desktop, the pill sits directly under
  the bar and is fully readable — emblem, "Phonix Mini PC", AC badge, WIFI ONLINE chip and the
  chevron all visible (`artifacts/session/topbar-docked-top.png`), where it was previously hidden
  behind the bar.
- [measured] Tapping the pill with the bar on top expands the full bar below the taskbar instead of
  under it (`artifacts/session/topbar-expanded-top.png`).
- [measured] Crash buffer empty.

Not run: the compact phone-panel form with a top bar (the pill shortens to "Phonix" below 600 dp).

## 19. Audit fixes: lost settings, rescue guest, updater threading (2026-11-01)

An audit pass over every feature found six defects that either lost user state or broke a promise
made in these documents. All six are fixed here.

| Defect | Fix | File |
|---|---|---|
| The icon-frame toggle never came back: the saver wrote `showIconFramesV3`, the reader asked for `showIconFramesV2` | reader follows the writer; V2 is still read for layouts written before the rename | `util/DesktopPrefs.kt` |
| `areAllWindowsHidden` was set in the view model and never written to disk | saved and restored, so "Show Desktop" survives a relaunch | `util/DesktopPrefs.kt` |
| The prune deleted the bundled Alpine tree, so "the rescue image is never pruned" was not true | a bundled distro is excluded from the prune | `engine/LinuxContainer.kt` |
| Bundled extraction deleted the working tree *before* unpacking, so a failure left no userspace | unpack into `.alpine-staging`, promote only after a complete extraction | `engine/LinuxContainer.kt` |
| `UpdateInstaller.install()` did PackageManager queries and the APK copy on the caller's (main) thread, and kept one static receiver slot | work moved to a single-thread executor, results posted back to main, one receiver per session | `engine/UpdateInstaller.kt` |
| `readFeed()` accepted a plain-http feed URL although the class documents HTTPS | `require(url.startsWith("https://"))` | `engine/UpdateInstaller.kt` |

`DesktopPrefsRoundTripTest` is new and pins the two prefs fixes: a save/load round trip of
`showIconFrames`, `areAllWindowsHidden`, `iconSize`, `taskbarPosition` and `lockIconsToGrid`, plus a
legacy layout written with `showIconFramesV2` still being honoured.

### Checks

- `assembleDebug testDebugUnitTest` — **BUILD SUCCESSFUL, 60 tests, 0 failures** (58 + the 2 new
  prefs tests).
- [measured] `tools/verify-shipping.sh --apk … --serial …` — **PASS**: install `Success`,
  `1.5 (versionCode 6)`, crash buffer clean, device owner intact, `rootfs trees: alpine debian tmp`,
  bundled Alpine rescue present.
- [measured] The saved layout on the unit now carries both repaired keys
  (`"showIconFramesV3":false`, `"areAllWindowsHidden":false`) inside `desktop_state_v1`.
- [measured] After this reinstall the desktop came back with the taskbar still on the **top** edge it
  had been left on, i.e. the stored position is applied at launch (`artifacts/session/fix-1-top.png`);
  docking it to the right from the bar's own menu still works (`fix-2-dock.png`).
- [measured] Crash buffer empty after install, launch and a re-dock.

### Not exercised on device

The three container/updater repairs are code- and unit-level only:

- bundled re-extraction into staging (needs the Alpine tree deleted first — destructive, and the
  project rule is not to remove a working userspace),
- the prune exclusion (needs a real distro switch, ~90 MB download),
- the HTTPS feed guard (no feed is hosted; the update path still has no end-to-end run).

### Remaining audit findings, knowingly left

`Windows11StartMenu` counts Android apps under an "App Collection (N)" label above a mixed list;
`ScreenMirror` allocates a frame per 300 ms without recycling the previous bitmap; `HostDebloat`
sizes only the base APK so the storage-weight view under-reports split apps; the dock menu's
`onDismiss` is unused and the hint line is not clickable; the vertical bar drops the IP readout; the
distro download/installed sizes in `LinuxDistro` are stale (measured today: 90.6 / 121.8 / 170.0 /
74.1 MB, and the Debian tree on the unit is 1.3 GB, not ~300 MB); `PhoneMirrorWindow` still suggests
`adb shell input tap` for consent, which VERIFICATION §14 measured as not working; `HANDOFF` §12
credits an xz dependency this build does not have.

## §20 — Content keeps clear of the bar only on the edge the bar is on (2026-11-01)

`DesktopWindowFrame` drew a maximized window as `fillMaxSize().padding(bottom = 50.dp)` while the
window layer around it was *already* inset by the docked bar on whichever edge it sat on. A bottom bar
therefore reserved 100 dp — 50 dp of dead space between the window and the bar — and a bar on the
right, left or top still reserved 50 dp at the bottom, so a maximized Phonix Browser hung off the
bottom edge with nothing under it.

The maximized branch is `Modifier.fillMaxSize()` now; the layer's own inset is the only reserve.

- [measured] Bar **right**, Phonix Browser maximized on the 1920x1080 DeX display: the window's accent
  border occupies rows 1078-1079, i.e. the window reaches the bottom edge of the screen
  (`artifacts/session/max-right2.png`).
- [measured] Bar **bottom**, the same maximized window: border at 1028-1029, bar at 1030-1079 —
  exactly the 50 dp the bar needs and no gap (`artifacts/session/max-bottom.png`).
- [measured] `assembleDebug testDebugUnitTest` green, 60 tests, 0 failures; crash buffer empty after
  install, launch, maximize/restore and two dock changes.
- Not measured: left and top bars with a maximized window (same branch, same single expression), and
  non-maximized windows, which were never affected — their size comes from `WindowBounds`.

The same edge change moved the bar's bottom reserve from 54 dp to 12 dp under a *dragged* desktop
icon, which stores its offset inside the icon area. `sc_browser` held `hasCustomPosition:true,
posY:918.7`; measured with the bar docked bottom the Browser tile and its caption sat on the bar
(`artifacts/session/icons-bottom.png`). Custom positions are now clamped into the current icon area,
so the icon is lifted clear of a bottom bar and returns to its stored spot when the bar leaves that
edge. Fixed render-side only: the stored value is unchanged, so nothing drifts across dock changes.

## §21 — "Lock Icons to Grid" snapped a second, imaginary grid (2026-11-01)

`toggleLockIconsToGrid` kept its own grid: an 88 x 94 dp pitch measured from a 16,16 origin in the
top-left corner, columns clamped at 5 and rows at 7, and it only visited shortcuts that already had
a custom position. The renderer meanwhile laid un-dragged icons out bottom-left on a 90 x 88 dp
pitch. So locking the grid moved the one dragged icon (the Phonix Browser) to row 7 — the middle of
the screen — and left every other icon exactly where it was.

- [measured] Before: `"sc_browser":{"gridCol":2,"gridRow":7,"posX":192,"posY":674}` in
  `phonix_desktop_prefs.xml`, with File Explorer, Terminal and Settings still on the bottom row.
- Fix: `model/DesktopIconGrid` is the one grid (90 x 88 dp pitch, rows counted from the **bottom**
  row). The desktop reports its live column count and area height (`setIconGrid`), and locking snaps
  *every* icon to its nearest free slot, marking each as explicitly positioned.
- [measured] After the fix, on the DeX display: Disable Grid Snap → Align Shortcuts to Bottom-Left →
  Enable Grid Snap leaves all four icons (File Explorer, Terminal, Phonix Browser, Settings) in one
  row at the bottom-left, stored as `(0,0) (1,0) (2,0) (3,0)` with `posX` 0/90/180/270 and `posY` 880
  (`artifacts/session/lock2.png`). Nothing jumps to the middle of the screen.
- [measured] `assembleDebug testDebugUnitTest` green — 61 tests, 0 failures; the new test pins both
  halves (every icon snapped to its auto slot; an icon dropped at 300,500 dp lands on column 3, its
  nearest slot, not mid-screen).
- Not measured on device: dropping an icon onto an occupied slot while locked (unit tests cover the
  displacement).

### Follow-ups from §20

- **Maximized window, bar docked top** — [measured] the window starts below the 50 dp bar and its
  accent border sits on the last row (1078), so a top bar reserves height only at the top
  (`artifacts/session/maxtop.jpg`). **Left is still unmeasured**; it shares the same branch.
- **Dragged icons follow the work area** — `PhonixViewModel.reanchorShortcuts` shifts every freeform
  icon by the delta of the icon area's height, so an icon on the bottom row stays on the bottom row
  when the bar changes edge. [static]: the render-side clamp from §20 remains as the outer guard.
  Not re-measured across a dock change.
- **Re-audit of every `taskbarPosition` reader** — one more unconditional reserve turned up: the
  toast host in `MainActivity` was pinned 56 dp above the bottom edge whatever edge the bar was on;
  it now clears the bar only when the bar is docked bottom (12 dp otherwise). The windows layer, the
  icon paddings, the Start menu anchor, the top notch and the dock menu were already conditional.

## §22 — The debloater could not tell a hidden package from an installed one (2026-11-01)

Audit of every surface that acts on the host device. Four defects, all of them visible to the user:

| Defect | Why it happened | Fix |
| --- | --- | --- |
| A hidden package kept reading **INSTALLED** | nothing called `DevicePolicyManager.isApplicationHidden`; `getPackageInfo` answers for hidden packages | `HostDebloat.packageState` / `stateOf` return INSTALLED / HIDDEN / ABSENT, and the view model publishes `debloatHidden` beside `debloatInstalled` |
| Hiding was **not reversible** although the KDoc promised it | the UI only ever offered Hide | `HostDebloat.unhidePackage` + `PhonixViewModel.unhideHostPackage`; the Hide button becomes **Restore** for a hidden package |
| "Screenless Mini PC debloat" ticked **nothing** | it matched categories `Telephony & Carrier`, `Biometrics & Sensors`, `OEM Bloat`, none of which exist in `DebloatCatalog` | `DebloatCatalog.applianceCategories` names real categories, and a test asserts every name exists and selects a non-empty set |
| Storage per package **under-reported** | `HostDebloat.getPackageApkSize` counted `sourceDir` (base APK only) | it delegates to `PackageFacts.apkBytes`, which sums the splits |

Also fixed: the debloater list now reads `uiState.debloatPackages` instead of re-reading the
catalogue, so a tick in the wizard and the tri-state here cannot disagree; "Rescan" refreshes
through `refreshSetupFacts` instead of bumping a local key before the async hide has finished; and
`WallpaperProvisioner` logged its success line twice.

Checks: [measured] `assembleDebug testDebugUnitTest` — BUILD SUCCESSFUL, **64 tests, 0 failures**
(three new: tri-state mapping, preset categories exist, preset selects packages).
**Not run on the device:** this round's debloater UI was not re-opened on the unit — the RDX taps in
this session were going to the phone panel until `input -d 2` was used, so a screenshot of the new
HIDDEN/Restore states is still owed. The device-owner hide path itself is unchanged from §19.


## §23 — Distro switching: not exercised end to end (2026-11-01)

The request was to prove a real switch. It was **not** run, and this is what blocked it: the
Distro Hub window could be opened (`Linux System` tile → hub showed `Debian 12 (Bookworm) 12.15`),
but a switch needs a 78–178 MB download plus extraction, and the session ran out of budget while
still locating the hub's controls under synthetic taps. Nothing was left half-installed — the
install path stages into `files/linux/tmp` and only promotes after extraction succeeds.

What was verified on the unit, non-destructively:

- [measured] `files/linux` holds `alpine`, `debian`, `tmp`; the bundled rescue image is intact
  (`files/linux/alpine/etc/os-release` resolves).
- [measured] The active guest is Debian 12; the terminal reports
  `PRETTY_NAME="Debian GNU/Linux 12 (bookworm)"`.
- [measured] Crash buffer 0 lines after every step.

Owed next session: install Arch or Kali from the hub, capture the progress text and the staged
tree, then confirm (a) the new tree is promoted only after extraction, (b) `alpine` and `debian`
both survive, (c) `cat /etc/os-release` in the terminal shows the new distro.

**Operational note for the next session:** relaunching the desktop by hand needs the *namespace*
class name, not the package name: `am start --display 2 -n
com.aistudio.phonix.vpxmkr/com.example.MainActivity`. Using
`com.aistudio.phonix.vpxmkr.MainActivity` fails with "Activity class does not exist" and leaves
DeX on Samsung's launcher. UI automation also needs `uiautomator dump --display 2` (the default
dump reads the phone panel) and `input -d 2` for every tap.
