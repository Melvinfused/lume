# Contributing to Android TV Launcher

Thank you for your interest in contributing to the Android TV Launcher! We welcome bug reports, feature suggestions, documentation improvements, and code contributions.

---

## Getting Started

### Prerequisites
- **JDK:** OpenJDK 17 or higher
- **Android Studio:** Jellyfish | 2023.3.1 or newer recommended
- **Android SDK:**
  - Compile SDK: `34`
  - Target SDK: `34`
  - Minimum SDK: `21` (Android 5.0 Lollipop)

### Setup
1. Fork and clone the repository:
   ```bash
   git clone https://github.com/<your-username>/android-tv-launcher.git
   cd android-tv-launcher
   ```
2. Copy the sample SDK properties template:
   ```bash
   cp local.properties.example local.properties
   ```
   Edit `local.properties` to specify your local Android SDK location.
3. Build the debug APK out of the box (uses open-source fonts and vector icons):
   ```bash
   ./gradlew assembleDebug
   ```

---

## Important Rules for Assets & Licensing

To protect the open-source integrity of this project:
1. **Never commit proprietary fonts:** While the launcher is architecturally proportioned to look truly complete with authentic Segoe UI and Segoe MDL2 typography in private setups, pull requests must never commit `segoeui.ttf`, `segmdl2.ttf`, or any other proprietary binaries to Git. All pull requests must compile cleanly with the bundled open-source Inter and Material Symbols fonts.
2. **Never commit proprietary or third-party trademarked icon packs:** App brand icons must be original vector drawables (`.xml`) or freely licensed assets. Custom personal icon overlays belong strictly in `local-assets/icons/`, which is ignored by Git.
3. **Verify licensing:** Any new asset, font, or library must be accompanied by its license details in `THIRD_PARTY_NOTICES.md` and be compatible with the project's open-source license.

---

## Development Guidelines

### Android TV & D-Pad Navigation
- Every interactable view **must** be focusable (`android:focusable="true"`, `android:clickable="true"`).
- Always supply clear visual focus states (elevation, stroke highlights, or scaling).
- Test directional navigation (Up, Down, Left, Right, Center/D-Pad Center).
- Long-click interactions (context menus) should map naturally to holding the D-pad Center or remote Menu button.

### Architecture & Code Style
- **Language:** Kotlin 1.9+.
- Follow official Kotlin coding conventions.
- Keep UI thread free of blocking I/O (use Kotlin Coroutines and `Dispatchers.IO`).
- Use AndroidX Leanback and TvProvider components where applicable for TV channels and Watch Next rows.

---

## Submitting Pull Requests

1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/my-new-feature
   ```
2. Ensure the project builds cleanly without errors:
   ```bash
   ./gradlew assembleDebug
   ```
3. Commit your changes with clear, concise commit messages.
4. Push your branch to your fork and submit a Pull Request referencing any open issue it resolves.
