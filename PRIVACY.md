# Privacy Policy for Salah

**Last Updated:** November 23, 2026

Salah is designed with privacy as a foundational principle. We believe that your spiritual schedule, location, and device habits belong entirely to you.

## 1. Zero Account & Zero Authentication
- **No Sign-In:** Salah does not require you to sign in, create an account, or authenticate.
- **No Profile:** There are no user profiles, user IDs, emails, phone numbers, or passwords collected or stored.
- **No Third-Party Auth:** The app contains zero Firebase Authentication, Google Identity, OAuth, or cloud identity SDKs.

## 2. Location Privacy
Salah needs geographic coordinates to calculate Islamic prayer times and determine the direction of the Kaaba (Qibla). You maintain full control over how location is handled:
- **Offline City Selection (Recommended for Maximum Privacy):** You can search and select from thousands of global cities packaged completely offline inside the local SQLite database. If chosen, **no location permissions are requested, and zero network calls are ever made for location.**
- **Automatic Positioning (Optional):** If you tap "Use current location", Android requests standard location permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`). This is a **one-shot, foreground-only query**. Salah has zero background location tracking, zero location listener services, and zero geolocation logging.

## 3. Network Communication, Location Transmission & Upstream Processing
- **AlAdhan Public HTTPS Endpoint (Optional Online Sync):** When connected to the internet, Salah queries the public AlAdhan API (`api.aladhan.com`) over HTTPS to fetch or refresh prayer schedules.
  - **Data Transmitted by Salah:** Requests include the selected geographic coordinates (latitude and longitude), civil date, calculation method ID, and Asr juristic school ID.
  - **No Device Identifiers:** Salah does not include user accounts, device serial numbers, advertising IDs (GAID), phone numbers, IMEI, or device hardware IDs in the request payload or HTTP headers.
  - **Network & Server Metadata:** As with any standard HTTPS connection over the internet, network transmission protocols inherently expose the device's public IP address to network carriers and the remote server hosting AlAdhan. Salah makes no independent representation or guarantee regarding the data retention, logging, or operational practices of third-party network providers or AlAdhan's hosting infrastructure.
  - **Encrypted Transport:** Cleartext HTTP traffic is blocked via Android Network Security Configuration (`android:usesCleartextTraffic="false"` and `network_security_config.xml`).
- **Complete On-Device Offline Calculation Engine:** If network access is disabled, offline mode is active, or the remote endpoint is unreachable, Salah falls back entirely to the embedded **Batoul Apps Adhan** Kotlin library. In this mode, prayer schedules are computed locally on your device CPU using astronomical formulas, requiring **zero internet connectivity, zero network queries, and zero data transmission**.

## 4. On-Device Data Storage
- All app preferences (appearance theme, calculation method, Asr juristic rule, time offsets, 12h/24h display) are stored locally in Android Jetpack DataStore on your device.
- Recent schedule records (up to 16 calendar entries) are cached in a local SQLite Room database.
- Android Auto-Backup and Cloud Backup are explicitly disabled (`android:allowBackup="false"`) to prevent local prayer and location caches from leaking into third-party cloud drives.
- You can wipe all stored location and prayer cache data at any time directly in the app under **Settings > Clear cached data**.

## 5. Analytics, Tracking & Advertising
- **Zero Ads:** Salah contains no advertisements or sponsored banners.
- **Zero Trackers:** Salah contains no analytics libraries (Google Analytics, Firebase, Mixpanel, Segment, etc.).
- **Zero Telemetry:** Zero crash telemetry or diagnostics are uploaded to external servers.

## 6. Permissions Summary
| Permission | Purpose | Optional? |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | Retrieve prayer schedule tables from the public AlAdhan HTTPS endpoint. | Yes (App functions completely offline). |
| `android.permission.ACCESS_COARSE_LOCATION` | Approximate one-shot foreground location when the user explicitly requests automatic detection. | Yes (Manual offline search requires zero permissions). |
| `android.permission.ACCESS_FINE_LOCATION` | Accurate one-shot foreground location for precise Qibla and prayer timing. | Yes (Manual offline search requires zero permissions). |

## 7. Open Source & Transparency
Salah is 100% open-source software licensed under the **Apache License, Version 2.0**. The complete source code, build scripts, issue tracker, and documentation are publicly auditable at [github.com/abbasahamed/salah-app](https://github.com/abbasahamed/salah-app).

## 8. Contact & Questions
If you have questions about Salah's privacy architecture, please open an issue on GitHub:
https://github.com/abbasahamed/salah-app/issues

