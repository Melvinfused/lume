# Third-Party Notices and Licenses

This project incorporates open-source libraries, fonts, and assets under various licenses. This document acknowledges and lists the third-party components utilized.

---

## 1. Typography & Glyphs

### Inter Font Family
- **Author:** Rasmus Andersson and Contributors
- **License:** SIL Open Font License, Version 1.1 (OFL 1.1)
- **Source:** [https://github.com/rsms/inter](https://github.com/rsms/inter)
- **Full Text:** [`licenses/OFL.txt`](licenses/OFL.txt)
- **Usage:** Default system UI typography (`font/inter_regular.ttf`, `font/inter_bold.ttf`).

### Material Icons / Material Symbols (Fallback Glyphs)
- **Author:** Google LLC
- **License:** Apache License, Version 2.0
- **Source:** [https://github.com/google/material-design-icons](https://github.com/google/material-design-icons)
- **Full Text:** [`licenses/LICENSE-MaterialIcons.txt`](licenses/LICENSE-MaterialIcons.txt)
- **Usage:** Subsetted fallback icon font (`font/segmdl2_fallback.ttf`) providing status icons (WiFi, Ethernet, Battery, Settings, Media controls).

---

## 2. Vector Icons & Graphics

### Meteocons Weather Icons
- **Author:** Bas Milius
- **License:** MIT License
- **Source:** [https://github.com/basmilius/weather-icons](https://github.com/basmilius/weather-icons)
- **Usage:** Vector weather state drawables (`drawable/ic_meteo_*.xml`).

---

## 3. Libraries & Dependencies

### AndroidX, Android Support & Google Play Services
- **Author:** The Android Open Source Project & Google LLC
- **License:** Apache License, Version 2.0
- **Libraries Used:**
  - `androidx.core:core-ktx`
  - `androidx.appcompat:appcompat`
  - `com.google.android.material:material`
  - `androidx.constraintlayout:constraintlayout`
  - `androidx.leanback:leanback`
  - `androidx.tvprovider:tvprovider`
  - `androidx.media:media`

### Kotlin Standard Library & Coroutines
- **Author:** JetBrains s.r.o.
- **License:** Apache License, Version 2.0
- **Libraries Used:**
  - `org.jetbrains.kotlin:kotlin-stdlib`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android`

---

## 4. Web Services & Data Providers

### Open-Meteo Weather API
- **Provider:** Open-Meteo.com
- **License / Terms:** Creative Commons Attribution 4.0 International (CC BY 4.0)
- **Website:** [https://open-meteo.com/](https://open-meteo.com/)
- **Usage:** Dynamic live weather forecasting for the launcher home tile.

---

## 5. Proprietary Assets Disclaimer (Not Distributed)

This open-source repository **does not** redistribute proprietary fonts or third-party trademarked icon packs:
- **Microsoft Segoe UI & Segoe MDL2 Assets:** These are proprietary fonts owned by Microsoft Corporation. The repository provides open-source equivalents (Inter and Material Symbols fallback). Users who own valid Windows licenses may place their own font files in `local-assets/fonts/` for personal builds.
- **Projectivy Launcher Icon Pack:** Third-party brand icons are not bundled into this repository. High-contrast, open-source vector drawables are supplied out of the box. Users wishing to overlay custom PNG brand icons can place them into `local-assets/icons/`.
