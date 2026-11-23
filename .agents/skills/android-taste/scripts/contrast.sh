#!/usr/bin/env bash
# contrast.sh - WCAG contrast checks for Android themes.
#
# Usage:
#   contrast.sh '#RRGGBB' '#RRGGBB'     contrast ratio of two colors, with AA/AAA verdicts
#   contrast.sh --library [PALETTE]     check the skill's dealt palettes (all, or one)
#   contrast.sh --project DIR           scan a project's color resources for Material 3 role pairs
#
# --project reads <color name="..."> entries from res XML (values-night = dark theme) and
# `val Name = Color(0xAARRGGBB)` lines in Kotlin. It recognizes Material 3 role names in either
# style (md_theme_light_onPrimary, PrimaryLight, dealt_on_primary, onPrimary ...) and checks the
# standard foreground/background pairs per light and dark set. Best effort: it cannot see colors
# built in code (Color(red, green, blue), HSL math, dynamic color). Exits 1 if any pair fails.
#
# Thresholds: text pairs 4.5:1, outline and primary against surface 3:1 (non-text and large text).

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LIB="$SCRIPT_DIR/../library"
die() { echo "contrast.sh: $*" >&2; exit 2; }
usage() { sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

AWK_FUNCS='
function hexval(h,   i, c, v, d) { v = 0; h = toupper(h)
  for (i = 1; i <= length(h); i++) { c = substr(h, i, 1); d = index("0123456789ABCDEF", c) - 1; v = v * 16 + d } return v }
function chan(x) { x = x / 255; return (x <= 0.04045) ? x / 12.92 : exp(2.4 * log((x + 0.055) / 1.055)) }
function lum(h) { h = toupper(h); sub(/^#/, "", h); if (length(h) == 8) h = substr(h, 3, 6)
  return 0.2126 * chan(hexval(substr(h, 1, 2))) + 0.7152 * chan(hexval(substr(h, 3, 2))) + 0.0722 * chan(hexval(substr(h, 5, 2))) }
function ratio(a, b,   la, lb, t) { la = lum(a); lb = lum(b); if (la < lb) { t = la; la = lb; lb = t } return (la + 0.05) / (lb + 0.05) }
'

pair_check() {
  # stdin: group<TAB>mode<TAB>name<TAB>hex   (mode: light|dark|auto)
  awk -F'\t' "$AWK_FUNCS"'
  BEGIN {
    nr = split("primary onprimary primarycontainer onprimarycontainer secondary onsecondary secondarycontainer onsecondarycontainer tertiary ontertiary tertiarycontainer ontertiarycontainer error onerror errorcontainer onerrorcontainer background onbackground surface onsurface surfacevariant onsurfacevariant outline outlinevariant inversesurface inverseonsurface inverseprimary surfacecontainerhighest", rl, " ")
    for (i = 1; i <= nr; i++) role[rl[i]] = 1
    np = split("onprimary:primary:4.5 onsecondary:secondary:4.5 ontertiary:tertiary:4.5 onprimarycontainer:primarycontainer:4.5 onsecondarycontainer:secondarycontainer:4.5 ontertiarycontainer:tertiarycontainer:4.5 onerror:error:4.5 onerrorcontainer:errorcontainer:4.5 onbackground:background:4.5 onsurface:surface:4.5 onsurfacevariant:surface:4.5 onsurfacevariant:surfacevariant:4.5 onsurface:surfacecontainerhighest:4.5 inverseonsurface:inversesurface:4.5 primary:surface:3.0 outline:surface:3.0", pr, " ")
  }
  function norm(n, path,   s, mode) {
    s = tolower(n); gsub(/_/, "", s)
    mode = "auto"
    if (s ~ /dark/) mode = "dark"; else if (s ~ /light/) mode = "light"
    gsub(/mdtheme|dealt|light|dark/, "", s)
    if (!(s in role) && s ~ /^md/ && (substr(s, 3) in role)) s = substr(s, 3)
    if (!(s in role) && s ~ /^color/ && (substr(s, 6) in role)) s = substr(s, 6)
    NORMMODE = mode
    return s
  }
  {
    g = $1; m = $2; n = $3; h = $4
    s = norm(n, g)
    if (!(s in role)) next
    if (m == "auto") { m = NORMMODE; if (m == "auto") m = "light" }
    if (g ~ /values-[A-Za-z0-9-]*night/) m = "dark"
    key = g SUBSEP m
    val[key, s] = h; groups[key] = 1; found = 1
  }
  END {
    fails = 0; checked = 0
    for (key in groups) {
      split(key, kp, SUBSEP)
      for (i = 1; i <= np; i++) {
        split(pr[i], q, ":")
        fg = q[1]; bg = q[2]; mn = q[3] + 0
        if (((key, fg) in val) && ((key, bg) in val)) {
          r = ratio(val[key, fg], val[key, bg]); checked++
          if (r < mn) { fails++; printf "FAIL  %-40s %-5s %s on %s = %.2f:1 (needs %.1f)  %s on %s\n", kp[1], kp[2], fg, bg, r, mn, val[key, fg], val[key, bg] }
        }
      }
    }
    if (!found) { print "No Material 3 role colors recognized."; exit 3 }
    printf "Checked %d pairs, %d failing.\n", checked, fails
    exit (fails > 0) ? 1 : 0
  }'
}

[[ $# -ge 1 ]] || { usage; exit 2; }

case "$1" in
  -h|--help) usage ;;
  --library)
    [[ -f "$LIB/palettes_colors.tsv" ]] || die "missing $LIB/palettes_colors.tsv"
    if [[ $# -ge 2 ]]; then
      awk -F'\t' -v p="$2" '/^#/ {next} $1==p {print $1 "\t" $2 "\t" $3 "\t" $4}' "$LIB/palettes_colors.tsv" | pair_check
    else
      awk -F'\t' '/^#/ {next} {print $1 "\t" $2 "\t" $3 "\t" $4}' "$LIB/palettes_colors.tsv" | pair_check
    fi
    ;;
  --project)
    [[ $# -ge 2 && -d "$2" ]] || die "--project needs a directory"
    ROOT="$2"
    EXCL=(--exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git --exclude-dir=.idea --exclude-dir=generated --exclude-dir=.android-taste)
    {
      grep -rnoE "${EXCL[@]}" --include='*.xml' -e '<color name="[^"]+">#[0-9A-Fa-f]{6,8}</color>' -- "$ROOT" 2>/dev/null \
        | sed -E 's|^([^:]+):[0-9]+:<color name="([^"]+)">#([0-9A-Fa-f]{6,8})</color>$|\1\tauto\t\2\t#\3|' || true
      grep -rnoE "${EXCL[@]}" --include='*.kt' -e 'val +[A-Za-z0-9_]+ *= *Color\(0x[0-9A-Fa-f]{8}\)' -- "$ROOT" 2>/dev/null \
        | sed -E 's|^([^:]+):[0-9]+:val +([A-Za-z0-9_]+) *= *Color\(0x([0-9A-Fa-f]{8})\)$|\1\tauto\t\2\t#\3|' || true
    } | pair_check
    ;;
  *)
    [[ $# -eq 2 ]] || die "give two colors, or use --library / --project (see --help)"
    awk "$AWK_FUNCS"'BEGIN {
      r = ratio(ARGV[1], ARGV[2]); ARGV[1] = ""; ARGV[2] = ""
      printf "%.2f:1\n", r
      printf "  AA  normal text (4.5): %s\n", (r >= 4.5) ? "pass" : "FAIL"
      printf "  AA  large text / UI (3.0): %s\n", (r >= 3.0) ? "pass" : "FAIL"
      printf "  AAA normal text (7.0): %s\n", (r >= 7.0) ? "pass" : "fail"
    }' "$1" "$2"
    ;;
esac
