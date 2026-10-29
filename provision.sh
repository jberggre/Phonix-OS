#!/usr/bin/env bash
#
# provision.sh - Phonix OS bench provisioning for recycled Galaxy S-series Mini PCs.
#
# Turns a stock carrier Galaxy into a Phonix appliance in one pass:
#   1. reports what the device actually is (model, Android, free space, owner state)
#   2. claims Android device-owner for the Phonix admin receiver
#   3. applies the mini-PC settings (DeX auto-start on HDMI, never sleep, no keyguard)
#   4. strips carrier bloat with `pm uninstall --user 0` (reversible)
#   5. installs the Phonix APK and launches the OOBE
#
# Idempotent: every step inspects current state first and skips what is already done.
# Non-destructive by default: without --apply it only reports what it *would* do.
#
# Usage:
#   bash provision.sh                                  # dry run, reports only
#   bash provision.sh --apply                          # provision this device
#   bash provision.sh --apply --apk app-debug.apk      # also install the APK
#   bash provision.sh --apply --aggressive             # also remove social/preload apps
#   bash provision.sh --serial 192.168.0.186:41587 --apply
#
# Override the adb binary with ADB=/path/to/adb.
#
# Notes for whoever runs this next:
#   * Claiming device owner requires a device with NO accounts on it. If a Google
#     or Samsung account is present the device must be factory-reset first; the
#     script detects this and refuses rather than half-provisioning.
#   * `pm uninstall --user 0` removes the app for the current user only. It is
#     reversible with `pm install-existing <package>`. System-partition preloads
#     free no disk space this way (their APK lives on a read-only partition); the
#     third-party apps installed into /data are the ones that reclaim storage.
#   * Do not remove com.samsung.android.mtp, com.samsung.desktopsystemui,
#     com.sec.android.app.desktoplauncher or com.sec.android.desktopmode.uiservice:
#     those are the MTP bridge and DeX itself.

set -uo pipefail

# Git Bash (MSYS) rewrites arguments that look like POSIX paths, which turns
# `df -h /data` into `df -h C:/Program Files/Git/data` and silently breaks any
# remote path we pass to the device. Harmless on macOS/Linux.
export MSYS_NO_PATHCONV=1

APPLY=0
AGGRESSIVE=0
INSTALL_APK=""
SERIAL=""
ADB="${ADB:-adb}"

APK_PKG="com.aistudio.phonix.vpxmkr"
APK_ACTIVITY="${APK_PKG}/com.example.MainActivity"
ADMIN_COMPONENT="${APK_PKG}/com.example.receiver.PhonixDeviceAdminReceiver"

# ---------------------------------------------------------------------------
# Bloat: removed by default. All verified present on a Cricket SM-G973U image.
# ---------------------------------------------------------------------------
BLOAT=(
  # Carrier services, upsells and subscription funnels
  com.asurion.android.mediabackup.vault.cricket
  com.asurion.asurionapp
  com.digitalturbine.cricketbar
  com.adfone.aditup
  com.mizmowireless.vvm
  us.sliide.siren
  com.wbd.stream
  com.particlenews.newsbreak
  com.glance.ailynk
  com.localweather.radar.climate
  # Preinstalled trialware games
  ball.sort.puzzle.color.sorting.bubble.games
  com.blackout.spades
  com.block.juggle
  com.easybrain.art.puzzle
  com.king.candycrushsaga
  com.nebula.mahjongtile
  com.oakever.arrows
  com.puzzlegames.puzzlebrickslegend
  com.tripledot.blockbash
  com.tripledot.solitaire
  com.vitastudio.mahjong
  in.playsimple.go1v1.crossword.go
  in.playsimple.newwordsearch
  in.playsimple.triple.tile.matchgame.pair.three.puzzle.object.royale
  # Junk utilities shipped as ads
  com.documentreader.free.viewer.all
  com.revoke.health.alarmclock
  com.greenrose.ontime.alarmclock
  com.einnovation.temu
  # Platform stubs nobody asked for
  com.facebook.appmanager
  com.facebook.services
  com.facebook.system
  com.linkedin.android
)

# ---------------------------------------------------------------------------
# Aggressive: only with --aggressive. Social, streaming and banking preloads.
# Kept separate because a refurbisher may want some of these left in place.
# ---------------------------------------------------------------------------
AGGRESSIVE_BLOAT=(
  com.zhiliaoapp.musically
  com.instagram.android
  com.whatsapp
  com.pandora.android
  com.onedebit.chime
)

# global/secure/system settings that turn the phone into an appliance.
# Format: "<namespace> <key> <value>"
SETTINGS=(
  # DeX starts the moment HDMI is connected - essential on a unit whose panel
  # is either removed or never looked at.
  "global force_desktop_mode_on_external_displays 1"
  # Stay awake while on mains power (7 = AC | USB | wireless).
  "global stay_on_while_plugged_in 7"
  # Never time out the display on a mains-powered desktop.
  "system screen_off_timeout 2147483647"
  # Skip the lock screen; the device owner call also disables the keyguard.
  "secure lockscreen.disabled 1"
  # Pre-confirm immersive mode so the "swipe to exit" prompt never appears.
  "secure immersive_mode_confirmations confirmed"
  # Lock orientation: this is a fixed appliance, not a handheld.
  "system accelerometer_rotation 0"
)

# ---------------------------------------------------------------------------
# plumbing
# ---------------------------------------------------------------------------
say()  { printf '%s\n' "$*"; }
head1() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }
ok()   { printf '  \033[32mok\033[0m   %s\n' "$*"; }
skip() { printf '  \033[90mskip\033[0m %s\n' "$*"; }
warn() { printf '  \033[33mwarn\033[0m %s\n' "$*"; }
fail() { printf '  \033[31mfail\033[0m %s\n' "$*"; }
plan() {
  if [ "$APPLY" -eq 1 ]; then printf '  \033[36mrun \033[0m %s\n' "$*"
  else printf '  \033[35mplan\033[0m %s\n' "$*"; fi
}

# Run a state-changing adb command (honours dry-run).
act() {
  local desc="$1"; shift
  plan "$desc"
  if [ "$APPLY" -eq 1 ]; then
    "$@" 2>&1 | sed 's/^/       /'
    local rc=${PIPESTATUS[0]}
    [ "$rc" -ne 0 ] && warn "(exit $rc)"
    return 0
  fi
}

# Run a read-only adb shell query and return its output.
q() { "$ADB" -s "$SERIAL" shell "$@" 2>/dev/null | tr -d '\r'; }

usage() { sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'; }

while [ $# -gt 0 ]; do
  case "$1" in
    --apply)      APPLY=1 ;;
    --dry-run)    APPLY=0 ;;
    --aggressive) AGGRESSIVE=1 ;;
    --apk)        INSTALL_APK="${2:-}"; shift ;;
    --serial)     SERIAL="${2:-}"; shift ;;
    -h|--help)    usage; exit 0 ;;
    *) say "unknown option: $1 (try --help)"; exit 2 ;;
  esac
  shift
done

# ---------------------------------------------------------------------------
# 0. preflight
# ---------------------------------------------------------------------------
command -v "$ADB" >/dev/null 2>&1 || { fail "adb not found; set ADB=/path/to/adb"; exit 1; }

if [ -z "$SERIAL" ]; then
  # Ignore the mDNS transport: it points at the same physical device and only
  # makes adb complain about multiple devices.
  mapfile -t _devs < <("$ADB" devices | awk 'NR>1 && $2=="device" && $1 !~ /_adb-tls-connect/ {print $1}')
  if [ "${#_devs[@]}" -eq 1 ]; then
    SERIAL="${_devs[0]}"
  elif [ "${#_devs[@]}" -eq 0 ]; then
    fail "no authorised device. Connect USB and accept the debugging prompt, or pass --serial."
    exit 1
  else
    fail "multiple devices; choose one with --serial: ${_devs[*]}"
    exit 1
  fi
fi

MODEL="$(q getprop ro.product.model)"
[ -z "$MODEL" ] && { fail "cannot talk to $SERIAL"; exit 1; }

if [ "$APPLY" -eq 0 ]; then
  say "PHONIX PROVISIONING - DRY RUN (nothing will be changed; add --apply to execute)"
else
  say "PHONIX PROVISIONING - APPLYING CHANGES to $SERIAL"
fi

# ---------------------------------------------------------------------------
# 1. device report
# ---------------------------------------------------------------------------
head1 "device"
say "  serial    $SERIAL"
say "  model     $MODEL ($(q getprop ro.product.device))"
say "  android   $(q getprop ro.build.version.release) / sdk $(q getprop ro.build.version.sdk) / oneui $(q getprop ro.build.version.oneui)"
say "  storage   $(q df -h /data | awk 'NR==2 {print $4 " free of " $2}')"

DEX_PKGS=0
for p in com.sec.android.app.desktoplauncher com.samsung.desktopsystemui \
         com.sec.android.desktopmode.uiservice com.sec.android.app.dexonpc; do
  q pm list packages | grep -qx "package:$p" && DEX_PKGS=$((DEX_PKGS + 1))
done
if [ "$DEX_PKGS" -eq 4 ]; then ok "DeX stack complete (4/4 packages)"
else warn "DeX stack incomplete ($DEX_PKGS/4) - desktop mode will not work on this unit"; fi

if q dumpsys display | grep -q "mDisplayId=1"; then
  ok "external display attached (DeX live)"
else
  skip "no external display attached"
fi

# ---------------------------------------------------------------------------
# 2. APK
# ---------------------------------------------------------------------------
head1 "phonix apk"
if [ -n "$INSTALL_APK" ]; then
  if [ ! -f "$INSTALL_APK" ]; then fail "apk not found: $INSTALL_APK"; exit 1; fi
  act "adb install -r -d $INSTALL_APK" "$ADB" -s "$SERIAL" install -r -d "$INSTALL_APK"
elif q pm list packages | grep -qx "package:$APK_PKG"; then
  ok "$APK_PKG already installed ($(q dumpsys package "$APK_PKG" | grep -m1 versionName | awk '{print $1}'))"
else
  warn "$APK_PKG not installed - pass --apk <path> (device owner cannot be claimed without it)"
fi

# ---------------------------------------------------------------------------
# 2b. secure settings
# ---------------------------------------------------------------------------
# The Privacy Hub can only write Android's Private DNS fields if it holds this
# development permission. There is no runtime dialog for it; adb grants it once.
head1 "secure settings"
if q dumpsys package "$APK_PKG" 2>/dev/null | grep -q "WRITE_SECURE_SETTINGS: granted=true"; then
  skip "WRITE_SECURE_SETTINGS already granted"
elif q pm list packages | grep -qx "package:$APK_PKG"; then
  act "pm grant $APK_PKG android.permission.WRITE_SECURE_SETTINGS" \
      "$ADB" -s "$SERIAL" shell pm grant "$APK_PKG" android.permission.WRITE_SECURE_SETTINGS
else
  warn "skipping: Phonix is not installed"
fi

# ---------------------------------------------------------------------------
# 3. device owner
# ---------------------------------------------------------------------------
head1 "device owner"
OWNER="$(q dpm list-owners 2>/dev/null | grep -i "Device Owner" || q dumpsys device_policy | grep -i "Device Owner" | head -1)"
if [ -n "$OWNER" ]; then
  ok "already provisioned: $(echo "$OWNER" | sed 's/^ *//')"
else
  ACCOUNTS="$(q dumpsys account | grep -cE "Account \{name=")"
  if [ "${ACCOUNTS:-0}" -gt 0 ]; then
    warn "$ACCOUNTS account(s) on the device - Android refuses device owner until it is factory-reset"
    warn "skipping step; run again after a fresh setup (do not add any account)"
  elif ! q pm list packages | grep -qx "package:$APK_PKG"; then
    warn "skipping: Phonix must be installed first (see above)"
  else
    act "adb shell dpm set-device-owner $ADMIN_COMPONENT" \
        "$ADB" -s "$SERIAL" shell dpm set-device-owner "$ADMIN_COMPONENT"
  fi
fi

# ---------------------------------------------------------------------------
# 4. mini-PC settings
# ---------------------------------------------------------------------------
head1 "settings"
for entry in "${SETTINGS[@]}"; do
  # shellcheck disable=SC2086
  set -- $entry
  ns="$1"; key="$2"; want="$3"
  have="$(q settings get "$ns" "$key")"
  if [ "$have" = "$want" ]; then
    skip "$ns/$key already $want"
  else
    act "settings put $ns $key $want   (was: ${have:-unset})" \
        "$ADB" -s "$SERIAL" shell settings put "$ns" "$key" "$want"
  fi
done

# ---------------------------------------------------------------------------
# 4b. background activity starts
# ---------------------------------------------------------------------------
# Since Android 10 a background app cannot open an activity, which is exactly what
# Phonix must do at boot to put the desktop on the monitor. Holding DRAW_OVER_OTHER_APPS
# is one of the documented exemptions (device owner is another), and it is also the
# permission a taskbar overlay will need later.
head1 "background start"
if [ "$(q appops get "$APK_PKG" SYSTEM_ALERT_WINDOW 2>/dev/null | grep -c allow)" -gt 0 ]; then
  skip "SYSTEM_ALERT_WINDOW already allowed"
else
  act "appops set $APK_PKG SYSTEM_ALERT_WINDOW allow" \
      "$ADB" -s "$SERIAL" shell appops set "$APK_PKG" SYSTEM_ALERT_WINDOW allow
fi

# ---------------------------------------------------------------------------
# 5. debloat
# ---------------------------------------------------------------------------
head1 "debloat"
PKGS="$(q pm list packages)"
TARGETS=("${BLOAT[@]}")
[ "$AGGRESSIVE" -eq 1 ] && TARGETS+=("${AGGRESSIVE_BLOAT[@]}")

present=0; removed=0
for pkg in "${TARGETS[@]}"; do
  if printf '%s\n' "$PKGS" | grep -qx "package:$pkg"; then
    present=$((present + 1))
    act "pm uninstall --user 0 $pkg" \
        "$ADB" -s "$SERIAL" shell pm uninstall --user 0 "$pkg"
  else
    removed=$((removed + 1))
  fi
done
say "  $present present, $removed already gone ($([ "$AGGRESSIVE" -eq 1 ] && echo aggressive || echo default) list)"

# ---------------------------------------------------------------------------
# 6. launch
# ---------------------------------------------------------------------------
head1 "desktop"
if q pm list packages | grep -qx "package:$APK_PKG"; then
  act "am start -n $APK_ACTIVITY" "$ADB" -s "$SERIAL" shell am start -n "$APK_ACTIVITY"
else
  warn "Phonix not installed; nothing to launch"
fi

# ---------------------------------------------------------------------------
# 7. post-flight
# ---------------------------------------------------------------------------
head1 "result"
say "  storage   $(q df -h /data | awk 'NR==2 {print $4 " free of " $2}')"
say "  owner     $(q dumpsys device_policy | grep -i "Device Owner" | head -1 | sed 's/^ *//' || true)"
say "  dex auto  force_desktop_mode_on_external_displays=$(q settings get global force_desktop_mode_on_external_displays)"
say "  screen    screen_off_timeout=$(q settings get system screen_off_timeout), stay_on=$(q settings get global stay_on_while_plugged_in)"

say ""
if [ "$APPLY" -eq 0 ]; then
  say "Dry run complete. Re-run with --apply to make these changes."
else
  say "Provisioning applied. Plug in HDMI and the unit should boot straight to the Phonix desktop."
fi
