# Lume (Android TV Launcher)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform: Android TV](https://img.shields.io/badge/Platform-Android%20TV%20%7C%20Google%20TV-green.svg)](https://developer.android.com/tv)
[![API](https://img.shields.io/badge/API-21%2B%20(Lollipop%20to%2014)-orange.svg)](https://developer.android.com/about/dashboards)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20100%25-purple.svg)](https://kotlinlang.org/)

A lightweight, elegant, and blazing-fast Android TV launcher inspired by Windows Metro and modern Acrylic fluid design. Built purely in Kotlin with native Android UI components, responsive D-pad remote navigation, bundled dynamic widget tiles (Clock, Flipping Weather, and Now Playing), ambient wallpapers, Continue Watching integration, and zero telemetry or bloat.

---

## ✨ Features

- **Fluent / Acrylic Design:** Translucent frosted acrylic tiles, responsive focus elevation, glowing border animations, and customizable tile dimensions.
- **100% D-Pad Remote Navigation:** Natural, snappy navigation tailored specifically for TV remotes with zero lag or mouse requirements.
- **Bundled Dynamic Information Widgets:**
  - 🕒 **Live Clock Tile:** Informative widget displaying 12-hour formatted time with uppercase AM/PM, full weekday, and complete date.
  - ⛅ **3D Flipping Weather Tile:** Current meteorological conditions powered by the free Open-Meteo API. Flips smoothly across the Y-axis to reveal detailed forecasts, featuring auto-flip rotation and air-mouse hover safety.
  - 🎵 **Now Playing Media Tile:** Real-time media playback detector supporting Spotify, YouTube Music, Apple Music, VLC, and other Android media apps, complete with album artwork and track metadata.
- **Clean App Grid Tiles:** Fast, lightweight Metro-style launch cards for all your installed Android TV apps with customizable pinning, favoriting, and focus highlights.
- **Continue Watching / Watch Next Row:** Native Android TV `TvProvider` integration showing ongoing shows and movies with progress indicators and poster thumbnails.
- **Dynamic Wallpaper Engine & Palette Tinting:**
  - Cycles through high-resolution ambient wallpapers or lets you pick a custom local wallpaper (JPG, PNG, GIF).
  - Automatically samples dominant wallpaper colors to generate tailored tile themes and matching focus borders.
  - 5-step tile transparency control (20%, 40%, 60%, 80%, 100%).
- **Ambient Mode (Screensaver):** Automatic screen dimming when idle with live Now Playing media info, rotating 4K wallpapers, formatted clock, and weather. Automatically keeps the screen awake without letting the system screensaver interrupt when the system screensaver is disabled. Safe wake-up prevents accidental tile clicks.
- **Performance Modes & Focus Selectors:**
  - Modes: **Smooth** (fluid easing curves), **Balanced** (default optimized transitions), and **Performance** (zero animations for ultra-low latency).
  - Focus Borders: **Breathing Glow** (gentle pulse), **Pulse** (cursor-style rhythm), and **Static** (solid classic Metro outline).
- **App Management:** Context menu (long-press OK or press Menu) to pin apps to the top row, favorite, hide unwanted apps, or view app info.
- **Privacy-First & Legally Compliant:** 100% open source, zero data collection or tracking, and ships with freely licensed typography (Inter) and vector icons.

---

## 📱 Compatibility

- **Target Platforms:** Android TV and Google TV devices (Chromecast with Google TV, Nvidia Shield, Xiaomi TV Box, Mecool, onn. Google TV, Sony/TCL/Hisense Android TVs, etc.).
  - *Note:* Fire TV (Fire OS) is currently **untested**. While the APK may install on Fire OS, Amazon does not provide standard Android TV Leanback integrations and actively blocks custom default launchers on recent Fire OS updates.
- **Supported Android Versions:** Android 5.0 Lollipop (API 21) through Android 14 (API 34).
- **Input Devices:** Standard TV infrared/Bluetooth remotes, air mice, game controllers, and keyboards.

---

## 📖 How-To Guides

### 1. How to Install on Your TV

#### Method A: Sideload via USB Drive or TV File Transfer (Easiest)
1. Download or build the APK file (`app-debug.apk` or `app-release.apk`).
2. Transfer the APK to your TV using one of the following methods:
   - **USB Flash Drive:** Copy the APK to a USB drive, plug it into your Android TV, and open a TV file manager (such as *FX File Explorer*, *TV File Commander*, or *File Manager*).
   - **Send Files to TV:** Install the free **Send Files to TV** app from the Google Play Store on both your phone/PC and your TV to send the APK wirelessly.
   - **Downloader App:** Use the popular **Downloader by AFTVnews** app on your TV to download the APK from a local network share or URL.
3. In your TV's file manager, click the APK and select **Install**. If prompted, grant permission to "Install unknown apps" for that file manager in TV Settings.

#### Method B: Sideload via ADB (Command Line)
If your TV has Developer Options and Network/USB Debugging enabled:
```bash
# Connect to your TV over Wi-Fi (replace with your TV's local IP address)
adb connect 192.168.1.100:5555

# Install the APK onto the TV
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### 2. How to Set as Default Home Launcher

#### Standard Android TV
1. Press the **HOME** button on your remote.
2. A prompt will appear: "Select a Home app".
3. Choose **Lume** and select **Always**.
*(Alternatively, navigate to TV **Settings > Apps > Default apps > Home app** and select Lume).*

#### Google TV (Where Stock Launcher is Locked)
Some Google TV and OEM-locked TV devices restrict changing the default launcher from the graphical settings. You can set Lume as your primary launcher via ADB or helper utilities:
- **ADB Command:**
  ```bash
  adb shell cmd package set-home-activity com.metrolauncher.tv/.MainActivity
  ```
- **Launcher Remappers:** Apps like **Button Mapper** or **Projectivy Launcher Accessibility Service** can be used to remap the remote's Home button to open Lume instantly.

---

### 3. How to Access Launcher Settings

Metro TV keeps the dashboard clutter-free by tucking its configuration options neatly into the header:

1. Use your D-Pad **Up** arrow to navigate from the app grid into the top header row.
2. Navigate **Right** to highlight the TV Settings gear icon (`btn_settings`).
3. Press **Right** once more on your D-pad!
4. The hidden **Launcher Config** button will smoothly slide into view from the right.
5. Press **OK / Center** to open the Lume Configuration modal.

*(Note: Navigating away from the button or pressing Back will smoothly slide it back out of sight to preserve the minimalist interface).*

---

### 4. How to Enable the "Now Playing" Music Tile

The Now Playing tile integrates with active music players (such as Spotify, YouTube Music, Apple Music, VLC) to display real-time track metadata and album artwork directly on your home screen.

1. Open **Launcher Config** (see guide above).
2. Scroll to the **Now Playing Tile Widget** section.
3. Click on the **Notification Access** item.
4. Android will open the **Special App Access > Notification Access** settings menu.
5. Find **Lume** in the list and toggle the switch to **Allowed**.
6. Return to Lume. Play music in your favorite app—the Now Playing tile will now automatically display track info and cover art!

---

### 5. How to Configure Live Weather

The weather tile automatically fetches live temperatures and forecasts from the free Open-Meteo API with offline caching.

1. Open **Launcher Config**.
2. Scroll down to the **Weather Location** section.
3. To set a custom city:
   - Click the text box, enter your city name (e.g. `London`, `Tokyo`, `Berlin`, `New York`), and select **Save**.
   - Lume will geocode the city coordinates and immediately update the live tile.
4. Quick Presets: Alternatively, click any of the preset location chips for instant configuration.

---

### 6. How to Customize Wallpapers & Tile Colors

1. Open **Launcher Config**.
2. **Custom Wallpaper:**
   - Click **Choose Custom Wallpaper (JPG/PNG/GIF)** to select an image from your device's local storage or a connected USB drive.
   - Click **Reset to Default Wallpaper** at any time to return to the built-in ambient wallpaper engine.
3. **Tile Color & Theme:**
   - **Default Acrylic:** Classic Metro dark frosted glass tiles with clean white borders.
   - **Wallpaper Color:** Dynamically extracts the dominant color palette from your current wallpaper and applies matching tile tints and glowing focus strokes.
4. **Transparency:**
   - Choose between **20%**, **40%**, **60%**, **80%**, or **100%** opacity to balance legibility against wallpaper visibility.

---

### 7. How to Manage Apps (Context Menu)

You can customize your home screen apps directly from the remote:

1. Use the D-pad to highlight any app tile in the grid.
2. **Long-press the remote Center (OK) button** or press the **Menu** button on your remote.
3. The Context Menu dialog will open with options:
   - **Pin to Top Row:** Move essential apps to the prominent top app row.
   - **Move / Reorder:** Shift tiles horizontally to organize your favorites.
   - **Favorite / Unfavorite:** Add a quick-access star marker.
   - **Hide from Home:** Hide system utilities or unwanted pre-installed apps from cluttering the launcher.
   - **App Info:** Open Android's system application details screen (for cache clearing, permissions, or uninstallation).

---

### 8. How to Use Metro Ambient Mode as Your Screensaver (Overriding System Screensaver)

Android TV and Google TV include a stock system screensaver ("Backdrop" / "Colors" / "Ambient Mode") that activates after 5–15 minutes of inactivity. When the system screensaver starts, it launches over Lume and hides the custom launcher backdrop.

To let **Lume's Ambient Mode** serve as your primary screensaver (featuring rotating 4K wallpapers, dimming, live weather, formatted time, and Spotify/music playback cover art) without being interrupted:

#### Option A: Via Launcher Settings (Fastest)
1. Open **Launcher Config** (navigate to the top Settings gear and press **Right** on the D-Pad).
2. Scroll to the **Ambient Mode & Screensaver** section.
3. Click **System Screen Saver Settings**.
4. In Android's Screen Saver settings menu:
   - Set **When to start** / **Screen saver** to **Never** (or **Turn off screen saver**).
   - Set **Turn off display** / **Energy saver** to your preferred idle duration (e.g. 1 hour, 3 hours, or never).
5. Lume will now smoothly slide in its own Ambient Mode after 5 minutes of inactivity and keep the display alive and beautifully dimmed!

#### Option B: Via TV Settings GUI
1. Open Android TV / Google TV **Settings**.
2. Navigate to **System** > **Ambient mode** (or **Device Preferences** > **Screen saver**).
3. Set **Screen saver** / **When to start** to **Never**.

#### Option C: Via ADB Command
```bash
# Disable Android's built-in dream service / screensaver
adb shell settings put secure screensaver_enabled 0
adb shell settings put secure screensaver_activate_on_sleep 0
```
*(To re-enable Android's stock screensaver at any time: `adb shell settings put secure screensaver_enabled 1`)*

---

## 🎮 TV Remote Navigation Cheat Sheet

| Button | Action |
|:---|:---|
| **D-Pad Arrows (Up/Down/Left/Right)** | Move focus across header, information tiles, and app grid |
| **Center / OK / Enter** | Launch selected app or activate settings option |
| **Long-Press Center / OK** | Open tile Context Menu (Pin, Hide, Favorite, App Info) |
| **Menu Button** | Open tile Context Menu on the focused item |
| **D-Pad Right (past Settings cog)** | Reveal the hidden Launcher Config button in top header |
| **Back Button** | Close menus / dialogs; dismiss Ambient Mode; return focus to home |
| **Any Key (during Ambient Mode)** | Wake screen safely without triggering background tile launches |

---

## 🛠️ Building from Source (Developer Guide)

### Prerequisites

- [OpenJDK 17+](https://adoptium.net/)
- [Android Studio](https://developer.android.com/studio) (Jellyfish or newer) or Android Command Line Tools (`cmdline-tools`)
- Android SDK Platform `android-34` and Build-Tools `34.0.0`

### 1. Build the Standard Open-Source APK

By default, the repository is configured to build a 100% open-source APK using the **Inter** font family, **Material Symbols** status icons, and clean vector graphics out of the box.

```bash
# Clone the repository
git clone https://github.com/Melvinfused/lume.git
cd lume

# Configure your Android SDK path
cp local.properties.example local.properties
# Edit local.properties and set sdk.dir to your Android SDK location

# Build the Debug APK
./gradlew assembleDebug
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🎨 Design Philosophy: Visual Completeness & Typography

### The Authentic Metro Aesthetic
Lume was architected from the ground up to pay homage to the classic Windows Metro and modern Fluent design languages. Every tile margin, clock proportion, and header alignment was designed and calibrated around the exact metric proportions, tracking, and optical weights of **Segoe UI** and the icon grid of **Segoe MDL2 Assets**.

> **💡 Design Note on Visual Fidelity:**  
> While the bundled open-source fonts (**Inter** and **Material Symbols**) look sleek and ensure the project remains 100% legally distributable out of the box, **the launcher feels truly complete and achieves its authentic, 1:1 intended visual identity** when rendered with genuine Segoe UI typography and MDL2 glyphs.

### How to Build with Original Typography (Personal & Licensed Builds)
To maintain strict copyright compliance while enabling designers and enthusiasts to experience the complete, intended visual aesthetic on their personal setups:

1. **The Automated Asset Drop-In:**
   The repository includes a dedicated, Git-ignored directory at [`local-assets/fonts/`](file:///c:/Users/thoma/Github%20Repo/android-tv-launcher/local-assets/fonts/README.md).
2. **Personal Compilation:**
   If you hold an appropriate license or build for private personal use, place your font files there:
   ```text
   local-assets/
   └── fonts/
       ├── segoeui.ttf      # Segoe UI Regular
       └── segmdl2.ttf      # Segoe MDL2 Assets
   ```
3. **Automatic Build Integration:**
   When you run `./gradlew assembleDebug` or `./gradlew assembleRelease`, the build pipeline (`prepareLauncherAssets`) automatically detects the local fonts, compiles them into the app assets, and activates the genuine Metro typography and glyph layout.
4. **Graceful Fallback:**
   If the directory is empty (such as on any public fork or CI/CD runner), the build automatically uses the open-source Inter and Material Symbols fonts without throwing any errors.

---

## ⚖️ Font Licensing & Legal FAQ

### Is it legal to extract and use Microsoft Segoe UI / Segoe MDL2 Assets?

**Short answer:**
- **In Public Releases / Distributed APKs:** **No.** You cannot legally redistribute Microsoft Segoe UI (`segoeui.ttf`) or Segoe MDL2 Assets (`segmdl2.ttf`) inside an APK or public Git repository without an explicit commercial font embedding license from Microsoft / Monotype.
- **For Private, Personal Use on Your Own PC/Device:** Extracting fonts from `C:\Windows\Fonts\` for personal use on a non-Windows device exists in a legal grey area under Microsoft's Windows EULA (which restricts font usage to the licensed Windows operating system). While Microsoft generally does not enforce personal, non-commercial home use, distributing any build containing those files is a direct copyright infringement.

### How Lume Solves This Legally

To ensure that Lume is **100% legal, open-source, and freely distributable**, the repository does **not** bundle or require any proprietary Microsoft fonts:

1. **Default Open-Source Typography:**
   - **Inter Font Family:** Bundled under the [SIL Open Font License 1.1](licenses/OFL.txt). Provides a crisp, modern, legible UI matching the clean aesthetic.
   - **Material Symbols Glyphs:** Bundled under the [Apache License 2.0](licenses/LICENSE-MaterialIcons.txt). Custom-subsetted fallback icon font providing system icons (WiFi, Ethernet, Battery, Media controls, Settings).
   - **Meteocons:** Bundled under the [MIT License](THIRD_PARTY_NOTICES.md) for weather condition graphics.

2. **Decoupled Local Asset Pipeline (`local-assets/`):**
   - The build script (`app/build.gradle.kts`) contains an optional preprocessor (`prepareLauncherAssets`).
   - If an individual developer chooses to place their own custom fonts (`local-assets/fonts/segoeui.ttf`, `local-assets/fonts/segmdl2.ttf`) or icon packs (`local-assets/icons/*.png`) on their local machine, Gradle will automatically inject them into that developer's private build.
   - The `local-assets/` directory is strictly **Git-ignored** in `.gitignore` to prevent any proprietary or unverified assets from ever being committed to source control or distributed in public builds.

---

## 🏛️ Architecture & Project Structure

```text
lume/
├── app/src/main/
│   ├── java/com/metrolauncher/tv/
│   │   ├── MainActivity.kt               # TV dashboard, D-pad navigation, widget timers, ambient mode
│   │   ├── AppsAdapter.kt                # RecyclerView adapter with Metro acrylic card bindings
│   │   ├── AppItem.kt                    # App model, package mappings, tile tinting & icon resolution
│   │   ├── WatchNextAdapter.kt           # Carousel adapter for Android TV "Continue Watching"
│   │   ├── WatchNextRepository.kt        # ContentProvider queries for Android TV TvContract
│   │   ├── NowPlayingManager.kt          # MediaSessionManager observer for live playing status
│   │   ├── NowPlayingListenerService.kt  # NotificationListenerService for media notifications
│   │   ├── WallpaperThemeManager.kt      # Dynamic palette extraction from wallpapers
│   │   ├── AmbientWallpaperManager.kt    # Automatic wallpaper rotation & backdrop transitions
│   │   ├── CustomWallpaperManager.kt     # User-selected wallpaper loading and caching
│   │   ├── LauncherConfig.kt             # SharedPreferences manager for launcher settings
│   │   ├── LauncherSettingsDialog.kt     # TV modal settings (themes, weather, performance, modes)
│   │   └── TileContextMenuDialog.kt      # Remote context menu (pin, favorite, hide, app info)
│   └── res/
│       ├── layout/                       # UI layouts (activity_main, dialog_launcher_settings, etc.)
│       ├── font/                         # Open-source font definitions (Inter, Material Symbols)
│       └── drawable/                     # Clean vector drawables and acrylic tile backgrounds
├── local-assets/                         # Optional personal assets (Git-ignored)
│   ├── fonts/                            # Optional custom TTF fonts for personal builds
│   └── icons/                            # Optional custom PNG icons for personal builds
└── licenses/                             # Open-source license texts
```

---

## 🤝 Contributing

Contributions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) for code formatting, pull request workflows, and guidelines.

---

## 📄 License & Notices

- **Source Code:** Released under the [MIT License](LICENSE).
- **Third-Party Notices:** See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for detailed license texts and attributions.
