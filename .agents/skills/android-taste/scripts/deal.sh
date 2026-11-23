#!/usr/bin/env bash
# deal.sh - deals a complete design direction for an Android app from the skill's library.
#
# Usage:
#   deal.sh --kind KIND [--seed N] [--palette NAME] [--out DIR] [--no-files]
#   deal.sh --list
#
# KIND: utility | productivity | finance | health | media | social | commerce | education | travel | game
#
# Deals: palette (Material 3 light + dark roles), type pairing, shape scale, home-screen
# layout archetype, motion personality and two "moments". Prints the direction, and writes
# ready-to-merge theme files to --out (default ./.android-taste/deal). It never touches your
# project's source files. Same --seed + same --kind = same deal.
#
# Works with bash 3.2+ and POSIX awk (gawk, mawk, BSD awk).

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
LIB="$SCRIPT_DIR/../library"
KINDS="utility productivity finance health media social commerce education travel game"

usage() { sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }
die() { echo "deal.sh: $*" >&2; exit 2; }

KIND=""; SEED=""; PALETTE=""; OUT=".android-taste/deal"; WRITE=1; LIST=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --kind)     [[ $# -ge 2 ]] || die "--kind needs a value"; KIND="$2"; shift 2 ;;
    --seed)     [[ $# -ge 2 ]] || die "--seed needs a value"; SEED="$2"; shift 2 ;;
    --palette)  [[ $# -ge 2 ]] || die "--palette needs a value"; PALETTE="$2"; shift 2 ;;
    --out)      [[ $# -ge 2 ]] || die "--out needs a value"; OUT="$2"; shift 2 ;;
    --no-files) WRITE=0; shift ;;
    --list)     LIST=1; shift ;;
    -h|--help)  usage; exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done

for f in palettes_meta.tsv palettes_colors.tsv type.tsv shapes.tsv layouts.tsv moments.tsv motion.tsv; do
  [[ -f "$LIB/$f" ]] || die "missing library file: $LIB/$f"
done

if [[ "$LIST" -eq 1 ]]; then
  echo "Palettes:"; awk -F'\t' '/^#/ {next} {printf "  %-18s [%s] %s\n", $1, $2, $3}' "$LIB/palettes_meta.tsv"
  echo; echo "Type pairings:"; awk -F'\t' '/^#/ {next} {printf "  %-18s [%s] %s / %s\n", $1, $2, $3, $4}' "$LIB/type.tsv"
  echo; echo "Shapes:"; awk -F'\t' '/^#/ {next} {printf "  %-8s [%s] corners %s/%s/%s/%s/%s dp\n", $1, $2, $3, $4, $5, $6, $7}' "$LIB/shapes.tsv"
  echo; echo "Layouts:"; awk -F'\t' '/^#/ {next} {printf "  %-20s [%s]\n", $1, $2}' "$LIB/layouts.tsv"
  exit 0
fi

[[ -n "$KIND" ]] || { usage >&2; die "--kind is required"; }
case " $KINDS " in *" $KIND "*) ;; *) die "unknown kind '$KIND'. Choose one of: $KINDS" ;; esac

if [[ -z "$SEED" ]]; then SEED=$(( (RANDOM * 32768 + RANDOM) % 1000000 )); fi
[[ "$SEED" =~ ^[0-9]+$ ]] || die "--seed must be a non-negative integer"

# pick FILE KIND KINDCOL SALT -> one random row whose kinds column contains KIND (or 'all')
pick() {
  awk -F'\t' -v kind="$2" -v kc="$3" -v seed="$SEED" -v salt="$4" '
    function nxt() { st = (st * 1103515245 + 12345) % 2147483648; return st / 2147483648 }
    function init(seed, salt,   i) { st = (seed * 7919 + salt * 104729 + 12345) % 2147483648; for (i = 0; i < 6; i++) nxt() }
    /^#/ { next }
    { n = split($kc, a, ","); ok = 0
      for (i = 1; i <= n; i++) if (a[i] == kind || a[i] == "all") ok = 1
      if (ok) rows[++c] = $0 }
    END {
      if (c == 0) exit 3
      init(seed, salt)
      print rows[int(nxt() * c) + 1]
    }' "$1"
}

# pick2 FILE KIND KINDCOL SALT -> two distinct rows (or one if only one matches)
pick2() {
  awk -F'\t' -v kind="$2" -v kc="$3" -v seed="$SEED" -v salt="$4" '
    function nxt() { st = (st * 1103515245 + 12345) % 2147483648; return st / 2147483648 }
    function init(seed, salt,   i) { st = (seed * 7919 + salt * 104729 + 12345) % 2147483648; for (i = 0; i < 6; i++) nxt() }
    /^#/ { next }
    { n = split($kc, a, ","); ok = 0
      for (i = 1; i <= n; i++) if (a[i] == kind || a[i] == "all") ok = 1
      if (ok) rows[++c] = $0 }
    END {
      if (c == 0) exit 3
      init(seed, salt)
      i1 = int(nxt() * c) + 1
      print rows[i1]
      if (c > 1) { i2 = int(nxt() * (c - 1)) + 1; if (i2 >= i1) i2++; print rows[i2] }
    }' "$1"
}

if [[ -n "$PALETTE" ]]; then
  PAL_ROW="$(awk -F'\t' -v n="$PALETTE" '/^#/ {next} $1==n {print; exit}' "$LIB/palettes_meta.tsv")"
  [[ -n "$PAL_ROW" ]] || die "unknown palette '$PALETTE' (see --list)"
else
  PAL_ROW="$(pick "$LIB/palettes_meta.tsv" "$KIND" 2 11)" || die "no palette for kind $KIND"
fi
TYPE_ROW="$(pick "$LIB/type.tsv" "$KIND" 2 23)"   || die "no type pairing for kind $KIND"
SHAPE_ROW="$(pick "$LIB/shapes.tsv" "$KIND" 2 37)" || die "no shape set for kind $KIND"
LAYOUT_ROW="$(pick "$LIB/layouts.tsv" "$KIND" 2 41)" || die "no layout for kind $KIND"
MOTION_ROW="$(awk -F'\t' -v k="$KIND" '/^#/ {next} $1==k {print; exit}' "$LIB/motion.tsv")"
MOMENT_ROWS="$(pick2 "$LIB/moments.tsv" "$KIND" 2 53)" || die "no moments for kind $KIND"

field() { printf '%s' "$1" | awk -F'\t' -v n="$2" '{print $n}'; }

PAL_NAME="$(field "$PAL_ROW" 1)"; PAL_MOOD="$(field "$PAL_ROW" 3)"
TYPE_NAME="$(field "$TYPE_ROW" 1)"; TYPE_DISPLAY="$(field "$TYPE_ROW" 3)"; TYPE_BODY="$(field "$TYPE_ROW" 4)"
TYPE_NUM="$(field "$TYPE_ROW" 5)"; TYPE_NOTE="$(field "$TYPE_ROW" 6)"
SHAPE_NAME="$(field "$SHAPE_ROW" 1)"; SH_XS="$(field "$SHAPE_ROW" 3)"; SH_S="$(field "$SHAPE_ROW" 4)"
SH_M="$(field "$SHAPE_ROW" 5)"; SH_L="$(field "$SHAPE_ROW" 6)"; SH_XL="$(field "$SHAPE_ROW" 7)"; SH_NOTE="$(field "$SHAPE_ROW" 8)"
LAY_ID="$(field "$LAYOUT_ROW" 1)"; LAY_HOME="$(field "$LAYOUT_ROW" 3)"
LAY_COMPACT="$(field "$LAYOUT_ROW" 4)"; LAY_EXPANDED="$(field "$LAYOUT_ROW" 5)"
MO_PERS="$(field "$MOTION_ROW" 2)"; MO_DUR="$(field "$MOTION_ROW" 3)"; MO_EASE="$(field "$MOTION_ROW" 4)"; MO_NOTE="$(field "$MOTION_ROW" 5)"

PRIMARY_L="$(awk -F'\t' -v p="$PAL_NAME" '$1==p && $2=="light" && $3=="primary" {print $4}' "$LIB/palettes_colors.tsv")"
PRIMARY_D="$(awk -F'\t' -v p="$PAL_NAME" '$1==p && $2=="dark" && $3=="primary" {print $4}' "$LIB/palettes_colors.tsv")"
[[ -n "$PRIMARY_L" ]] || die "palette '$PAL_NAME' has no colors in palettes_colors.tsv"

direction() {
  printf 'DEALT DIRECTION\n'
  printf '  kind      : %s   (seed %s, rerun with --seed %s to reproduce)\n' "$KIND" "$SEED" "$SEED"
  printf '  palette   : %s - %s\n' "$PAL_NAME" "$PAL_MOOD"
  printf '              light primary %s, dark primary %s\n' "$PRIMARY_L" "$PRIMARY_D"
  printf '  type      : %s / %s   (%s)\n' "$TYPE_DISPLAY" "$TYPE_BODY" "$TYPE_NAME"
  printf '              numerals: %s\n' "$TYPE_NUM"
  printf '              %s\n' "$TYPE_NOTE"
  printf '  shape     : %s - corners xs/s/m/l/xl = %s/%s/%s/%s/%s dp (%s)\n' "$SHAPE_NAME" "$SH_XS" "$SH_S" "$SH_M" "$SH_L" "$SH_XL" "$SH_NOTE"
  printf '  layout    : %s\n' "$LAY_ID"
  printf '              home      : %s\n' "$LAY_HOME"
  printf '              compact   : %s\n' "$LAY_COMPACT"
  printf '              expanded  : %s\n' "$LAY_EXPANDED"
  printf '  motion    : %s, %s ms, %s (%s)\n' "$MO_PERS" "$MO_DUR" "$MO_EASE" "$MO_NOTE"
  printf '  moments   :\n'
  printf '%s\n' "$MOMENT_ROWS" | awk -F'\t' '{printf "              - %s: %s\n                Compose: %s\n                Views:   %s\n", $1, $3, $4, $5}'
  printf '  always    : a hero entrance on the home screen, edge-to-edge, dark theme, window size classes, 48dp targets\n'
}

direction

if [[ "$WRITE" -eq 0 ]]; then exit 0; fi

mkdir -p "$OUT/compose" "$OUT/res/values" "$OUT/res/values-night"

# ---- Compose color schemes and shapes
compose_scheme() {  # palette mode fnName ctor
  awk -F'\t' -v p="$1" -v m="$2" -v fn="$3" -v ctor="$4" '
    BEGIN {
      n = split("primary onPrimary primaryContainer onPrimaryContainer inversePrimary secondary onSecondary secondaryContainer onSecondaryContainer tertiary onTertiary tertiaryContainer onTertiaryContainer background onBackground surface onSurface surfaceVariant onSurfaceVariant surfaceTint inverseSurface inverseOnSurface error onError errorContainer onErrorContainer outline outlineVariant scrim surfaceBright surfaceDim surfaceContainer surfaceContainerHigh surfaceContainerHighest surfaceContainerLow surfaceContainerLowest", order, " ")
    }
    /^#/ { next }
    $1 == p && $2 == m { v[$3] = $4 }
    END {
      v["surfaceTint"] = v["primary"]; v["scrim"] = "#000000"
      printf "val %s = %s(\n", fn, ctor
      for (i = 1; i <= n; i++) printf "    %s = Color(0xFF%s),\n", order[i], substr(v[order[i]], 2)
      print ")"
    }' "$LIB/palettes_colors.tsv"
}

{
  echo "// Dealt by android-taste: palette=$PAL_NAME shape=$SHAPE_NAME (kind=$KIND, seed=$SEED)."
  echo "// Merge into your theme package. Add 'package ...' and imports to match your project."
  echo "// surfaceContainer*, surfaceBright and surfaceDim need a recent Compose Material 3;"
  echo "// delete lines your version's ColorScheme does not have."
  echo "import androidx.compose.foundation.shape.RoundedCornerShape"
  echo "import androidx.compose.material3.Shapes"
  echo "import androidx.compose.material3.darkColorScheme"
  echo "import androidx.compose.material3.lightColorScheme"
  echo "import androidx.compose.ui.graphics.Color"
  echo "import androidx.compose.ui.unit.dp"
  echo
  compose_scheme "$PAL_NAME" light DealtLightColorScheme lightColorScheme
  echo
  compose_scheme "$PAL_NAME" dark DealtDarkColorScheme darkColorScheme
  echo
  echo "val DealtShapes = Shapes("
  echo "    extraSmall = RoundedCornerShape(${SH_XS}.dp),"
  echo "    small = RoundedCornerShape(${SH_S}.dp),"
  echo "    medium = RoundedCornerShape(${SH_M}.dp),"
  echo "    large = RoundedCornerShape(${SH_L}.dp),"
  echo "    extraLarge = RoundedCornerShape(${SH_XL}.dp),"
  echo ")"
} > "$OUT/compose/DealtTheme.kt"

# ---- XML colors (light + night) and a Material 3 theme mapping
xml_colors() {  # palette mode
  awk -F'\t' -v p="$1" -v m="$2" '
    function snake(s,   i, c, o) { o = ""; for (i = 1; i <= length(s); i++) { c = substr(s, i, 1); if (c ~ /[A-Z]/) o = o "_" tolower(c); else o = o c } return o }
    /^#/ { next }
    $1 == p && $2 == m { names[++n] = $3; v[$3] = $4 }
    END {
      print "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
      print "<!-- Dealt by android-taste (palette " p ", " m "). -->"
      print "<resources>"
      for (i = 1; i <= n; i++) printf "    <color name=\"dealt_%s\">%s</color>\n", snake(names[i]), v[names[i]]
      print "</resources>"
    }' "$LIB/palettes_colors.tsv"
}
xml_colors "$PAL_NAME" light > "$OUT/res/values/dealt_colors.xml"
xml_colors "$PAL_NAME" dark  > "$OUT/res/values-night/dealt_colors.xml"

awk -v xs="$SH_XS" -v s="$SH_S" -v m="$SH_M" -v l="$SH_L" -v xl="$SH_XL" -v pal="$PAL_NAME" -v shp="$SHAPE_NAME" 'BEGIN {
  print "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
  print "<!-- Dealt by android-taste (palette " pal ", shape " shp ")."
  print "     Attribute availability depends on your Material Components version (surfaceContainer* needs a recent one)."
  print "     Rename the style and set it as the application theme. Light/dark values come from values/ and values-night/. -->"
  print "<resources>"
  print "    <style name=\"Theme.App.Dealt\" parent=\"Theme.Material3.DayNight.NoActionBar\">"
  n = split("colorPrimary:primary colorOnPrimary:on_primary colorPrimaryContainer:primary_container colorOnPrimaryContainer:on_primary_container colorSecondary:secondary colorOnSecondary:on_secondary colorSecondaryContainer:secondary_container colorOnSecondaryContainer:on_secondary_container colorTertiary:tertiary colorOnTertiary:on_tertiary colorTertiaryContainer:tertiary_container colorOnTertiaryContainer:on_tertiary_container colorError:error colorOnError:on_error colorErrorContainer:error_container colorOnErrorContainer:on_error_container android:colorBackground:background colorOnBackground:on_background colorSurface:surface colorOnSurface:on_surface colorSurfaceVariant:surface_variant colorOnSurfaceVariant:on_surface_variant colorOutline:outline colorOutlineVariant:outline_variant colorSurfaceInverse:inverse_surface colorOnSurfaceInverse:inverse_on_surface colorPrimaryInverse:inverse_primary colorSurfaceDim:surface_dim colorSurfaceBright:surface_bright colorSurfaceContainerLowest:surface_container_lowest colorSurfaceContainerLow:surface_container_low colorSurfaceContainer:surface_container colorSurfaceContainerHigh:surface_container_high colorSurfaceContainerHighest:surface_container_highest", pairs, " ")
  for (i = 1; i <= n; i++) { k = split(pairs[i], kv, ":"); if (k == 3) { attr = kv[1] ":" kv[2]; col = kv[3] } else { attr = kv[1]; col = kv[2] }
    printf "        <item name=\"%s\">@color/dealt_%s</item>\n", attr, col }
  print "        <item name=\"shapeAppearanceCornerExtraSmall\">@style/ShapeAppearance.Dealt.Corner.ExtraSmall</item>"
  print "        <item name=\"shapeAppearanceCornerSmall\">@style/ShapeAppearance.Dealt.Corner.Small</item>"
  print "        <item name=\"shapeAppearanceCornerMedium\">@style/ShapeAppearance.Dealt.Corner.Medium</item>"
  print "        <item name=\"shapeAppearanceCornerLarge\">@style/ShapeAppearance.Dealt.Corner.Large</item>"
  print "        <item name=\"shapeAppearanceCornerExtraLarge\">@style/ShapeAppearance.Dealt.Corner.ExtraLarge</item>"
  print "    </style>"
  split("ExtraSmall:" xs " Small:" s " Medium:" m " Large:" l " ExtraLarge:" xl, sh, " ")
  for (i = 1; i <= 5; i++) { split(sh[i], p, ":")
    printf "    <style name=\"ShapeAppearance.Dealt.Corner.%s\">\n        <item name=\"cornerFamily\">rounded</item>\n        <item name=\"cornerSize\">%sdp</item>\n    </style>\n", p[1], p[2] }
  print "</resources>"
}' > "$OUT/res/values/dealt_themes.xml"

# ---- Direction record for later steps
{
  echo "# Dealt direction"
  echo
  direction | sed 's/^/    /'
  echo
  echo "Files in this folder are starting points. Merge them into the project, then delete the Dealt prefix"
  echo "where it makes sense. Run scripts/contrast.sh --project <root> after merging."
} > "$OUT/DEAL.md"

echo
echo "Wrote: $OUT/compose/DealtTheme.kt, $OUT/res/values/dealt_colors.xml, $OUT/res/values-night/dealt_colors.xml, $OUT/res/values/dealt_themes.xml, $OUT/DEAL.md"
