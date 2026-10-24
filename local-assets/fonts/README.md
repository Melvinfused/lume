# Local Fonts (Personal & Licensed Builds)

This directory is **strictly Git-ignored** and reserved for personal local builds only.

## The Authentic Metro Aesthetic

Lume's interface geometry, clock layout, tile margins, and status bar icons were specifically conceived, scaled, and spaced to match the metric proportions of **Segoe UI** and the glyph grid of **Segoe MDL2 Assets**. 

While the public open-source repository ships with **Inter** and **Material Symbols** out of the box to guarantee 100% legal distributability, **the launcher achieves its complete, intended 1:1 visual identity and retro-modern aesthetic** when built with genuine Segoe typography and MDL2 glyphs.

## Usage

If you possess a valid license for these fonts or wish to compile an authentic personal build for your own device, place your files here:

- `segoeui.ttf` (Segoe UI Regular)
- `segmdl2.ttf` (Segoe MDL2 Assets)

### How it Works

During the Gradle build process (`./gradlew assembleDebug` or `./gradlew assembleRelease`):
1. **When detected:** The automated `prepareLauncherAssets` task in `app/build.gradle.kts` will inject them directly into your personal build, unlocking the authentic typography.
2. **When absent:** The build seamlessly falls back to the open-source **Inter** and **Material Symbols** fallback font with zero build errors.

> **Legal Notice:** Do not commit, distribute, or submit pull requests containing proprietary font files. This directory is ignored by Git to protect project integrity.
