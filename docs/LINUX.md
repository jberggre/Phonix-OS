# Linux on Phonix

Phonix runs a real Linux userspace inside the app via `proot`. No root, no repartitioning, no
second OS install, and Android keeps working as shipped — which is what lets a daily-driver phone,
foldable, or recycled Galaxy motherboard act as a Linux workstation without giving up Android apps.

**Status (2026-11-09 workstation update + 2026-10-29 hardware bench):**
- **Hardware Bench Record (2026-10-29, 16:29 build on SM-G973U):** Alpine 3.24.3 extracted from the APK, `llama-server` installed from Alpine community, the pinned Qwen2.5 1.5B Q4_K_M model was downloaded and SHA-256 verified, and it answered at 8.2–17.2 tok/s. Xvfb + x11vnc + websockify + IceWM + xterm were reinstalled from scratch (`OK: 400 MiB in 184 packages`) and verified on display (`screenshots/linux-02-desktop.png`).
- **2026-11-09 Workstation Expansion (Code & JVM Unit Verified):**
  1. **Multi-Distro Rootfs Engine:** Alpine 3.24 remains the bundled offline default (`assets/alpine-rootfs.tgz`). Users can also choose **Debian 12 (Bookworm)**, **Ubuntu 24.04 LTS (Noble)**, **Arch Linux ARM**, or **Kali Linux** in the Setup Wizard (Step 3) or the **Linux System** tab. Selecting a non-bundled distro streams its official ARM64 rootfs archive over HTTPS into `filesDir/linux/<distroId>` and extracts `.tar.gz` or `.tar.xz` on device.
  2. **Distro-Aware Package Management:** `LinuxContainer` and `PhonixViewModel` translate package install, remove, and probe commands across `apk` (Alpine), `apt-get` / `dpkg -s` (Debian/Ubuntu/Kali), and `pacman` (Arch).
  3. **Live X11 Desktop Environment Switcher (`DISPLAY=:1`):** Switch between **IceWM** (~35 MB RAM), **XFCE4** (`xfce4-session`, ~140 MB RAM), **Openbox** (~25 MB RAM), and **LXQt** (`startlxqt`, ~110 MB RAM) from the Linux Desktop window header.
  4. **Desktop Shortcuts & `phonix-pin` CLI:** Every provisioned rootfs receives `/usr/local/bin/phonix-pin`. Running `phonix-pin "Title" "command" [--gui]` emits a `PHONIX_PIN:` protocol line intercepted by `PhonixViewModel` to create a persistent desktop shortcut on the Phonix Desktop. Installing packages in the terminal also triggers an automatic 1-click **Pin to Desktop** banner.
  5. **Multi-Tab Terminal & Termux Extra Keys:** Independent terminal tabs (`+` / `×`), per-tab command history (`↑` / `↓`), live guest `TAB` completion, `Ctrl+C` (`pkill` child processes), and `Ctrl+D`.

## What is actually running

`proot` (a ptrace-based syscall emulator) makes the guest believe it is chrooted and running
as root (`uid 0`). Each distribution lives in its own isolated rootfs directory under
`context.filesDir/linux/<distroId>` (e.g., `files/linux/alpine`, `files/linux/debian`, `files/linux/ubuntu`, `files/linux/arch`).

| Piece | Path in the repo | Why |
|---|---|---|
| `proot` | `app/src/main/jniLibs/arm64-v8a/libproot.so` | Android 10+ (`W^X`) forbids executing binaries from writable app data, so `proot` lives in read-only `nativeLibraryDir` |
| `proot` loader | `.../libproot_loader.so` | `proot` injects this into traced guest processes to intercept `execve` |
| `libtalloc` | `.../libtalloc.so` | Termux `proot` links against `libtalloc.so.2`; the APK packager only extracts `lib*.so`, so `proot`'s `DT_NEEDED` is patched (`tools/patch_proot_needed.py`) |
| `libandroid-shmem` | `.../libandroid-shmem.so` | System V shm shim used by X11 and desktop binaries on Android |
| Alpine rootfs (bundled) | `app/src/main/assets/alpine-rootfs.tgz` | Unpacked offline by `TarGzExtractor`; named `.tgz` so AAPT2 does not gunzip it during APK packaging |
| On-demand distros | Downloaded into `files/linux/<distroId>/` | Official ARM64 rootfs tarballs streamed over HTTPS only when the user explicitly selects them |

### Supported Distributions & Rootfs Sources

| Distribution | ID | Delivery | Package Manager | Rootfs Archive Format & Extraction Path |
|---|---|---|---|---|
| **Alpine Linux 3.24** | `alpine` | **Bundled in APK** (`assets/alpine-rootfs.tgz`, ~3.4 MB) | `apk` | Extracted offline in ~2s via Kotlin `TarGzExtractor` |
| **Debian 12 (Bookworm)** | `debian` | On-demand HTTPS download (`images.linuxcontainers.org` arm64 cloud rootfs) | `apt` (`apt-get` / `dpkg`) | `.tar.xz` extracted via bundled Alpine `proot` + `busybox tar -xJf` helper (with `.tar.gz` fallback) |
| **Ubuntu 24.04 LTS (Noble)** | `ubuntu` | On-demand HTTPS download (`cdimage.ubuntu.com/ubuntu-base` arm64) | `apt` (`apt-get` / `dpkg`) | `.tar.gz` extracted via streaming `TarGzExtractor` |
| **Arch Linux ARM** | `arch` | On-demand HTTPS download (`os.archlinuxarm.org` aarch64 latest) | `pacman` | `.tar.gz` extracted via streaming `TarGzExtractor` |
| **Kali Linux Rolling** | `kali` | On-demand HTTPS download (Debian ARM64 rootfs base + `apt`) | `apt` (`apt-get` / `dpkg`) | `.tar.xz` / `.tar.gz` streaming extractor |

Runtime code:

- `engine/LinuxContainer.kt` — unpacks bundled Alpine or downloads/extracts on-demand distros (`ensureRootfsForDistro`), configures `/etc/resolv.conf`, `/etc/hosts`, `/etc/profile`, writes `/usr/local/bin/phonix-pin`, and provides distro-aware `installPackagesCommand`, `removePackageCommand`, and `isPackageInstalledCommand` helpers.
- `engine/LinuxRuntime.kt` — process-wide owner of the persistent `/bin/sh` session inside `proot`, supporting switching active distros (`start(context, distro)`). `LinuxServerService` keeps the process alive when configured.
- `engine/LinuxDesktop.kt` — manages the X11 stack (`Xvfb :1` + `x11vnc` + `websockify`/`noVNC`) and supports switching between 4 window managers (`LinuxDesktopEnvironment`: `ICEWM`, `XFCE4`, `OPENBOX`, `LXQT`).
- `engine/LocalAiEngine.kt` — installs `llama-server`, downloads/verifies the Qwen2.5 1.5B GGUF, starts the loopback server, streams chat, and stops it.
- `service/ScreenMirror.kt` — `MediaProjection` capture for the read-only Phone Screen window; runs on the Android host.
- `ui/components/LinuxDesktopWindow.kt` — the X11 desktop window with live Desktop Environment selector chips (`IceWM`, `XFCE4`, `Openbox`, `LXQt`), `+ xterm` launcher, and WebView onto `127.0.0.1:6080`.
- `ui/components/HelpDocsScreen.kt` — in-app interactive Help & Technical Guide tab (`AppNavTab.HELP`).

## Multi-Tab Terminal, Extra Keys & `phonix-pin`

The Terminal window provides:
- **Multi-Tab Sessions:** Create new shell tabs with `+`, switch tabs without losing output scrollback, and close tabs with `×`.
- **Termux-Style Extra Keys Bar:**
  - `↑` / `↓`: Navigate per-tab command history (`navigateTerminalHistory`).
  - `TAB`: Queries the running guest shell (`compgen` / `/bin` / `/usr/bin` / path matching) to complete partial commands or file paths in the input field.
  - `Ctrl+C`: Sends `pkill -INT` to active child jobs inside the `proot` session and resets the input prompt.
  - `Ctrl+D`: Sends `exit` or closes the active secondary tab.
  - Quick symbols: `~`, `/`, `-`, `|`, `&&`, `>`.
- **`/usr/local/bin/phonix-pin` CLI & Auto-Detection Banner:**
  - Inside any terminal tab, run:
    ```sh
    phonix-pin "My Backup Script" "sh /root/backup.sh"
    phonix-pin "Firefox Browser" "firefox" --gui
    ```
  - `phonix-pin` prints `PHONIX_PIN:<Title>|<Command>|<cli|gui>`, which `PhonixViewModel` intercepts to add a shortcut to the Phonix Desktop (`DesktopPrefs`).
  - Additionally, running `apk add <pkg>`, `apt install <pkg>`, or `pacman -S <pkg>` in the terminal automatically displays a 1-click **Pin to Desktop** banner above the input bar.

## What works today (Hardware Bench + Automated Verification)

Verified on a Galaxy S10 (SM-G973U, Android 12) over DeX:

- `uname -a` reports the Android kernel, `/etc/os-release` reports the guest distribution.
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
  screen does when you press Install — it runs the real command and then probes the package manager / `pidof`.

## Samba SMB (`:4445`), Pi-hole DNS Sinkhole (`:5353`) & Docker (`udocker` / QEMU)

Every guest rootfs provisioned by `LinuxContainer.writeGuestConfig` includes three built-in CLI helpers in `/usr/local/bin` (also accessible via 1-click actions in the **Servers** tab):

1. **`phonix-smb` (`samba` / `smbd` on port `4445`):**
   - Unrooted Android blocks port `445` (`< 1024`). `phonix-smb start` writes `/etc/samba/smb.conf` bound to port `4445` (`smb ports = 4445`) with guest access enabled on `/root/shared` and starts `smbd -D`.
   - Connect from any LAN device via `smb://<LAN-IP>:4445/phonix`.
2. **`phonix-adblock` (Pi-hole's `dnsmasq` DNS sinkhole engine on port `5353`):**
   - Standard Pi-hole's `curl -sSL https://install.pi-hole.net | bash` script requires `systemd` and privileged port `53`. However, Pi-hole's underlying DNS sinkhole engine is `dnsmasq` (`FTLDNS`) paired with a hosts blocklist.
   - `phonix-adblock start` configures `/etc/dnsmasq.d/phonix-adblock.conf` on port `5353` backed by `/etc/phonix-adblock.hosts`.
   - `phonix-adblock update` downloads StevenBlack's unified hosts blocklist (130,000+ ad/telemetry domains) into `/etc/phonix-adblock.hosts` and reloads `dnsmasq`.
3. **`phonix-docker` (Termux-style Docker on unrooted Android):**
   - Native `dockerd` requires Linux kernel `cgroups`, `overlayfs`, and `CAP_SYS_ADMIN` namespaces, which Android's unprivileged app sandbox blocks inside `proot`.
   - **Mode A (`phonix-docker setup-udocker`):** Installs `udocker` (`python3` + `curl` + `proot` backend) so users can pull and run OCI / Docker Hub images (`udocker pull alpine:latest && udocker run alpine:latest sh`) directly inside PRoot without a daemon.
   - **Mode B (`phonix-docker setup-qemu`):** Installs `qemu-system-aarch64` (`apk add qemu-system-aarch64 qemu-img` or `apt-get install qemu-system-arm`) and generates `/root/docker-vm/start-vm.sh` with user-mode NIC port forwarding (`2223->22`, `8090->80`, `9000->9000`) to boot a full Linux kernel VM capable of running native `dockerd` and `docker compose`.

## Symlink-Safe Rootfs Verification (`hasWorkingShell` & `TarGzExtractor`)

In Alpine (`alpine-rootfs.tgz`) and merged-`/usr` Linux distributions, `/bin/sh` is a symbolic link to `/bin/busybox` (an absolute path starting with `/`). On Android, `java.io.File.exists()` calls `stat()`, which dereferences symlinks against the host Android root `/` (where `/bin/busybox` does not exist) and returns `false`.
- `LinuxContainer.hasWorkingShell` and `LinuxContainer.guestEntryExists` check `candidate.isFile || isSymlinkStatic(candidate)` via `Files.isSymbolicLink` / `Os.lstat`, and if symlink creation was blocked by the filesystem, copy `bin/busybox` to `bin/sh` as a standalone binary fallback.
- `TarGzExtractor` uses pure lexical path normalization in `safeResolve` (never calling `canonicalFile` on guest symlinks) and `readFully` instead of API 33+ `InputStream.readNBytes` so extraction works across Android 8.0–15 (API 26–35).

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

The **PC Apps** tab installs desktop programs into the guest with one click. It runs the active
distro's package manager (`apk add --no-cache <package>`, `DEBIAN_FRONTEND=noninteractive apt-get install -y <package>`, or `pacman -Sy --noconfirm <package>`) in the live shell and shows that command's own output. A package
is only marked *Installed* after the guest's package database (`apk info -e`, `dpkg -s`, or `pacman -Qi`) confirms it, so the badge reflects the package manager rather than the button that was pressed. Every installed package also offers a **Pin to Desktop** button.

The catalog download sizes are measured from Alpine 3.24's aarch64 `APKINDEX` (package names are automatically mapped when running Debian/Ubuntu `apt` or Arch `pacman`, e.g. `firefox-esr` on Debian):

| Package | Download (Alpine aarch64) |
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

**What this is not:** x86-64 Windows programs. The guest is aarch64 Linux, and no Wine,
Box64 or x86 emulator is bundled. A `.exe` will not open. Software that needs a 3D hardware GPU
(Blender's viewport, 3D games) will be slow because `Xvfb` is a CPU software framebuffer.

## The Linux Desktop window & Desktop Environment Switcher

Start → **Linux Desktop** (or launch any pinned Linux GUI shortcut). It starts `Xvfb` (`:1`, 1280×720×24),
`x11vnc` (`127.0.0.1:5900`), `websockify` (`127.0.0.1:6080`), and the selected Desktop Environment inside the
guest, then renders the session in a WebView (`phonix.html`).

### Supported X11 Desktop Environments (`LinuxDesktopEnvironment`)

Users can switch window managers live from the selector chips in the **Linux Desktop** window header:

| Desktop Environment | Session Command | Approx. RAM | Best For |
|---|---|---|---|
| **IceWM** (Default) | `icewm-session` | ~35 MB | Fast startup, classic bottom taskbar, custom Phonix dark theme |
| **XFCE4** | `startxfce4` / `xfce4-session` | ~140 MB | Full desktop environment with panels, file manager (`thunar`), settings |
| **Openbox** | `openbox-session` | ~25 MB | Ultra-minimal window manager for 4 GB RAM devices |
| **LXQt** | `startlxqt` | ~110 MB | Modern lightweight Qt desktop environment |

When a user selects a Desktop Environment whose binary is not yet present in the active distro,
`LinuxDesktop.ensureAndStart` automatically installs the required packages via `apk`, `apt-get`, or `pacman`
before launching the X11 session.

The window loads `phonix.html`, which `LinuxDesktop` copies into the guest's noVNC web root:
- Declares `<meta name="viewport">` with `useWideViewPort = true` so the 1280×720 canvas scales cleanly without cropping.
- Publishes `window.__phonix.state` (`connecting`/`connected`/`disconnected`) and auto-reconnects.

To inspect the guest display from a PC:

```bash
adb forward tcp:6080 tcp:6080
# then open http://127.0.0.1:6080/phonix.html
```

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
is replaced.

## Limits worth knowing

- **Software rendering.** Xvfb paints into RAM and VNC encodes frames. Fine for terminals,
  editors, dev tools and light browsers; not a 3D GPU desktop.
- **Line-oriented pipe shell in the main Terminal window.** The main Compose Terminal window communicates with `/bin/sh` over standard pipes (`stdin`/`stdout`), not a raw kernel PTY (`openpty`). Line-oriented CLI tools (`apk`, `apt`, `pacman`, `git`, `python3`, `curl`, `phonix-pin`) work directly, and `↑`/`↓`/`TAB`/`Ctrl+C` are handled by the terminal bar. Full-screen curses apps that require raw terminal ioctls (`htop`, `vim`, `nano`, `tmux`) should be launched inside `+ xterm` in the **Linux Desktop (X11)** window, which provides a true X11 terminal emulator with full PTY geometry.
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
