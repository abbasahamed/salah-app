# Contributing to Salah

Thank you for your interest in contributing to Salah! Salah is a free, privacy-first, open-source Android prayer-time and Qibla companion.

## Guiding Principles

Before proposing changes, please keep our core tenets in mind:
1. **Privacy Above All:** No sign-in, no telemetry, no tracking, no ads, and no cloud accounts. Any PR introducing user registration or analytics will be closed.
2. **Deterministic Correctness:** Prayer times, date conversions, and Qibla bearings must remain astronomically and religiously accurate. Every timing modification must be accompanied by unit tests.
3. **Editorial Luxury Aesthetic:** Salah uses a bespoke, compact visual design built on Jetpack Compose, Geist typography, and a refined graphite/champagne color palette. Do not replace styled components with unstyled Material defaults.
4. **Offline Resilience:** All features must function reliably without network connectivity.

---

## Development Setup

### Prerequisites
- **Android Studio Ladybug / Meerkat** or newer.
- **JDK 17** (or 21).
- **Android SDK Platform 35** with Build Tools 35.0.0.
- Android device or emulator running API 23 (Android 6.0) through API 36 (Android 16).

### Clone & Build
```sh
git clone https://github.com/abbasahamed/salah-app.git
cd salah-app

# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint checks
./gradlew lintDebug
```

### Running Instrumentation Tests
To run UI and connected tests on an attached device or emulator:
```sh
./gradlew connectedDebugAndroidTest
```

---

## Architecture Overview

Salah is structured as a modular Gradle project:

- `:app` — Activity lifecycle, top-level navigation, Hilt dependency injection, Glance home-screen widgets (2×2 & 4×2), and screen shells.
- `:core:prayer` — Astronomical calculation engine (Batoul Apps Adhan), AlAdhan API network client, Room local database cache, and next-prayer resolver.
- `:core:location` — One-shot GPS location provider, SQLite offline global city database, and coordinate-to-timezone mapping.
- `:core:preferences` — Android Jetpack DataStore preferences for appearance, calculation methods, juristic rules, and manual time offsets.
- `:core:designsystem` — Typography (Geist variable font), Material 3 color schemes (Graphite & Warm Porcelain), custom icons, and shared composables.

---

## Submitting Pull Requests

1. **Open an Issue First:** For major features or design adjustments, please open an issue to discuss your proposal before investing significant engineering time.
2. **Create a Feature Branch:** Branch off `main` (e.g. `feature/my-enhancement` or `fix/timezone-edge-case`).
3. **Follow Kotlin Conventions:** Maintain idiomatic Kotlin, explicit type signatures where helpful, and clean Compose practices.
4. **Ensure Tests Pass:** Run `./gradlew testDebugUnitTest lintDebug` before pushing.
5. **Write Descriptive Commit Messages:** Follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `fix(qibla): correct sensor orientation smoothing`, `docs: update build prerequisites`).

---

## Code of Conduct

Please maintain a kind, respectful, and collaborative environment across all issue discussions and pull request reviews.

