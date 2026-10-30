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

## What we have not verified

- Motorola's live compatibility table could not be fetched directly during research; the model
  rows above come from its indexed listing and should be re-checked on the support site.
- Whether every Motorola or LG model keeps the external desktop alive with the phone screen
  off. Motorola documents the timeout behaviour; LG does not.
- Pixel 11 external-display support: no authoritative model-specific confirmation was found.
- Any vendor-supported way to make a third-party app the desktop shell. None of the four
  platforms documents one.
