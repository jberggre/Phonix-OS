# Phonix OS — handoff for the next session

Written 2026-10-29, after the round that replaced the local-AI placeholder and restored
Phone Screen as a real view. This page is the **work queue**. Read
[VERIFICATION.md](VERIFICATION.md) for the evidence behind every claim, [ROADMAP.md](ROADMAP.md)
for the research and sources, and [PROVISIONING.md](PROVISIONING.md) before touching the unit.

## Shipping round (2026-10-31, cloud worktree — no phone attached)

This round started from a failed session. Its commit (`9df837b`, “support multi-distro
environments”) was in the tree but undocumented, unbuilt and never installed. The audit found the
feature was a facade:

- `LinuxContainer.switchDistro` **never downloaded anything**. For every non-bundled distro it
  copied Alpine's `bin/` into a new directory, wrote an `/etc/os-release` saying
  `NAME="Debian 12 (Bookworm)"`, and marked the tree installed. The terminal would have run
  Alpine busybox while every surface claimed Debian, Ubuntu, Arch or Kali.
- It **deleted the previous rootfs before installing**, then flipped `active_distro_id` before the
  install had succeeded — so a failed or cancelled switch cost the user their working guest.
- The catalogue URLs (`.../default/rootfs.tar.xz`) return **404**; the real images live in dated
  subdirectories. Measured from this worktree: the bare archive path 404s, the dated directory and
  its `SHA256SUMS` answer 200.
- The knowledge-tier UI promised 24/65/130 documents and “~800 KB / 2.8 MB / 7.5 MB”. The library
  holds **25** documents total, and nothing in the app measures library bytes.

What changed this round:

1. **`engine/DistroInstaller.kt` (new).** Resolves the newest dated arm64 build from the Linux
   Containers index, downloads it with byte progress, requires the published `SHA256SUMS` entry to
   match, extracts the `rootfs.tar.xz` (new `org.tukaani:xz` dependency — the only format the image
   server publishes), writes the guest's `resolv.conf`/`hosts`, and only then renames the staged
   tree into place. A checksum mismatch, an empty archive, a short disk or a network failure aborts
   with a message and leaves nothing changed. `DistroIndex` holds the parsing as pure functions.
2. **`LinuxContainer.switchDistro`** is now `suspend`, installs first, activates the pref last, and
   prunes other rootfs trees only after success. **Alpine is never pruned**: it ships in the APK,
   so it is the offline rescue image if a downloaded distro ever fails to boot.
3. **`LinuxRuntime.switchDistro`** returns `Boolean`, and on failure logs the reason and restarts
   the distro that was already active.
4. **`DistroHubScreen`** shows a real error card (“The switch did not happen … Nothing was
deleted”), byte-level progress, and copy that no longer claims the previous image is purged before
   the download. Download sizes are the **measured** image sizes (Debian 95 MB, Ubuntu 128 MB,
   Arch 178 MB, Kali 78 MB) and disk figures are marked `≈`.
5. **Knowledge tiers** lost their invented sizes; the UI prints the real per-tier document count.
6. **Version:** `1.3 (versionCode 4)`, with the About fallback, updater defaults and model-setup
   user agent updated to match.

Verification on this tree (cloud VM, JDK 21 + Gradle 9.3.1 + SDK 36): `assembleDebug` and
`testDebugUnitTest` both pass, **46 tests, 0 failures**. `DistroInstallerTest` covers newest-build
selection, colon encoding, checksum parsing and rejection, the “every streamable distro has an
https index” contract, the Alpine fallback, and the tier counts. APK signing used a throwaway
debug keystore generated in the VM because `debug.keystore` is gitignored — **that APK cannot
update the device-owner install**; rebuild with the real key before installing.

**Done on hardware, 2026-10-31 (owner's laptop + wireless adb):** 1.3 was installed with
`adb install -r` over the device-owner build (Success, `versionCode=4`), the device owner is intact,
the crash buffer is clean, and the unit turns out to have a **real ES-G34C5 HDMI monitor attached**
— so DeX is verified for the first time: Phonix renders on Samsung's `Desktop` display (1920x1080)
with its top bar, widgets and taskbar. The running guest is a **real Debian 12 rootfs under proot**
(`-r …/files/linux/debian`, genuine `os-release`, `apt`, dpkg database), not Alpine wearing a
Debian label.

**Still owed:** an install started by 1.3's own downloader (checksum gate, progress UI, failure
path have not run end to end on the phone), the debloat hide walkthrough, and the Start-menu view
modes. Details in [VERIFICATION.md](VERIFICATION.md) §12.

## B4 glass round (2026-10-30, S10)

- **B5 and B6 are done in the committed tree.** B6 restores the open-window layout per 8 dp
  desktop size, so the portrait and landscape layouts are stored separately.
- **1920x1080 at 160 dpi:** dragged Terminal to the right, bottom and top-left edges, resized on
  both axes, and kept the caption, close button and grip reachable every time.
- **Live change to 640 dpi with the window open:** the window normalised into the work area above
  the taskbar; the right-edge drag and a horizontal resize passed. The 640 dpi desktop is only
  about 172.5 dp tall, so the 180 dp window minimum makes the window full-height; vertical
  movement is therefore pinned to the work area.
- **Two B4 defects found on glass and fixed in `13d4f9b`:** the resize grip keyed its gesture on
  the window's own width/height, so the first resize recomposed and cancelled the drag; and the
  window bounds included the 50 dp taskbar, so a full-height window covered the bar at 640 dpi.
  The work area now excludes the bar and the grip stays above it.
- **High-density host chrome fixed in `ce94f4b`:** the expanded bar put its identity and actions
  on one `SpaceBetween` row, so at 640 dpi the collapse control was laid out past the right edge,
  and the collapsed pill was 270 dp wide and covered most of a full-height window's title bar.
  Collapse is now pinned in the header, the subtitle ellipsizes, and the action row scrolls.
  Below 600 dp the collapsed notch is a ~90 dp `Phonix` pill with a compact network button and
  expands on tap/click rather than pointer entry. Measured at 1920x1080/640: `Hide` at
  `[1772,301][1844,373]`, Debloat/STOPPED at y=483..589, and a title drag from `(1500,280)` to
  `(1100,280)` moved the Terminal window without reopening the notch.
- **Taskbar compact mode:** below 600 dp the redundant `Phonix OS` brand block is hidden and the
  app strip is horizontally scrollable. At 1080x2280/420, a swipe from x=550 to x=120 exposed the
  Terminal and Settings entries that were previously clipped. The high-density Start menu still
  clips its pinned row and all-apps list; that is the remaining host-chrome issue.
- Verification: `assembleDebug` + `testDebugUnitTest` green, **38 tests, 0 failures**. The APK
  (29,682,604 bytes) was installed over the device-owner build with the shared debug keystore.
  The S10 was restored to `1080x2280`/420 dpi, rotation 0, accelerometer on; the device owner is
  unchanged, the Linux guest was stopped, the crash buffer has no package entry, and no screen
  mirror was started.

## Where the tree stands

- **Current worktree:** `versionCode 4`, `versionName 1.3` (see the shipping-round section above;
  it supersedes the `ce94f4b`/1.2 description kept in the rest of this page). It builds and tests
  green: `assembleDebug` plus `testDebugUnitTest` pass with **46 tests**. The 1.2 build described
  below was installed in place over the device-owner build on the S10 under the shared debug
  keystore; **1.3 never has been**.
- Changes add fold posture and connected-display reporting, adaptive shortcut/window geometry,
  saved opt-in device behavior, an OOBE compatibility/setup step, and focused classifier/geometry/
  preference/version tests. `git diff --check` passes; this is only a whitespace check.
- Three defects were found by measurement after the first install and are fixed in the tree:
  1. `containerStatus` defaulted to RUNNING, so a fresh launch advertised a Linux guest that was
     not running (the false "running" tile seen on the S10). It now defaults to STOPPED.
  2. A guest start that threw still reported RUNNING. It now reports STOPPED and keeps the
     background service off.
  3. Terminal `shutdown`/`poweroff` called the power *toggle*, so it booted a stopped container.
     It now calls an idempotent `stopContainer()`. The old unit test only passed because of (1).
- **External-monitor detection was wrong on foldables.** `DesktopAutoLaunch` accepted any
  non-default display that was ON. A Pixel 9 Pro Fold exposes its cover panel as display 3
  (`type INTERNAL`, no `FLAG_PRIVATE`), so folding the phone would have reported an attached
  monitor and opened the desktop on the cover screen. Both paths now require `FLAG_PRESENTATION`.
  Full pre-flight readings are in [COMPATIBILITY.md](COMPATIBILITY.md).
- **Pixel 9 Pro Fold is pre-flighted, not verified:** Android 17 / API 37, inner 2076x2152 and cover
  1080x2424 panels, unfolded ≈532x552 dp, `force_desktop_mode_on_external_displays=0`. The APK
  exists; the physical matrix (posture, external display route, windows) is still owed. Do not
  enroll device owner or run provisioning commands on the Pixel.
- **Taskbar and desktop were the same list, and that lost an app.** Removing a desktop icon also
  removed the app's taskbar entry (measured: deleting Local AI removed it from the bar too).
  `DesktopCustomization.taskbarPins` is now the taskbar's own list; deleting a shortcut only
  touches `shortcuts`, and a long-press on a bar icon unpins it without touching the desktop.
  Context-menu wording is now "Remove from Desktop" / "Pin to Taskbar" so the scope is explicit.
- **Desktop icons are conventional now:** File Explorer, Terminal, Browser and Settings, in the
  spirit of This PC/Home plus the usual launchers. Photos, the distro store and the hardware
  monitor are gone (they are the OS card and Vitals widgets); everything else is in the taskbar
  or Start menu. Keep that rule when adding surfaces: a desktop icon is a choice, not a directory.
- **Phone Screen no longer draws its own recursion.** The capture mirrors display 0, so when the
  desktop itself is on display 0 the feed could only ever contain Phonix. The window now detects
  that and says so, and shows the real phone screen when the desktop runs on an external display.
- **New platform fact worth a decision:** this Pixel ships Android 17's own Linux terminal
  (`com.android.virtualization.terminal`, `android.software.virtualization_framework`). Phonix still
  ships its own Alpine proot. Whether to detect and offer the platform VM on API 37+ devices is an
  open product question, not a bug.

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

## Setup wizard round (2026-10-30, walked live on the S10)

The OOBE was reworked after running it as a new user on the bench unit. Each change below
answers a defect that was seen on glass, not a hypothetical:

1. **Step 1 said "Storage: reading..." forever.** `refreshStorage()` is only called by the
   storage screens, so nothing ever sampled it during setup. Step 1 now calls
   `refreshSetupFacts()` on entry. The reading was always live behind that text; nothing was a
   cached image and nothing had been written to the device.
2. **Step 1 is now a welcome, not a hardware card.** A drawn phoenix/ember intro (Canvas, no
   video asset) plus the question the rest of the wizard depends on: everyday phone, cyberdeck,
   or mini-PC. The answer sets use mode, workload profile and the trim preset in one go, and
   every part of it stays editable. The battery-free DC switch is now labelled as a build note
   that changes nothing in Android, and it only appears for the two appliance personas.
3. **Step 2 now drives the trim list**, and says how many packages it pre-ticked. Daily-driver
   profiles tick only analytics and carrier telemetry; every other profile starts with the
   whole catalogue.
4. **Step 4 no longer implies sizes it does not know.** Installed browsers are a real
   `PackageManager` lookup with a measured APK size; a browser that is not installed says so and
   defers installation until after the trim step. Nothing claims a download size.
5. **Step 5 lists all eight trim targets** (it was `take(5)`), marks packages this phone does not
   have and disables their switches, and confirms a copy with the exact text that went to the
   clipboard. The Cyberdeck gate on *pressing Copy* is gone — the text is inert until pasted; the
   `allowDebloatCommands` switch now only governs the automatic copy at the end of setup.
6. **Step 6 explains the two modes** with a five-row difference table, because "appliance-style
   startup" told the user nothing.
7. **Step 7 states what the button will do** before it is pressed, colours log lines by the
   prefixes actually emitted (`OK`/`x`/`-`; the old check looked for `✔`/`➜` and never matched),
   and labels the button by what it will really do.
8. **Debloat is real where Android allows it.** On a device-owner install the deploy step now
   hides each selected package with `DevicePolicyManager.setApplicationHidden` and logs the result
   per package (`HostDebloat`). On an ordinary install it logs that nothing can be removed instead
   of implying otherwise. Hiding is reversible; nothing wipes or enrols.

Verification: `assembleDebug` + `testDebugUnitTest` green, **26 tests** (3 new: daily persona
keeps every invasive flag off, cyberdeck persona opts in but never enables erase or Private DNS,
and trim targets skip packages absent from the device). **Not yet run on hardware**: the wizard
was re-run live while this round was being written, so installing would have thrown the tester
out of an in-progress setup.

## Local AI round (2026-10-30)

- **The assistant can finally write code.** The old prompt said *"you cannot execute commands"*, capped
  answers at 180 words and let a regex notes table intercept "code" requests. That is why writing a
  snake game needed persuasion. The prompt now asks for complete runnable code and the notes table is
  only a fallback.
- **Offline library** (`model/KnowledgeLibrary.kt`): eight shelves matching the quick chips, 13 authored
  documents that work with no model and no network (water, first aid, shelter/fire, farming, radio,
  DC power, code/shell, hardware/Linux), browsable in a dialog with copy and "ask the model". This is a
  starter corpus, **not** an imported book pack; the retrieval layer takes documents as data, so an
  importer is plumbing, not a redesign.
- **Scoped retrieval:** the model reads only the selected shelf, or all shelves when "All" is chosen.
  Scoping improves answer precision and time-to-first-token; it does **not** raise tok/s, which is
  fixed by model size and CPU. Do not re-litigate this as a speed feature.
- **Text is selectable and copyable** in replies, agent commands and agent output. Before this the only
  way to move generated code into the terminal was retyping it.
- **Shell agent** (`⌘ Agent` in the AI window): the model answers with one `RUN <command>` or `DONE`
  line per turn, the user approves each command, output is captured and fed back, 8-step cap,
  auto-run off by default. **Privilege boundary:** the agent's shell is the proot Alpine guest, which
  has no adb and no authority over Android packages, so it can never uninstall host apps. Package
  removal stays with the deploy step (`HostDebloat` via DevicePolicyManager, device-owner only).
- **Debloat review in the wizard:** Step 5 can send the real ticked list to the model for risk
  advice, labelled as model output, with a deterministic built-in summary when no model is running.
  It is advice only; the card says so.

Verification: `assembleDebug` + `testDebugUnitTest` green, **29 tests** (library shelf isolation,
code-shelf ranking, no-context-for-gibberish, labelled debloat advice without a model). Nothing from
this round has run on glass yet.

## Desktop feel round (2026-10-30)

- **Icon dragging moved to a graphics layer.** The drag delta used to be applied with
  `Modifier.offset`, so every pointer event relaid out the desktop and re-rasterised the tile,
  border and shadow — that is the stutter. Layout position is now fixed and the delta is a
  draw-time `translationX/Y`; nothing relayouts until the drop commits. Not profiled on glass yet.
- **Icon frames can be turned off** (`DesktopCustomization.showIconFrames`, right-click desktop →
  "Icon Tiles: On/Off"). One switch removes both the outer card and the glyph tile, leaving glyph
  and caption.
- **One glyph resolver** (`ui/components/AppIconGlyphs.kt`) now feeds the desktop, taskbar and Start
  menu. `APP_STORE` and `SETTINGS` previously had no case and rendered the generic laptop; a pinned
  phone app always got the browser globe. Pinned apps use their real launcher icon when readable and
  a keyword-derived glyph otherwise.
- **The designed icon set is specified, not drawn:** [ICON-BRIEF.md](ICON-BRIEF.md) has the story,
  palette, per-surface imagery, filename mapping and delivery rules for an image model. The local
  model is a text LLM and cannot generate images; refitting `AppIconGlyphs` is the only code change
  the artwork needs.

## Icon sheet + input fixes (2026-10-30)

- **The designed icon set is in the app.** A 1024x572 sheet of 20 icons was sliced by connected
  component (not by a fixed grid, which cut labels and mis-ordered the thin thermometer icons),
  edge-flood-filled to transparency and written to `res/drawable-nodpi/` (~380 KB). The desktop
  and taskbar resolve through `AppIconGlyphs.artFor`; because the artwork carries its own tile,
  the app's tile and border are skipped when art is present or the frame would double. Start menu
  tiles still use Material glyphs.
- **Dragging now tracks the cursor.** The pointer handler sat *inside* the translated graphics
  layer, so hit-testing followed the moving icon and the reported delta shrank as the icon moved —
  it lagged behind the finger. Pointer input is now outside the transform; layout stays fixed and
  only the draw layer moves.
- **Right-click on the wallpaper works every time.** `detectTapGestures` has no mouse-secondary
  handling, so a right-click only opened the menu when the button happened to be held past the
  long-press timeout. Replaced with an explicit gesture loop: secondary button opens immediately,
  long hold opens, short press closes menus, and a swipe does nothing.
- **"Align to Bottom-Left" really goes bottom-left.** The grid origin was hardcoded to `0.dp`, so
  the action and its label both lied and the block landed top-left. The origin is now computed from
  the icon block height and the available area.
- **Developer section added to the desktop right-click menu:** Android Developer Options, copy a
  device report (model, API, ABI, display metrics, display route, device-owner state, guest state),
  toggle the alignment grid, restart the Linux guest.

Verification: `assembleDebug` + `testDebugUnitTest` green, 31 tests. **No visual check on glass:**
no device was reachable (`adb devices` empty, wireless endpoint refused), so the sliced artwork and
both gesture fixes are unverified on hardware.

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

0. **DONE: the 1.2 candidate builds, tests green, and installs.** On the S10 bench unit
   (`SM-G973U`, Android 12/API 31) `:app:assembleDebug` produced versionCode 3 / versionName 1.2 and
   `adb install -r` updated the existing 1.1 install in place, keeping the shared `debug.keystore`
   identity. `:app:testDebugUnitTest` is green: 20 tests, 0 failures, 0 errors
   (`DeviceCompatibilityTest` 4, `ExampleRobolectricTest` 10, `ExampleUnitTest` 6).
   Post-update launch is crash-free and the new consent defaults held: `phonix_os_prefs.xml` saved
   `DAILY_DRIVER` with every `device_*` flag false, `dumpsys activity services` showed no
   `LinuxServerService`, and there was no `proot` process, so the wallpaper, wakelock service,
   boot start and display watcher stayed off until chosen. The first launch after the update does
   auto-open the wizard on the new Step 6, which is how those defaults got saved.
   Still owed: the non-destructive Pixel 9 Pro Fold matrix in [COMPATIBILITY.md](COMPATIBILITY.md)
   (fold simulation is not a substitute for physical display routing). Note that this S10's front
   panel has a failing digitizer that emits phantom taps, so it keeps opening the desktop's App
   Store shortcut on its own; drive UI checks with a keyboard/mouse or expect to fight it.

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
# Stable copy created 2026-10-29; %LOCALAPPDATA%\Temp\phonix-toolchain is the older one and can be
# deleted by cleanup, so prefer this path. local.properties must use forward slashes in sdk.dir.
export JAVA_HOME="$HOME/android-toolchain/jdk"
export ANDROID_HOME="$HOME/android-toolchain/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
"$HOME/android-toolchain/gradle-9.3.1/bin/gradle" \
  --offline --no-daemon assembleDebug testDebugUnitTest

export PATH="$HOME/android-toolchain/sdk/platform-tools:$PATH"
adb install -r app/build/outputs/apk/debug/app-debug.apk

tools/deck-check.sh baseline
tools/deck-check.sh crashes
tools/deck-check.sh matrix     # restores the recorded 1080x2280 / 420 dpi
tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk
                               # 1.3 acceptance: version, crash buffer, guest trees,
                               # device owner; non-destructive, never uninstalls
```

A clean offline build is about 7 minutes; an incremental Kotlin compile about 3. The test task
was last run green in this round (`:app:testDebugUnitTest`, 2m06s offline).

If `%LOCALAPPDATA%\Temp\phonix-toolchain` has been cleaned up, a stable copy of the same toolchain
(JDK 21, Gradle 9.3.1, SDK platform 36.1 + build-tools 36.1.0, platform-tools) lives in
`~/android-toolchain`, with `sdk.dir` written to the gitignored `local.properties`. The Gradle
module cache in `~/.gradle/caches` already holds every dependency this build needs.

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
