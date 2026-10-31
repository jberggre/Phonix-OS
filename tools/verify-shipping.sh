#!/usr/bin/env bash
#
# verify-shipping.sh - acceptance pass for the 1.3 shipping candidate.
#
# Answers, from a machine that actually has the phone attached:
#   * did the APK install over the device-owner build, and what version is on the unit
#   * is the package's crash buffer clean
#   * is the Linux guest running, and which rootfs trees exist
#   * is the bundled Alpine rescue image still on disk
#   * is the device owner intact, and did any debloat actually hide a package
#
# Non-destructive by design: it never uninstalls, never force-stops (that is not a
# recovery option for a device-owner install) and never changes device policy.
#
# Usage:
#   tools/verify-shipping.sh                       # report only, no install
#   tools/verify-shipping.sh --apk app/build/outputs/apk/debug/app-debug.apk
#   tools/verify-shipping.sh --serial 192.168.0.3:5555
#
#   --expect 1.3        versionName to require (default 1.3)
#   --apk <path>        install -r this APK first, with the real device-owner keystore
#
# Exit status is 0 only when every required check passes.

set -uo pipefail
export MSYS_NO_PATHCONV=1

PKG="com.aistudio.phonix.vpxmkr"
SERIAL=""
APK=""
EXPECT="1.3"
ADB="${ADB:-adb}"
FAILED=0

while [ $# -gt 0 ]; do
  case "$1" in
    --apk)    APK="${2:-}"; shift ;;
    --serial) SERIAL="${2:-}"; shift ;;
    --expect) EXPECT="${2:-}"; shift ;;
    -h|--help) sed -n '2,26p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) printf 'unknown option: %s (try --help)\n' "$1"; exit 2 ;;
  esac
  shift
done

say()   { printf '%s\n' "$*"; }
head1() { printf '\n\033[1m== %s ==\033[0m\n' "$*"; }
ok()    { printf '  \033[32mPASS\033[0m %s\n' "$*"; }
bad()   { printf '  \033[31mFAIL\033[0m %s\n' "$*"; FAILED=$((FAILED + 1)); }
warn()  { printf '  \033[33mNOTE\033[0m %s\n' "$*"; }

q() { "$ADB" -s "$SERIAL" shell "$@" 2>/dev/null | tr -d '\r'; }

command -v "$ADB" >/dev/null 2>&1 || { say "adb not found; set ADB=/path/to/adb"; exit 1; }

if [ -z "$SERIAL" ]; then
  mapfile -t devs < <("$ADB" devices | awk 'NR>1 && $2=="device" {print $1}')
  if [ "${#devs[@]}" -eq 1 ]; then
    SERIAL="${devs[0]}"
  elif [ "${#devs[@]}" -eq 0 ]; then
    say "No authorised device."
    say "Wireless debugging: 'adb pair <ip>:<pair-port>' with the 6-digit code, then"
    say "'adb connect <ip>:<debug-port>' using the port on the Wireless debugging screen."
    exit 1
  else
    say "Multiple devices; choose one with --serial: ${devs[*]}"; exit 1
  fi
fi

MODEL="$(q getprop ro.product.model)"
[ -z "$MODEL" ] && { say "cannot talk to $SERIAL"; exit 1; }

head1 "unit"
say "  serial  $SERIAL"
say "  model   $MODEL ($(q getprop ro.product.device))"
say "  android $(q getprop ro.build.version.release) / sdk $(q getprop ro.build.version.sdk)"

head1 "install"
q pm list packages | grep -qx "package:$PKG" || { bad "$PKG is not installed"; exit 1; }
if [ -n "$APK" ]; then
  [ -f "$APK" ] || { bad "apk not found: $APK"; exit 1; }
  say "  installing $APK"
  "$ADB" -s "$SERIAL" install -r "$APK" 2>&1 | sed 's/^/       /'
  [ "${PIPESTATUS[0]}" -eq 0 ] || bad "adb install failed"
fi

VERSION_NAME="$(q dumpsys package "$PKG" | grep -m1 versionName | awk -F= '{print $2}')"
VERSION_CODE="$(q dumpsys package "$PKG" | grep -m1 versionCode | awk -F= '{print $2}' | awk '{print $1}')"
say "  installed ${VERSION_NAME:-unknown} (versionCode ${VERSION_CODE:-?})"
[ "$VERSION_NAME" = "$EXPECT" ] && ok "version is $EXPECT" || bad "expected $EXPECT, found ${VERSION_NAME:-unknown}"

head1 "stability"
CRASHES="$(q logcat -b crash -d 2>/dev/null | grep -c "$PKG")"
[ "${CRASHES:-0}" -eq 0 ] && ok "crash buffer has no entry for $PKG" || bad "crash buffer has $CRASHES entries for $PKG"

head1 "linux guest"
RUNNING="$(q dumpsys activity services "$PKG" 2>/dev/null | grep -c LinuxServerService)"
if [ "${RUNNING:-0}" -gt 0 ]; then
  warn "LinuxServerService is running (expected if the guest was left on)"
else
  ok "no background Linux service"
fi
DISTROS="$(q run-as "$PKG" ls files/linux 2>/dev/null | tr '\n' ' ')"
say "  rootfs trees: ${DISTROS:-none}"
printf '%s' "$DISTROS" | grep -q alpine && ok "bundled Alpine rescue image present" \
  || warn "Alpine not unpacked yet (the first Linux launch unpacks it from the APK)"
say "  open Linux System in the app and confirm the hub names the distro you installed;"
say "  a downloaded distro is only real if its tree contains .phonix-installed."

head1 "device owner and debloat"
# API 31 prints "admin=com.…,DeviceOwner,Affiliated"; older wording was "Device Owner".
if q dpm list-owners 2>/dev/null | grep -qiE "device[ _]?owner"; then
  ok "device owner intact"
else
  warn "not device owner: hide-based debloat is unavailable, native uninstall only"
fi
DISABLED="$(q pm list packages -d 2>/dev/null | wc -l | tr -d ' ')"
say "  disabled packages on the unit: ${DISABLED:-0}"
say "  hiding is not uninstalling: confirm the package you chose is gone from the launcher."

head1 "result"
if [ "$FAILED" -eq 0 ]; then
  say "  PASS - required checks passed. UI checks (distro install, debloat hide, Start-menu"
  say "  modes) still need a human finger: this script cannot click for you."
  exit 0
fi
say "  FAIL - $FAILED required check(s) failed; see above."
exit 1
