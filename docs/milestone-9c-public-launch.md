# SALAH 3.0 — MILESTONE 9C: OFFICIAL OPEN-SOURCE PUBLIC LAUNCH REPORT
## GitHub Publication, Signed APK Distribution & Downloaded Binary Verification

**Publication Date:** November 23, 2026  
**Agent:** Antigravity (Principal Android Release Engineer, GitHub Maintainer)  
**Repository URL:** [https://github.com/abbasahamed/salah-app](https://github.com/abbasahamed/salah-app)  
**GitHub Release URL:** [https://github.com/abbasahamed/salah-app/releases/tag/v1.0.0](https://github.com/abbasahamed/salah-app/releases/tag/v1.0.0)  
**Published Release Tag:** `v1.0.0` (Annotated tag pointing to `0d0348fbe85f5a07a38b6155cffd0a08d68c1f7b`)  
**Audited Release Git Commit:** `0d0348fbe85f5a07a38b6155cffd0a08d68c1f7b`  
**License Adopted:** Apache License, Version 2.0  

---

## 1. Executive Summary: Launch Status

```
+======================================================================+
|                                                                      |
|                   SALAH 1.0.0 — OFFICIALLY PUBLISHED                 |
|                                                                      |
|  Source code, documentation, and Git history pushed to GitHub.       |
|  Annotated release tag v1.0.0 created and verified on remote.        |
|  GitHub Release v1.0.0 published with signed universal APK.          |
|  Downloaded APK verified bit-for-bit against published SHA-256.      |
|  Clean installation verified on SprintPixel (API 36).                |
|  Signing keystore verified and backed up to secure isolated vault.   |
|                                                                      |
+======================================================================+
```

---

## 2. Key Release Artifacts & Integrity Verification

| Verification Target | Value / Result | Status |
| :--- | :--- | :---: |
| **Release Version** | `v1.0.0` (Version Code `1`, Version Name `1.0.0`) | `PASS` |
| **Release APK Filename** | `Salah-v1.0.0-universal.apk` | `PASS` |
| **File Size** | 22.6 MB (22,644,565 bytes) | `PASS` |
| **Local Checksum** | `382ac0e55a01db3c313631e2bf8b41b8f25ea374e54a584a750f8923a7e3e731` | `PASS` |
| **Published Remote Checksum** | `382ac0e55a01db3c313631e2bf8b41b8f25ea374e54a584a750f8923a7e3e731` | `PASS` |
| **Downloaded Binary Checksum**| `382ac0e55a01db3c313631e2bf8b41b8f25ea374e54a584a750f8923a7e3e731` | **`PASS` (Bit-for-bit match)** |
| **Signing Schemes** | APK Signature Scheme v1 (JAR) and v2 (APK Scheme v2) | `PASS` |
| **Signing Certificate SHA-256** | `227e8b0862e5bc7f26a895e21e37bc42d3e25abc58f146af67df73f7b1d6cb20` | `PASS` |
| **Signing Certificate DN** | `CN=Salah Open Source, OU=Mobile, O=Salah, L=Chennai, ST=Tamil Nadu, C=IN` | `PASS` |

---

## 3. Mandatory Signing-Key Backup Verification Gate

- **Active Keystore:** `~/.salah-release-keys/salah-release.jks` (`chmod 600`, directory `0700`).
- **Secure Vault Backup:** Created isolated backup at `~/.salah-secure-backup-vault/salah-release.jks`.
- **Integrity Validation:** SHA-256 checksums of the original and backup files were computed and verified identical:
  - `c21d7697f41a290a93a9193366515163d4628ffdb05d789a081430432c13994c`
- **Recoverability:** Non-destructive `keytool -list` was executed on the backup file, confirming valid `PrivateKeyEntry` under alias `salah`.
- **Zero Secrets in Git:** Neither keystores nor credentials exist within the Git repository.

---

## 4. End-to-End Download & Installation Verification

1. Downloaded `Salah-v1.0.0-universal.apk` directly from the public GitHub Release asset endpoint (`https://github.com/abbasahamed/salah-app/releases/download/v1.0.0/Salah-v1.0.0-universal.apk`) into an isolated temporary directory.
2. Verified computed SHA-256 matches `Salah-v1.0.0-universal.apk.sha256`.
3. Executed `apksigner verify --verbose --print-certs` on the downloaded binary, confirming valid signatures and certificate fingerprints.
4. Installed the downloaded APK onto SprintPixel (`emulator-5554`, Android 16, API 36).
5. Launched `com.salah.app/.MainActivity` and dumped the live accessibility node hierarchy:
   - Confirmed immediate startup with zero sign-in or registration screens.
   - All navigation tabs active: Today, Calendar, Qibla, Settings.
   - Full timetable populated accurately without crashes or layout errors.

---

## 5. Security & Repository Protections Review

- **Minimal CI Token Privileges:** `.github/workflows/ci.yml` declares `permissions: contents: read`.
- **Zero Signing Secrets in Workflows:** GitHub Actions does not hold signing keystores; releases are verified and signed locally under developer control.
- **Dependency Audit:** Zero critical/high CVEs in runtime classpath; all dependencies pinned via Gradle version catalog.
- **Privacy Enforcement:** Updated [PRIVACY.md](https://github.com/abbasahamed/salah-app/blob/main/PRIVACY.md) and [README.md](https://github.com/abbasahamed/salah-app/blob/main/README.md) accurately detail that online AlAdhan queries transmit selected coordinates and standard connection IP metadata over HTTPS, while offline city search functions with zero network traffic.

---

## 6. Official Public Links

- **Source Code Repository:** [https://github.com/abbasahamed/salah-app](https://github.com/abbasahamed/salah-app)
- **Official v1.0.0 Release:** [https://github.com/abbasahamed/salah-app/releases/tag/v1.0.0](https://github.com/abbasahamed/salah-app/releases/tag/v1.0.0)
- **Direct APK Download:** [https://github.com/abbasahamed/salah-app/releases/download/v1.0.0/Salah-v1.0.0-universal.apk](https://github.com/abbasahamed/salah-app/releases/download/v1.0.0/Salah-v1.0.0-universal.apk)
- **SHA-256 Checksum Download:** [https://github.com/abbasahamed/salah-app/releases/download/v1.0.0/Salah-v1.0.0-universal.apk.sha256](https://github.com/abbasahamed/salah-app/releases/download/v1.0.0/Salah-v1.0.0-universal.apk.sha256)
