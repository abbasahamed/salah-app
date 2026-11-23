# Development Setup Guide

This guide walks through setting up your local environment to build, run, and test **Salah**.

---

## Prerequisites

- **Operating System:** macOS, Linux, or Windows (WSL2 recommended).
- **Java Development Kit:** JDK 21 (Temurin, OpenJDK, or Android Studio bundled JBR 21).
- **Android Studio:** Ladybug (2024.2.1), Meerkat, or newer.
- **Android SDK:**
  - Compile SDK: API 35
  - Target SDK: API 35
  - Minimum SDK: API 23 (Android 6.0 Marshmallow)
  - Android SDK Build-Tools: 35.0.0
- **Device / Emulator:** An Android Virtual Device (AVD) running API 34+ or a physical Android device with USB debugging enabled.

---

## 1. Cloning the Repository

```bash
git clone https://github.com/abbasahamed/salah-app.git
cd salah-app
```

---

## 2. Environment Configuration

Ensure your `JAVA_HOME` environment variable points to a JDK 21 installation.

### macOS / Linux
```bash
# Example with SDKMAN or Homebrew
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"
```

Verify with:
```bash
java -version
```

---

## 3. Building from the Command Line

The repository includes the Gradle wrapper:

```bash
# Build the debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Installing onto a Connected Device / Emulator

```bash
# Check connected devices
adb devices

# Install debug build
./gradlew installDebug
```

---

## 5. Troubleshooting Common Issues

### Incompatible Daemon JVM
If Gradle reports an incompatible JVM version, ensure you are not running JDK 27 or newer on older AGP tooling. Salah is standardized on JDK 21 for both local builds and GitHub Actions CI.

### ADB Signature Mismatch
If you previously had the signed release build installed and attempt to install a debug build:
```bash
adb uninstall com.salah.app
./gradlew installDebug
```
Release and debug APKs use different signing keys and cannot be installed over each other without uninstalling first.

