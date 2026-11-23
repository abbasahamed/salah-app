#!/usr/bin/env bash
# gate.sh - the finish gate. Runs the taste checks and decides whether the build may be handed over.
#
# Usage:
#   gate.sh [--project DIR] [--shots DIR] [--need-shots N] [--lint] [--max-rounds N]
#   gate.sh --reset [--project DIR]
#
# Checks:
#   taste lint      FAIL on any unwaived T0 or T1 finding, WARN on T2   (scripts/taste_lint.sh)
#   contrast        FAIL on any Material 3 role pair below WCAG        (scripts/contrast.sh --project)
#   screenshots     with --need-shots N, FAIL unless DIR has >= N PNGs (scripts/shoot.sh output)
#   gradle lint     with --lint, runs ./gradlew lint and FAILs if it fails
#
# Waivers: put one substring per line in DIR/.android-taste/waivers.txt (lines starting # are comments)
# to accept a finding on purpose, e.g. "System font only". Write the reason on the line above.
#
# Rounds: each failing run increments DIR/.android-taste/gate_round. After --max-rounds (default 3)
# failing rounds it exits 3: stop fixing, hand over, and list what is still open. A passing run resets it.
# Exit codes: 0 pass, 1 fail (fix and rerun), 3 failed and out of rounds, 2 usage error.

set -uo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="."; SHOTS=""; NEED=0; GRADLE_LINT=0; MAXR=3; RESET=0
die() { echo "gate.sh: $*" >&2; exit 2; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --project)     [[ $# -ge 2 ]] || die "--project needs a value"; PROJECT="$2"; shift 2 ;;
    --shots)       [[ $# -ge 2 ]] || die "--shots needs a value"; SHOTS="$2"; shift 2 ;;
    --need-shots)  [[ $# -ge 2 ]] || die "--need-shots needs a value"; NEED="$2"; shift 2 ;;
    --lint)        GRADLE_LINT=1; shift ;;
    --max-rounds)  [[ $# -ge 2 ]] || die "--max-rounds needs a value"; MAXR="$2"; shift 2 ;;
    --reset)       RESET=1; shift ;;
    -h|--help)     sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) die "unknown argument: $1 (try --help)" ;;
  esac
done
[[ -d "$PROJECT" ]] || die "not a directory: $PROJECT"
[[ "$NEED" =~ ^[0-9]+$ && "$MAXR" =~ ^[0-9]+$ ]] || die "--need-shots and --max-rounds must be integers"

STATE_DIR="$PROJECT/.android-taste"
ROUND_FILE="$STATE_DIR/gate_round"
WAIVERS="$STATE_DIR/waivers.txt"
if [[ "$RESET" -eq 1 ]]; then rm -f "$ROUND_FILE"; echo "gate round counter reset."; exit 0; fi
[[ -n "$SHOTS" ]] || SHOTS="$PROJECT/taste-shots"

mkdir -p "$STATE_DIR"
round=0; [[ -f "$ROUND_FILE" ]] && round="$(cat "$ROUND_FILE" 2>/dev/null || echo 0)"
[[ "$round" =~ ^[0-9]+$ ]] || round=0
round=$((round + 1))

FAILS=0; WARNS=0
pass() { printf '  PASS  %s\n' "$1"; }
warn() { printf '  WARN  %s\n' "$1"; WARNS=$((WARNS + 1)); }
fail() { printf '  FAIL  %s\n' "$1"; FAILS=$((FAILS + 1)); }

echo "android-taste gate, round $round of $MAXR, project: $(cd "$PROJECT" && pwd)"

# ---- taste lint with waivers
lint_out="$(bash "$SCRIPT_DIR/taste_lint.sh" --max 3 "$PROJECT" 2>&1 || true)"
waive_pat=""
if [[ -f "$WAIVERS" ]]; then waive_pat="$(grep -vE '^[[:space:]]*(#|$)' "$WAIVERS" || true)"; fi
unwaived="$(printf '%s\n' "$lint_out" | awk -v waivers="$waive_pat" '
  BEGIN { n = split(waivers, w, "\n") }
  /^== \[T[012]\]/ {
    skip = 0
    for (i = 1; i <= n; i++) if (w[i] != "" && index($0, w[i]) > 0) skip = 1
    if (!skip) print
  }')"
t0="$(printf '%s\n' "$unwaived" | grep -c '^== \[T0\]' || true)"
t1="$(printf '%s\n' "$unwaived" | grep -c '^== \[T1\]' || true)"
t2="$(printf '%s\n' "$unwaived" | grep -c '^== \[T2\]' || true)"
if [[ "$t0" -gt 0 || "$t1" -gt 0 ]]; then
  fail "taste lint: $t0 T0 and $t1 T1 finding(s) open ($t2 T2)"
  printf '%s\n' "$unwaived" | grep -E '^== \[T[01]\]' | sed 's/^== /        /' | cut -c1-150
elif [[ "$t2" -gt 0 ]]; then
  warn "taste lint: $t2 T2 polish finding(s) open"
  printf '%s\n' "$unwaived" | grep -E '^== \[T2\]' | sed 's/^== /        /' | cut -c1-150
else
  pass "taste lint: clean"
fi

# ---- contrast
c_out="$(bash "$SCRIPT_DIR/contrast.sh" --project "$PROJECT" 2>&1)"; c_rc=$?
case "$c_rc" in
  0) pass "contrast: $(printf '%s\n' "$c_out" | tail -1)" ;;
  1) fail "contrast: role pairs below WCAG"; printf '%s\n' "$c_out" | grep '^FAIL' | head -8 | sed 's/^/        /' | cut -c1-170 ;;
  3) warn "contrast: no Material 3 role colors recognized (check contrast by eye in screenshots)" ;;
  *) warn "contrast: checker error: $c_out" ;;
esac

# ---- screenshots
if [[ "$NEED" -gt 0 ]]; then
  have=0; [[ -d "$SHOTS" ]] && have="$(find "$SHOTS" -maxdepth 1 -type f -name '*.png' | wc -l | tr -d ' ')"
  if [[ "$have" -ge "$NEED" ]]; then pass "screenshots: $have in $SHOTS"
  else fail "screenshots: $have of $NEED required in $SHOTS (run scripts/shoot.sh, then look at them)"; fi
fi

# ---- gradle lint
if [[ "$GRADLE_LINT" -eq 1 ]]; then
  if [[ -x "$PROJECT/gradlew" ]]; then
    if (cd "$PROJECT" && ./gradlew lint >/tmp/android-taste-lint.log 2>&1); then pass "gradle lint"
    else fail "gradle lint failed (see /tmp/android-taste-lint.log)"; fi
  else warn "gradle lint requested but $PROJECT/gradlew is missing or not executable"; fi
fi

echo
if [[ "$FAILS" -eq 0 ]]; then
  rm -f "$ROUND_FILE"
  if [[ "$WARNS" -gt 0 ]]; then echo "GATE PASSED with $WARNS warning(s). Hand over."; else echo "GATE PASSED. Hand over."; fi
  exit 0
fi
printf '%s\n' "$round" > "$ROUND_FILE"
if [[ "$round" -ge "$MAXR" ]]; then
  echo "GATE FAILED and out of rounds ($round of $MAXR). Stop fixing. Hand over with the open findings listed, or waive them on purpose in $WAIVERS."
  exit 3
fi
echo "GATE FAILED ($FAILS). Fix the findings above and rerun (round $round of $MAXR)."
exit 1
