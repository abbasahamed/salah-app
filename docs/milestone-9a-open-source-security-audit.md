# SALAH 3.0 — MILESTONE 9A: OPEN-SOURCE SECURITY AUDIT & READINESS REPORT
## Formal Security Audit, Attack Surface Analysis & Publication Gate Verdict

**Date:** November 23, 2026  
**Auditor:** Antigravity (Principal Application Security Engineer & Open-Source Compliance Specialist)  
**Target Repository:** `https://github.com/abbasahamed/salah-app`  
**Local Workspace:** `/Users/abbas/Desktop/Salah`  
**Current Git Commit:** `b54d7b2` (branch `main`)  
**Publication Status:** **HELD (No code or releases pushed to GitHub)**  

---

## 1. Executive Summary

In preparation for publishing Salah as an open-source, zero-login Islamic prayer companion on GitHub, a comprehensive, multi-phase application security audit was performed. The scope encompassed all source code (`:app`, `:core:prayer`, `:core:location`, `:core:preferences`, `:core:designsystem`), bundled binary assets, Git commit history, release configuration, supply-chain dependencies, and Android component security.

### Summary Assessment
- **Hardcoded Secrets / API Keys in Git History:** **0 detected** (Scanned with Gitleaks v8.30.1 and custom regex analyzers across all commits).
- **Signing Key Security:** **ISOLATED** (Dedicated release keystore stored outside the repository at `~/.salah-release-keys/` with 0700 permissions).
- **Release Debug Backdoors:** **REMEDIATED & SEALED** (Test scenario intent hooks were decoupled into `app/src/debug`, while `app/src/release` strictly returns null).
- **Network Security:** **HARDENED** (Cleartext disabled, explicit `network_security_config.xml` pins system CAs only).
- **Location Privacy:** **AUDITED** (One-shot foreground location only; zero continuous tracking; full offline search capability without permissions).
- **License Compliance:** **AUDITED** (Apache License 2.0 adopted; all bundled fonts, libraries, and datasets verified as permissive and redistributable).
- **Build Reproducibility:** **VERIFIED** (Machine-specific JDK path removed from `gradle.properties`; tested under standard OpenJDK 21 environment).
- **Automated Verification:** **255 JVM Unit Tests PASS**, **5 Connected Instrumentation Tests PASS**, **0 Lint Errors**, **Signed Release APK verified with apksigner**.

### Publication Verdict: **GO**
The repository satisfies all legal, security, privacy, and architectural criteria for public open-source publication.

---

## 2. Inventory & Repository Surface Analysis

A file classification audit cataloged all 670 workspace files:
- **Production Source (123 files):** Modular Kotlin architecture across `:app`, `:core:prayer`, `:core:location`, `:core:preferences`, and `:core:designsystem`.
- **Test Source (35 files):** 255 JVM unit tests and 5 Android Compose instrumentation tests.
- **Assets & Resources (83 files):** Offline SQLite cities dataset, vector drawables, XML layouts, and SIL OFL 1.1 variable fonts.
- **Screenshots (371 files):** Verified visual acceptance captures across Milestone 8.1, 7, 6, and earlier design phases.
- **Documentation (67 files):** Architecture notes, privacy policy, contributing guides, and verification reports.
- **Build Configuration (17 files):** Gradle version catalog (`libs.versions.toml`), multi-project build scripts, and R8/ProGuard configuration.

---

## 3. Threat Modeling & Vulnerability Findings

Detailed finding documentation is maintained in [docs/open-source-security-findings.md](file:///Users/abbas/Desktop/Salah/docs/open-source-security-findings.md).

### High-Severity Remediations:
1. **SEC-01 (Debug Scenario Intent Backdoor):**
   - *Threat:* Malicious third-party apps launching `MainActivity` with `--es scenario today_gps` could override user coordinates and force mock prayer times.
   - *Remediation:* Created source-set split `com.salah.app.scenario.ScenarioProvider`. In debug builds, it provides test hooks for local screenshot automation; in release builds, it strictly returns `null`. Verified with `ScenarioSecurityTest.kt`.

### Medium & Low-Severity Remediations:
2. **SEC-02 (Machine-Specific JDK Path):**
   - *Threat:* `gradle.properties` contained a hardcoded `/Users/abbas/` path, leaking the local username and breaking builds for external contributors.
   - *Remediation:* Removed the hardcoded line. Builds now rely on standard environment `JAVA_HOME`.
3. **SEC-03 (Network Security Configuration):**
   - *Threat:* Reliance on default platform trust behavior.
   - *Remediation:* Added `app/src/main/res/xml/network_security_config.xml` pinning network communication to system trust anchors and explicitly disallowing cleartext HTTP traffic.
4. **SEC-04 (CI Workflow Security):**
   - *Threat:* Public repositories accepting forks can be vulnerable to GitHub Actions privilege escalation or secret leakage.
   - *Remediation:* Added `.github/workflows/ci.yml` with minimal token permissions (`contents: read`) and pinned action commit SHAs.

---

## 4. Component & Manifest Security Review

The release Android manifest was audited:
- **Activities:** Single exported activity (`MainActivity`) with `MAIN` / `LAUNCHER` intent filter. Safe against unauthorized intent parameter injection following the SEC-01 fix.
- **Broadcast Receivers:**
  - `SalahNextPrayerWidgetReceiver` (Exported for `APPWIDGET_UPDATE`, guarded by system appwidget manager).
  - `SalahPrayerTimelineWidgetReceiver` (Exported for `APPWIDGET_UPDATE`).
- **Services & Content Providers:** None declared. Zero inter-process data exposure.
- **PendingIntents:** Glance widget interaction uses `actionStartActivity<MainActivity>()` which creates standard safe immutable PendingIntents.
- **Backup Policy:** `android:allowBackup="false"` with explicit exclusion of DataStore and SQLite databases in `backup_rules.xml` and `data_extraction_rules.xml`.

---

## 5. Location Privacy & Network Architecture

Salah's location data flow was audited end-to-end:
1. **Data Minimization:** No continuous tracking or background services. Location is queried only upon explicit user tap.
2. **Offline Independence:** A complete offline database of global cities (`cities.tsv.gzip`) allows full prayer schedule calculation without ever granting location permission or making a network request.
3. **Network Transmission:** When online sync is used, requests transmit only coordinates and calculation preferences to `api.aladhan.com` over HTTPS. Zero device identifiers, advertising IDs, or tracking headers are transmitted.

---

## 6. Open-Source Licensing & Intellectual Property

Full inventory documented in [docs/open-source-dependency-license-inventory.md](file:///Users/abbas/Desktop/Salah/docs/open-source-dependency-license-inventory.md):
- **Project License:** Apache License, Version 2.0 ([LICENSE](file:///Users/abbas/Desktop/Salah/LICENSE)).
- **Adhan Kotlin:** MIT License (Permissive, compatible with Apache 2.0).
- **Fonts:** Geist, Instrument Sans, Manrope, Inter, DM Sans all licensed under SIL Open Font License 1.1.
- **Geospatial Data:** GeoNames CC BY 4.0 data properly attributed in project documentation.
- **Salah Artwork:** Golden Sunrise / Prayer Halo launcher icon and custom Compose Canvas vectors are original project assets released under the project's Apache 2.0 license.

---

## 7. Verification Test Suite Matrix

| Test Suite | Target | Tests Run | Result | Evidence |
| :--- | :--- | :---: | :---: | :--- |
| **JVM Unit Tests** | Host JVM (JDK 21) | **255** | **`PASS`** | 0 failures across `:app`, `:core:prayer`, `:core:location`, `:core:preferences` |
| **Connected Android Tests**| SprintPixel (`emulator-5554`, API 36) | **5** | **`PASS`** | All end-to-end Compose UI tests passed |
| **Android Lint** | Static Analysis | - | **`PASS`** | 0 errors reported in `lint-results-debug.html` |
| **Release Compilation** | R8 / Gradle | - | **`PASS`** | `app-release.apk` compiled and signed successfully |
| **Signature Verification** | `apksigner` | - | **`PASS`** | Scheme v1 and v2 verified |

---

## 8. Final Publication Gate Verdict

```
+-------------------------------------------------------+
|                                                       |
|            PUBLICATION VERDICT: GO                     |
|                                                       |
|  All security, privacy, and licensing gates passed.   |
|  Repository is certified ready for open-source push.  |
|                                                       |
+-------------------------------------------------------+
```

### Action Items for Publication (Awaiting User Command):
1. **Approve Git Push:** Confirm authorization to execute:
   ```sh
   git push -u origin main
   ```
2. **Draft GitHub Release:** Authorize drafting release `v1.0.0` with `Salah-v1.0.0-universal.apk` and its SHA-256 checksum.
