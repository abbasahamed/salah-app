# Salah Security Model & Compliance

Salah is designed with a defense-in-depth security architecture appropriate for a standalone, privacy-first Android application.

---

## 1. Zero Trust & Privacy Model

- **No Accounts or User Authentication:** Salah has no concept of user accounts, passwords, sessions, or cloud profiles. The application is immediately usable upon launch without registration.
- **No Analytics or Telemetry:** No tracking libraries (such as Firebase Analytics, Mixpanel, AppsFlyer) are included in the build.
- **No Advertising SDKs:** The application contains zero advertising frameworks or monetization trackers.
- **No Third-Party Analytics SDKs:** All analytics and crash reporting dependencies are absent.

---

## 2. Network Security Architecture

Salah defines an explicit Android Network Security Configuration (`app/src/main/res/xml/network_security_config.xml`):

```xml
<network-security-config>
    <!-- Disable cleartext HTTP entirely across the application -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <!-- Trust only verified system root CA certificates -->
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

### Guarantees
- **Cleartext Traffic Blocked:** All unencrypted `http://` traffic is blocked at the OS network stack level.
- **User CA Bypass Denied:** User-installed CA certificates are rejected for production network queries, mitigating interception proxies.
- **Minimal Network Surface:** The only remote network endpoint ever queried is the AlAdhan API (`api.aladhan.com`) over TLS 1.3 when online prayer times are enabled. No user identification data is transmitted.

---

## 3. Test Isolation & Backdoor Prevention

During development, mock scenario providers were used to test diverse geographic locations and UI states. 

To eliminate intent injection vulnerabilities in production:
- Scenario parsing and mock data providers are compiled **exclusively** into the `app/src/debug` source set.
- In `app/src/release`, `ScenarioProvider` unconditionally returns `null`.
- The application cannot be manipulated via external ADB intents (`am start --es scenario ...`) on release builds.

---

## 4. Local Data Security

- **Backup Exclusions:** `android:allowBackup="false"` is enforced in `AndroidManifest.xml`, preventing extraction of user preferences or location data via cloud backups or ADB backup tools (`backup_rules.xml` and `data_extraction_rules.xml`).
- **Internal Storage Only:** Preferences are persisted using AndroidX DataStore in private app-internal sandbox storage (`/data/data/com.salah.app/files/datastore/`).

---

## 5. Vulnerability Reporting

Security vulnerabilities should be reported responsibly via GitHub Private Vulnerability Reporting:
https://github.com/abbasahamed/salah-app/security/advisories/new

