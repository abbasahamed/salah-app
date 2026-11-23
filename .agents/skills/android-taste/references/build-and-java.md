# Kotlin DSL (Gradle), Java and Bash: the build and glue side of taste

Contents: 1 Dependencies for the design stack, 2 Compose and View Binding switches, 3 JVM 21, Java 8 bytecode and desugaring, 4 Java UI code, 5 Bash: running the checks, 6 CI

Do not upgrade the toolchain (AGP, Kotlin, Compose BOM, Gradle) to get a nicer API. Add only what is missing, with versions taken from the project's catalog or confirmed in the official release notes. Never invent a version number.

## 1. Dependencies for the design stack

Put versions in `gradle/libs.versions.toml`. Entries below show structure; fill in versions from the project or official docs. Libraries managed by the Compose BOM take no version.

```toml
[versions]
composeBom = "..."           # keep the project's BOM if it has one
material = "..."             # Material Components (Views)
coreSplashscreen = "..."

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material3-window-size = { group = "androidx.compose.material3", name = "material3-window-size-class" }
androidx-compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "coreSplashscreen" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
```

Module `build.gradle.kts`:

```kotlin
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)   // only if used
    implementation(libs.androidx.compose.ui.text.google.fonts)    // only for downloadable fonts
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)                                  // Views / XML themes
}
```

Adaptive Material 3 artifacts (`NavigationSuiteScaffold`, list-detail scaffolds) and the extended icon set are separate artifacts. Add them only if the screens use them, and prefer a handful of icons as vector drawables over pulling in the entire extended icon library.

## 2. Compose and View Binding switches

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)           // not needed if the project's AGP version provides built-in Kotlin; match the project
    alias(libs.plugins.kotlin.compose)           // org.jetbrains.kotlin.plugin.compose, Kotlin 2.0+
}

android {
    buildFeatures {
        compose = true
        viewBinding = true                        // only for Views screens
    }
}
```

Mixed apps: Compose inside Views via `ComposeView`, Views inside Compose via `AndroidView`; share the same dealt tokens (Compose `AppTheme` and the XML `Theme.App`) so both halves look like one app.

## 3. JVM 21, Java 8 bytecode and desugaring (what matters for UI work)

The JDK used to build (a JDK 21 toolchain is common), the Java/Kotlin bytecode target, and core library desugaring are separate settings. The unslop skill's `gradle-kotlin-dsl.md` covers them in depth; for taste work remember three things:

- Keep Java `sourceCompatibility` / `targetCompatibility` and Kotlin `jvmTarget` equal in a module. A mismatch breaks the build with an inconsistent JVM target error.
- Locale-correct formatting is part of taste (see `content-and-copy.md`): `java.time` formatters and `NumberFormat`. Below the API level that ships `java.time`, turn on core library desugaring (`isCoreLibraryDesugaringEnabled = true` plus the `coreLibraryDesugaring` dependency) rather than falling back to `SimpleDateFormat`. Java 8 bytecode targets are fine with it.
- Language features in Java UI code follow the module's configured level: do not introduce records, sealed types or `var` in a Java 8 module. D8/R8 desugar the supported syntax, but the language level is set in the module.

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17   // match the module; 1.8 stays 1.8
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true          // only if java.time etc. is used below its native API level
    }
}
dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs) }
```

## 4. Java UI code

Java screens get the same system. Equivalents of the Kotlin snippets:

```java
// Application.onCreate(): only for system-feel apps that want wallpaper colors
DynamicColors.applyToActivitiesIfAvailable(this);

// Activity.onCreate()
SplashScreen.installSplashScreen(this);   // before super.onCreate
EdgeToEdge.enable(this);                  // before super.onCreate (androidx.activity)
super.onCreate(savedInstanceState);

// Theme-aware color in code, never a hex literal
int primary = MaterialColors.getColor(view, androidx.appcompat.R.attr.colorPrimary);
// with Material Components: com.google.android.material.R.attr.colorPrimary

// Transitions between fragments
setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

// Locale-correct values
NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.getDefault());
String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(localDate);

// Count-up number (skip when the animator duration scale is 0)
ValueAnimator animator = ValueAnimator.ofInt(0, target);
animator.setDuration(800);
animator.setInterpolator(new DecelerateInterpolator());
animator.addUpdateListener(a -> textView.setText(NumberFormat.getIntegerInstance().format(a.getAnimatedValue())));
animator.start();

// Tiny physical confirmation, API-guarded
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
}
```

Use the Material Components attribute class that matches the project (`com.google.android.material.R.attr.colorPrimary` for Material Components themes). Avoid holding an `Activity` in animators or listeners that outlive it: cancel animators in `onDestroyView` / `onDestroy`.

## 5. Bash: running the checks

All scripts live in `scripts/`, take `--help`, never modify project source files, and work with Bash 3.2+ and standard awk and grep.

| Script | Purpose |
|---|---|
| `deal.sh --kind K` | deals the direction and writes merge-ready theme files to `.android-taste/deal` |
| `contrast.sh` | WCAG ratio for two colors, the dealt library, or a project's M3 role colors |
| `taste_lint.sh` | finds template and generic-UI tells (T0, T1, T2) |
| `shoot.sh --package P` | light/dark and font-scale screenshots via adb; restores device settings |
| `gate.sh` | runs lint, contrast, screenshot count and optional Gradle lint with a three-round limit |

Shell rules the scripts follow and your own glue scripts should too: `set -euo pipefail`, quoted variables, `adb -s "$SERIAL"` so a second attached device cannot be hit by accident, `trap` to restore device settings, no secrets on the command line.

## 6. CI

```bash
set -euo pipefail
./gradlew :app:assembleDebug lint
# On a runner with an emulator booted and the debug APK installed:
bash scripts/shoot.sh --package "$APP_ID" --serial "$ANDROID_SERIAL" --out taste-shots
bash scripts/gate.sh --project . --shots taste-shots --need-shots 4
```

Upload `taste-shots/` as a build artifact even when the gate fails; a failing screenshot is the most useful thing a reviewer can open. Pin the emulator image and API level so screenshots are comparable between runs.
