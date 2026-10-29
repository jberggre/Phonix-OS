# Linux on Phonix

Phonix runs a real Alpine Linux userspace inside the app. No root, no repartitioning, no
second OS install, and Android keeps working exactly as Samsung shipped it — which is what
lets a recycled Galaxy act as a mini PC without giving up the phone apps.

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
- `ui/components/LinuxDesktopWindow.kt` — the window; a WebView onto `127.0.0.1:6080`.

## What works today

Verified on a Galaxy S10 (SM-G973U, Android 12) over DeX:

- `uname -a` reports the Android kernel, `/etc/os-release` reports Alpine 3.24.3.
- `apk update` and `apk add htop` install from the network (DNS comes from the phone).
- `free -m` sees the real 7.4 GB, `nproc` the real CPU set.
- The container survives the desktop being closed (foreground service + wake lock).
- Xvfb + x11vnc + websockify/noVNC install and serve the desktop over loopback.
- IceWM and an xterm come up with the desktop, in the Phonix theme (see below), with DejaVu Xft
  fonts and the Phonix wallpaper on the X root.
- Packages install from the network (`apk add nginx` works), which is also what the Servers
  screen does when you press Install — it runs the real command and then probes `apk`/`pidof`.

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
  window (back arrow, pin, minimise/maximise/close) is system decor drawn outside the app's
  window. Measured on One UI 4: a dark window theme, forced fullscreen windowing,
  `captionBar()` insets and the legacy immersive flags all leave it in place, and it stays light
  against the dark desktop. One UI 8 has a hidden immersive mode; this hardware does not. Treat
  ~40 px of chrome as part of the product, or design the top of the desktop to sit under it.
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
