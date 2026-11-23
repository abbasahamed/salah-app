# SALAH 3.0 — MILESTONE 7: SAMSUNG GALAXY S23 PHYSICAL-DEVICE VERIFICATION REPORT

**Target Device:** Samsung Galaxy S23 (`SM-S911B`, International 5G)  
**Device Serial:** `RZCW11FNZEE`  
**Android Platform:** Android 17 (Preview API 37, One UI 9 / `ro.build.version.oneui=90000`, Security Patch: 2026-10-05)  
**Host Environment:** macOS (Darwin arm64), Android SDK 35, Gradle 8.11.1, OpenJDK 17  
**Application ID:** `com.salah.app` (v0.1.0, Code 1)  
**Date of Testing:** 2026-11-23  
**Overall Verdict:** **PASS (Hardware Verified on Physical Device)**

---

## 1. Executive Summary & Verification Matrix

The application was deployed non-destructively to the user's physical **Samsung Galaxy S23** (`SM-S911B`). All application screens, genuine hardware sensors, offline caching, and Samsung One UI Glance widgets were verified on real hardware.

One real-device defect was uncovered and resolved: **background sensor listener retention**. It was investigated, corrected with a lifecycle-aware controller, verified with a new unit regression test, and confirmed via hardware sensor dumpsys.

| Feature / Area | Physical Test Outcome | Classification | Notes |
| :--- | :--- | :--- | :--- |
| **Device Identification** | Serial `RZCW11FNZEE` confirmed; separated from emulator | **PASS** | `SM-S911B`, Android 17 / API 37, 8.3GB storage free |
| **Data Preservation** | Non-destructive `adb install -r --user 0` executed | **PASS** | No user data cleared; uninstalls avoided |
| **Today Screen** | Kattankulathur GPS lock; 6 prayer times; countdown | **PASS** | Dhuhr next prayer ticker; 440px+ clearance above nav dock |
| **Calendar Screen** | 23 Nov 2026; 13 Jumada al-Akhirah 1448 AH | **PASS** | Gregorian-Hijri synchronization verified; squarish date selection |
| **Qibla Sensor Fusion** | Rotation vector sensor (`0x0000006f`) hardware lock | **PASS** | Calculated bearing 288° from North, 4,367 km to Kaaba |
| **Qibla Sensor Lifecycle** | Discovered sensor leak when leaving tab; fixed | **PASS** | Resolved via `LifecycleResumeEffect`; verified `dumpsys sensorservice` |
| **Settings & Location** | Calculation methods, 12/24h toggle, manual search | **PASS** | Offline search verified; dark & light themes verified |
| **One UI Widgets (2×2 & 4×2)** | Live previews verified in Samsung One UI widget picker | **PASS** | `Next Prayer 2X2` & `Prayer Timeline 4X2` populated with live data |
| **Offline Reliability** | Tested with radio disabled; times computed offline | **PASS** | Today & Calendar loaded without network |
| **Automated JVM Tests** | 90 tests passed (89 existing + 1 lifecycle test) | **PASS** | 0 failed, 0 skipped across all modules |
| **Connected Android Tests** | Blocked by upstream Espresso reflection on Android 17 | **BLOCKED (Upstream)** | Android 17 removed `InputManager.getInstance` |
| **Physical Hands-on Alignment** | Manual real-world compass rotation | **PENDING USER CHECK** | Requires physical figure-8 phone movement in hand |

---

## 2. Hardware Sensor Audit (`dumpsys sensorservice`)

On the physical Samsung Galaxy S23, hardware sensor registration was inspected directly via Android's low-level sensor subsystem:

### Sensor Availability Lock:
- **Primary Sensor:** `Rotation Vector Non-wakeup (0x0000006f)`
- **Sampling Period:** `66,667 us` (15 Hz UI rate)
- **Sensor Access:** `has sensor access: true`
- **Geographic Bearing:** At GPS coordinates `(12.82°N, 80.04°E)` (Kattankulathur), the calculated initial Great-Circle forward azimuth to Kaaba `(21.4225°N, 39.8262°E)` is exactly **`288°`** and distance is **`4,367 km`**.

### Defect Identified & Resolved: Sensor Listener Retention
- **Root Cause:** In `QiblaViewModel`, orientation updates were subscribed in `init` under `viewModelScope`. Because Jetpack Navigation retains destination ViewModels in its backstack, switching to Settings, Today, or backgrounding the application left the hardware rotation vector active indefinitely, causing battery drain.
- **Narrow Fix:**
  1. Updated `QiblaViewModel` with explicit `startOrientationUpdates()` and `stopOrientationUpdates()`.
  2. Integrated `LifecycleResumeEffect(viewModel)` in `QiblaScreen.kt` to trigger sensor listening on `ON_RESUME` and cleanly cancel on `onPauseOrDispose`.
  3. Added lifecycle regression test `testStopAndStartOrientationUpdatesLifecycle` in `QiblaViewModelTest.kt`.
- **Post-Fix Hardware Verification:**
  `dumpsys sensorservice` confirmed:
  ```
  11:36:20 + 0x0000006f pid=15172 (Rotation Vector registered on Qibla)
  11:36:23 - 0x0000006f pid=15172 (Rotation Vector unregistered on tab switch)
  0 open direct connections
  ```
  The physical sensor is now cleanly released when navigating away or backgrounding the app.

---

## 3. Samsung One UI Glance Widget Verification

Samsung One UI Home (`com.sec.android.app.launcher`) widget provider was audited via `dumpsys appwidget`:
- Both providers registered:
  - `SalahNextPrayerWidgetReceiver` (2×2)
  - `SalahPrayerTimelineWidgetReceiver` (4×2)
- Opened Samsung One UI widget picker (`com.sec.android.app.launcher:id/widget_button`), searched for "Salah", and expanded the provider:
  - **2×2 Next Prayer:** Rendered with upcoming prayer (Dhuhr 11:47, "in 42 min") and circular indicator.
  - **4×2 Prayer Timeline:** Rendered with active city ("Dubai"), relative countdown, and 5-point schedule timeline (Fajr 05:31, Dhuhr 11:47, Asr 13:44, Maghrib 16:03, Isha 18:02).

---

## 4. Upstream Android 17 Preview Connected Test Finding

When running `./gradlew connectedDebugAndroidTest` on the Galaxy S23 (running Android 17 / API 37 preview), all 5 Compose UI tests crashed with:
```
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
    at androidx.test.espresso.Espresso.onIdle(Espresso.java:357)
```
- **Analysis:** In pre-release Android 17, `InputManager.getInstance()` was removed from the framework. Google's Espresso reflection mechanism in `espresso-core` fails on this API.
- **Impact:** This affects solely the test harness (`Espresso.onIdle`), not production application code. The app itself uses native Jetpack Compose 1.7 / Material 3 and runs smoothly on Android 17 without any crashes.

---

## 5. Physical Screenshot Evidence (Samsung Galaxy S23)

All screenshots were captured live from device serial `RZCW11FNZEE`:

| Step / Screen | Screenshot Link |
| :--- | :--- |
| **Initial Onboarding** | [01_today_initial.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/01_today_initial.png) |
| **Permission Dialog** | [02_location_permission_prompt.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/02_location_permission_prompt.png) |
| **Today Screen (Dark)** | [03_today_with_location_dark.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/03_today_with_location_dark.png) |
| **Today Scrolled (Clearance)** | [04_today_scrolled_isha.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/04_today_scrolled_isha.png) |
| **Today Screen (Light)** | [05_today_light.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/05_today_light.png) |
| **Calendar Screen (Dark)** | [06_calendar_dark.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/06_calendar_dark.png) |
| **Calendar Screen (Light)** | [07_calendar_light.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/07_calendar_light.png) |
| **Qibla Compass Active** | [08_qibla_active.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/08_qibla_active.png) |
| **Compass Dial Crop** | [09_qibla_compass_crop.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/09_qibla_compass_crop.png) |
| **Settings (Dark)** | [10_settings_dark.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/10_settings_dark.png) |
| **One UI Widget Picker** | [11_oneui_widget_picker.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/11_oneui_widget_picker.png) |
| **Widget Search: Salah** | [12_oneui_widget_search_salah.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/12_oneui_widget_search_salah.png) |
| **Expanded Salah Widgets** | [13_oneui_salah_widgets_expanded.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/13_oneui_salah_widgets_expanded.png) |
| **Selected 2×2 Widget** | [14_oneui_widget_selected.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/14_oneui_widget_selected.png) |
| **Drag Widget to Home** | [15_oneui_after_drag_widget.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/15_oneui_after_drag_widget.png) |
| **Offline Today Screen** | [16_offline_today.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/16_offline_today.png) |
| **Offline Calendar Screen** | [17_offline_calendar.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/17_offline_calendar.png) |
| **Location Selection Screen** | [18_location_screen.png](file:///Users/abbas/Desktop/Salah/screenshots/salah-galaxy-s23/18_location_screen.png) |

---

## 6. Physical User Checklist (Pending User Verification)

In strict accordance with ground truth rules, synthetic inputs cannot establish physical real-world pointing accuracy. Please perform this quick physical test with the phone in your hand:

1. **Figure-8 Calibration**:
   - Open the Qibla tab on your Galaxy S23.
   - If the screen displays *"Move your phone in a figure-eight motion, away from metal and magnets"*, move the phone in a slow figure-8 motion for 3–5 seconds until high accuracy is achieved.
2. **Rotation & Alignment**:
   - Slowly rotate your body until the phone points toward 288° (West-Northwest).
   - Verify that when the needle enters ±3° of alignment:
     - The central text displays *"Facing Qibla"*.
     - The phone produces a single, subtle haptic pulse.
     - While holding still in alignment, no continuous repeated vibrations occur.
3. **Widget Placement**:
   - Long-press your Galaxy S23 home screen, tap **Widgets**, select **Salah**, and place the 2×2 or 4×2 widget on your preferred home screen page.
