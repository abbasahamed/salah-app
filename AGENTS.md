# AI Coding Agent & Contributor Guidelines

This document provides technology-neutral architectural guidelines, invariants, and coding conventions for AI assistants (such as Antigravity, Claude Code, GitHub Copilot, Cursor) and human contributors working on the **Salah** codebase.

---

## 1. Core Mission & Tenets

Salah is a free, privacy-first, open-source Android prayer-time and Qibla companion.

All code modifications must strictly uphold the following invariants:

1. **Absolute Privacy:** Zero user accounts, zero sign-in, zero authentication SDKs, zero telemetry, zero analytics, and zero advertising SDKs. The app never queries remote services for location or user identity.
2. **Astronomical & Religious Accuracy:** Prayer calculation times, Hijri date mappings, and Qibla bearing calculations must remain exact. Never fabricate, approximate, or offset prayer times for UI convenience or aesthetic spacing.
3. **Offline-First Resilience:** The application must remain fully functional offline. The bundled offline city database, timezone spatial boundaries, and on-device Adhan calculation engine ensure complete standalone capability.
4. **Editorial Luxury Aesthetic:** The UI follows a refined, compact design language featuring Geist typography, squarish calendar selections, custom vector iconography, and balanced Graphite (dark) and Warm Porcelain (light) themes.
5. **No Intent or Test Backdoors:** Test scenarios and debug mock providers must remain isolated exclusively to `app/src/debug`. Production release builds must unconditionally reject scenario overrides.

---

## 2. Architecture & Module Structure

Salah is organized as a multi-module Android project:

- `:app` — Main Android application module containing Jetpack Compose UI screens (Today, Calendar, Qibla, Settings, Location), Glance AppWidgets (2×2 and 4×2), navigation, ViewModels, and dependency injection wiring.
- `:core:prayer` — Astronomical prayer calculation engine (Adhan Kotlin) and AlAdhan HTTPS online sync provider. Handles calculation methods, Asr juristic conventions, high-latitude rules, and user manual minute offsets.
- `:core:location` — Offline city database search, offline timezone spatial lookup, and foreground GPS provider (`LocationManager`).
- `:core:preferences` — User preferences persistence via AndroidX DataStore (theme, calculation method, Asr convention, high-latitude rule, manual prayer adjustments, selected location).
- `:core:designsystem` — Design tokens, typography (Geist variable font), color palettes, custom prayer vectors (`SalahIcons`), and base UI components.

---

## 3. UI & Jetpack Compose Standards

- **State Management:** Screens consume immutable UI state models from ViewModels using `StateFlow` and `collectAsStateWithLifecycle()`. Never launch side-effects or network calls inside composables.
- **Accessibility:** Ensure touch targets meet minimum 48dp criteria, provide meaningful `contentDescription` for screen readers (TalkBack), and support dynamic system font scaling up to 200% without layout clipping.
- **Responsive Layout:** Test layouts against compact 320dp widths, standard 390–412dp devices, and tablets. Ensure scrollable containers have adequate bottom content padding (`WindowInsets.safeDrawing`) to clear the floating bottom navigation bar.
- **Gold Accent Hierarchy:** Champagne gold (`#C5A880`) is reserved for significant sacred elements (the Kaaba marker, compass needle, active prayer highlight). Do not overuse gold for ordinary status badges or secondary text.

---

## 4. Testing & Verification Requirements

Before submitting changes, all automated gates must pass:

```bash
# JVM Unit Tests
./gradlew testDebugUnitTest

# Android Lint
./gradlew lintDebug

# Android Instrumentation Tests (on SprintPixel emulator)
./gradlew connectedDebugAndroidTest

# Build verification
./gradlew assembleDebug assembleRelease
```

When modifying calculations, add unit tests in `:core:prayer:testDebugUnitTest`. When modifying database search or location logic, add unit tests in `:core:location:testDebugUnitTest`.

