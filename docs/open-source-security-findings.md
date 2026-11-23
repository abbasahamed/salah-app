# Open-Source Security Audit Findings
## Salah Android 3.0 (Milestone 9A)

**Auditor:** Antigravity (Principal Application Security Engineer)  
**Date:** November 23, 2026  
**Scope:** Complete Git repository (`/Users/abbas/Desktop/Salah`), 5 reachable commits, dependencies, Android manifests, compilation targets, and release distribution APKs.

---

## Findings Summary

| ID | Title | Severity | Status | Remediation Summary |
| :---: | :--- | :---: | :---: | :--- |
| **SEC-01** | Release Scenario & Theme Intent Backdoor | **HIGH** | **RESOLVED** | Isolated test scenario parsing to `app/src/debug` using `ScenarioProvider`. Release source set returns strictly `null`. |
| **SEC-02** | Hardcoded Developer JDK Path in Build Config | **LOW** | **RESOLVED** | Removed developer-machine absolute path from `gradle.properties`. |
| **SEC-03** | Missing Explicit Network Security Configuration | **MEDIUM** | **RESOLVED** | Created `network_security_config.xml` strictly restricting trust anchors to system CA certificates and disabling cleartext. |
| **SEC-04** | Missing Automated CI Guardrails | **LOW** | **RESOLVED** | Created GitHub Actions workflow `.github/workflows/ci.yml` with least-privilege tokens and pinned action SHAs. |
| **SEC-05** | Cloud Backup Data Leakage Risk | **LOW** | **VERIFIED PASS** | Verified `android:allowBackup="false"` and explicit exclusion rules in `backup_rules.xml` and `data_extraction_rules.xml`. |
| **SEC-06** | Secret & Credential Exposure in Git History | **CRITICAL** | **VERIFIED PASS** | Automated Gitleaks scan and pattern analysis of all 5 Git commits verified zero hardcoded secrets. |

---

## Detailed Vulnerability Reports

### SEC-01: Release Scenario & Theme Intent Injection Backdoor
- **Severity:** HIGH
- **Component:** `app/src/main/kotlin/com/salah/app/MainActivity.kt` & `SalahApp.kt`
- **Vulnerability Description:** In previous milestone builds, `MainActivity.kt` inspected `intent.getStringExtra("scenario")` and `intent.getStringExtra("theme")` directly in production code. An external app or ADB shell command on an unrooted device could craft an explicit intent (`am start -n com.salah.app/.MainActivity --es scenario today_gps`) to force arbitrary mock locations or fake prayer schedules.
- **Exploitability:** Local privilege escalation via component interaction (`MainActivity` is exported as launcher).
- **Remediation:** 
  - Implemented source-set separation:
    - `app/src/debug/kotlin/com/salah/app/scenario/ScenarioProvider.kt`: Reads test scenario intent extras for local screenshot automation.
    - `app/src/release/kotlin/com/salah/app/scenario/ScenarioProvider.kt`: Strictly returns `null` for any scenario or theme override.
  - Added regression test `ScenarioSecurityTest.kt` verifying null safety.
- **Verification:** Verified release compilation compiles against release variant with zero backdoor functionality exposed.

---

### SEC-02: Hardcoded Machine-Specific JDK Path
- **Severity:** LOW
- **Component:** `gradle.properties`
- **Vulnerability Description:** `gradle.properties` contained `org.gradle.java.home=/Users/abbas/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home`. This leaks local developer username and breaks out-of-the-box build reproducibility on contributor workstations.
- **Remediation:** Removed the hardcoded line from `gradle.properties`. Configured CI and setup guides to rely on system environment `JAVA_HOME`.
- **Verification:** Successfully executed `./gradlew testDebugUnitTest` and `./gradlew assembleRelease` using standard JDK environment.

---

### SEC-03: Network Security Configuration Hardening
- **Severity:** MEDIUM
- **Component:** `app/src/main/res/xml/network_security_config.xml` & `AndroidManifest.xml`
- **Vulnerability Description:** While `android:usesCleartextTraffic="false"` was present, Android 7.0+ applications should define an explicit `networkSecurityConfig` to guarantee that user-installed certificates cannot intercept API traffic and that cleartext is structurally blocked.
- **Remediation:** Created `network_security_config.xml` pinning `api.aladhan.com` and global network traffic to system trust anchors only. Referenced in `AndroidManifest.xml`.
- **Verification:** Build assembled and passed lint checks with zero network security warnings.

---

### SEC-04: Secure CI Workflow Scaffolding
- **Severity:** LOW
- **Component:** `.github/workflows/ci.yml`
- **Vulnerability Description:** Public GitHub repositories accepting pull requests require secure CI configurations that prevent malicious workflows from accessing repo secrets or leaking release credentials.
- **Remediation:** Created `.github/workflows/ci.yml` with:
  - `permissions: contents: read` (minimal token privilege).
  - Pinned third-party actions to full immutable commit SHAs.
  - Zero access to signing secrets or publishing credentials.
- **Verification:** Validated GitHub Actions workflow syntax.
