# Salah 3.0 · Milestone 3 Smart Location Handoff

Salah Milestone 3 integrates full smart location capabilities into Salah 3.0's native Midnight & Champagne Jetpack Compose interface. Location serves as a validated input to the existing prayer calculation pipeline across both debug and release variants. The Today dial, six-row schedule, floating navigation dock, calculation controls, and placeholder tabs maintain their exact design tokens and component hierarchy.

---

## 1. Architecture Overview

| Module / Component | Responsibilities |
| --- | --- |
| `:core:location` | Typed immutable models (`LocationCoordinates`, `LocationAccuracy`, `SalahLocation`, `LocationMode`, `LocationPermission`), fused and platform `DeviceLocationProvider`, offline `CityIndex` reader, offline polygon-based `BoundaryTimezoneResolver`, geocoding `AndroidPlaceLabelResolver`, and DataStore-backed `LocationPreferences`. |
| `:app` | First-launch onboarding card (`LocationOnboarding`), native Midnight & Champagne search/picker screen (`LocationScreen`), `LocationViewModel`, settings controls (`LocationSettings`), Today header location trigger (`SalahHeader`), and lifecycle-aware permission observation. |
| `:core:prayer` | Location-scoped prayer requests (`PrayerLocation` with coordinates and IANA `ZoneId`), multi-key Room schedule cache, in-flight deduplication, cancellation on subscriber departure, and cache invalidation on location clear. |
| `:core:preferences` | Desugaring-enabled user preference storage for prayer calculations and appearance. |
| `:core:designsystem` | Semantic Midnight & Champagne tokens (Inter, Manrope, Canvas dial arch, container surfaces, and vector icons). |

Each location selection is validated and stored as an atomic record in DataStore. Today derives its active calendar date and daily schedule from the selected location's IANA time zone. When switching cities or resolving a new position, any obsolete schedule presentation is immediately replaced by the new location's state.

---

## 2. Location Provider & Permission Behavior

### Runtime Permission Lifecycle & Edge Cases

Salah adheres strictly to Android runtime permission guidelines:
- **No Background Location**: Salah never requests or declares `ACCESS_BACKGROUND_LOCATION`.
- **Explicit User Action Only**: Runtime permissions (`ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`) are only requested upon tapping **Use current location** or **Refresh current location**.
- **Coarse vs. Precise Support**:
  - When precise (`ACCESS_FINE_LOCATION`) is granted, Salah utilizes high-accuracy fused location and passes coordinates to the Android `Geocoder` for locality and sub-administrative labeling.
  - When approximate (`ACCESS_COARSE_LOCATION`) only is granted, Salah requests balanced-power coarse fixes, marks the location as approximate, and intentionally **bypasses reverse-geocoding** to preserve user privacy and avoid erratic label snapping. The UI displays **Approximate area**.
- **Denial & Re-prompting**: If denied once, Salah displays an inline rationale notice ("Location permission was denied. Choose a city or try again.") while keeping "Use current location" available for retry.
- **Permanent Denial / Blocked State**: If permanently denied (or user selects "Don't ask again"), Salah automatically transitions the card to show "Location access is off for Salah" with an **Open app settings** button that links to Android `ACTION_APPLICATION_DETAILS_SETTINGS`.
- **Zero Lock-in**: The user is never trapped behind a permission gate. The bundled offline city index and search remain immediately accessible at all times regardless of permission state.
- **Settings Return Path**: When permissions are modified or granted in Android system settings, Salah's `ON_RESUME` lifecycle observer immediately updates the permission state, clears the blocked flag, and restores automatic location capability.
- **Location Services Disabled**: If the system-wide location toggle is disabled, Salah identifies `LocationFailure.DISABLED` and presents a direct link to `Settings.ACTION_LOCATION_SOURCE_SETTINGS`.

### Device Location Provider (`AndroidLocationProvider`)

- Utilizes Google Play services Location 21.3.0 `FusedLocationProviderClient` with `CurrentLocationRequest` and a 9-second timeout.
- Falls back to Android `LocationManagerCompat` using foreground GPS and Network providers when Google Play services is absent or times out.
- Fixes under 30 seconds old (`FRESH_AGE_MS`) are used immediately.
- Cached fixes up to 24 hours old require explicit user confirmation via **Use last known location**. Fixes older than 24 hours are discarded.
- In-flight detection tasks cancel immediately when exiting the picker or navigating away.
- A 2 km proximity threshold allows reusing schedule coordinates for automatic fixes within the same zone and accuracy class.

---

## 3. Offline Capabilities & Bundled Assets

Salah requires zero network connectivity to search cities, resolve time zones, calculate prayer schedules, or restart.

### Bundled Offline City Index (`CityIndex`)
- Built from GeoNames `cities15000` dump (populated places >15,000 and administrative seats) supplemented with Kattankulathur (SRM IST campus coordinates).
- Packaged as a compressed TSV asset at `location/cities.tsv.gzip` (~2.85 MiB).
- Search executes fully on-device via `Dispatchers.IO`: normalizes accents/diacritics, debounces input by 300 ms, matches exact aliases and prefixes, ranks by match tier and population, and returns the top 12 matches.
- No remote autocomplete service, API keys, or network requests are involved.

### Bundled Timezone Resolver (`BoundaryTimezoneResolver`)
- Built from Timezone Boundary Builder 2026f land polygons.
- Packaged as a binary asset with delta-encoded coordinates on a 0.00001° grid at `location/timezones.bin.gzip` (~7.8 MiB).
- Resolves coordinates to exact IANA `ZoneId` entirely offline.
- Detects civil boundary proximity and uncertainty; rejects ambiguous or antimeridian points.
- Packaged with `assets/location/NOTICE.txt` and `assets/location/ODbL.txt` adhering to OpenStreetMap and ODbL 1.0 redistribution requirements.

---

## 4. Verification History & Root Cause Analysis

### Investigation of Pre-Takeover Verification Failure

During Codex's final verification, an automation command failed during a permission-edge-case test sequence. Investigation of system and logcat traces (`11-21 22:44:20`–`22:44:40`) revealed:

1. **Root Cause**:
   - In Android (API 36 / Android 16), when runtime permissions are revoked or modified in system settings (`com.android.settings/.spa.SpaActivity`), Android's `ActivityManager` intentionally terminates the app process (`Killing com.salah.app (adj 0): permissions revoked`).
   - The automation script executed an immediate command against Salah before the application process was re-attached and in the foreground, resulting in a shell/automation timeout.
2. **Classification**:
   - **Test automation / UI synchronization failure** under OS process lifecycle behavior, **not** a Salah application defect.
3. **Application Soundness**:
   - Salah's actual permission handling correctly handles process restarts, state restoration, and resumption from App Info settings without crashes or corrupted DataStore states.

### Desugaring Configuration Correction

During takeover verification, root test execution exposed that `:core:preferences` was missing `isCoreLibraryDesugaringEnabled = true` and `coreLibraryDesugaring(libs.desugar)`, causing an AAR metadata mismatch when running cross-module Android test checks. This was cleanly resolved in `core/preferences/build.gradle.kts`.

---

## 5. Verification Matrix & Results

### Automated Test Suites

| Test Suite | Scope | Result |
| --- | --- | --- |
| `:core:preferences:testDebugUnitTest` | Preferences persistence & DataStore | 4 passed, 0 failed |
| `:core:prayer:testDebugUnitTest` | Providers, domain model, MockWebServer, hybrid cache | 24 passed, 0 failed |
| `:core:location:testDebugUnitTest` | City search, timezone resolver, location repository | 11 passed, 0 failed |
| **All JVM Unit Tests** | Monorepo debug & release unit tests | **39 passed, 0 failed** |
| `:app:connectedDebugAndroidTest` | Compose UI instrumentation on SprintPixel (API 36) | **3 passed, 0 failed** |
| Android Lint (`lintDebug`) | Monorepo static analysis | **0 errors, 16 warnings** (targetSdk & dependency version suggestions only) |
| Crash Buffer (`logcat -b crash`) | SprintPixel system crash logs | **0 crashes observed** |

### Verified Permission Scenarios (15/15)

1. **Fresh install with no saved location**: Onboarding card displayed with "Use current location" and "Choose a city". Dial hidden.
2. **Precise permission granted**: Successfully resolved Kattankulathur GPS simulation, fetched AlAdhan timings, rendered dial.
3. **Approximate-only permission granted**: Successfully resolved coarse location, displayed "Approximate area", bypassed Geocoder, rendered accurate timings.
4. **Permission denied**: Card displayed "Location permission was denied. Choose a city or try again." User not trapped.
5. **Permission denied repeatedly**: Card transitioned to permanently denied state.
6. **Permanently denied / blocked state**: "Use current location" replaced by "Open app settings"; manual city selection remained fully functional.
7. **Open Android application settings**: Tapping button correctly launched Android App Info (`ACTION_APPLICATION_DETAILS_SETTINGS`).
8. **Permission restored via system settings**: Granting permission in settings and returning to Salah cleared blocked state and restored one-shot detection.
9. **Device location services disabled**: Displayed "Device location is turned off" with button linking to location settings.
10. **Location provider returning null (`NO_FIX`)**: Handled gracefully via `LocationFailure.NO_FIX` notice.
11. **Manual city selection without GPS permission**: Successfully searched and selected Kattankulathur, loaded schedule, persisted across launches.
12. **Switching from manual to automatic**: Selected current location; mode updated to AUTOMATIC.
13. **Switching from automatic to manual**: Selected city from search; mode updated to MANUAL.
14. **App restart after selecting a city**: Cold start restored saved city and schedule immediately without network.
15. **Permission revocation after initial success**: Revoking permissions preserved saved location record; schedule continued functioning with disclosure in Settings.

### Geographic Accuracy & Timezone Matrix

Simulated across SprintPixel (API 36) running device timezone `Asia/Kolkata` for November 21, 2026:

| Location | Coordinates | Timezone | Fajr | Sunrise | Dhuhr | Asr | Maghrib | Isha | Verified State |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **Kattankulathur, IN** | 12.8231, 80.0442 | `Asia/Kolkata` | 04:57 | 06:11 | 11:56 | 15:14 | 17:40 | 18:55 | Overnight Fajr countdown |
| **Makkah, SA** | 21.4225, 39.8262 | `Asia/Riyadh` | 05:18 | 06:35 | 12:07 | 15:16 | 17:38 | 18:55 | Overnight Fajr countdown |
| **London, UK** | 51.5074, -0.1278 | `Europe/London` | 05:30 | 07:28 | 11:46 | 13:44 | 16:04 | 18:03 | Active late afternoon Isha countdown |
| **New York, US (Offline)** | 40.7128, -74.0060 | `America/New_York` | 05:14 | 06:50 | 11:43 | 14:13 | 16:34 | 18:09 | Offline Adhan calculation |

---

## 6. Visual Review & Design Compliance

In compliance with `salah-design`, `frontend-design`, and `emil-design-eng`:
- **Midnight & Champagne Theme**: Pure dark mode preserved with champagne accents and tiered container hierarchy.
- **Porcelain & Bronze Theme**: Light mode verified across Today, Settings, and Location picker.
- **Narrow Viewport (320 dp / 840×2400)**: Layout gracefully reflows; bottom navigation adapts, city search rows remain legible.
- **200% Font Scale**: Text scaling checked with zero clipping, accessible touch targets (≥48 dp), and vertical scrolling preserved.

Screenshots captured directly on SprintPixel are saved in `screenshots/salah-milestone-3/`:
- `01-onboarding-dark.png`
- `02-permission-denied-dark.png`
- `03-permission-blocked-dark.png`
- `04-search-kattankulathur-dark.png`
- `05-kattankulathur-manual-dark.png`
- `06-kattankulathur-automatic-dark.png`
- `07-kattankulathur-zone-dark.png`
- `08-device-location-disabled-dark.png`
- `09-makkah-automatic-dark.png`
- `10-makkah-zone-dark.png`
- `11-london-automatic-dark.png`
- `12-london-zone-dark.png`
- `14-offline-city-search-dark.png`
- `15-offline-startup-dark.png`
- `16-today-light.png`
- `17-location-settings-light.png`
- `18-picker-light.png`
- `19-picker-light-narrow-200pct.png`
- `20-results-light-narrow-200pct.png`
- `21-approximate-one-time-permission.png`
- `22-approximate-result-dark.png`

---

## 7. Known Limitations & Physical Device Testing Requirements

### Known Limitations
- Extreme polar latitudes during summer/winter solstices where twilight events do not occur fall back to twilight-angle adjustment rules in Adhan Kotlin.
- Approximate location resolution does not provide neighborhood-level reverse-geocoded place names (by design for user privacy).

### Physical Device Testing Checklist (Pre-Release)
- Test GPS hardware TTFF (Time-To-First-Fix) under indoor and obstructed conditions.
- Test physical devices lacking Google Play services (e.g. AOSP or microG) to confirm fallback `LocationManagerCompat` behavior.
- Validate Android 12+ runtime permission dialog behavior on vendor OEM skins (Samsung One UI, Xiaomi HyperOS).
- Test one-time permission expiry after app backgrounding.
- Verify TalkBack screen-reader navigation order and spoken accessibility labels.
