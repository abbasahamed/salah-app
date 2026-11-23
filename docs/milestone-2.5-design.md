# Salah Milestone 2.5 design

| Before | After | Why |
| --- | --- | --- |
| App bar plus oversized Today title | Compact wordmark, location and date | Prayer information starts higher on screen. |
| Tall accent block with evenly weighted text | Prayer name and tabular start time above a fine progress track | The next prayer has one clear focal point. |
| Wide provider label inside the schedule | Short method and source below the schedule | More room for all six entries. |
| Filled settings buttons and a single long card | Full-width selectors, grouped adjustments and provider information | Related settings are easier to scan. |

Light uses ivory #F8F9F6, white surfaces, ink #202B28 and deep sage #386650. Dark uses charcoal #131917, elevated #1D2521 and soft sage #A9D6BD. The hero uses a pale sage fill in light and a restrained tinted charcoal in dark. Sans-serif roles use 28, 22, 16, 14 and 12 sp. Prayer times and countdown use tabular numerals. Layout uses 18 dp screen gutters, 12 dp between sections and 20 dp card corners.

Rapid interactions use native press and menu feedback. Tab selection changes quickly, with brief color interpolation only. The progress track draws the fraction elapsed between available obligatory prayers. Countdown text updates directly and the track has no repeating animation.

The layout reuses the existing scrollable Compose scaffold and switches to a navigation rail on wide windows. The bottom bar reserves system insets, keeps 48 dp minimum tab targets, and uses icon labels for accessibility when the visual labels are hidden. Settings uses native menus, grouped adjustments and a three-way appearance selector that stacks at larger text sizes.

## Design skills

- Anthropic frontend-design informed the shared token palette, readable hierarchy and restraint around decoration.
- Emil Kowalski's design-engineering guidance informed the short selection animation, the prayer-name transition at boundaries and the decision to update the countdown without a loop.
- Salah design set the Android accessibility, source accuracy and architecture constraints used for implementation and emulator review.

The global frontend-design skill was listed in this session. The project-local Emil and Salah skills were read from their installed files because the active session did not list those local copies.

## Files

- `TodayScreen.kt` adds the header, next-prayer hero, progress track, schedule and source label.
- `SalahApp.kt` refines bottom navigation. `Screens.kt` and `CalculationSettings.kt` group appearance, calculation, adjustments and provider information.
- `SalahTheme.kt`, `Components.kt`, `MainActivity.kt` and `strings.xml` update typography, surfaces, spacing, system-bar colors and labels.
- `SalahAppTest.kt` updates its selectors for the new labels while exercising the same behaviors.

The source checkpoint is `/Users/abbas/Desktop/Salah-m2.5-backup.12ZDLf/source.tar.gz`. The project has no Git metadata. The prayer and preferences modules and both ViewModels match the checkpoint.

## Verification

- Debug APK and release APK assembled. All 26 JVM tests and both Compose UI tests passed. Android lint reported 0 errors and 14 existing target SDK, tooling and dependency update warnings.
- SprintPixel on API 36 rendered the online AlAdhan schedule, then served the saved schedule on a cold start with networking disabled. An uncached Hanafi selection rendered the offline schedule and survived a cold restart. The original Standard convention and emulator network settings were restored.
- Today showed the following day's Fajr and a changing countdown. The signed release build displayed the real no-location and unavailable states. The debug build was reinstalled and left on Today in the graphical emulator.
- Visual review covered both themes, native selectors, a 320 dp viewport, and 200% font scale. The first pass exposed a misaligned Sunrise label and an overly tall appearance group. The final pass confirmed the updated alignment, selected-state contrast and that all six times were reachable at large text sizes.
- Screenshot files are under `screenshots/ui-redesign/`. No Salah crash appeared in the emulator's crash buffer. TalkBack and physical hardware were outside the observed checks.
