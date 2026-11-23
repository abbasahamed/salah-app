# Salah 3.0 — Milestone 6: Premium Qibla Compass & Floating Navigation Refinement

## 1. Overview & Objectives

Milestone 6 introduces a native, bespoke **Qibla Compass** and refines Salah's signature **floating bottom navigation dock**, while guaranteeing that all scrollable screens throughout the application (Today, Calendar, Qibla, and Settings) have sufficient bottom clearance to prevent content obstruction.

### Key Deliverables:
- **Deliverable A — Native Qibla Compass**:
  - Pure Kotlin `QiblaBearingCalculator` implementing great-circle forward azimuth and Haversine distance to the Kaaba ($21.422487^\circ\text{ N}, 39.826206^\circ\text{ E}$).
  - Lifecycle-aware `DeviceOrientationProvider` fusing `Sensor.TYPE_ROTATION_VECTOR` with fallback to `Sensor.TYPE_ACCELEROMETER` + `Sensor.TYPE_MAGNETIC_FIELD`.
  - Display rotation compensation (`Surface.ROTATION_0`, `90`, `180`, `270`) via `SensorManager.remapCoordinateSystem`.
  - Android `GeomagneticField` True North magnetic declination correction.
  - Continuous shortest-path circular smoothing filter across the $0^\circ / 360^\circ$ discontinuity.
  - Alignment hysteresis ($\pm 3^\circ$ entry threshold, $\pm 5^\circ$ exit threshold) with single haptic pulse.
  - Bespoke Jetpack Compose `Canvas` dial: 36 ticks, cardinal markers, lubber notch, center reticle, and stylized black-and-gold Kaaba marker.
  - Graceful fallback states: No-sensor static bearing reference, figure-eight calibration banner, and unconfigured location prompt.
- **Deliverable B — Refined Floating Navigation Dock & Clearance**:
  - Concentric capsule geometry ($R_{\text{outer}} = 26\text{dp}$, $R_{\text{inner}} = 23\text{dp}$) with dark graphite container.
  - Spring-animated selection plate (`dampingRatio = 0.85f`, `stiffness = 420f`).
  - Standardized $112\text{dp}$ bottom clearance across all scrollable containers, ensuring the final elements (e.g. Isha row in Calendar, clear location in Settings) scroll completely above the dock.

---

## 2. Mathematical Formulations

### Great-Circle Forward Azimuth (Qibla Initial Bearing)
Given the observer's coordinates $(\phi_1, \lambda_1)$ and the Kaaba's reference coordinates $(\phi_2, \lambda_2)$:
$$\Delta\lambda = \lambda_2 - \lambda_1$$
$$y = \sin(\Delta\lambda) \cdot \cos(\phi_2)$$
$$x = \cos(\phi_1) \cdot \sin(\phi_2) - \sin(\phi_1) \cdot \cos(\phi_2) \cdot \cos(\Delta\lambda)$$
$$\theta = \operatorname{atan2}(y, x)$$
$$\text{Bearing}_{\text{True North}} = ((\theta \cdot \frac{180}{\pi}) \bmod 360 + 360) \bmod 360$$

### Great-Circle Distance (Haversine Formula)
$$\Delta\phi = \phi_2 - \phi_1$$
$$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1) \cos(\phi_2) \sin^2\left(\frac{\Delta\lambda}{2}\right)$$
$$c = 2 \cdot \operatorname{asin}(\min(1.0, \sqrt{\max(0.0, a)}))$$
$$d = R_{\text{Earth}} \cdot c \quad (R_{\text{Earth}} = 6371.0\text{ km})$$
- When $d < 0.1\text{ km}$ ($100\text{ m}$), the user is recognized as **At the Kaaba** (`isAtKaaba = true`, distance = $0\text{ km}$).

---

## 3. Sensor Fusion, Coordinate Remapping & Declination

### Sensor Selection & Fallback
1. **Primary**: `Sensor.TYPE_ROTATION_VECTOR` provides software sensor fusion combining accelerometer, gyroscope, and magnetometer into a quaternion rotation vector.
2. **Fallback**: `Sensor.TYPE_ACCELEROMETER` + `Sensor.TYPE_MAGNETIC_FIELD` combined via `SensorManager.getRotationMatrix(...)`.
3. **Emulator / No Sensor**: If neither sensor configuration is available, `DeviceOrientationEvent.SensorUnavailable` is emitted. The UI renders the dial fixed to True North with the Qibla needle pointing at the calculated bearing, accompanied by a clean tonal advisory card.

### Screen Display Rotation Remapping
To ensure orientation remains accurate regardless of device holding angle (portrait vs landscape):
```kotlin
when (displayRotation) {
    Surface.ROTATION_90 -> {
        axisX = SensorManager.AXIS_Y
        axisY = SensorManager.AXIS_MINUS_X
    }
    Surface.ROTATION_180 -> {
        axisX = SensorManager.AXIS_MINUS_X
        axisY = SensorManager.AXIS_MINUS_Y
    }
    Surface.ROTATION_270 -> {
        axisX = SensorManager.AXIS_MINUS_Y
        axisY = SensorManager.AXIS_X
    }
    else -> {
        axisX = SensorManager.AXIS_X
        axisY = SensorManager.AXIS_Y
    }
}
SensorManager.remapCoordinateSystem(rotationMatrix, axisX, axisY, remappedMatrix)
```

### True North Magnetic Declination
Compass sensors detect Magnetic North. Android's `GeomagneticField` calculates local magnetic declination based on the World Magnetic Model (WMM):
$$\text{Heading}_{\text{True}} = ((\text{Heading}_{\text{Magnetic}} + \text{Declination}) \bmod 360 + 360) \bmod 360$$

### Continuous Shortest-Path Smoothing Filter
$$\Delta = ((\text{Target} - \text{Smoothed} + 540^\circ) \bmod 360^\circ) - 180^\circ$$
$$\text{Smoothed}_{\text{new}} = ((\text{Smoothed} + \alpha \cdot \Delta) \bmod 360^\circ + 360^\circ) \bmod 360^\circ \quad (\alpha = 0.25)$$
This prevents $360^\circ$ wrap-around spinning when heading fluctuates across True North ($359^\circ \leftrightarrow 1^\circ$).

---

## 4. Alignment Hysteresis & Guidance

- **Alignment Threshold**: Device is aligned when $|\text{Relative Angle}| \le 3.0^\circ$.
- **Hysteresis Exit Threshold**: Device remains in the aligned ("Facing Qibla") state until deviation exceeds $\pm 5.0^\circ$.
- **Haptics**: `HapticFeedbackType.LongPress` is fired exactly once upon entering the aligned state (no looping or battery-draining continuous vibration).
- **Guidance Pill**:
  - Aligned: Champagne gold container (`primaryContainer`), check icon, "Facing Qibla".
  - Turning: Neutral container (`surfaceContainer`), directional guidance ("Turn 18° right" or "Turn 25° left").
- **Sensor Calibration**: If sensor accuracy reports `SENSOR_STATUS_UNRELIABLE` or `SENSOR_STATUS_ACCURACY_LOW`, an animated banner advises: "Move phone in a figure-eight to calibrate compass".

---

## 5. Floating Navigation Dock & Bottom Clearance

- **Dock Surface**: $54\text{dp}$ height, $26\text{dp}$ outer radius, $23\text{dp}$ inner radius, $3\text{dp}$ inset padding.
- **Selection Plate**: Concentric capsule pill with spring physics (`dampingRatio = 0.85f`, `stiffness = 420f`).
- **Clearance Solution**:
  - `ScreenContent` (Settings): Added `Spacer(Modifier.height(112.dp))` at the end of the scrollable column.
  - `CalendarScreen`: Has `Spacer(Modifier.height(128.dp))` ensuring the Isha timing card and footnote scroll completely above the dock.
  - `TodayScreen`: Added `Spacer(Modifier.height(112.dp))` in the scrollable branch for accessibility and high font scaling.
  - `QiblaScreen`: Added `Spacer(Modifier.height(112.dp))` so compass and metadata scroll cleanly.

---

## 6. Verification & Screenshot Evidence

All 13 required verification states have been captured in `screenshots/salah-qibla/`:
1. `qibla-dark.png` — Dark theme with aligned Qibla compass, gold Kaaba indicator, and champagne guidance pill.
2. `qibla-light.png` — Light theme with warm canvas, bronze accents, and light floating dock.
3. `qibla-no-sensor.png` — Graceful fallback state with fixed True North reference and informational card.
4. `qibla-low-accuracy.png` — Active compass with "Turn 18° right" guidance and figure-eight calibration banner.
5. `qibla-manual-location.png` — Qibla screen displaying manual city label ("Direction from Dubai (Manual)").
6. `qibla-130pct-font.png` — Layout verified at 130% Android system font scaling.
7. `qibla-200pct-font.png` — Layout verified at 200% accessibility font scaling with stacked metadata.
8. `qibla-narrow.png` — Viewport verified on compact 320dp equivalent device width.
9. `qibla-landscape.png` — Orientation verified with NavigationRail and scrollable canvas.
10. `navigation-dark.png` — Dark mode floating navigation dock with concentric pill slider.
11. `navigation-light.png` — Light mode floating navigation dock.
12. `calendar-isha-clearance.png` — Calendar scrolled to the bottom showing Isha timing completely unobstructed.
13. `settings-bottom-clearance.png` — Settings scrolled to the bottom showing location actions completely unobstructed.

---

## 7. Real Hardware Testing Protocol (Samsung Galaxy S23)

Because emulator environments lack physical magnetometers, final sensor calibration verification must be performed on physical hardware:
1. **Compass Sensor Availability**:
   - Verify that `DefaultDeviceOrientationProvider` successfully detects `Sensor.TYPE_ROTATION_VECTOR` on Galaxy S23 (Snapdragon 8 Gen 2 sensor hub).
2. **Figure-Eight Calibration**:
   - Perform a physical figure-eight motion with the device; verify that the accuracy transitions from `LOW` to `HIGH` and the calibration banner dismisses smoothly.
3. **True North Alignment**:
   - Compare compass readout against a calibrated physical compass or sun azimuth at solar noon; confirm that local magnetic declination is correctly applied.
4. **Haptic Feedback**:
   - Rotate the phone slowly across the Qibla bearing; verify a crisp, subtle haptic tick upon entering the $\pm 3^\circ$ threshold and verify that no vibration triggers while maintaining alignment.
5. **Display Rotation**:
   - Rotate device to landscape left and right; confirm that the compass lubber notch remains aligned with the device's physical forward axis.

