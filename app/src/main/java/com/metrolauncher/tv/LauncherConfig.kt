package com.metrolauncher.tv

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

enum class PerformanceMode {
    PERFORMANCE, // All nonessential animations disabled, maximum speed
    BALANCED,    // Standard smooth & lightweight TV transitions (Default)
    SMOOTH       // Fluid polished animations with refined easing
}

enum class SelectorAnimation {
    BREATHING_GLOW, // Calm sinusoidal breathing animation with soft radiant glow (Default)
    PULSE,          // Distinctive rhythmic brightness pulse inspired by terminal cursor
    STATIC          // Clean solid focus border with no animation
}

enum class TileColorMode {
    DEFAULT,   // Dark acrylic tint (#090E1A)
    WALLPAPER  // Adaptive based on background wallpaper colors
}

data class WeatherLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Centralized configuration repository for Launcher Performance Modes,
 * Focus Selector Animation, Home-screen Now Playing visibility, Tile colors/transparency,
 * Weather Location, and Pinned Top Row Apps.
 */
object LauncherConfig {

    private const val PREFS_NAME = "metro_launcher_config"
    private const val KEY_PERF_MODE = "perf_mode"
    private const val KEY_SELECTOR_ANIMATION = "selector_animation"
    private const val KEY_HOME_NOW_PLAYING = "home_now_playing_enabled"
    private const val KEY_WEATHER_CITY = "weather_city"
    private const val KEY_WEATHER_LAT = "weather_lat"
    private const val KEY_WEATHER_LON = "weather_lon"
    private const val KEY_TILE_COLOR_MODE = "tile_color_mode"
    private const val KEY_TILE_TRANSPARENCY = "tile_transparency"
    private const val KEY_PINNED_TOP_ROW = "pinned_top_row_apps"
    private const val KEY_CONTINUE_WATCHING = "continue_watching_enabled"

    val TRANSPARENCY_OPTIONS = listOf(0.20f, 0.40f, 0.60f, 0.80f, 1.00f)

    val PRESET_LOCATIONS = listOf(
        WeatherLocation("Aluva, Kerala", 10.1076, 76.3516),
        WeatherLocation("Kochi, Kerala", 9.9312, 76.2673),
        WeatherLocation("Trivandrum, Kerala", 8.5241, 76.9366),
        WeatherLocation("Bengaluru, India", 12.9716, 77.5946),
        WeatherLocation("Mumbai, India", 19.0760, 72.8777),
        WeatherLocation("New Delhi, India", 28.6139, 77.2090),
        WeatherLocation("London, UK", 51.5074, -0.1278),
        WeatherLocation("New York, USA", 40.7128, -74.0060),
        WeatherLocation("Tokyo, Japan", 35.6762, 139.6503),
        WeatherLocation("Dubai, UAE", 25.2048, 55.2708)
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPerformanceMode(context: Context): PerformanceMode {
        val name = prefs(context).getString(KEY_PERF_MODE, PerformanceMode.BALANCED.name)
        return try {
            PerformanceMode.valueOf(name ?: PerformanceMode.BALANCED.name)
        } catch (e: Exception) {
            PerformanceMode.BALANCED
        }
    }

    fun setPerformanceMode(context: Context, mode: PerformanceMode) {
        prefs(context).edit().putString(KEY_PERF_MODE, mode.name).apply()
    }

    fun getSelectorAnimation(context: Context): SelectorAnimation {
        val name = prefs(context).getString(KEY_SELECTOR_ANIMATION, SelectorAnimation.BREATHING_GLOW.name)
        return try {
            SelectorAnimation.valueOf(name ?: SelectorAnimation.BREATHING_GLOW.name)
        } catch (e: Exception) {
            SelectorAnimation.BREATHING_GLOW
        }
    }

    fun setSelectorAnimation(context: Context, anim: SelectorAnimation) {
        prefs(context).edit().putString(KEY_SELECTOR_ANIMATION, anim.name).apply()
    }

    fun isHomeNowPlayingEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_HOME_NOW_PLAYING, true)
    }

    fun setHomeNowPlayingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HOME_NOW_PLAYING, enabled).apply()
    }

    fun isContinueWatchingEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_CONTINUE_WATCHING, true)
    }

    fun setContinueWatchingEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONTINUE_WATCHING, enabled).apply()
    }

    fun getWeatherLocation(context: Context): WeatherLocation {
        val sp = prefs(context)
        val name = sp.getString(KEY_WEATHER_CITY, "Aluva, Kerala") ?: "Aluva, Kerala"
        val lat = sp.getFloat(KEY_WEATHER_LAT, 10.1076f).toDouble()
        val lon = sp.getFloat(KEY_WEATHER_LON, 76.3516f).toDouble()
        return WeatherLocation(name, lat, lon)
    }

    fun setWeatherLocation(context: Context, location: WeatherLocation) {
        prefs(context).edit()
            .putString(KEY_WEATHER_CITY, location.name)
            .putFloat(KEY_WEATHER_LAT, location.latitude.toFloat())
            .putFloat(KEY_WEATHER_LON, location.longitude.toFloat())
            .apply()
    }

    // Tile Color Mode & Transparency
    fun getTileColorMode(context: Context): TileColorMode {
        val name = prefs(context).getString(KEY_TILE_COLOR_MODE, TileColorMode.DEFAULT.name)
        return try {
            TileColorMode.valueOf(name ?: TileColorMode.DEFAULT.name)
        } catch (e: Exception) {
            TileColorMode.DEFAULT
        }
    }

    fun setTileColorMode(context: Context, mode: TileColorMode) {
        prefs(context).edit().putString(KEY_TILE_COLOR_MODE, mode.name).apply()
    }

    fun getTileTransparency(context: Context): Float {
        return prefs(context).getFloat(KEY_TILE_TRANSPARENCY, 0.40f)
    }

    fun setTileTransparency(context: Context, transparency: Float) {
        prefs(context).edit().putFloat(KEY_TILE_TRANSPARENCY, transparency).apply()
    }

    fun getActiveTileTheme(context: Context): TileTheme {
        val mode = getTileColorMode(context)
        return if (mode == TileColorMode.WALLPAPER) {
            WallpaperThemeManager.getActiveTheme(context)
        } else {
            WallpaperThemeManager.getDefaultTheme()
        }
    }

    fun getTileBackgroundColor(context: Context): Int {
        val theme = getActiveTileTheme(context)
        val alphaRatio = getTileTransparency(context)
        val alpha = (255 * alphaRatio).toInt().coerceIn(0, 255)
        val baseRgb = theme.tileBackgroundRgb

        return Color.argb(
            alpha,
            Color.red(baseRgb),
            Color.green(baseRgb),
            Color.blue(baseRgb)
        )
    }

    fun getTilePrimaryTextColor(context: Context): Int {
        return getActiveTileTheme(context).primaryTextColor
    }

    fun getTileSecondaryTextColor(context: Context): Int {
        return getActiveTileTheme(context).secondaryTextColor
    }

    fun createTileBackgroundDrawable(context: Context): android.graphics.drawable.Drawable {
        val theme = getActiveTileTheme(context)
        val alphaRatio = getTileTransparency(context)
        val alpha = (255 * alphaRatio).toInt().coerceIn(0, 255)

        val normalColor = Color.argb(
            alpha,
            Color.red(theme.tileBackgroundRgb),
            Color.green(theme.tileBackgroundRgb),
            Color.blue(theme.tileBackgroundRgb)
        )

        val focusedColor = Color.argb(
            (alpha * 1.18f).toInt().coerceIn(0, 255),
            Color.red(theme.focusedBackgroundRgb),
            Color.green(theme.focusedBackgroundRgb),
            Color.blue(theme.focusedBackgroundRgb)
        )

        return MetroFocusBorderDrawable(
            context = context,
            fillColor = normalColor,
            focusedFillColor = focusedColor,
            strokeColor = theme.focusStrokeColor,
            glowColor = theme.focusGlowColor,
            cornerRadiusDp = 2f,
            strokeWidthDp = 3f
        )
    }

    // Pinned Top Row Apps
    fun getPinnedTopRowApps(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_PINNED_TOP_ROW, emptySet()) ?: emptySet()
    }

    fun pinAppToTopRow(context: Context, packageName: String) {
        val current = getPinnedTopRowApps(context).toMutableSet()
        current.add(packageName)
        prefs(context).edit().putStringSet(KEY_PINNED_TOP_ROW, current).apply()
    }

    fun unpinAppFromTopRow(context: Context, packageName: String) {
        val current = getPinnedTopRowApps(context).toMutableSet()
        current.remove(packageName)
        prefs(context).edit().putStringSet(KEY_PINNED_TOP_ROW, current).apply()
    }

    // Animation timing helpers based on selected performance mode
    fun getAmbientSlideDuration(context: Context): Long {
        return when (getPerformanceMode(context)) {
            PerformanceMode.PERFORMANCE -> 0L
            PerformanceMode.BALANCED -> 280L
            PerformanceMode.SMOOTH -> 350L
        }
    }

    fun getTurnstileDuration(context: Context): Long {
        return when (getPerformanceMode(context)) {
            PerformanceMode.PERFORMANCE -> 0L
            PerformanceMode.BALANCED -> 240L
            PerformanceMode.SMOOTH -> 300L
        }
    }

    fun getWidgetFlipDuration(context: Context): Long {
        return when (getPerformanceMode(context)) {
            PerformanceMode.PERFORMANCE -> 0L
            PerformanceMode.BALANCED -> 280L
            PerformanceMode.SMOOTH -> 340L
        }
    }

    fun getWidgetSlideDuration(context: Context): Long {
        return when (getPerformanceMode(context)) {
            PerformanceMode.PERFORMANCE -> 0L
            PerformanceMode.BALANCED -> 260L
            PerformanceMode.SMOOTH -> 320L
        }
    }

    fun isAnimationEnabled(context: Context): Boolean {
        return getPerformanceMode(context) != PerformanceMode.PERFORMANCE
    }
}
