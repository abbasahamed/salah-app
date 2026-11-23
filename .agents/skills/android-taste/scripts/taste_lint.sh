#!/usr/bin/env bash
# taste_lint.sh - read-only lint for generic, template-looking Android UI.
#
# Usage: taste_lint.sh [--max N] [project-root]
#   --max N   show at most N example hits per rule (default 4)
#
# Severity:
#   T0  ships the template or placeholder look (default palette, Greeting, lorem ipsum, #3DDC84 icon)
#   T1  breaks the baseline (inline colors, no dark theme, no edge-to-edge, no adaptive layout,
#       mixed icon families, emoji as icons, many odd dp values)
#   T2  polish (system font only, generic copy, no motion, no splash, no themed icon, no previews)
#
# Candidates only: read each hit in context. The last line is machine readable:
#   TASTE_SUMMARY T0=<n> T1=<n> T2=<n>
# Always exits 0; scripts/gate.sh decides what blocks.
#
# Works with bash 3.2+ and GNU/BSD grep.

set -uo pipefail

MAX=4
ROOT="."
while [[ $# -gt 0 ]]; do
  case "$1" in
    --max) [[ $# -ge 2 ]] || { echo "--max needs a number" >&2; exit 2; }; MAX="$2"; shift 2 ;;
    -h|--help) sed -n '2,19p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) ROOT="$1"; shift ;;
  esac
done
[[ -d "$ROOT" ]] || { echo "Not a directory: $ROOT" >&2; exit 2; }
[[ "$MAX" =~ ^[0-9]+$ ]] || { echo "--max must be a non-negative integer" >&2; exit 2; }

EXCL=(--exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git --exclude-dir=.idea
      --exclude-dir=generated --exclude-dir=node_modules --exclude-dir=.android-taste --exclude-dir=taste-shots)
T0=0; T1=0; T2=0

bump() { case "$1" in T0) T0=$((T0 + $2)) ;; T1) T1=$((T1 + $2)) ;; T2) T2=$((T2 + $2)) ;; esac; }

show() {  # SEV LABEL COUNT HITS
  printf '\n== [%s] %s (%s) ==\n' "$1" "$2" "$3"
  if [[ "$MAX" -gt 0 && -n "${4:-}" ]]; then
    printf '%s\n' "$4" | head -n "$MAX" | cut -c1-170 | sed 's/^/  /'
    if [[ "$3" -gt "$MAX" ]]; then printf '  ... %s more\n' "$(($3 - MAX))"; fi
  fi
}

# scanf SEV LABEL GREPFLAGS REGEX GLOB...
scanf() {
  local sev="$1" label="$2" flags="$3" regex="$4"; shift 4
  local inc=() g out count
  for g in "$@"; do inc+=("--include=$g"); done
  # shellcheck disable=SC2086
  out="$(LC_ALL=C grep -rn $flags "${EXCL[@]}" "${inc[@]}" -e "$regex" -- "$ROOT" 2>/dev/null || true)"
  [[ -z "$out" ]] && return 0
  count="$(printf '%s\n' "$out" | wc -l | tr -d ' ')"
  bump "$sev" "$count"; show "$sev" "$label" "$count" "$out"
}

has_match() {  # REGEX GLOB... -> 0 if found
  local regex="$1"; shift
  local inc=() g
  for g in "$@"; do inc+=("--include=$g"); done
  LC_ALL=C grep -rqE "${EXCL[@]}" "${inc[@]}" -e "$regex" -- "$ROOT" 2>/dev/null
}
has_dir() { find "$ROOT" \( -name build -o -name .git -o -name .gradle \) -prune -o -type d -name "$1" -print 2>/dev/null | grep -q .; }
has_files() { find "$ROOT" \( -name build -o -name .git -o -name .gradle \) -prune -o -type f -name "$1" -print 2>/dev/null | grep -q .; }
absent() {  # SEV LABEL  (reports a missing capability, counts 1)
  bump "$1" 1; printf '\n== [%s] %s ==\n' "$1" "$2"
}

printf 'android-taste lint of: %s\n' "$(cd "$ROOT" && pwd)"

HAS_KT=0; HAS_XML_RES=0; HAS_COMPOSE=0
has_files '*.kt' && HAS_KT=1
has_dir 'res' && HAS_XML_RES=1
has_match 'androidx[.]compose' '*.kt' '*.kts' '*.toml' && HAS_COMPOSE=1

# ------------------------------------------------------------ T0: template and placeholder look
scanf T0 "Android Studio template palette (Purple/Pink baseline)" -iE '0xFF(D0BCFF|CCC2DC|EFB8C8|6650a4|625b71|7D5260)|[^A-Za-z](Purple|PurpleGrey|Pink)(40|80)[^0-9]' '*.kt'
scanf T0 "Template palette in XML colors" -iE '#(FF)?(D0BCFF|CCC2DC|EFB8C8|6650a4|625b71|7D5260)' '*.xml'
scanf T0 "Template greeting / placeholder content" -E 'Hello (Android|World)|fun Greeting[(]|"Hello \$name|Greeting[(]' '*.kt' '*.java' '*.xml'
scanf T0 "Lorem ipsum" -iE 'lorem ipsum|dolor sit amet' '*.kt' '*.java' '*.xml' '*.json'
scanf T0 "Template theme name (Theme.MyApplication / MyApp)" -E 'Theme[.](MyApplication|MyApp|Application|Test|Demo|Sample)([^A-Za-z]|$)' '*.xml' '*.kt'
scanf T0 "Default launcher icon green (#3DDC84)" -iE '#3DDC84' '*.xml'
scanf T0 "Named Android colors (Color.Red, Color.Blue, ...)" -E 'Color[.](Red|Green|Blue|Yellow|Cyan|Magenta|Gray|LightGray|DarkGray)([^A-Za-z]|$)' '*.kt'

# ------------------------------------------------------------ T1: baseline
scanf T1 "dynamicColor defaulted to true (dealt palette is ignored on Android 12+ unless deliberate)" -E 'dynamicColor *: *Boolean *= *true' '*.kt'
scanf T1 "Color.White / Color.Black literals (break dark theme)" -E 'Color[.](White|Black)([^A-Za-z]|$)' '*.kt'
scanf T1 "Hardcoded hex in layouts and drawables" -E '(android|app):[A-Za-z_]+ *= *"#[0-9A-Fa-f]{3,8}"' '*.xml'
scanf T1 "Platform color resources (@android:color/holo, white, black)" -E '@android:color/(holo|white|black|darker_gray|background_)' '*.xml'

# inline Color(0x...) outside theme/color files
inline="$(LC_ALL=C grep -rn "${EXCL[@]}" --include='*.kt' -e 'Color(0x[0-9A-Fa-f]\{6,8\})' -- "$ROOT" 2>/dev/null \
  | grep -viE '^[^:]*/[^/:]*(color|theme|palette|scheme|dealt)[^/:]*[.]kt:' || true)"
if [[ -n "$inline" ]]; then
  c="$(printf '%s\n' "$inline" | wc -l | tr -d ' ')"; bump T1 "$c"
  show T1 "Inline Color(0x...) outside theme files (use MaterialTheme.colorScheme roles)" "$c" "$inline"
fi

if [[ "$HAS_KT" -eq 1 || "$HAS_XML_RES" -eq 1 ]]; then
  if ! has_dir 'values-night' && ! has_match 'darkColorScheme|isSystemInDarkTheme|DayNight|AppCompatDelegate[.]setDefaultNightMode' '*.kt' '*.xml' '*.java'; then
    absent T1 "No dark theme found (no values-night, darkColorScheme, DayNight)"
  fi
  if ! has_match 'enableEdgeToEdge|EdgeToEdge[.]enable|setDecorFitsSystemWindows|WindowCompat' '*.kt' '*.java'; then
    absent T1 "No edge-to-edge setup found (enableEdgeToEdge / WindowCompat)"
  fi
  if ! has_match 'WindowSizeClass|currentWindowAdaptiveInfo|calculateWindowSizeClass|NavigationSuiteScaffold|ListDetailPaneScaffold|SupportingPaneScaffold|SlidingPaneLayout|WindowMetrics|WindowInfoTracker' '*.kt' '*.java' \
     && ! has_dir 'layout-sw*' && ! has_dir 'layout-w*' && ! has_dir 'values-sw*' && ! has_dir 'values-w*'; then
    absent T1 "No adaptive layout (no window size classes, no sw600dp/w840dp resources): tablets, foldables and ChromeOS get a stretched phone UI"
  fi
fi

# icon families
fam=0
for f in 'Icons[.](Filled|Default)[.]' 'Icons[.]Outlined[.]' 'Icons[.]Rounded[.]' 'Icons[.]Sharp[.]' 'Icons[.]TwoTone[.]'; do
  if has_match "$f" '*.kt'; then fam=$((fam + 1)); fi
done
fam_names="$(for f in 'Icons[.](Filled|Default)[.]:Filled' 'Icons[.]Outlined[.]:Outlined' 'Icons[.]Rounded[.]:Rounded' 'Icons[.]Sharp[.]:Sharp' 'Icons[.]TwoTone[.]:TwoTone'; do
  re="${f%%:*}"; nm="${f##*:}"; has_match "$re" '*.kt' && printf '%s ' "$nm"; done)"
if [[ "$fam" -ge 2 ]]; then
  bump T1 1; printf '\n== [T1] Mixed icon families in use: %s(pick one) ==\n' "$fam_names"
fi

scanf T1 "Emoji in UI text or resources (use real icons)" -E $'\xf0\x9f' '*.kt' '*.java' 'strings*.xml'

# odd / fractional dp values
dp_all="$({ LC_ALL=C grep -rhoE "${EXCL[@]}" --include='*.kt' -e '[0-9]+([.][0-9]+)?[.]dp' -- "$ROOT" 2>/dev/null | sed -E 's/[.]dp$//'
            LC_ALL=C grep -rhoE "${EXCL[@]}" --include='*.xml' -e '[">][0-9]+([.][0-9]+)?dp["<]' -- "$ROOT" 2>/dev/null | sed -E 's/^[">]//; s/dp["<]$//'; } || true)"
if [[ -n "$dp_all" ]]; then
  stats="$(printf '%s\n' "$dp_all" | awk '
    { t++; v = $1 + 0; frac = (v != int(v)); odd = (!frac && v > 1 && (v % 2) == 1)
      if (frac || odd) { bad++; cnt[$1]++ } }
    END { if (t == 0) exit; ex = ""; n = 0
      for (k in cnt) { if (n < 5) { ex = ex k "dp x" cnt[k] " "; n++ } }
      printf "%d %d %s\n", t, bad + 0, ex }')"
  tot="${stats%% *}"; rest="${stats#* }"; badn="${rest%% *}"; ex="${rest#* }"
  if [[ "${badn:-0}" -gt 0 ]]; then
    if [[ "$tot" -ge 20 && $((badn * 100 / tot)) -ge 15 ]]; then sev=T1; else sev=T2; fi
    bump "$sev" 1
    printf '\n== [%s] Off-grid dp values: %s of %s are odd or fractional (4dp grid, 2dp for fine detail). e.g. %s ==\n' "$sev" "$badn" "$tot" "$ex"
  fi
fi

# ------------------------------------------------------------ T2: polish
if [[ "$HAS_KT" -eq 1 || "$HAS_XML_RES" -eq 1 ]]; then
  if ! has_dir 'font' && ! has_match 'GoogleFont|FontFamily[(]|Font[(] *R[.]font|android:fontFamily|app:fontFamily' '*.kt' '*.xml'; then
    absent T2 "System font only (fine if deliberate; a dealt type pairing is what gives the app a voice)"
  fi
  if ! has_match 'animate[A-Za-z]*AsState|AnimatedVisibility|AnimatedContent|Crossfade|updateTransition|animateContentSize|animateItem|MaterialSharedAxis|MaterialContainerTransform|MaterialFadeThrough|ObjectAnimator|ViewPropertyAnimator|ValueAnimator|MotionLayout|TransitionManager|Animatable' '*.kt' '*.java' '*.xml'; then
    absent T2 "No motion at all (no animate*/AnimatedVisibility/transitions)"
  fi
  if ! has_match 'installSplashScreen|Theme[.]SplashScreen|core-splashscreen|windowSplashScreen' '*.kt' '*.java' '*.xml' '*.toml' '*.kts'; then
    absent T2 "No splash screen setup (core-splashscreen) for the launch hand-off"
  fi
  if { has_files 'ic_launcher.xml' || has_files 'ic_launcher_round.xml'; } && ! has_match '<monochrome' 'ic_launcher.xml' 'ic_launcher_round.xml'; then
    absent T2 "Adaptive icon has no <monochrome> layer (themed icons on Android 13+)"
  fi
fi

scanf T2 "Generic copy (Oops, Something went wrong, No data, Click here, Submit...)" -iE '(Oops|Something went wrong|An error occurred|No data( available)?|No results found|Click here|Welcome to|Success!|Error!|Please wait)' 'strings*.xml' '*.kt'
scanf T2 "ALL CAPS or shouty strings (sentence case reads calmer)" -E '>[A-Z0-9 ]{7,}<|>[^<]*!![^<]*<|>[^<]*![^<]*![^<]*<' 'strings*.xml'
scanf T2 "Hardcoded font sizes (use typography roles)" -E 'fontSize *= *[0-9.]+[.]sp|android:textSize *= *"[0-9.]+sp"' '*.kt' '*.xml'

comp="$(LC_ALL=C grep -rhoE "${EXCL[@]}" --include='*.kt' -e '@Composable' -- "$ROOT" 2>/dev/null | wc -l | tr -d ' ')"
prev="$(LC_ALL=C grep -rhoE "${EXCL[@]}" --include='*.kt' -e '@Preview' -- "$ROOT" 2>/dev/null | wc -l | tr -d ' ')"
if [[ "$comp" -gt 10 && "$prev" -eq 0 ]]; then absent T2 "$comp composables and no @Preview (design is judged by looking)"; fi

cpi="$(LC_ALL=C grep -rhoE "${EXCL[@]}" --include='*.kt' -e 'CircularProgressIndicator[(]' -- "$ROOT" 2>/dev/null | wc -l | tr -d ' ')"
if [[ "$cpi" -ge 3 ]]; then
  bump T2 1; printf '\n== [T2] %s bare CircularProgressIndicator() loading states (skeletons or inline progress feel designed) ==\n' "$cpi"
fi

printf '\n================ SUMMARY ================\n'
printf 'T0 (template look): %s   T1 (baseline): %s   T2 (polish): %s\n' "$T0" "$T1" "$T2"
echo "Candidates only. Read each in context before changing it."
printf 'TASTE_SUMMARY T0=%s T1=%s T2=%s\n' "$T0" "$T1" "$T2"
exit 0
