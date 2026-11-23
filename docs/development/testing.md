# Testing & Verification Guide

Salah maintains a comprehensive test suite covering astronomical calculations, timezone queries, offline databases, and UI behavior.

---

## Test Suites Overview

| Test Target | Scope & Responsibility | Command |
| :--- | :--- | :--- |
| **JVM Unit Tests** | Core domain logic, prayer algorithms, timezone lookups, and ViewModel states. | `./gradlew testDebugUnitTest` |
| **Android Lint** | Code quality, accessibility, unused resources, and API compatibility. | `./gradlew lintDebug` |
| **Instrumentation Tests** | Real-device / emulator verification of Jetpack Compose UI and sensor providers. | `./gradlew connectedDebugAndroidTest` |
| **Release Build Verification** | R8 ProGuard rule verification and compilation of unsigned release APKs. | `./gradlew assembleRelease` |

---

## Running JVM Unit Tests

All unit tests execute quickly on the local JVM without requiring an emulator:

```bash
./gradlew testDebugUnitTest
```

### Module Test Breakdown
- `:core:prayer:testDebugUnitTest` — Astronomical angle calculations, juristic rules, high-latitude adaptations, and AlAdhan normalization.
- `:core:location:testDebugUnitTest` — Offline city database search, diacritic normalization, and timezone spatial polygon tests.
- `:core:preferences:testDebugUnitTest` — DataStore read/write operations and migration sanity checks.
- `:app:testDebugUnitTest` — ViewModels and UI state transformation testing.

---

## Running Android Instrumentation Tests

Instrumentation tests execute on an Android emulator or physical device.

```bash
# Ensure an emulator is running
adb devices

# Run instrumentation tests
./gradlew connectedDebugAndroidTest
```

---

## Android Lint Quality Checks

To verify code quality and ensure no accessibility or resource issues:

```bash
./gradlew lintDebug
```

Reports are generated at `app/build/reports/lint-results-debug.html`.

---

## Continuous Integration (CI)

Every pull request and push to `main` triggers automated verification via GitHub Actions (`.github/workflows/ci.yml`), running Gradle wrapper validation, JVM unit tests, Android Lint, and debug/release compilation.

