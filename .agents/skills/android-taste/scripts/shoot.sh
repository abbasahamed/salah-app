#!/usr/bin/env bash
# shoot.sh - screenshot an installed Android app on a device or emulator across
# light/dark mode and font scales, so design is judged by looking, not by reading code.
#
# Usage:
#   shoot.sh --package com.example.app [options]
#
# Options:
#   --package PKG      application id (required)
#   --serial S         adb serial (or set ANDROID_SERIAL). Required when more than one device is attached.
#   --activity ACT     launch this activity (".MainActivity" or full class); default: the launcher activity
#   --link URI         also capture a deep link (repeatable), e.g. --link myapp://settings
#   --modes "light dark"   default: "light dark"
#   --scales "1.0 1.5"     font scales, default: "1.0 1.5"
#   --out DIR          default: taste-shots
#   --settle SEC       seconds to wait after launch before the screenshot (default 2)
#   --dry-run          print what would run, touch nothing
#
# Install the app first (./gradlew :app:installDebug). The script restores your device's original
# dark-mode and font-scale settings when it exits. Output: DIR/NN-<target>-<mode>-fs<scale>.png and
# DIR/index.txt. Needs adb (PATH or $ANDROID_HOME/platform-tools). Android 10+ for "cmd uimode night".
#
# The settle delay is a screenshot courtesy, not synchronization: raise --settle for slow screens.

set -euo pipefail

PKG=""; SERIAL="${ANDROID_SERIAL:-}"; ACTIVITY=""; OUT="taste-shots"; MODES="light dark"; SCALES="1.0 1.5"
SETTLE=2; DRY=0; LINKS=()

die() { echo "shoot.sh: $*" >&2; exit 2; }
usage() { sed -n '2,24p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package)  [[ $# -ge 2 ]] || die "--package needs a value"; PKG="$2"; shift 2 ;;
    --serial)   [[ $# -ge 2 ]] || die "--serial needs a value"; SERIAL="$2"; shift 2 ;;
    --activity) [[ $# -ge 2 ]] || die "--activity needs a value"; ACTIVITY="$2"; shift 2 ;;
    --link)     [[ $# -ge 2 ]] || die "--link needs a value"; LINKS+=("$2"); shift 2 ;;
    --modes)    [[ $# -ge 2 ]] || die "--modes needs a value"; MODES="$2"; shift 2 ;;
    --scales)   [[ $# -ge 2 ]] || die "--scales needs a value"; SCALES="$2"; shift 2 ;;
    --out)      [[ $# -ge 2 ]] || die "--out needs a value"; OUT="$2"; shift 2 ;;
    --settle)   [[ $# -ge 2 ]] || die "--settle needs a value"; SETTLE="$2"; shift 2 ;;
    --dry-run)  DRY=1; shift ;;
    -h|--help)  usage; exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done
[[ -n "$PKG" ]] || { usage >&2; die "--package is required"; }
for m in $MODES; do [[ "$m" == light || "$m" == dark ]] || die "modes must be 'light' and/or 'dark' (got '$m')"; done
for s in $SCALES; do [[ "$s" =~ ^[0-9]+([.][0-9]+)?$ ]] || die "font scales must be numbers (got '$s')"; done

ADB="$(command -v adb || true)"
if [[ -z "$ADB" && -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then ADB="$ANDROID_HOME/platform-tools/adb"; fi
[[ -n "$ADB" ]] || die "adb not found (add platform-tools to PATH or set ANDROID_HOME)"

if [[ -z "$SERIAL" ]]; then
  devices="$("$ADB" devices | awk 'NR > 1 && $2 == "device" {print $1}')"
  count="$(printf '%s\n' "$devices" | grep -c . || true)"
  if [[ "$count" -eq 1 ]]; then SERIAL="$devices"
  elif [[ "$count" -eq 0 ]]; then die "no device attached (start an emulator or connect a device)"
  else die "more than one device attached; pass --serial or set ANDROID_SERIAL. Attached: $(printf '%s ' $devices)"
  fi
fi

ad() { "$ADB" -s "$SERIAL" "$@"; }
run() { if [[ "$DRY" -eq 1 ]]; then printf 'DRY: %s\n' "$*" >&2; else "$@"; fi; }

if [[ "$DRY" -eq 0 ]]; then
  ad wait-for-device
  ad shell pm path "$PKG" 2>/dev/null | grep -q '^package:' || die "package $PKG is not installed on $SERIAL (run ./gradlew :app:installDebug)"
fi

# Remember original state and restore it on exit.
ORIG_SCALE="null"; ORIG_NIGHT="auto"
if [[ "$DRY" -eq 0 ]]; then
  ORIG_SCALE="$(ad shell settings get system font_scale | tr -d '\r' || echo null)"
  night_line="$(ad shell cmd uimode night 2>/dev/null | tr -d '\r' || true)"
  case "$night_line" in *yes*) ORIG_NIGHT="yes" ;; *no*) ORIG_NIGHT="no" ;; *) ORIG_NIGHT="auto" ;; esac
fi
restore() {
  [[ "$DRY" -eq 1 ]] && return 0
  ad shell cmd uimode night "$ORIG_NIGHT" >/dev/null 2>&1 || true
  if [[ "$ORIG_SCALE" == "null" || -z "$ORIG_SCALE" ]]; then ad shell settings delete system font_scale >/dev/null 2>&1 || true
  else ad shell settings put system font_scale "$ORIG_SCALE" >/dev/null 2>&1 || true; fi
}
trap restore EXIT

mkdir -p "$OUT"
INDEX="$OUT/index.txt"
[[ "$DRY" -eq 1 ]] || : > "$INDEX"

launch() {  # TARGET ("home" or a link)
  run "$ADB" -s "$SERIAL" shell am force-stop "$PKG"
  if [[ "$1" == "home" ]]; then
    if [[ -n "$ACTIVITY" ]]; then
      local comp="$ACTIVITY"; [[ "$comp" == .* ]] && comp="${PKG}${comp}"; [[ "$comp" == */* ]] || comp="$PKG/$comp"
      run "$ADB" -s "$SERIAL" shell am start -W -n "$comp" >/dev/null
    else
      run "$ADB" -s "$SERIAL" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
    fi
  else
    run "$ADB" -s "$SERIAL" shell am start -W -a android.intent.action.VIEW -d "$1" "$PKG" >/dev/null
  fi
}

n=0; failed=0
TARGETS=("home")
if [[ ${#LINKS[@]} -gt 0 ]]; then TARGETS+=("${LINKS[@]}"); fi

for mode in $MODES; do
  nightval="no"; [[ "$mode" == dark ]] && nightval="yes"
  run "$ADB" -s "$SERIAL" shell cmd uimode night "$nightval"
  for scale in $SCALES; do
    run "$ADB" -s "$SERIAL" shell settings put system font_scale "$scale"
    for target in "${TARGETS[@]}"; do
      n=$((n + 1))
      label="$(printf '%s' "$target" | sed -E 's|^[A-Za-z][A-Za-z0-9+.-]*://||; s|[^A-Za-z0-9]+|-|g; s|^-+||; s|-+$||')"
      [[ -n "$label" ]] || label="screen"
      file="$(printf '%s/%02d-%s-%s-fs%s.png' "$OUT" "$n" "$label" "$mode" "$scale")"
      launch "$target"
      if [[ "$DRY" -eq 1 ]]; then
        printf 'DRY: sleep %s; screencap -> %s\n' "$SETTLE" "$file"
        continue
      fi
      sleep "$SETTLE"
      if ad exec-out screencap -p > "$file" 2>/dev/null && [[ -s "$file" && "$(wc -c < "$file")" -gt 1000 ]]; then
        printf '%s\t%s\tmode=%s\tfont_scale=%s\n' "$file" "$target" "$mode" "$scale" >> "$INDEX"
        echo "captured $file"
      else
        failed=$((failed + 1)); rm -f "$file"
        echo "FAILED to capture $target ($mode, fs$scale)" >&2
      fi
    done
  done
done

if [[ "$DRY" -eq 0 ]]; then
  echo "Done: $((n - failed)) of $n screenshots in $OUT (index: $INDEX)."
  echo "Look at them: check contrast in both modes, clipping at font scale >= 1.5, insets under system bars, empty and error states."
fi
[[ "$failed" -eq 0 ]] || exit 1
