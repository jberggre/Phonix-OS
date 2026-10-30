#!/usr/bin/env bash
# Phonix deck/pocket verification harness.
#
# Runs on the Windows bench under Git Bash with platform-tools installed. It automates only
# what adb can honestly observe: device facts, the metric matrix (screenshot + crash buffer),
# the crash buffer on its own, process-death recovery, and a soak recorder. The feature table
# in docs/VERIFICATION.md §3 is the checklist; this script is its evidence collector.
#
# It never leaves the device altered: wm size/density and rotation are snapshotted first and
# restored from an EXIT trap, including on Ctrl-C.
#
#   tools/deck-check.sh baseline
#   tools/deck-check.sh matrix
#   tools/deck-check.sh crashes
#   tools/deck-check.sh death
#   tools/deck-check.sh soak --hours 24
#   tools/deck-check.sh reset        # restore recorded originals by hand
#
# Deliberately absent: `monkey`. This app's own buttons can run debloat commands and reboot
# the unit, so random tapping is destructive rather than diagnostic. Run it by hand only on
# a unit you are willing to reset.

set -uo pipefail

# Git Bash rewrites arguments that start with "/" into Windows paths, which breaks
# `adb shell` reads such as /proc/uptime. Disable that for this script.
export MSYS2_ARG_CONV_EXCL='*'

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if ! command -v adb >/dev/null 2>&1 && [ -x "$ROOT/.tools/platform-tools/adb.exe" ]; then
  PATH="$ROOT/.tools/platform-tools:$PATH"
fi

PKG="${PHONIX_PKG:-com.aistudio.phonix.vpxmkr}"
ACTIVITY="$PKG/.MainActivity"
OUT="${OUT:-artifacts/deck}"
ORIG="$OUT/device-originals.txt"
SERIAL="${ANDROID_SERIAL:-}"

ADB=(adb)
if [ -n "$SERIAL" ]; then
  ADB=(adb -s "$SERIAL")
fi

say()  { printf '%s\n' "$*"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; }
die()  { fail "$*"; exit 1; }

require_adb() {
  command -v adb >/dev/null 2>&1 || {
    say "adb is not installed or not on PATH."
    say "Windows:  winget install --id Google.PlatformTools"
    say "Then close and reopen the shell so PATH is refreshed."
    exit 2
  }
  local state
  state="$("${ADB[@]}" get-state 2>/dev/null | tr -d '\r')"
  [ "$state" = "device" ] || die "no device in 'device' state (adb get-state: ${state:-none}). Check the cable and the RSA prompt."
  mkdir -p "$OUT"
}

# ---------------------------------------------------------------- device snapshots

wm_size_raw()    { "${ADB[@]}" shell wm size | tr -d '\r'; }
wm_density_raw() { "${ADB[@]}" shell wm density | tr -d '\r'; }
size_override()  { wm_size_raw    | sed -n 's/^Override size: //p'; }
density_override() { wm_density_raw | sed -n 's/^Override density: //p'; }

snapshot_originals() {
  mkdir -p "$OUT"
  {
    say "# captured $(date '+%Y-%m-%d %H:%M:%S')"
    say "model=$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')"
    say "sdk=$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
    say "release=$("${ADB[@]}" shell getprop ro.build.version.release | tr -d '\r')"
    say "wm_size_line=$(wm_size_raw | tr '\n' '|')"
    say "wm_density_line=$(wm_density_raw | tr '\n' '|')"
    say "density_physical=$(wm_density_raw | sed -n 's/^Physical density: //p' | tr -d '\r')"
    say "density_forced=$("${ADB[@]}" shell settings get system display_density_forced | tr -d '\r')"
    say "accelerometer_rotation=$("${ADB[@]}" shell settings get system accelerometer_rotation | tr -d '\r')"
    say "user_rotation=$("${ADB[@]}" shell settings get system user_rotation | tr -d '\r')"
  } > "$ORIG"
}

restore_originals() {
  [ -f "$ORIG" ] || return 0
  local s d ar ur phys df
  s="$(sed -n 's/^wm_size_line=//p' "$ORIG" | tr -d '\r')"
  d="$(sed -n 's/^wm_density_line=//p' "$ORIG" | tr -d '\r')"
  ar="$(sed -n 's/^accelerometer_rotation=//p' "$ORIG" | tr -d '\r')"
  ur="$(sed -n 's/^user_rotation=//p' "$ORIG" | tr -d '\r')"
  phys="$(sed -n 's/^density_physical=//p' "$ORIG" | tr -d '\r')"
  df="$(sed -n 's/^density_forced=//p' "$ORIG" | tr -d '\r')"

  case "$s" in
    *"Override size: "*) "${ADB[@]}" shell wm size "${s##*Override size: }" >/dev/null 2>&1 ;;
    *)                   "${ADB[@]}" shell wm size reset              >/dev/null 2>&1 ;;
  esac
  case "$d" in
    *"Override density: "*) "${ADB[@]}" shell wm density "${d##*Override density: }" >/dev/null 2>&1 ;;
    *) # Samsung's `wm density reset` jumps to the panel's 560 dpi (measured on the S10);
       # restore the recorded base density instead.
       [ -n "$phys" ] && "${ADB[@]}" shell wm density "$phys" >/dev/null 2>&1 ;;
  esac
  if [ -n "$df" ] && [ "$df" != "null" ]; then
    "${ADB[@]}" shell settings put system display_density_forced "$df" >/dev/null 2>&1
  fi
  [ -n "$ar" ] && "${ADB[@]}" shell settings put system accelerometer_rotation "$ar" >/dev/null 2>&1
  [ -n "$ur" ] && "${ADB[@]}" shell settings put system user_rotation "$ur" >/dev/null 2>&1
  say "restored: $(wm_size_raw | tr '\n' ' ') / $(wm_density_raw | tr '\n' ' ')"
}

# ---------------------------------------------------------------- commands

cmd_baseline() {
  local f="$OUT/baseline.txt"
  {
    say "# Phonix baseline $(date '+%Y-%m-%d %H:%M:%S')"
    say "serial=$("${ADB[@]}" get-serialno | tr -d '\r')"
    say "model=$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')"
    say "device=$("${ADB[@]}" shell getprop ro.product.device | tr -d '\r')"
    say "release=$("${ADB[@]}" shell getprop ro.build.version.release | tr -d '\r')"
    say "sdk=$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
    say "oneui=$("${ADB[@]}" shell getprop ro.build.version.oneui | tr -d '\r')"
    say "dex_vendor=$("${ADB[@]}" shell getprop ro.samsung.dex 2>/dev/null | tr -d '\r')"
    say ""
    say "## window metrics"
    "${ADB[@]}" shell wm size | tr -d '\r'
    "${ADB[@]}" shell wm density | tr -d '\r'
    say ""
    say "## displays"
    "${ADB[@]}" shell dumpsys display | grep -aiE 'DisplayDeviceInfo|mDisplayId|FLAG_DESKTOP|type=VIRTUAL' | head -40
    say ""
    say "## package"
    "${ADB[@]}" shell dumpsys package "$PKG" | grep -aE 'versionName|versionCode|firstInstallTime|lastUpdateTime' | head
    say ""
    say "## device owner / accounts / play"
    "${ADB[@]}" shell dumpsys device_policy | grep -aiE 'Device Owner|admin=' | head -5
    "${ADB[@]}" shell dumpsys account | grep -ai 'Account {' | head -5
  } > "$f" 2>&1
  say "wrote $f"
  snapshot_originals
  say "originals recorded in $ORIG"
}

# One launch, then metric changes in place: the activity is not recreated for size/density
# (the manifest declares those configs), which is exactly the in-place resize the deck needs
# to survive. force-stop is not used because the device-owner package ignores it.
cmd_matrix() {
  snapshot_originals
  trap restore_originals EXIT INT TERM

  "${ADB[@]}" shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
  sleep 3

  local profile size density rotation note
  while IFS='|' read -r profile size density rotation note; do
    say ""
    say "== $profile — $note"
    "${ADB[@]}" shell settings put system accelerometer_rotation 0 >/dev/null 2>&1
    "${ADB[@]}" shell settings put system user_rotation "$rotation" >/dev/null 2>&1
    if [ "$size" = "original" ]; then
      # Restore the exact recorded metrics, not `reset`: on Samsung `wm density reset`
      # switches to the panel's 560 dpi instead of the 420 dpi base (measured).
      local os od dp
      os="$(sed -n 's/^wm_size_line=//p' "$ORIG" | tr -d '\r')"
      od="$(sed -n 's/^wm_density_line=//p' "$ORIG" | tr -d '\r')"
      case "$os" in
        *"Override size: "*) "${ADB[@]}" shell wm size "${os##*Override size: }" >/dev/null 2>&1 ;;
        *)                   "${ADB[@]}" shell wm size reset >/dev/null 2>&1 ;;
      esac
      case "$od" in
        *"Override density: "*) "${ADB[@]}" shell wm density "${od##*Override density: }" >/dev/null 2>&1 ;;
        *) dp="$(sed -n 's/^density_physical=//p' "$ORIG" | tr -d '\r')"; [ -n "$dp" ] && "${ADB[@]}" shell wm density "$dp" >/dev/null 2>&1 ;;
      esac
    else
      "${ADB[@]}" shell wm size "$size" >/dev/null 2>&1
      "${ADB[@]}" shell wm density "$density" >/dev/null 2>&1
    fi
    sleep 4
    "${ADB[@]}" exec-out screencap -p > "$OUT/$profile.png" 2>/dev/null
    "${ADB[@]}" logcat -b crash -d -v time 2>/dev/null | grep -a "$PKG" | tail -40 > "$OUT/$profile-crash.txt"
    say "   $(wm_size_raw | tr '\n' ' ')"
    say "   $(wm_density_raw | tr '\n' ' ')"
    say "   screenshot: $OUT/$profile.png (open it; this is a visual check)"
    if [ -s "$OUT/$profile-crash.txt" ]; then
      fail "$profile produced crash-buffer lines — see $OUT/$profile-crash.txt"
    else
      say "   no crash-buffer lines for $PKG"
    fi
  done <<'PROFILES'
pocket-portrait|original|original|0|phone panel, touch, portrait
pocket-landscape|original|original|1|phone panel, touch, landscape (icon grid overflow check)
deck-fhd-mdpi|1920x1080|160|0|DeX workspace metrics: 1920x1080 dp at mdpi
deck-fhd-xxxhdpi|1920x1080|640|0|density stress: same raster, 480x270 dp (layout must not clip)
deck-quarter|960x540|160|0|quarter of a 1080p workspace: 960x540 dp
PROFILES

  say ""
  say "matrix complete. Compare screenshots; then run 'crashes' and 'death'."
}

cmd_crashes() {
  local f="$OUT/crashes.txt"
  {
    say "# crash/ANR scan $(date '+%Y-%m-%d %H:%M:%S')"
    say "## crash buffer"
    "${ADB[@]}" logcat -b crash -d -v time 2>/dev/null | grep -aA12 "$PKG"
    say "## events"
    "${ADB[@]}" logcat -b events -d 2>/dev/null | grep -aE 'am_crash|am_anr|am_low_memory' | tail -40
    say "## dropbox (today)"
    "${ADB[@]}" shell dumpsys dropbox 2>/dev/null | grep -a "$(date '+%Y-%m-%d')" | grep -aiE 'crash|anr' | head -20
  } > "$f" 2>&1
  say "wrote $f"
  grep -aqE 'FATAL EXCEPTION|am_crash|am_anr' "$f" && fail "crash/ANR evidence found in $f" || say "no crash/ANR evidence for $PKG"
}

cmd_death() {
  say "== process-death recovery"
  "${ADB[@]}" shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
  sleep 3
  "${ADB[@]}" exec-out screencap -p > "$OUT/death-before.png" 2>/dev/null

  local pid_before pid_after
  pid_before="$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
  say "pid before: ${pid_before:-none}"

  # am kill only reaps background processes, and a device owner cannot be force-stopped.
  "${ADB[@]}" shell input keyevent KEYCODE_HOME >/dev/null 2>&1
  sleep 2
  "${ADB[@]}" shell am kill "$PKG" >/dev/null 2>&1
  sleep 2
  pid_after="$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
  say "pid after kill: ${pid_after:-none}"

  "${ADB[@]}" shell am start -W -n "$ACTIVITY" >/dev/null 2>&1
  sleep 4
  "${ADB[@]}" exec-out screencap -p > "$OUT/death-after.png" 2>/dev/null
  "${ADB[@]}" shell pidof "$PKG" >/dev/null 2>&1

  say "app relaunched. Compare death-before.png with death-after.png:"
  say "  - if the arranged desktop returned, persistence (B6) is fixed;"
  say "  - if defaults came back, that is the expected 1.1 behaviour, not a new bug."
  say "if pid did not change, the device-owner package ignored the kill (expected on a provisioned"
  say "unit); process-death recovery needs the in-app reboot or a factory-reset unit."
}

cmd_soak() {
  local hours=24
  while [ $# -gt 0 ]; do
    case "$1" in
      --hours) hours="${2:-24}"; shift 2 ;;
      *) die "unknown soak argument: $1" ;;
    esac
  done
  local csv="$OUT/soak.csv"
  [ -f "$csv" ] || say "timestamp,uptime_s,pid,battery_temp_c,thermal_max_c,crash_lines" > "$csv"
  local end=$(( $(date +%s) + hours * 3600 ))
  say "recording every 60 s for ${hours}h into $csv (Ctrl-C stops; the unit keeps running)"
  while [ "$(date +%s)" -lt "$end" ]; do
    local up pid btemp tmax cr
    up="$("${ADB[@]}" shell cat /proc/uptime 2>/dev/null | awk '{print int($1)}')"
    pid="$("${ADB[@]}" shell pidof "$PKG" 2>/dev/null | tr -d '\r')"
    btemp="$("${ADB[@]}" shell dumpsys battery 2>/dev/null | sed -n 's/.*temperature: //p' | tr -d '\r')"
    tmax="$("${ADB[@]}" shell 'cat /sys/class/thermal/thermal_zone*/temp 2>/dev/null' | tr -d '\r' | sort -n | tail -1)"
    cr="$("${ADB[@]}" logcat -b crash -d 2>/dev/null | grep -ac "$PKG")"
    say "$(date '+%Y-%m-%dT%H:%M:%S'),${up:-},${pid:-},${btemp:-},${tmax:-},${cr:-0}" >> "$csv"
    sleep 60
  done
  say "soak finished: $csv"
}

usage() {
  awk 'NR > 1 { if ($0 !~ /^#/) exit; sub(/^# ?/, ""); print }' "$0"
}

main() {
  local cmd="${1:-help}"
  shift || true
  case "$cmd" in
    baseline) require_adb; cmd_baseline ;;
    matrix)   require_adb; cmd_matrix ;;
    crashes)  require_adb; cmd_crashes ;;
    death)    require_adb; cmd_death ;;
    soak)     require_adb; cmd_soak "$@" ;;
    reset)    require_adb; restore_originals ;;
    help|-h|--help) usage ;;
    *) die "unknown command '$cmd' (try: help)" ;;
  esac
}

main "$@"
