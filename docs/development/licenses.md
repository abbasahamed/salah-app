# Open-Source Dependency & License Inventory
## Salah Android 3.0 (v1.0.0)

**Date:** November 23, 2026  
**Repository:** `/Users/abbas/Desktop/Salah`  
**License Adopted:** Apache License, Version 2.0  

---

## 1. Direct Software Dependencies

| Library Module | Pinned Version | License | Origin / Maintainer | Purpose | Redistribution Compliance |
| :--- | :---: | :---: | :--- | :--- | :---: |
| `com.batoulapps.adhan:adhan2` | `0.0.5` | **MIT** | Batoul Apps | Embedded astronomical calculation engine | :white_check_mark: Permissive |
| `androidx.compose:compose-bom` | `2024.12.01` | **Apache 2.0** | Google Android Open Source Project | Jetpack Compose UI toolkit & Material 3 | :white_check_mark: Permissive |
| `androidx.glance:glance-appwidget` | `1.2.0` | **Apache 2.0** | Google AOSP | Jetpack Glance 2×2 & 4×2 home screen widgets | :white_check_mark: Permissive |
| `androidx.room:room-runtime` | `2.7.2` | **Apache 2.0** | Google AOSP | Local SQLite prayer cache persistence | :white_check_mark: Permissive |
| `androidx.datastore:datastore-preferences` | `1.1.4` | **Apache 2.0** | Google AOSP | Key-value settings storage | :white_check_mark: Permissive |
| `com.squareup.okhttp3:okhttp` | `4.12.0` | **Apache 2.0** | Square, Inc. | HTTPS network client for AlAdhan sync | :white_check_mark: Permissive |
| `com.squareup.moshi:moshi` | `1.15.2` | **Apache 2.0** | Square, Inc. | JSON parsing for API responses | :white_check_mark: Permissive |
| `com.google.dagger:hilt-android` | `2.56.2` | **Apache 2.0** | Google LLC | Dependency injection framework | :white_check_mark: Permissive |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android`| `1.9.0` | **Apache 2.0** | JetBrains s.r.o. | Asynchronous concurrency | :white_check_mark: Permissive |
| `com.google.android.gms:play-services-location` | `21.3.0` | **Android Software Development Kit License** | Google LLC | Fused foreground GPS location provider | :white_check_mark: Binary SDK permitted |
| `com.android.tools:desugar_jdk_libs` | `2.1.5` | **Apache 2.0** | Google AOSP | Java 8+ API desugaring for `java.time` | :white_check_mark: Permissive |

---

## 2. Bundled Visual & Typographic Assets

| Asset / File Path | Author / Origin | License | Notice / Permitted Usage |
| :--- | :--- | :--- | :--- |
| `core/designsystem/src/main/res/font/geist_variable.ttf` | Vercel & Basement Studio | **SIL Open Font License 1.1** | Freely usable, bundlable, and redistributable in open-source software. |
| `core/designsystem/src/main/res/font/instrument_sans_variable.ttf`| Instrument & Rodrigo Fuenzalida | **SIL Open Font License 1.1** | Freely redistributable under OFL. |
| `core/designsystem/src/main/res/font/manrope_variable.ttf` | Mikhail Sharanda | **SIL Open Font License 1.1** | Freely redistributable under OFL. |
| `core/designsystem/src/main/res/font/inter_variable.ttf` | Rasmus Andersson | **SIL Open Font License 1.1** | Freely redistributable under OFL. |
| `core/designsystem/src/main/res/font/dm_sans_variable.ttf` | Colophon Foundry & Google | **SIL Open Font License 1.1** | Freely redistributable under OFL. |
| `app/src/main/res/mipmap-*/ic_launcher*.png` | Salah Original Artwork (Option 3: Golden Sunrise / Prayer Halo) | **Apache 2.0** (Project) | Custom-designed native adaptive launcher icon. |
| `core/designsystem/src/main/kotlin/com/salah/designsystem/SalahIcons.kt` | Custom Compose Canvas Vectors | **Apache 2.0** (Project) | Original geometric prayer and navigation vectors. |

---

## 3. Bundled Geospatial & Offline Datasets

| Dataset | Origin | License | Attribution Requirement |
| :--- | :--- | :--- | :--- |
| `core/location/src/main/assets/location/cities.tsv.gzip` | GeoNames (`cities15000.txt`) | **Creative Commons Attribution 4.0 (CC BY 4.0)** | Attributed in [docs/milestone-3.md](file:///Users/abbas/Desktop/Salah/docs/milestone-3.md) and [PRIVACY.md](file:///Users/abbas/Desktop/Salah/PRIVACY.md). |
| `core/location/src/main/assets/location/zones.bin.gzip` | Timezone Boundary Builder | **Open Database License (ODbL) / CC BY-SA** | Polygon boundaries compiled to packed binary format. |

---

## 4. License Compatibility Assessment

The project adopted the **Apache License, Version 2.0**.
- **MIT components (Adhan Kotlin)** are fully compatible with Apache 2.0.
- **OFL 1.1 fonts** are bundled as font software without modification, adhering to OFL bundling clauses.
- **CC BY 4.0 GeoNames dataset** is attributed in documentation and project metadata.
- **Zero GPL / copyleft dependencies** exist in the runtime classpath.

**Conclusion:** All bundled software, fonts, and assets permit open-source redistribution.

