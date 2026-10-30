# Linux on Phonix

Phonix runs a real Alpine Linux userspace inside the app. No root, no repartitioning, no
second OS install, and Android keeps working exactly as Samsung shipped it — which is what
lets a recycled Galaxy act as a mini PC without giving up the phone apps.

**Bench status (2026-10-29, 16:29 build):** the app was updated in place and kept its data, where
the guest and X11 stack live. The guest was started for the local-AI work: `llama-server` was
installed from Alpine community, the pinned 1.5B Q4_K_M model was downloaded and hash-verified,
and it answered at 8.2–17.2 tok/s. The X11 window itself was not re-run in this round (no monitor
attached), so those claims remain prior hardware results. The container-stop path and 24 h soak
remain unverified. Current app-level gaps are in [VERIFICATION.md](VERIFICATION.md) §11.

## What is actually running

`proot` (a ptrace-based syscall emulator) makes the guest believe it is chrooted and running
as root. The guest root filesystem is Alpine 3.24.3 aarch64, unpacked from the APK into
app-private storage on first launch.

| Piece | Path in the repo | Why |
|---|---|---|
| `proot` | `app/src/main/jniLibs/arm64-v8a/libproot.so` | Android 10+ forbids executing from app data, so it must live in `nativeLibraryDir` |
| `proot` loader | `.../libproot_loader.so` | proot injects this into traced processes to intercept `execve` |
| `libtalloc` | `.../libtalloc.so` | Termux proot links against `libtalloc.so.2`; the APK packager only accepts `lib*.so`, so `proot`'s `DT_NEEDED` is patched (`tools/patch_proot_needed.py`) |
| `libandroid-shmem` | `.../libandroid-shmem.so` | memfd/shm shim used by Termux binaries |
| Alpine rootfs | `app/src/main/assets/alpine-rootfs.tgz` | unpacked by `TarGzExtractor`; `.tgz` because AAPT2 rewrites `.tar.gz` |

Runtime code:

- `engine/LinuxContainer.kt` — unpack, guest `resolv.conf`/`hosts`, launch proot, tar reader.
- `engine/LinuxRuntime.kt` — process-wide owner of the shell. The desktop attaches a listener;
  `LinuxServerService` keeps the process alive so closing the desktop does not kill the machine.
- `engine/LinuxDesktop.kt` — starts Xvfb + x11vnc + websockify/noVNC inside the guest.
- `engine/LocalAiEngine.kt` — installs `llama-server`, downloads/verifies the GGUF, starts the
  loopback server, streams chat, and stops it.
- `service/ScreenMirror.kt` — MediaProjection capture for the read-only Phone Screen window; it
  does not run inside the guest.
- `ui/components/LinuxDesktopWindow.kt` — the window; a WebView onto `127.0.0.1:6080`.

## What works today

Verified on a Galaxy S10 (SM-G973U, Android 12) over DeX:

- `uname -a` reports the Android kernel, `/etc/os-release` reports Alpine 3.24.3.
- `apk update` and `apk add htop` install from the network (DNS comes from the phone).
- `free -m` sees the real 7.4 GB, `nproc` the real CPU set.
- The container survives the desktop being closed (foreground service + wake lock).
- Xvfb + x11vnc + websockify/noVNC install and serve the desktop over loopback. Reinstalled
  from scratch on 2026-10-29 after the app data was wiped: `apk` reported `OK: 400 MiB in 184
  packages`, and IceWM, xterm and the Phonix wallpaper came up on the monitor
  (`screenshots/linux-02-desktop.png`).
- IceWM and an xterm come up with the desktop, in the Phonix theme (see below), with DejaVu Xft
  fonts and the Phonix wallpaper on the X root.
- Packages install from the network (`apk add nginx` works), which is also what the Servers
  screen does when you press Install — it runs the real command and then probes `apk`/`pidof`.

## Local model server

The Local AI window installs `llama-server` into the guest on first use:

```sh
apk add --no-cache llama-server
llama-server -m /root/models/qwen2.5-1.5b-instruct-q4_k_m.gguf \
  --host 127.0.0.1 --port 8081 -c 4096 -t 4
```

The model is **not in the APK** and is not started at boot. It is stored in the app-private guest
root at `/root/models`, uses about 1.0 GB on disk and ~1.3 GB RAM while loaded, and serves an
OpenAI-compatible endpoint on loopback only. `/health` is the readiness gate; the app never turns
the badge green from the launch command. Stop with the pid file, not a broad `pkill` pattern.

Measured: Qwen2.5 1.5B Q4_K_M, SHA-256
`1adf0b11065d8ad2e8123ea110d1ec956dab4ab038eab665614adba04b6c3370`, 8.2 tok/s on a 185-token
water answer and 17.2 tok/s on a short one. The first water answer was fluent but conflated boiling
with SODIS; after the safety prompt was strengthened, the final answer recommended a one-minute
boil. The curated notes remain the safety reference.

## PC software (the PC Apps tab)

The **PC Apps** tab installs desktop programs into the guest with one click. It runs
`apk add --no-cache <package>` in the live shell and shows that command's own output. A package
is only marked *Installed* after `apk info -e` confirms it, so the badge reflects the package
manager rather than the button that was pressed.

The catalogue is limited to packages that exist in Alpine 3.24's aarch64 repositories, and each
download size is read from that release's `APKINDEX`, not estimated:

| Package | Download |
|---|---|
| `firefox` | 93 MB |
| `chromium` | 146 MB |
| `thunderbird` | 93 MB |
| `libreoffice-writer` / `-calc` | 4 MB / 10 MB (plus the shared LibreOffice runtime) |
| `gimp` | 20 MB |
| `inkscape` | 8 MB |
| `vlc` / `mpv` | 6 MB / 1 MB |
| `audacity` | 9 MB |
| `geany` / `python3` / `git` | 5 MB / 8 MB / 4 MB |
| `htop` | 1 MB |

Verified on the unit on 2026-10-29: `mpv` installed from the tab and the probe then reported it
installed; `python3` already shows as installed because it is part of the Alpine base.

**What this is not:** x86-64 Windows programs. The guest is aarch64 musl Linux, and no Wine,
Box64 or x86 emulator is installed. A `.exe` will not open. Alpine does carry a Wine build for
ARM64 Windows binaries, but that is a different, unmeasured path and Phonix does not advertise
it. Software that needs a GPU (Blender's viewport, games) will be slow: Xvfb is a software
framebuffer.

## The Linux Desktop window

Start → search "linux" → **Linux Desktop**. It starts Xvfb (`:1`, 1280×720), x11vnc, websockify and
IceWM inside the guest, then shows the session in a WebView.

The window loads `phonix.html`, which `LinuxDesktop` copies into the guest's noVNC web root. It is
not one of noVNC's own sample pages on purpose:

- the samples ship no `<meta name="viewport">`, so a WebView lays them out at Chromium's 980 px
  fallback and the framebuffer is cropped instead of scaled. The window sets
  `useWideViewPort = true` and our page declares the viewport, so the canvas fits the window
  and letterboxes onto Phonix's background.
- the appliance does not want a top bar, a "Send CtrlAltDel" button or a connect splash.
- the window needs one honest signal: the page publishes `window.__phonix.state`
  (`connecting`/`connected`/`disconnected`) and reconnects itself, so the toolbar can show the
  real state instead of scraping DOM text.

To inspect the guest display from a PC:

```bash
adb forward tcp:6080 tcp:6080
# then open http://127.0.0.1:6080/phonix.html
```

The window logs what it sees under the `PhonixVnc` tag (`adb logcat -s PhonixVnc`), including the
WebView's viewport size at connect time — the quickest way to tell a layout problem from a
connection problem.

## The Phonix look (and how it survives an update)

Stock IceWM is a light, 1990s-looking desktop. `engine/LinuxTheme.kt` writes the Phonix theme
into the guest before IceWM starts, so a fresh container looks right without a manual setup
step:

| File in the guest | What it does |
|---|---|
| `~/.icewm/preferences` | theme name, bottom taskbar, clock format, Xft fonts |
| `~/.icewm/themes/phonix/default.theme` | slate/cyan palette for windows, menus, taskbar |
| `~/.icewm/menu`, `~/.icewm/toolbar` | Terminal entry and "Restart desktop" |
| `~/.icewm/startup` | starts `icewmbg` on every IceWM start |
| `etc/motd` | what the guest is, shown when the terminal opens |
| `root/phonix-wallpaper.jpg` | 1280×720 crop of the Phonix wallpaper, set by `icewmbg` |

The files are compared byte-for-byte on every launch, so an app update that changes the theme
rewrites them and restarts IceWM once; open windows survive because only the window manager
is replaced. The wallpaper ships without the mock status bar that was baked into the original
asset (a fake "Nov 23 16:30" clock), so no false time appears on the Linux desktop.

## Limits worth knowing

- **Software rendering.** Xvfb paints into RAM and VNC encodes frames. Fine for terminals,
  editors, dev tools and light browsers; not a GPU desktop.
- **No PTY.** The guest shell is line-oriented over pipes, so full-screen curses apps that
  demand a terminal (`htop` past its first screen, `vim` in raw mode) degrade. A PTY bridge is
  the next step.
- **DeX home cannot host widgets.** `dumpsys appwidget` shows only the phone launcher has a
  widget host; Samsung's DeX launcher has none. Phonix's own desktop is the widget surface.
- **DeX's window title bar cannot be hidden by an app.** The strip along the top of a maximised
  window (back arrow, pin, minimise/maximise/close) is system decor drawn by DeX around the app.
  Measured on this One UI 4 unit, none of these change it: a dark window theme, forced
  fullscreen windowing (`--windowingMode 1`), `captionBar()` insets, the legacy immersive flags,
  `com.samsung.android.dex.launchwidth/launchheight=0`, or `transient_bar_delay`. It also stays
  light against the dark desktop. Research confirms there is no documented Samsung/Android/Knox
  switch for it: the Android docs say a system caption bar survives immersive apps, and Knox's
  `DexManager` has no window-decoration API. One UI 8 adds a user-operated *Immersive* mode
  (maximise, then long-press maximise) but that is not on this hardware and has no app API.
  Treat ~40 px of chrome as part of the product, or design the top of the desktop to sit under it.
- **Kiosk and DeX do not combine.** Knox documents that DeX is not supported in kiosk/lock-task
  mode, so lock-task cannot be used to strip DeX's chrome either.
- **DeX wallpaper is separate** from the phone wallpaper and needs Samsung's own
  "Set as Samsung DeX wallpaper" confirmation once per unit. The app sets the phone's home
  wallpaper (`WallpaperManager`), but `setBitmap` rejects the extra desktop flag with "Must
  specify a valid wallpaper category", so the DeX background stays a manual provisioning
  step; there is no public API for it.

## Licensing (read before selling units)

Phonix's own code is unaffected, but these binaries are shipped inside the APK:

| Component | Licence | Obligation |
|---|---|---|
| `proot` | GPL-2.0 | Separate executable, invoked — not linked into Phonix. Still needs a source offer. |
| busybox / Alpine rootfs | GPL-2.0 and others | Source is published by Alpine; include a written offer or link in the box. |
| `libtalloc` | LGPL-3.0 | Dynamic library, replaceable; keep its licence notice. |
| `libandroid-shmem` | MIT | Include copyright notice. |
| noVNC | MPL-2.0 | Unmodified, served from the guest. |
| `llama.cpp` / `llama-server` | MIT | Optional runtime package installed from Alpine, not bundled. |
| Qwen2.5 1.5B weights | Apache-2.0 | Downloaded at the customer’s request, not shipped in the APK. |

A `THIRD-PARTY-NOTICES` file with links to each project's source satisfies the common case;
check with counsel before volume shipping.

## Rebuilding the runtime

```bash
python tools/deb_extract.py proot.deb out data/data/com.termux/files/usr/bin/proot
python tools/deb_extract.py proot.deb out data/data/com.termux/files/usr/libexec/proot/loader
python tools/deb_extract.py libtalloc.deb out data/data/com.termux/files/usr/lib/libtalloc.so.2.5.0
python tools/deb_extract.py libandroid-shmem.deb out data/data/com.termux/files/usr/lib/libandroid-shmem.so
python tools/patch_proot_needed.py out/proot libtalloc.so.2 libtalloc.so
# then copy into app/src/main/jniLibs/arm64-v8a under the names in the table above
```

Alpine rootfs: `https://dl-cdn.alpinelinux.org/alpine/v3.24/releases/aarch64/alpine-minirootfs-3.24.3-aarch64.tar.gz`
(SHA-256 `4d3f80afaebd447c5591a63a73bde380a149fe8b2d85488cafabb2ff97551f50`), saved as
`app/src/main/assets/alpine-rootfs.tgz`.
