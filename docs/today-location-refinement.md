# SALAH 3.0 — TODAY LOCATION HEADER REFINEMENT

## 1. Executive Summary

A minimal, premium location-and-date row has been integrated directly above the existing "Next prayer" hero on the Today screen.
All approved aspects of the Today screen (the next-prayer hero readout, 68sp start time, prayer timetable with all six rows, horizontal progress bar, countdown, typography, colors, and floating bottom navigation dock) have been preserved.

---

## 2. Refinements Implemented

### 2.1 Visual Design & Typography
- **Positioning**: Slim horizontal row positioned ~10dp beneath the system status area and ~18dp above the "Next prayer" eyebrow.
- **Left (Location Touch Target)**:
  - Subtle iconography:
    - **Device-Derived / GPS**: `SalahIcons.NearMe` (clean navigation arrow vector)
    - **Manual Selection**: `SalahIcons.Place` (clean location pin vector)
    - **No Location Selected**: `SalahIcons.Place` with `"Set location"` action label
  - Typography: Geist, 15sp, `FontWeight.Medium`, in neutral secondary gray (`colors.onSurfaceVariant`). No champagne gold used.
  - Subtle chevron: `SalahIcons.ChevronRight` (14dp, subtle opacity).
  - Touch target: Minimum 48dp height and width (`heightIn(min = 48.dp)`), accessible TalkBack descriptions, and `testTag("today-location")`.
  - Interaction: Tapping opens the existing location picker (`openLocation()`).
- **Right (Date Readout)**:
  - Typography: Geist, 13sp, `FontWeight.Normal`, in neutral secondary gray (`colors.onSurfaceVariant.copy(alpha = 0.85f)`).
  - Format: Locale-aware, concise pattern (`EEE, d MMM`, e.g. `Sun, 22 Nov`).
  - Timezone-aware: Explicitly uses the selected location's timezone (`location.zone`), completely consistent with the active prayer schedule. Automatically rolls over at midnight in the location's timezone.
- **Cardless & Unadorned**: Placed directly on the canvas without background containers, cards, chips, or dividers.

### 2.2 Responsive Architecture
- **Reference Devices (390–412dp @ 100% font scale)**:
  - Entire screen fits on a single viewport without scrolling:
    1. Location and date header
    2. "Next prayer" eyebrow
    3. Hero prayer name & start time
    4. Countdown & progress bar
    5. Previous prayer context
    6. "Today’s prayer times" section header
    7. All 6 prayer rows (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha)
    8. Floating bottom navigation dock
- **Long City Names**:
  - The date text on the right is measured first with its intrinsic content width.
  - The location row takes the remaining width and truncates with a one-line ellipsis immediately before the chevron. The date is never pushed off screen or hidden.
- **High Font Scaling (130% & 200%)**:
  - The date neatly wraps below the city row with an indented offset (`padding(start = 22.dp)`).
  - The screen seamlessly activates its accessible vertical scrolling container with 112dp bottom navigation clearance.

---

## 3. Verification & Quality Gates

### 3.1 Automated Test Suites
- **Connected Android Tests**: `5/5 PASSED` on `SprintPixel:5554` (API 36).
  - `singleScreenDisplaysHeaderAllSixPrayersAndFitsAboveBottomNav`: PASSED
  - `manualCitySwitchPersistsAndClearingReturnsToOnboarding`: PASSED
  - `clickingLocationOpensPickerAndSchedulesPrayer`: PASSED
- **Unit Tests**: `100% PASSED` (`./gradlew testDebugUnitTest`).
- **Lint**: `0 ERRORS`, `0 WARNINGS` (`./gradlew lintDebug`).
- **Release Build**: `BUILD SUCCESSFUL` (`./gradlew assembleRelease`).
- **Debug Build**: `BUILD SUCCESSFUL` (`./gradlew assembleDebug`).

### 3.2 Visual Captures (`screenshots/salah-today-location/`)
1. `today-dark-location.png`: Dark mode with GPS location icon (`NearMe`), city, chevron, and date.
2. `today-light-location.png`: Light mode showing warm cream background, neutral secondary text, and all 6 rows above floating navigation.
3. `today-long-city.png`: Long city name ("Kattankulathur, Chengalpattu, Ta... ›") with single-line ellipsis and intact date.
4. `today-no-location.png`: Clear "Set location ›" action row with onboarding card below.
5. `today-130pct.png`: 130% font scaling with date wrapped below city and intact timetable.
6. `today-200pct.png`: 200% font scaling with full accessibility scrolling.
7. `today-reference-all-prayers.png`: Reference single-screen layout with all six prayer rows visible above bottom navigation.

