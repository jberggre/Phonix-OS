# Phonix OS — handoff for the next session

Written 2026-10-29, after the round that replaced the local-AI placeholder and restored
Phone Screen as a real view. This page is the **work queue**. Read
[VERIFICATION.md](VERIFICATION.md) for the evidence behind every claim, [ROADMAP.md](ROADMAP.md)
for the research and sources, and [PROVISIONING.md](PROVISIONING.md) before touching the unit.

## Phone Screen round (2026-11-01, S10 over wireless adb + HDMI monitor)

The window that exists to show the cracked phone panel was showing the desktop. Measured with the
desktop fullscreen on the HDMI display: the capture service sized its surface from its own
`defaultDisplay` (1920x1080 at 160 dpi → **540x303, 16:9**) and the frames held Phonix's own
network-onboarding card, while display 0 was showing the phone's Gemini dialog.

Fixed in `service/ScreenMirror.kt` and `ui/components/PhoneMirrorWindow.kt`: the surface now comes
from display 0's real metrics (`panel 1080x2280 @420dpi -> capturing 540x1140`), the projection is
requested from a display-0 context because One UI 4.1/DeX binds the capture to the requesting
context's display, and API 34+ asks for `MediaProjectionConfig.createConfigForDefaultDisplay()`.
`ScreenMirror.captureSize()` is pure, with 4 new tests (58 total, green). The APK was installed over
the device-owner build and the frame was checked against `screencap -d 0`.

**Still owed on this path:** the stock consent window did not accept injected taps on this DeX
display (a real pointer does; the run used a temporary `PROJECT_MEDIA` app-op that was reset), the
display-0 recursion guard is unchanged, rotation during capture and the API 34 path are unmeasured.
**Also found and fixed in the same round:** Start laid out only 11 of its 21 app cells, and
the Pinned view sliced its third row, because both were lazy grids with hard height caps
inside a menu that already scrolls. They are whole rows in the menu's own scroller now, and a
drag reaches the last pin. Details in [VERIFICATION.md](VERIFICATION.md) §15.

## Taskbar docking round (2026-11-01, S10 over wireless adb + HDMI monitor)

The bar was hard-placed at `Alignment.BottomCenter` and `TaskbarPosition` only had `BOTTOM`/`TOP`,
with no caller for `updateTaskbarPosition` — so a monitor permanently lost 50 dp of height to a bar
that could not be moved. Right-click (mouse) or long-press (finger) on the bar now offers Bottom,
Left, Right and Top; the vertical bar is a 56 dp column with the tray stacked, and the work area,
icon grid, window layer and Start menu all read the same edge so nothing lands under the bar.

Measured on the DeX display: all four edges applied on 1920x1080, the dock menu opens without
covering the bar, Start opens beside a vertical bar, a Terminal window opens inside the inset work
area, and the choice is stored (`"taskbarPosition":"RIGHT"` in `phonix_desktop_prefs.xml`). Build
green, 58 tests, crash buffer empty. **Not measured:** real mouse right-click (adb only injects the
primary button) and the portrait phone layout with a vertical bar. Details in
[VERIFICATION.md](VERIFICATION.md) §16.

Follow-up in the same round: the Start panel's 560 dp cap now follows the screen
(min(720 dp, 82%)), so All apps shows the Android-app list on a monitor instead of one clipped
row — [VERIFICATION.md](VERIFICATION.md) §17.

Same round: a top-docked bar covered the centred top notch, which is drawn before the bar.
`AutoHidingTopBar` takes a `topInset` now and hangs off the bar's bottom edge, collapsed and
expanded — [VERIFICATION.md](VERIFICATION.md) §18.

Audit pass, same round: six defects fixed — the icon-frame toggle and "Show Desktop" now
persist, the bundled Alpine rescue is never pruned, bundled extraction stages before it replaces,
and the updater no longer does package work on the main thread or share one receiver slot, and
refuses a plain-http feed. 60 tests green, shipping check PASS. Container/updater repairs are
code+unit evidence only — no distro switch or feed was run. Remaining findings listed at the end
of [VERIFICATION.md](VERIFICATION.md) §19.

Same round, after the audit: a maximized window carried its own 50 dp bottom reserve on top of the
window layer's bar inset, so a bar on the right (or left, or top) still lifted every maximized window
50 dp off the bottom edge, and a bottom bar reserved 100 dp. That reserve is gone — the layer's inset
is the only one — measured maximized with the bar right (border in rows 1078-1079) and bottom (border
at 1028-1029, bar 1030-1079). A dragged desktop icon also used to keep its raw offset when the bar
changed edge, which put the Browser caption on top of a bottom bar; custom offsets are clamped into
the current icon area now, render-side only, so nothing drifts. 60 tests green, crash buffer empty.
[VERIFICATION.md](VERIFICATION.md) §20.

"Lock Icons to Grid" then turned out to snap into a grid of its own (88 x 94 dp from the top corner,
rows clamped at 7) and to visit only icons that had already been dragged — so it sent the Phonix
Browser to the middle of the screen and left the other three alone. `model/DesktopIconGrid` is now
the single grid the renderer and the view model both use (90 x 88 dp, rows counted from the bottom
row), the desktop reports its live column count and area height, and locking snaps every icon to its
nearest free slot. Measured: all four icons in one bottom-left row, stored as (0..3, 0). 61 tests
green. [VERIFICATION.md](VERIFICATION.md) §21.

Debloater audit, same round: a hidden package kept showing INSTALLED because nothing called
`DevicePolicyManager.isApplicationHidden`, and there was no way to un-hide it at all; the
"screenless Mini PC debloat" preset matched three category names that do not exist in its own
catalogue, so it ticked nothing and copied an empty script; and per-package storage counted only
the base APK. All four fixed — tri-state INSTALLED/HIDDEN/ABSENT, a Restore button,
`DebloatCatalog.applianceCategories` with a test that every name exists, and `PackageFacts.apkBytes`
for sizes. 64 tests green. The debloater UI was **not** re-opened on the unit this round.
[VERIFICATION.md](VERIFICATION.md) §22.

## Update-path round (2026-10-31, S10 over USB)

The one deployment blocker left in README's boundary list was the update path: `checkAppUpdates()`
requested the GitHub API of a **private** repository, so a unit could only ever see a 404, and it
compared `tag_name` strings, so "1.9" sorted above "1.10". `downloadAndInstallUpdate()` then opened
a browser. Nothing verified a download.

What changed:

1. **`model/UpdateFeed.kt` (new).** A feed is one JSON document — `versionCode`, `versionName`, an
   https `apkUrl`, a 64-hex `sha256`, plus notes and publish date. Parsing is pure JVM (Moshi's
   streaming reader) and **returns null** for a missing digest, a plain-http URL or a non-positive
   build number: an unusable feed must never read as "up to date".
2. **`engine/UpdateInstaller.kt` (new).** Streams the APK into the app cache as `.part`, checks the
   byte count, requires the published SHA-256 to match, then hands the file to a
   `PackageInstaller` session. Before that it compares the archive's signing certificate with the
   installed one, so a mismatched key produces a message instead of a silent installer failure.
   Nothing installs silently: the session commits with Android's own confirmation dialog.
3. **`PhonixViewModel`** reads the feed from `BuildConfig.UPDATE_FEED_URL` (a build input:
   `-PphonixUpdateFeed=…` or `PHONIX_UPDATE_FEED`), compares integer `versionCode`, reports byte
   progress, and keeps every failure explicit. The old GitHub helper functions are gone.
4. **About** names the feed host (or "not configured in this build"), renders DOWNLOADING and
   READY_TO_INSTALL states, and says what the install button will do. Its Linux card now names the
   **active** distro instead of a hardcoded "Alpine".
5. `REQUEST_INSTALL_PACKAGES` is declared; without it the install session cannot reach the system
   installer.

Verification: `assembleDebug` + `testDebugUnitTest` **BUILD SUCCESSFUL, 54 tests, 0 failures**
(`UpdateFeedTest` is new: complete-feed parse, string versionCode, rejection of a missing or
non-64-hex digest, rejection of plain-http and empty APK URLs, rejection of zero build numbers,
1.10 > 1.9 by build number, digest validation).

[measured] on the bench S10 over USB (`R38M200YNRA`): `tools/verify-shipping.sh --apk …` reported
`adb install -r` **Success**, `1.5 (versionCode 6)`, crash buffer clean, device owner intact,
`rootfs trees: alpine debian tmp`, bundled Alpine rescue present. Launch afterwards: pid 3387, no
`FATAL`/`AndroidRuntime` error. **Not run:** an actual feed download and install, because no feed is
hosted yet — that path is compiled and unit-tested, not measured.

Still owed on the update path: host a feed, then run one check, one download and one install on a
unit; and decide whether the feed should be GitHub Releases (needs the repository to be public) or
the owner's own manifest host. See [VERIFICATION.md](VERIFICATION.md) §13. This is the only
deployment blocker left in the README boundary list, so it heads the queue below, where the hosting half is now cancelled (item 1).

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

**Merged from the faulted session.** That session's uncommitted installer (mirror `index-system`
resolution, in-guest `unxz | tar` unpack, promotion only after a shell/`os-release` check) was
recovered from `worktrees/926fecd3` and is now the shipped path; it had already downloaded Debian on
this unit. This round added a **SHA-256 `SHA256SUMS` gate** the recovered code lacked, and adopted
its three device-found fixes (IO dispatcher, `resolv.conf` symlink, `refreshStorage`).

**Since measured:** the checksum-gated path has run end to end on the unit for four targets —
[VERIFICATION.md](VERIFICATION.md) §25 (Ubuntu, alpine→debian), §27 (Kali and Debian, re-run from
the recovered tree) and §28 (Arch, which this page had recorded as failing). Still owed: the
debloat hide walkthrough and the Start-menu view modes, which need a pointer on the DeX display.

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

- **Current worktree:** `versionCode 6`, `versionName 1.5`. The shipping-round section above
  describes the 1.3 candidate (`versionCode 4`); later rounds bumped the version to 1.4 and then
  1.5, and [VERIFICATION.md](VERIFICATION.md) records them. This supersedes the
  `ce94f4b`/1.2 description kept in the rest of this page. It builds and tests green:
  `assembleDebug` plus `testDebugUnitTest` pass, re-run from scratch with `--rerun-tasks` on
  2026-10-31, **47 tests, 0 failures** at that point; the recovered round since then carries **77** in 12 suites (§27, §29), all green on 2026-11-02. `tools/verify-shipping.sh` then installed 1.5 over the
  device-owner build on the SM-G973U (`adb install -r`, Success, `versionCode 6`): version, crash
  buffer, bundled Alpine rescue tree and device owner all passed. The active guest is the Debian
  tree from the earlier round; the distro-install, debloat-hide and Start-menu checks still need
  a finger.
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

## Next work, in order (rewritten 2026-11-02)

The previous list opened with window geometry, desktop persistence, the landscape grid and the
broken updater endpoint. All four shipped - section 19-21 and the 2026-10-31 update round - so a
session following it in order would have rebuilt finished work, which is the failure this repo has
already had once. This list matches the tree as it stands.

1. **Drop the separate-host assumption.** The channel is settled by ownership rather than by
   policy: the GitHub account is the owner's own, so the repository is the whole pipeline - source
   public, APK attached to a release, and `update.json` as a release asset too if the feed is ever
   wanted. No static host, no CDN, no store account, no new credential on a work laptop. The feed
   stays dormant meanwhile: `BuildConfig.UPDATE_FEED_URL` is empty and `UpdateFeed`/`UpdateInstaller`
   are written, unit-tested and never executed, which is a correct state, not an unfinished one. The
   owner's install path is `gradle assembleDebug` + `adb install -r`. What carries over to item 2 is
   `UpdateInstaller.download()` - digest-checked, progress-reporting - because a pack download is the
   same job on a bigger file.
2. **Offline library packs.** The import half shipped on 2026-11-02 (VERIFICATION §29): a
   `#!phonix-pack 1` text file goes through the document picker, is parsed onto a shelf, stored in
   private storage, listed with its measured bytes and deleted again. Eight tests cover the parser
   and the store; the picker round trip is not measured on hardware. Still missing is the **download**
   half - fetching a pack from a mirror - which needs a published pack with a pinned digest before it
   is anything but a guess, plus a decision on what the reader does with a multi-gigabyte archive.
   The Light/Medium/Heavy tiers in [ROADMAP.md](ROADMAP.md) section "Offline library" are content
   rather than code: the tier selector already gates the authored corpus, and imported packs ignore
   it by design so the owner's own material is never hidden.
3. **The three walks that need a pointer on the DeX display.** The debloat hide/restore walkthrough
   (section 22 fixed the tri-state mapping and never re-opened the screen), the Start-menu view
   modes, and the Phone Screen consent dialog, whose stock window does not accept injected taps.
4. **Dock/undock and the 24-hour soak.** Real DeX is measured (sections 13, 25, 27); missing is a
   dock -> undock cycle with the watcher consent enabled, a window surviving the display change, and
   a plugged-in soak. Do not call DeX verified until then.
5. **VS Code experiment** (`code serve-web` on the guest, [ROADMAP](ROADMAP.md) section on VS Code).
   The badge is probe-gated and honest; there has never been an editor behind it.
6. **Phone Screen input - only if a touch-less display appears.** Decided read-only on 2026-11-02.
   Control would mean an explicit `AccessibilityService` opt-in with its own disclosure, never
   app-UID `input` injection.
7. **Local-model thermals.** The model stays at 1.5B; a 30-minute plugged-in generation soak
   recording tok/s, temperature and `/health` stability is what replaces the unmeasured claim.
8. **Deck layouts (registry and palette shipped 2026-11-02).** `ModuleRegistry` now declares a
   capability per window type and the dead `PhonixStartMenu` is deleted; the command palette is the
   start menu's search field, filtered through the registry (VERIFICATION §30, 9 tests). What remains
   is the deck part: saving, naming and restoring three arrangements, which has no UI and no
   persistence yet. The palette is `[static]` until someone types in it on a DeX display.
9. **Pixel 9 Pro Fold matrix.** The APK exists and the unit is pre-flighted, not verified; fold
   simulation is not display routing. Do not enroll device owner on the Pixel.
10. **OOBE on a spare unit, and the game install URLs.** The bench unit is past setup and a reset is
    destructive, so these two paths have no way to be measured here.

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
                               # 1.5 acceptance: version, crash buffer, guest trees,
                               # device owner; non-destructive, never uninstalls
```

A clean offline build is about 7 minutes; an incremental Kotlin compile about 3. The test task
was last run green in this round (`:app:testDebugUnitTest`, 2m06s offline).

If `%LOCALAPPDATA%\Temp\phonix-toolchain` has been cleaned up, a stable copy of the same toolchain
(JDK 21, Gradle 9.3.1, SDK platform 36.1 + build-tools 36.1.0, platform-tools) lives in
`~/android-toolchain`, with `sdk.dir` written to the gitignored `local.properties`. The Gradle
module cache in `~/.gradle/caches` already holds every dependency this build needs.

## Decisions taken (2026-11-02)

The owner answered the five open questions, and added a hardware constraint afterwards. This section is the answer, so a session does not ask
again; the reasoning is kept short and the evidence is linked.

1. **Phone Screen input - read-only stays.** Touch works on the mirrored phone in the owner's setup,
   so the window is usable as a viewer. An `AccessibilityService` opt-in is only worth revisiting if
   a display arrives on which touch does not work at all. Queue item 6 is conditional, not a task.
2. **Update channel - a Phonix-hosted HTTPS manifest.** The repository stays private; the feed is one
   `update.json` on a static host. It is still digest-checked and unsigned, so the trust boundary is
   unchanged: whoever controls the host can push an APK, and only builds signed with
   `debug.keystore` will install. **This unblocks queue item 1.**
3. **Sealing - deferred, not decided.** DeX and lock task do not combine (Samsung documents it), so
   the choice forks the product: an in-app supervisor PIN keeps the desktop, kiosk removes it. Decide
   before selling any unit; until then the install ships unlocked and says so.
4. **Hardware - stay on this SM-G973U (API 31).** Every measurement in this record is on it. The
   API 36/37 large-screen rules stay untested until a newer unit exists; do not write layout rules
   for them from documentation alone.
5. **Model size - stay at 1.5B.** The owner's actual want was not a larger model but an offline
   *library* in three sizes, removable again. That is now queue item 2 and
   [ROADMAP.md](ROADMAP.md) section "Offline library".

6. **Build machine - modest, so nothing large is staged here.** The owner reports no great hardware;
   measured: 12th-gen i5-1245U, 10 cores, 15.7 GB RAM, **27.6 GB free** on the system drive
   [measured 2026-11-02]. Building is not the constraint - this machine compiles the APK and ran the
   distro switch tests - disk is. A 16 GB or 85 GB pack cannot be downloaded, generated or mirrored
   here. Consequence: content packs stream from the upstream public mirror to the phone, and no multi-gigabyte staging step
   belongs in the build.

7. **No hosting service, and open source.** "I need to host literally nothing" on a company laptop
   with no accounts on it, and the GitHub account is the owner's own. So the distribution is the
   owner's own repository: source, release APK, and `update.json` as a release asset if the feed is
   ever revived. Content comes from upstream - Kiwix for packs, the distro mirrors for rootfs - so
   the project publishes no multi-gigabyte asset and pays for no bandwidth. Publishing the source is
   not blocked by anything sensitive: `git ls-files` holds no keystore, no `.env` and no secret, and
   the only binary is `app/src/main/assets/alpine-rootfs.tgz` at 4.1 MB [measured]. Two things do
   block a store-style publication, and neither is urgent: the AI Studio application ID, and the
   bundled rootfs. Both are written up in [ROADMAP.md](ROADMAP.md) section "Open-source route". The licence is Apache-2.0, added as `LICENSE` the same day, so a fork - and a unit running
   one - may be sold; the bundled Alpine userspace keeps its own licences in
   `docs/THIRD-PARTY-NOTICES.md`.


## Still open

1. **Distribution channel - parked.** Sideload plus the owner's own GitHub is the channel; Play is
   out on policy (self-update and `REQUEST_INSTALL_PACKAGES`), and F-Droid is optional and would cost
   an application-ID change plus a flavour that ships no rootfs. Nothing here blocks a build or a
   test, so no decision is owed. The researched constraints stay on file in
   [ROADMAP.md](ROADMAP.md) sections "Store distribution" and "Open-source route".
2. **Offline library contents and reader.** Tier sizes are measured; which packs belong in each tier,
   and whether the reader is embedded (GPLv3 - see ROADMAP) or handed to an installed Kiwix app, are
   not decided.
3. **Sealing**, per item 3 above, before the first sale.

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
