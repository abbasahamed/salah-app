# Prayer-time validation

## Chrome reference

The fixture uses 12.820254° N, 80.037819° E from an [SRMIST campus publication](https://webstor.srmist.edu.in/web_assets/downloads/2025/resonance-newsletter-volume4-issue2.pdf), with Asia/Kolkata, 21 November 2026, Karachi, zero custom adjustments, and an angle-based high-latitude rule. It is a campus reference point. The screenshot does not establish Google's coordinates, adjustment policy, rounding, or implementation.

Run the opt-in comparison against the production provider adapters with:

```sh
./gradlew :core:prayer:testDebugUnitTest --tests '*GoogleReferenceComparison' -PcomparePrayerReference
```

Observed results for Standard Asr. Deltas are signed minutes relative to the screenshot.

| Entry | Chrome | AlAdhan | Adhan Kotlin | Online Δ | Offline Δ |
| --- | --- | --- | --- | ---: | ---: |
| Fajr | 04:56 | 04:57 | 04:57 | +1 | +1 |
| Sunrise | 06:11 | 06:11 | 06:11 | 0 | 0 |
| Dhuhr | 11:55 | 11:56 | 11:57 | +1 | +2 |
| Asr | 15:14 | 15:14 | 15:13 | 0 | −1 |
| Maghrib | 17:40 | 17:40 | 17:40 | 0 | 0 |
| Isha | 18:55 | 18:55 | 18:55 | 0 | 0 |
| Next-day Fajr | 04:57 | 04:57 | 04:57 | 0 | 0 |

For Hanafi Asr, both providers reported 16:04 (+50). The other six values and deltas were identical to the Standard table. Standard is the closer observed configuration. Neither full schedule matched exactly. Adhan Kotlin rounds to the nearest minute and its Karachi preset includes an upstream +1 minute Dhuhr adjustment. Differences at other times may reflect provider algorithms, rounding, or coordinate differences. Neither production adapter applies screenshot-specific corrections.

## Independent reference

The offline engine is cross-checked against [Hamid Zarrabi-Zadeh's PrayTime 3.2.0](https://github.com/zarrabi/praytime), installed as `praytime@3.2.0`. It uses a separate JavaScript implementation and is a computational cross-check. Mosque timetables can follow separate local conventions.

| Fixture | Date | Coordinates | Zone | PrayTime method |
| --- | --- | --- | --- | --- |
| Kattankulathur | 2026-11-21 | 12.820254, 80.037819 | Asia/Kolkata | Karachi |
| Makkah | 2026-11-01 | 21.4225, 39.8262 | Asia/Riyadh | Makkah |
| London | 2026-06-21 | 51.5074, −0.1278 | Europe/London | MWL |
| New York | 2026-03-08 | 40.7128, −74.0060 | America/New_York | ISNA |

Reproduce each row with `new PrayTime(method).location([lat, lon]).timezone(zone).adjust({highLats: 'AngleBased', maghrib: '0 min'}).getTimes([year, month, day])`. This sets Maghrib to sunset, uses Standard Asr, and leaves nearest-minute rounding enabled. The stored rows are separate from the Chrome observations.

### Observed independent deltas

Deltas below are Adhan Kotlin minus PrayTime, in minutes. The automated cross-check bounds differences at two minutes. These four runs differed by at most one minute.

| Fixture | Fajr | Sunrise | Dhuhr | Asr | Maghrib | Isha |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Kattankulathur | 0 | 0 | +1 | −1 | 0 | 0 |
| Makkah | 0 | 0 | 0 | −1 | 0 | 0 |
| London | 0 | 0 | +1 | 0 | 0 | 0 |
| New York | 0 | 0 | +1 | +1 | 0 | 0 |
