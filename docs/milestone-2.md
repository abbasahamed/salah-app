# Salah Milestone 2 handoff

## 1. Workspace baseline and status

The project is at `/Users/abbas/Desktop/Salah`. The initial branch, HEAD, and `git status --short` checks all returned “not a git repository.” The directory contained the three Milestone 1 modules, the Gradle wrapper, README, three appearance unit tests, and two Compose tests. There was no branch or HEAD to record and no Git reset, stash, or restore was performed. The workspace remains a filesystem project without Git metadata.

## 2. Architecture

Today derives its requests from the visible screen's clock, location and persisted settings. The prayer repository returns complete, validated schedules across the provider boundary. It serves the matching Room entry first, refreshes when appropriate, and uses Adhan Kotlin for a cache miss after a network failure. The countdown resolves obligatory prayers as instants and loads an adjacent schedule where needed. Sunrise stays in the daily schedule.

## 3. Modules and files

- Added `:core:prayer`, including domain models, providers, Room database and exported schema, hybrid repository, resolver, unit and network tests, and the opt-in reference comparison.
- Extended `:core:preferences` with typed settings on the existing DataStore and a persistence test.
- Updated `:app` for the Today and Settings state, debug and release location inputs, Hilt bindings, and Android network and backup configuration. The Today Compose regression now checks that the screen renders six timed entries.
- Updated the root module list, version catalog and README. The design-system module was reused without source changes.

## 4. Online API

OkHttp 4.12.0 makes HTTPS calls to the AlAdhan coordinate/date endpoint, and Moshi 1.15.2 parses ISO 8601 timestamps. The adapter verifies the date, coordinates, IANA zone and UTC offsets, method, school, high-latitude rule, zero provider offsets, and all six ordered timestamps before the repository can cache the result. User offsets are applied uniformly in the domain model. [AlAdhan OpenAPI contract](https://api.aladhan.com/v1/documentation/openapi/prayer-times/yaml)

## 5. Offline library

Batoul Apps Adhan Kotlin 0.0.5 is pinned for Kotlin 2.1 compatibility. The JVM artifact uses kotlinx-datetime, with Java time desugaring enabled for Android API levels below 26. The wrapper maps all four methods, both Asr shadow conventions, nearest-minute rounding, and the twilight-angle high-latitude rule. The APK packages Batoul Apps' MIT notice. [Adhan Kotlin](https://github.com/batoulapps/adhan-kotlin) · [Pinned JVM artifact](https://central.sonatype.com/artifact/com.batoulapps.adhan/adhan2-jvm/0.0.5)

## 6. Cache and refresh

Room 2.7.2 retains the latest 16 validated online schedules. Its exact key includes the date, coordinates, zone, method, school, custom offsets, calculation rule, provider identity and cache format. A matching schedule is fresh for 24 hours and remains usable for its own request while a refresh is attempted. An HTTP or validation failure leaves that schedule intact. Concurrent refreshes for one key share a call. Failed automatic refreshes back off for five minutes. Retry can bypass this delay. There is no background polling.

## 7. Supported settings

Karachi, ISNA, Muslim World League, and Umm al-Qura map to AlAdhan IDs 1, 2, 3 and 4. Standard and Hanafi map to school IDs 0 and 1. Settings persists both selections, the 12/24-hour format and six individual offsets from −60 to +60 minutes. Default debug comparison settings are Karachi, Standard, and zero offsets.

## 8. Chrome comparison

The [observed comparison table](prayer-validation.md#chrome-reference) records both Asr conventions, six daily entries, next-day Fajr and signed differences. Standard was closer. AlAdhan matched five of seven observations exactly. The offline provider matched four, with the largest Standard difference at two minutes. Hanafi Asr was 50 minutes later in both providers.

## 9. Independent comparison

Independent PrayTime 3.2.0 fixtures for Kattankulathur, Makkah, London and New York span four calculation methods and dates that exercise a high-latitude summer configuration and a daylight-saving transition. Observed deltas across 24 entries were within one minute. [Fixture provenance, reproduction instructions and deltas](prayer-validation.md#independent-reference)

## 10. Automated tests and builds

| Verification | Result |
| --- | --- |
| Debug APK and unsigned release APK | Assembled |
| Domain and independent fixtures | 8 passed |
| MockWebServer API tests | 9 passed |
| Hybrid repository tests | 5 passed |
| DataStore tests including Milestone 1 | 4 passed |
| Compose instrumented tests on API 36, with network disabled | 2 passed |
| Opt-in live reference comparison | 1 run, four validated API requests |

Ordinary JVM tests do not call AlAdhan. Resolver tests cover the exact prayer boundary, overnight selection, date rollover, cross-midnight adjustments, and DST elapsed time.

## 11. Android lint

Debug lint completed with 0 errors and 14 warnings. One is for targeting API 35 while a newer Android target is available, three concern Gradle or AGP updates, and ten are optional dependency upgrades including Room. No warning was suppressed.

## 12. Emulator verification

The API 36 emulator displayed live AlAdhan results and then served a saved schedule after a cold restart with its radios disabled. With the radios still disabled, switching to an uncached Hanafi configuration changed Asr from 15:14 to 16:04 and displayed the offline source. An offline cold restart preserved that configuration and rendered the schedule again. Switching back to Standard restored the matching saved schedule. After Isha on 21 November, Today displayed 22 November Fajr at 04:57 with a live countdown. The Today screen was also checked in light and dark themes and at 200% font size on a narrow viewport.

## 13. Known limitations

The release build intentionally has no development fixture and shows the no-location state. Android 23 and physical devices were not exercised. Extreme polar dates may lack enough solar events for Adhan Kotlin. Its Umm al-Qura preset uses a fixed 90-minute Isha interval, with no automatic Ramadan adjustment. The independent comparisons cover the four documented configurations and dates.

## 14. Security and privacy

Online calculations send coordinates and prayer calculation settings to AlAdhan; the app discloses this in Settings. Cleartext traffic and redirects are disabled, the app uses no API key or request logger, and no device identifiers are sent by the app. The cache has a bounded retention count and its database and preferences are excluded from backups and transfers. There is no location permission, authentication, advertising or analytics.

## 15. Milestone 3 readiness

The future location layer can supply coordinates and an IANA zone through `PrayerLocation`. Scope the repository requests to the selected location and define consent and retention before sending user coordinates online. GPS, permission prompts and location selection were left for Milestone 3.
