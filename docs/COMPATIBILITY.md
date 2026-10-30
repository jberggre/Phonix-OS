# Desktop mode on other phones

Phonix is an Android app with a desktop of its own, and it depends on the phone providing an
**external display** that apps can draw on. That capability is not universal: it is the part
Samsung calls DeX. This page records what the other brands actually offer, what we verified,
and what remains unverified, so the compatibility list we put on a box is honest.

Last checked: October 2026. Prices, model lists and software versions change; confirm the
exact regional SKU before promising a customer anything.

**Current app status (2026-10-29, Phonix 1.1):** the S10 unit was re-verified in phone mode and
in simulated 1080p/mdpi metrics; a real DeX workspace was tested in earlier sessions, not in
the 1.1 session. The measured 1.1 gaps — fixed 850 px window clamp, no resize grip, landscape
icon clipping, and an off-screen window after a metric change — are app-side and affect every
desktop mode, not only DeX. They are recorded in [VERIFICATION.md](VERIFICATION.md) §2 and §3.

## Quick comparison

| | Samsung DeX | Google Pixel desktop mode | Motorola Mobile Desktop (Smart Connect) | LG Screen+ |
|---|---|---|---|---|
| Status | Shipping, long-standing | Stable since Android 16 QPR3 (March 2026) | Shipping, maintained (Sept 2026 app update) | Legacy; LG exited phones in 2021 |
| Display connection | USB-C → HDMI (wired) or wireless | USB-C DisplayPort Alt Mode | Wired on some models, wireless on others, PC-only on others | Wired on select models |
| Separate desktop session | Yes | Yes | Yes | Yes |
| Phone screen can sleep | Yes (DeX runs its own virtual display) | Reported yes on QPR3 | External display has its own timeout | Claimed by accessory vendors, not confirmed by LG |
| Third-party app can replace the desktop shell | No | No | No | No |
| Third-party app can auto-launch on connect | Not by public API | Not by public API | No documented way | No |

## What this means for Phonix

- **The app draws its own desktop surface, but finding the right surface is vendor-specific on
  Samsung.** An earlier version of this page claimed Phonix "does not need DeX-specific APIs".
  That is no longer true and has been removed. DeX does not publish its workspace in
  `DisplayManager.getDisplays()`; it exposes a virtual display named `Desktop` under the vendor
  category `com.samsung.android.hardware.display.category.DESKTOP`. Phonix resolves that
  category and waits for it, because launching on the raw HDMI output composes the window
  *under* DeX's presentation (invisible on the monitor) and moving it afterwards triggers
  DeX's "already running on another display" dialog. On other vendors the code falls back to
  the physical presentation display after a grace period; that fallback has only been exercised
  on Samsung hardware, so treat non-Samsung desktop mode as untested.
- **Phonix cannot become the system desktop.** On every one of these platforms the taskbar,
  window decorations and wallpaper of the *host* desktop belong to the phone vendor. The Phonix
  window is an app, so on Pixel the user still sees Pixel's taskbar, and on Motorola its own.
  DeX's caption bar is a visible example: we asked the window manager to hide it and it stayed.
- **Auto-launch is possible only through our own service** (the one Phonix already ships: it
  watches for a display and starts the activity there). Using Android's display APIs is
  supported; replacing the system desktop is not.

## Pixel desktop mode

- Generally available with **Android 16 QPR3** (March 2026); Android 17 continues it.
- Google names the **Pixel 8, 9 and 10 series**. The phone needs **DisplayPort Alt Mode over
  USB-C**; a plain USB-C cable or a charge-only hub is not enough.
- On connect the phone offers **Desktop** or **Mirror**; choosing Desktop with "don't ask
  again" makes it automatic.
- It is a desktop session on the external display, with a taskbar and movable, resizable
  windows, not screen mirroring. Reporting says the phone display can sleep while work
  continues on the monitor.
- Apps do not need changes just to appear, but adaptive layouts, keyboard/mouse handling and
  resizability make a large difference. Android 17 requires large-screen resizability for apps
  targeting it.
- We could not verify a model-by-model list beyond the series names, or any supported way for
  an app to substitute its own shell for Pixel's desktop.

Sources: [Google Android Developers — connected displays (March 2026)](https://android-developers.googleblog.com/2026/03/android-devices-extend-seamlessly-to.html),
[Android Help — desktop windowing](https://support.google.com/android/answer/16928875?hl=en),
[Android Authority — Pixel desktop vs DeX](https://www.androidauthority.com/pixel-desktop-vs-samsung-dex-3647938/),
[Ars Technica — Pixel 8 DisplayPort support](https://arstechnica.com/gadgets/2024/06/googles-pixel-8-series-finally-supports-displayport/).

## Motorola

- **Mobile Desktop**, the current name for what was **Ready For**, part of **Smart Connect**.
  Motorola's Play listing still describes a full desktop workspace and was updated in September
  2026, so the software is maintained.
- Compatibility is model-specific and the distinction matters: Motorola's chart separates
  **wired display**, **wireless display** and **PC** support. Several recent Edge models
  (for example Edge 50 / Edge 60 non-Pro variants) are listed as wireless-only for external
  displays, which is not enough for an HDMI mini PC.
- Wired-capable examples from Motorola's chart include Edge 20 Pro, Moto G100, Edge 30 Pro,
  Edge 40 Pro, Edge 50 Pro/Ultra and Edge (2024). Verify the exact SKU.
- Motorola's help text says the external display has its own screen timeout and that the phone
  locks only after both displays time out.
- No documented way for a third-party app to replace Mobile Desktop or auto-launch itself.

Sources: [Motorola Smart Connect compatibility chart](https://en-us.support.motorola.com/app/answers/detail/a_id/160549/~/smart-connect-compatibility-chart),
[Motorola Ready For announcement](https://motorolanews.com/unleash-your-smartphone-experiences-with-ready-for-platform-now-wirelessly-and-with-pcs/),
[Smart Connect on Google Play](https://play.google.com/store/apps/details?id=com.motorola.mobiledesktop).

## LG

- Select LG phones gained a DeX-like desktop with **Android 10**; on Android 11-era software it
  was branded **Screen+**. Reported models include G8 ThinQ, V50 ThinQ, V60 ThinQ, Velvet and
  Wing, usually wired over USB-C.
- LG closed its mobile business in 2021. Screen+ still runs on existing phones but is not a
  platform anyone should build a product line on.
- No supported third-party shell replacement or auto-launch mechanism was found.

Sources: [Android Central — LG Android 10 desktop mode](https://www.androidcentral.com/lgs-android-10-update-came-surprise-desktop-mode),
[GSMArena — models with desktop mode](https://www.gsmarena.com/lg_included_desktop_mode_with_its_android_10_update-news-41151.php),
[LG — closing the mobile business](https://www.lg.com/global/newsroom/news/corporate/lg-to-close-mobile-phone-business-worldwide/).

## Extra displays: mirror, extend, or a second app window

These are different outcomes and should not be sold as the same feature:

- **Mirror** — the second screen shows the phone's image (Smart View, most Cast routes, DisplayLink
  Presenter).
- **Extend** — one workspace continues across screens. Native DeX on this S10 does **not** provide
  a supported two-external-monitor extended desktop; a multi-HDMI hub does not create one.
- **Second app window** — Phonix draws its own content on a display Android exposes. This is the
  realistic app-level route.

| Route on the S10 | What it actually gives | Verdict |
|---|---|---|
| USB-C → HDMI + DeX | One external DeX workspace; the phone remains separately usable | Best first test |
| Wireless DeX / Miracast | One DeX workspace over Wi-Fi to a Miracast receiver | One display only |
| Smart View | Mirrors the phone screen | Not an extension |
| Chromecast / Google Cast | Media sender/receiver, not an Android desktop | Not a monitor route |
| `DisplayManager` + `Presentation` | App-owned UI on a secondary display Android exposes | Best app-level prototype |
| `ActivityOptions.setLaunchDisplayId()` | Launches a Phonix activity on an existing display (API 26+) | Needs a separate activity/instance path and a real display test |
| `MediaProjection` + `VirtualDisplay` | Captures/streams a display; not a free independent UI canvas | Streaming feature, not an extra desktop |
| scrcpy `--new-display` | PC-rendered Android virtual display running Phonix; ADB required, dies with scrcpy | Useful bench experiment, not an appliance feature |
| DisplayLink adapter | Vendor documents Android mirror mode, one display up to 1920×1080 | Not DeX extension |
| Multi-HDMI dock | Community reports mirroring/one active output, not two DeX desktops | Do not promise |

Samsung documents the S10 connecting to one TV/monitor by USB-C-to-HDMI, DeX Pad or DeX Station,
and says the phone can still be used while DeX runs. It also says apps generally run on only one
screen at a time and caps DeX at five apps. A dock with two HDMI outputs therefore does not give
two independent DeX desktops on this unit. The Android 16/17 connected-display desktop experience
is an OS feature on supported newer devices; it cannot be retrofitted to Android 12.

**Next experiments, in order:**

1. Connect a real USB-C-to-HDMI monitor and run Phonix in DeX; verify input, resize and the
   caption bar before adding display features.
2. Add a `DisplayManager` listener and prototype a `Presentation` with a separate Compose surface
   for a second displayed surface. Do not assume the DeX workspace display is the raw HDMI one;
   the existing `DesktopAutoLaunch` works around Samsung's separate `…DESKTOP` category.
3. Test `scrcpy --new-display=1920x1080 --start-app=com.aistudio.phonix.vpxmkr` on the bench PC
   as a development-only virtual display. It does not create a physical DeX output.
4. Only after (1)–(3) decide whether “second display” means a shared state control surface, a
   separate module pane, or a duplicated desktop. They need different UI and input models.

**Not verified on this unit:** `Presentation`/`launchDisplayId` against Samsung's DeX display,
whether a second Phonix instance can run on phone + external display simultaneously, and any
route to two independent external monitors. Treat community dual-output reports as claims to test,
not specifications.

Sources: [Android connected displays](https://developer.android.com/develop/adaptive-apps/guides/support-connected-displays),
[`Presentation`](https://android.googlesource.com/platform/frameworks/base/+/b267554/core/java/android/app/Presentation.java),
[Media projection](https://developer.android.com/media/grow/media-projection),
[scrcpy](https://github.com/Genymobile/scrcpy),
[Samsung DeX compatibility](https://www.samsung.com/us/support/answer/ANS10001955/),
[Samsung dual-output discussion](https://r1.community.samsung.com/t5/samsung-dex/dual-monitor-output-for-dex/td-p/14136930)
(community report, not a specification).

## Phone Screen capture (not an extra display)

Phonix now has a separate **Phone Screen (read-only)** window, restored on 2026-10-29. It uses
`MediaProjectionManager.createScreenCaptureIntent()` and a `mediaProjection` foreground service to
show the **default display**; it does not extend the desktop or create a second workspace.

- Android’s capture-consent dialog is mandatory. The token is per session and is lost when the
  service stops or the unit reboots; there is no public device-owner bypass.
- The window is read-only. Android does not let an ordinary app inject touches into another app’s
  window without an AccessibilityService, and Phonix does not ship one.
- On the phone’s own panel the captured window necessarily contains itself. The useful case is
  viewing the default display from DeX or another surface. **Capture while DeX is on a real
  monitor was not tested** because no monitor was attached in this session.
- Measured on the S10: 540×1140 first frame, foreground notification 4711, LIVE → OFF with
  `dumpsys media_projection` showing `null` after Stop. See [VERIFICATION.md](VERIFICATION.md) §11.
- On a shattered/headless unit, network ADB can accept the system prompt once with
  `adb shell input tap` (use `uiautomator dump` for the current coordinates). That is a bench
  workaround, not a shipping provisioning mechanism.

## 1.2 compatibility work and Pixel 9 Pro Fold test matrix

The current worktree contains an **unbuilt 1.2 candidate**. It adds WindowManager fold-posture
reporting, width-responsive shortcut placement, window normalization/resize behavior, and explicit
opt-in gates for host-device changes. These are implementation notes, not verification claims:
no APK has been built from this candidate and no Pixel 9 Pro Fold test has been run.

When a build is available, use this non-destructive phone checklist:

1. Install/launch normally and choose **Daily driver**; verify the wallpaper, Linux guest,
   background service, boot startup and display watcher stay off unless individually enabled.
   Do not enroll device owner or run `provision.sh`/`dpm set-device-owner`.
2. Open Phonix on the cover screen, unfold to the inner display, rotate, fold to tabletop, and
   return to the cover. Confirm the report changes from phone/large window to unfolded/half-opened
   when Android reports that posture, and that shortcut columns reflow without clipping.
3. Open, drag, resize and maximize a desktop window on both cover and inner displays. Confirm its
   title controls remain reachable after each posture/rotation/window-size change.
4. Re-run the same checks with **Cyberdeck** selected only if desired. Save each choice explicitly;
   confirm immersive bars and display watching apply only after save, and switch back to Daily
   driver to confirm the watcher stops and phone-screen bars return.
5. With a compatible USB-C DP Alt Mode adapter and display, record Android's actual prompt and
   select **Desktop** or **Mirror** as offered. Verify whether the external screen is a separate
   desktop or a mirror; Phonix's attached-display report cannot prove the USB-C link mode.
   Disconnect/reconnect once and record whether window placement and input recover.
6. Record Android version/build, Phonix versionCode, cover/inner widths, observed posture, chosen
   external-display mode, keyboard/mouse results, crashes and battery/thermal behavior. Do not use
   `wm size`, `wm density`, factory reset, device-owner commands or destructive provisioning on
   the personal Pixel during this test.

**The candidate now builds: versionCode 3 / versionName 1.2, and it was installed in place on the
S10 bench unit (Android 12, API 31) with the unit tests green (21 tests).** That does not advance
the Pixel rows below. An emulator can exercise WindowManager fold features, but it cannot establish the
Pixel's actual USB-C route or desktop-vs-mirror behavior, so the physical Pixel matrix stays
pending.

### Pre-flight readings, Pixel 9 Pro Fold (read-only)

Recorded over ADB before any install on the personal Pixel. Nothing on the phone was changed.

| Reading | Value |
|---|---|
| Model / codename | Pixel 9 Pro Fold (`comet`), manufacturer `Google` |
| OS | Android 17, API 37; the app targets 36 |
| Device state | `cmd device_state state` returns `OPENED`; states include CLOSED, HALF_OPENED, OPENED, REAR_DISPLAY_STATE, CONCURRENT_INNER_DEFAULT, REAR_DISPLAY_OUTER_DEFAULT |
| Inner panel | display 0, "Inner Display", 2076x2152 px, 390 dpi, type INTERNAL, ON |
| Cover panel | display 3, "Outer Display", 1080x2424 px, type INTERNAL, OFF while open |
| Unfolded logical size | 2152x2076 px -> about 532x552 dp, under the 600 dp large-window threshold |
| Desktop on external displays | `force_desktop_mode_on_external_displays=0`, so a monitor mirrors until this is enabled |
| Freeform | `android.software.freeform_window_management` present; `enable_freeform_support` unset |
| Hinge sensor | `android.hardware.sensor.hinge_angle` present |
| Platform Linux | `com.android.virtualization.terminal` and `android.software.virtualization_framework` present |

Two consequences for the test:

1. **A second panel is not a monitor.** The cover panel is an ordinary display id. A launch path
   that accepted "any non-default display that is ON" therefore reported a monitor and would open
   the desktop on the cover screen while the phone was folded. The candidate now requires
   `FLAG_PRESENTATION`, which neither pixel panel carries, and only real HDMI/DP outputs do. That
   is the same flag `DisplayManager` filters its presentation category on.
2. **Unfolded is 532 dp wide, not "large".** Posture, not width, has to drive the foldable layout:
   if `WindowInfoTracker` fails to report a `FoldingFeature`, the classifier falls back to PHONE
   rather than LARGE_WINDOW on this device. Watch for that specifically in step 2 of the checklist.

### DeX capture, S10 with a real monitor

Measured with the bench S10 docked to an external display, app launched with
`am start --display 2`. This is a capture pass, not the full matrix.

| Reading | Value |
|---|---|
| HDMI output | "HDMI Screen", 1920x1080, type EXTERNAL, **FLAG_PRESENTATION**, EDID `ES-G34C5` |
| DeX workspace | display 2, "Desktop", virtual, 3440x1440 at 160 dpi, **no FLAG_PRESENTATION** |
| App window | freeform task at 1920x1080 on the workspace, with DeX's own caption bar (unchanged) |
| Phone panel | display 0 keeps its own Android UI while DeX runs on the monitor |

That last row is the mini-PC split this product depends on, and it also confirms why the
`FLAG_PRESENTATION` discriminator added above is the right one: the physical output carries the
flag, the virtual workspace does not, and `workspaceDisplay()` still finds the workspace through
Samsung's display category rather than through that flag.

Verified in that configuration: the desktop icon set (File Explorer, Terminal, Browser, Settings),
two-line icon captions, taskbar pins including a real App Store glyph, and the OS card / Vitals
widgets in the top right. Captured with `screencap -d 2`.

Still owed on DeX: a dock/undock cycle with the watcher consent enabled, the capability card in the
wizard, the Phone Screen consent flow, and the 24-hour soak.

## What we have not verified

- Motorola's live compatibility table could not be fetched directly during research; the model
  rows above come from its indexed listing and should be re-checked on the support site.
- Whether every Motorola or LG model keeps the external desktop alive with the phone screen
  off. Motorola documents the timeout behaviour; LG does not.
- Pixel 11 external-display support: no authoritative model-specific confirmation was found.
- Any vendor-supported way to make a third-party app the desktop shell. None of the four
  platforms documents one.
