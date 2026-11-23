# SALAH 3.0 — MILESTONE 5: PRAYER CALENDAR
## Native Jetpack Compose Calendar Architecture, Implementation & Verification Report

---

### Executive Summary

Milestone 5 replaces Salah's placeholder calendar with a complete, production-grade native Jetpack Compose Prayer Calendar following the approved **Editorial Luxury** design identity.

The calendar provides:
- A navigable 7-column Gregorian monthly grid with locale-aware weekday alignment and first-day-of-week policies.
- Accurate, clearly documented calculated Hijri dates using the Umm al-Qura calendar (`HijrahChronology.INSTANCE`).
- Smooth, reactive selected-date switching that updates timings immediately without layout flicker or obsolete network requests.
- Complete 6-prayer schedule display (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) adhering to the user's active location, calculation methods, juristic Asr conventions, minute adjustments, and 12/24-hour time formats.
- Seamless hybrid online, Room cache, and Adhan Kotlin offline operation.
- High-contrast, accessibility-tested visual presentation supporting 130% and 200% system font scaling, narrow 320dp layouts, and full TalkBack semantics.

---

### 1. Architecture & Design Principles

#### Component Structure
```
app/src/main/kotlin/com/salah/app/calendar/
├── CalendarModels.kt          // CalendarDay, CalendarMonthData, CalendarScheduleState, CalendarUiState
├── CalendarDateConverter.kt   // Gregorian-to-Hijri converter & 7-column grid layout generator
├── CalendarRepository.kt      // Adapter layer abstracting Room cache, online API, and preferences
├── CalendarViewModel.kt       // Lifecycle-aware ViewModel with reactive state derivation & cancellation
└── CalendarScreen.kt          // Native Jetpack Compose screen with Editorial Luxury styling
```

#### State & Data Flow
```
User Tap / Month Navigation
       │
       ▼
CalendarViewModel (StateFlow<CalendarUiState>)
  ├── displayedMonth: YearMonth
  ├── selectedDate: LocalDate
  └── scheduleResultFlow: Flow<ScheduleStateSnapshot>
       │
       ├── LocationPreferences.selected
       ├── PrayerPreferences.settings
       └── CalendarRepository.schedule(request)
            ├── 1. Room Cache (instant lookup)
            ├── 2. AlAdhan API (background fetch & cache update)
            └── 3. Adhan Kotlin (offline calculation fallback)
```

---

### 2. Gregorian & Hijri Date Conversion Policy

#### Chronology & Range
- **Implementation**: Android's standard `java.time.chrono.HijrahChronology.INSTANCE` (`Hijrah-umalqura`, Umm al-Qura calendar).
- **Supported Range**: Hijri years 1300 AH to 1600 AH (approximately 1882 CE to 2076 CE). Dates outside this range are handled gracefully without application crashes.
- **Civil Date Alignment**: Traditional Islamic days begin at sunset (Maghrib). The calendar displays **civil-date calendar labels** aligned with midnight-to-midnight Gregorian days. This distinction is clearly documented in the UI via the caption: *"Calculated Umm al-Qura calendar"*.
- **Regional Variation Notice**: Islamic dates vary globally based on local astronomical calculations and regional moon-sighting declarations. The Umm al-Qura calendar represents calculated astronomical civil dates and does not claim to replace official local sightings.

#### Month Context & Span Summaries
When a Gregorian month spans two Hijri months, the header displays the calculated span:
- Example: **November 2026** spans 21 Jumada al-Awwal to 20 Jumada al-Akhirah:
  `Jumada al-Awwal – Jumada al-Akhirah 1448 AH`
- When crossing a Hijri year boundary (e.g., June 2025):
  `Dhu al-Hijjah 1446 – Muharram 1447 AH`

---

### 3. Visual Craft & Editorial Luxury Styling

#### Dark & Light Color Tokens
- **Dark Mode**:
  - Canvas: Obsidian `#090A0D`
  - Grid Container: Neutral Slate `#1C1D22`
  - Selected Day Pill: Champagne Gold `#D2AE70` with `#17130A` high-contrast bold numeral
  - Today Indicator (unselected): 1.5dp Champagne Gold border ring `#D2AE70`
  - Secondary Hijri Text: Muted Slate `#999BA3`
- **Light Mode**:
  - Canvas: Warm Porcelain `#F8F7F4`
  - Grid Container: Soft Tonal Surface `#F1EFEA`
  - Selected Day Pill: Deep Golden Bronze `#875F1A` with `#FFFFFF` text
  - Today Indicator (unselected): 1.5dp Bronze border ring `#875F1A`
  - Secondary Hijri Text: Neutral Gray `#71747E`

#### Typography
- Uses the project's variable **Geist** font:
  - Month Heading: 28sp SemiBold, `-0.5sp` tracking
  - Weekday Labels: 12sp Medium
  - Gregorian Day Numerals: 15–16sp Medium (tabular numerals `tnum`)
  - Hijri Day Numerals: 10–11sp Regular
  - Selected Date Heading: 20sp SemiBold
  - Prayer Times: 18sp Medium, tabular numerals (`tnum`)

#### Accessibility & 200% Font Scaling
- At extreme font scaling (`fontScale >= 1.5f`), secondary Hijri numbers inside individual calendar cells are automatically hidden to preserve minimum 44dp touch targets and eliminate text collisions.
- The full Hijri date is prominently presented in the selected-date schedule card directly below the grid.
- Full TalkBack semantics announce day, full Gregorian date, today status, selection status, and full Hijri date (e.g., *"Sunday, 22 November 2026, today, selected, 12 Jumada al-Akhirah 1448 AH"*).

---

### 4. Quality Gates & Test Results

```
./gradlew assembleDebug assembleRelease testDebugUnitTest connectedDebugAndroidTest lintDebug
```

| Verification Gate | Result | Notes |
| :--- | :--- | :--- |
| **assembleDebug** | **PASSED** | Compiled in 4s |
| **assembleRelease** | **PASSED** | R8 / ProGuard optimization clean |
| **testDebugUnitTest** | **PASSED** | 31 unit tests (100% pass rate) |
| **connectedDebugAndroidTest** | **PASSED** | 4/4 instrumentation tests on `SprintPixel (Android 16 API 36)` |
| **lintDebug** | **PASSED** | 0 errors, 0 warnings |

---

### 5. Verification Gallery

The following actual emulator screenshots are preserved under `screenshots/salah-calendar/`:

1. `calendar-dark.png`: Current month in dark mode with active schedule.
2. `calendar-light.png`: Current month in light mode with warm porcelain surfaces.
3. `calendar-selected-date.png`: Selecting 11 November with updated schedule.
4. `calendar-next-month.png`: Next month navigation (December 2026).
5. `calendar-previous-month.png`: Previous month navigation (October 2026).
6. `calendar-hijri-boundary.png`: Navigating to the exact lunar month boundary (1 Jumada al-Akhirah).
7. `calendar-offline.png`: Offline calculation fallback displaying the *"Offline"* source badge.
8. `calendar-no-location.png`: Polished no-location prompt with *"Choose a city"* action.
9. `calendar-130pct-font.png`: Accessible rendering at 130% font scale.
10. `calendar-200pct-font.png`: Responsive layout at 200% font scale with secondary Hijri cell hiding.
11. `calendar-narrow-320dp.png`: Responsive rendering on narrow 320dp display width.
12. `calendar-12h.png`: 12-hour format display (`5:31 AM`, `11:47 AM`).
13. `calendar-24h.png`: 24-hour format display (`05:31`, `11:47`).

