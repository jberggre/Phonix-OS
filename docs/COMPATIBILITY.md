# Desktop mode on other phones

Phonix is an Android app with a desktop of its own, and it depends on the phone providing an
**external display** that apps can draw on. That capability is not universal: it is the part
Samsung calls DeX. This page records what the other brands actually offer, what we verified,
and what remains unverified, so the compatibility list we put on a box is honest.

Last checked: October 2026. Prices, model lists and software versions change; confirm the
exact regional SKU before promising a customer anything.

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

## What we have not verified

- Motorola's live compatibility table could not be fetched directly during research; the model
  rows above come from its indexed listing and should be re-checked on the support site.
- Whether every Motorola or LG model keeps the external desktop alive with the phone screen
  off. Motorola documents the timeout behaviour; LG does not.
- Pixel 11 external-display support: no authoritative model-specific confirmation was found.
- Any vendor-supported way to make a third-party app the desktop shell. None of the four
  platforms documents one.
