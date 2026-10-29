# Provisioning a unit, and the numbers worth keeping

Two things live here: the per-unit record we should keep (for QA and for honest marketing
numbers), and the design for a USB provisioning stick that can do this without a PC.

## Per-unit record

Keep one row per phone. These are the fields that matter, where they come from, and why:

| Field | Source | Why it matters |
|---|---|---|
| Model / codename | `adb shell getprop ro.product.model` | Compatibility claim on the site |
| Android version + SDK | `getprop ro.build.version.release` / `.sdk` | DeX behaviour differs by One UI version |
| Serial | `adb shell getprop ro.serialno` | Warranty and RMA |
| Battery health / cycle count | Samsung Members or `dumpsys battery` | Sets the price tier and the battery guidance |
| Free storage before / after install | `df -h /data` | "How much space does Phonix use?" |
| APK install time | stopwatch around `adb install` | Fulfilment throughput |
| Guest unpack time | first launch to "Root filesystem ready" in the terminal | First-boot expectation on the site |
| X11 stack install time + size | first "Open Linux Desktop" to connected; `du -sh /usr` in guest | The biggest one-time wait a customer sees |
| Measured disk throughput | the Benchmark window (`dd` inside the guest) | A real performance number, not a synthetic score |
| Idle and load temperature | `/sys/class/thermal` via the Thermals screen | Thermal claim per cooling build |
| RAM used idle / with desktop open | `free -m` in the guest | Which phones are comfortable |
| DeX display mode + resolution | `dumpsys display` (`Desktop` virtual display) | Proof the monitor path works |
| DeX wallpaper set | visual check | It is manual per unit (see `LINUX.md`) |
| Debloat commands run | the list copied by the wizard | What was removed, for support |
| Test results | browser, terminal, server, Linux window, reboot | Pass/fail per unit |

Store it as CSV keyed by serial. A simple sheet is enough until volume justifies a database.

### Baseline measured on the S10 (SM-G973U, Android 12, 2026-10-29)

These are real readings from this unit, not estimates:

- RAM: 7,461 MB total; 4,148 MB used with the desktop, browser and Linux window open.
- Storage: 109 GB `/data`, 12 GB used, 96 GB free.
- Guest rootfs plus the X11 desktop stack: 447 MB in `/usr` after installing Xvfb, x11vnc,
  noVNC, IceWM, xterm, fonts and x11-utils.
- `dd` inside the guest: ~600–690 MB/s write, 1.2–1.7 GB/s read for a 64 MB file (page cache;
  treat it as an upper bound, not disk endurance).
- Shell loop (2M awk iterations, via `time`): ~1.5 s.
- DeX virtual display: 3440×1440 at 160 dpi, `type=VIRTUAL`, `FLAG_DESKTOP_DISPLAY`.
- Idle guest RAM is small; the desktop itself (Xvfb + IceWM + xterm) is the bulk.

Re-run these after changing the rootfs or the X11 package list; the numbers above are the
reference for the current build.

## The provisioning stick

A stick that does the work when plugged into a phone is a good idea, but the constraint is
ADB authorization, not hardware:

- **ADB over USB needs the phone to trust the host.** On a freshly reset phone, `adb` shows
  "unauthorized" until someone taps *Allow USB debugging*; the key is then stored in
  `/data/misc/adb/adb_keys`, which an app cannot write.
- **A Rubber Ducky cannot do it alone.** It is a keyboard: it can navigate menus and enable
  Developer options with keystrokes, but it cannot install an APK or run `adb` — there is no
  host on the other end of the cable.
- **A small Linux board can.** A Pi Zero 2 W (or CM4 carrier) in a USB-stick shape runs `adb`
  and the install script itself. That is the design that actually works.

### Recommended stick

1. Pi Zero 2 W + USB-A male plug + 16 GB card, powered from the phone's USB port (or a hub).
2. Boots a minimal Linux with `adb` and the Phonix APK on the card.
3. Script: wait for the device, `adb install -r phonix.apk`, grant the notification permission,
   launch the app once, open the Linux window once (to unpack the guest), then run the selected
   `pm uninstall --user 0` lines, write the unit's metrics row, and print a PASS/FAIL summary.
4. If the phone is not yet authorized, the stick cannot tap the dialog. Options: keep one
   technician tap per unit (fine at low volume), pre-authorize the stick's key on units you
   control, or use Knox/MDM enrolment for fleet volume.

### Do not promise

- "Fully automatic on any phone" — the RSA prompt stands in the way.
- Root, unlocked bootloader or custom ROM behaviour — none of that is required for Phonix, and
  none of it is something the stick can do over ADB.
- Debloating on the stick without ADB authorization; the app itself can only copy commands.

### Metrics the stick should emit (for the site and for support)

`model, android, serial, install_seconds, first_boot_seconds, x11_seconds, guest_mb, dd_write,
dd_read, loop_ms, temp_idle_c, temp_load_c, ram_idle_mb, dex_resolution, tests_passed,
tests_failed, debloat_list`

With a hundred rows of that, the compatibility table and the performance claims on the website
can be built from data instead of adjectives.
