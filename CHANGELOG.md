# Changelog

All notable changes to the **Salah** application are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-11-23

### Added
- **Today Prayer Dashboard:** Real-time countdown hero display, next prayer indicator, current location header, and full 6-prayer schedule table (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha).
- **Hybrid Calculation Engine:** Automatic online sync with AlAdhan API over HTTPS with resilient on-device fallback (Batoul Apps Adhan) requiring zero network access.
- **Offline Location Database:** Instant global city search powered by an embedded SQLite database with precomputed coordinates and civil time zones.
- **One-Shot Foreground GPS:** User-controlled, one-shot location detection with zero continuous background tracking or telemetry.
- **Interactive Hijri & Gregorian Calendar:** Monthly prayer timetable with date selection, quick navigation to Today, and automatic Isha scroll clearance.
- **Sensor-Driven Qibla Compass:** Smooth Kaaba bearing calculation with device sensor fusion, true-north declination, low-accuracy calibration banners, and vibration feedback when facing Mecca.
- **Jetpack Glance Home Screen Widgets:**
  - `2×2 Next Prayer Widget` — Displays current location, next prayer name, time, and live countdown.
  - `4×2 Daily Timetable Widget` — Displays full daily prayer schedule and next prayer highlight.
- **Calculation Preferences:** Customization for major calculation methods (Karachi, ISNA, Muslim World League, Umm al-Qura), Hanafi/Standard Asr juristic conventions, high-latitude rules, and ±60 minute manual prayer adjustments.
- **Editorial Luxury UI:** Bespoke compact interface with Geist typography, squarish calendar selections, custom prayer iconography, and seamless Light (Warm Porcelain) / Dark (Graphite) theme switching.
- **Android Themed & Adaptive App Icon:** Golden Sunrise / Prayer-Time Halo icon with full support for Android adaptive launcher masks and monochrome themed icons.
- **Complete Open-Source Baseline:** Public GitHub distribution with reproducible Gradle builds, R8 optimization, and zero account/sign-in requirements.

