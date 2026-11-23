# Salah Android

Salah is a native Android prayer-time app with user-controlled location and online, cached, and on-device prayer schedules.

## Modules

| Module | Responsibility |
| --- | --- |
| `:app` | Application and activity, Hilt setup, Today and Settings state, navigation, and screen shells |
| `:core:designsystem` | Material 3 light and dark palettes, typography, shapes, and shared cards and headings |
| `:core:preferences` | DataStore appearance and prayer preferences |
| `:core:location` | Foreground location, offline city search, coordinate-to-time-zone lookup, and a single saved selection |
| `:core:prayer` | Prayer models, AlAdhan and Adhan adapters, Room cache, repository, and next-prayer resolver |

The activity collects the ViewModel’s `StateFlow` with lifecycle awareness. Settings sends selection events to the ViewModel, and the UI updates from the persisted preference. System mode follows Android’s dark theme setting. The UI uses edge-to-edge insets, scrollable screen content, and a navigation rail on larger viewports. Route changes are immediate, and Material components use Compose’s system animation scaling.

Today lists Fajr, Sunrise, Dhuhr, Asr, Maghrib, and Isha. Sunrise appears in the schedule and is excluded from the countdown. Debug and release builds start with a choice between offline city search and a one-shot foreground location request. Today uses the saved location’s civil time zone. Calendar and Qibla remain placeholders.

On a request for a specific date and configuration, Salah first emits a matching Room entry, when present. A successful retrieval is fresh for 24 hours. An older entry remains usable for its original date and configuration while the app attempts a refresh. A failed or invalid response leaves the matching cache intact. On a cache miss, the on-device Adhan calculation supplies a whole schedule after the network attempt fails. If the calculation also fails, Today offers a retry. Network calls have a five-second deadline. The repository shares in-flight requests for identical inputs, applies a five-minute failure cooldown, and lets explicit retries bypass that cooldown. Refreshes occur on active schedule requests, such as entering Today, changing settings, or crossing into another date. The countdown reads the clock while Today is visible.

Room retains at most 16 validated AlAdhan entries. The key includes exact coordinates, local date, IANA time zone, method, Asr rule, adjustments, provider and format version. The cache stores raw UTC seconds and retrieval time. The same user-entered offsets are applied once to either provider's raw timings. DataStore holds calculation settings and appearance.

Online requests send the selected coordinates, date, time zone, method, and Asr convention to AlAdhan over HTTPS. Salah disables cleartext and HTTP redirects, uses no API key or request logger, and stores no device identifier. The app's backup rules exclude the schedule database and DataStore. Location permission is optional and requested only when the user chooses automatic location. Settings can clear the saved location and prayer schedule cache.

## Build and test

Use Android SDK Platform 35, Build Tools 35.0.0, and JDK 17 or 21. Set `ANDROID_HOME` or configure the SDK path in Android Studio. The app targets API 35 and supports API 23 and later.

```sh
./gradlew :app:assembleDebug :app:assembleRelease testDebugUnitTest lint
./gradlew :app:connectedDebugAndroidTest
```

The second command requires a running emulator or connected Android device. The debug APK is under `app/build/outputs/apk/debug/`.

| Dependency | Pinned version |
| --- | --- |
| Gradle Wrapper / Android Gradle Plugin | 8.11.1 / 8.10.1 |
| Kotlin and Compose compiler plugin / KSP | 2.1.10 / 2.1.10-1.0.30 |
| Compose BOM, including Material 3 and Compose UI testing | 2024.12.01 |
| Activity Compose / Lifecycle | 1.10.1 / 2.8.7 |
| Navigation Compose / DataStore | 2.8.9 / 1.1.4 |
| Hilt / Coroutines | 2.56.2 / 1.9.0 |
| Adhan Kotlin / Room | 0.0.5 / 2.7.2 |
| OkHttp and MockWebServer / Moshi | 4.12.0 / 1.15.2 |
| Core library desugaring | 2.1.5 |
| Play services location / AndroidX Core | 21.3.0 / 1.15.0 |
| JUnit / AndroidX JUnit extension / AndroidX test runner | 4.13.2 / 1.2.1 / 1.6.1 |

The [version catalog](gradle/libs.versions.toml) is the source of truth for dependency pins.

## Prayer calculation

| Method | AlAdhan ID | Adhan Kotlin mapping |
| --- | ---: | --- |
| University of Islamic Sciences, Karachi | 1 | KARACHI |
| Islamic Society of North America | 2 | NORTH_AMERICA |
| Muslim World League | 3 | MUSLIM_WORLD_LEAGUE |
| Umm al-Qura | 4 | UMM_AL_QURA |

Standard Asr maps to AlAdhan `school=0` and Adhan's Shafi shadow convention. Hanafi maps to `school=1` and Adhan's Hanafi shadow convention. Both adapters use the angle-based night rule where applicable. Batoul Apps maintains Adhan Kotlin under the MIT license. Salah pins 0.0.5, the Kotlin/JVM variant compatible with the project’s Kotlin 2.1 compiler. Newer releases require newer Kotlin metadata. The offline library uses nearest-minute rounding. It may lack usable sunrise or sunset at polar latitudes, in which case the app reports unavailable if the other sources also fail. The offline Umm al-Qura preset uses 90 minutes from Maghrib to Isha all year. A Ramadan-specific offline rule remains outside this milestone. Individual adjustments of −60 to +60 minutes and the 12/24-hour display preference are available in Settings.

[AlAdhan API contract](https://api.aladhan.com/v1/documentation/openapi/prayer-times/yaml) · [Batoul Apps Adhan Kotlin](https://github.com/batoulapps/adhan-kotlin) · [Reference and independent validation](docs/prayer-validation.md)

## Smart location

See the [Milestone 3 handoff](docs/milestone-3.md) for provider behavior, data attribution, privacy, verification, and physical-device checks.
