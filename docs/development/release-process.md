# Release Process & Key Management

This document describes the release engineering practices, signing architecture, and distribution pipeline for **Salah**.

---

## 1. Versioning Strategy

Salah adheres to [Semantic Versioning 2.0.0](https://semver.org/):

$$\text{MAJOR}.\text{MINOR}.\text{PATCH}$$

- **MAJOR:** Significant architectural changes or major redesigns.
- **MINOR:** New user-facing features (e.g., new widgets, additional calculation standards).
- **PATCH:** Bug fixes, performance improvements, and security updates.

Version numbers and internal version codes are declared in `app/build.gradle.kts`.

---

## 2. Release Signing Architecture

Salah builds use APK Signature Scheme v1 (JAR signing) and v2 (Full APK block signing) for broad Android compatibility from API 23 to API 36+.

### Security Principles for Signing Keys
1. **Never Commit Keystores:** Keystores, passphrases, and key aliases are strictly excluded via `.gitignore` and never committed to source control.
2. **Environment Variable Injection:** For release compilation, signing properties are passed via local environment variables or Gradle properties located outside the repository root:
   - `SALAH_RELEASE_STORE_FILE`
   - `SALAH_RELEASE_STORE_PASSWORD`
   - `SALAH_RELEASE_KEY_ALIAS`
   - `SALAH_RELEASE_KEY_PASSWORD`
3. **Backup Vault:** Production signing keys must be backed up in an isolated, encrypted vault outside the development repository.

---

## 3. Building a Production Signed Release

```bash
# 1. Clean previous build artifacts
./gradlew clean

# 2. Build release APK with R8 optimization
./gradlew assembleRelease

# 3. Verify signature with apksigner
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk

# 4. Generate SHA-256 checksum
shasum -a 256 app/build/outputs/apk/release/app-release.apk > Salah-vX.Y.Z-universal.apk.sha256
```

---

## 4. GitHub Release Publication Checklist

1. [ ] Ensure working tree is clean and all tests pass (`testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`).
2. [ ] Update `CHANGELOG.md` with release notes following Keep a Changelog conventions.
3. [ ] Create an annotated Git tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`.
4. [ ] Push commits and tag to GitHub: `git push origin main --tags`.
5. [ ] Create a GitHub Release referencing the tag.
6. [ ] Attach the signed APK (`Salah-vX.Y.Z-universal.apk`) and checksum (`Salah-vX.Y.Z-universal.apk.sha256`).
7. [ ] Download the published asset in a clean environment and verify the SHA-256 checksum matches bit-for-bit before announcing the release.

