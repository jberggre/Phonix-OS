# Phonix desktop wallpaper brief

This is the brief for the **desktop background art** (the old icon brief is retired: the icon set
shipped in `app/src/main/res/drawable-nodpi/`). It covers what every preset is supposed to look
like, what to draw if new artwork is commissioned, and how the "Your Photo" wallpaper behaves.

The wallpapers are not a gallery of unrelated pictures. Each one is a nod to something the project
actually is or actually uses, which is why the set is short.

## What ships today

Every preset below is drawn by the app from gradients, type and the existing brand art — no new
image files are required for any of them, and none of them claims to be a photograph.

| Preset | What it looks like | The nod |
|---|---|---|
| **Phoenix Cybernetic** (default) | Bundled `img_phoenix_wallpaper.png` | The project itself: a written-off phone flying again |
| **Ember Mark** | Near-black with a soft amber heat glow, the phoenix head mark centered, `PHONIX` letterspaced underneath | Reclaimed silicon, heat, the amber accent |
| **Alpine Peak** | Deep teal-to-near-black vertical gradient, the green chip-mountain icon centered, caption `ALPINE LINUX IN A PROOT` | Alpine Linux and musl, which the guest really runs |
| **Terminal Green** | `#030A05` background, a monospace prompt `root@phonix:~#` and a dimmer `uname -a · apk add · python3` line | Unix itself; the guest's own package manager is `apk` |
| **Retro PC Classic Teal** | Flat `#008080` | The 1990s desktop that every "make an old computer useful" project is descended from |
| **Obsidian Black** | Flat `#000000` | OLED: black pixels cost nothing, which matters on a phone kept plugged in |
| **Your Photo** | The user's own picture, cropped to fill, with a 20% black scrim so captions stay readable | Nothing — it is the user's |

### Naming and honesty rules

- A preset may only name software the app ships bundled out of the box. "Alpine" is fair because
  Alpine 3.24 ships inside the APK (`assets/alpine-rootfs.tgz`); Debian, Ubuntu, and Arch are
  downloaded on demand when selected by the user rather than bundled in the APK.
- No preset may imply a capability: no fake terminals printing commands nobody ran, no fake
  desktop icons, no "root" prompt on a screen where the guest is stopped. The Terminal Green
  wallpaper is wallpaper — it is a static prompt, and the caption is a list of real commands, not
  output.
- Keep the default wallpaper's filename (`img_phoenix_wallpaper.png`) stable:
  `WallpaperProvisioner` applies it to the Android home screen once per bundle version, and the
  desktop draws the same file.

## Commissioning new backgrounds

If new artwork is commissioned, these are the delivery rules. They exist so a new image drops in
without touching layout code.

- **Aspect:** 2560 × 1600 (16:10) master, safe to crop to 16:9. The Fold's inner screen is close
  to square, so keep the subject inside the middle 60% or it will be cropped away there.
- **Format:** PNG or high-quality JPEG, sRGB, no text baked in unless the text is the subject.
  Anything the app must read out loud belongs in the code, not in the pixels.
- **Dark by default:** icons and white captions sit on top. Keep the area behind the bottom-left
  quadrant (where the default shortcuts land) at or below about 20% luminance.
- **No logos you do not own.** No Windows, Apple, Samsung, DeX, Google or distro logos. Nods to
  open-source projects should be drawn, not copied from their trademarks — the Alpine nod here is
  a mountain in a chip package, not Alpine's actual mark.
- **File name:** `img_wallpaper_<preset>.png` in `app/src/main/res/drawable-nodpi/`, then add one
  `DesktopWallpaper` entry and one branch in `DesktopWallpaperBackground`.

## Your Photo

- Chosen through the system photo picker from **Personalize → Desktop Wallpaper → Choose a Photo**.
- The app copies the picture into its own private storage and downscales it to at most 1600 px on
  the long edge before writing a JPEG (`util/WallpaperStore.kt`). The original is never modified.
- It is never uploaded. There is no network call in the import path.
- It is removed with the app on uninstall, along with the rest of `filesDir`.
- Switching to another preset keeps the imported file so the user can switch back without picking
  it again; picking a different photo overwrites the stored copy.

## Not in scope

The Android **system** wallpaper is a separate thing: `WallpaperProvisioner` writes the bundled
Phoenix image to the home screen once, and only when setup's device-change switch allows it. A
custom photo does not touch the system wallpaper.
