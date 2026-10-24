package com.metrolauncher.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

/**
 * Coordinated tile theme definition for wallpaper-derived color modes.
 * Defines tile background, focused state, focus selector accent, and text colors.
 */
data class TileTheme(
    val id: String,
    val name: String,
    val tileBackgroundRgb: Int,
    val focusedBackgroundRgb: Int,
    val focusStrokeColor: Int,
    val focusGlowColor: Int,
    val primaryTextColor: Int = Color.WHITE,
    val secondaryTextColor: Int = Color.argb(204, 255, 255, 255)
)

/**
 * Manages local wallpaper color analysis, palette extraction, and theme caching.
 * Only runs extraction when the user explicitly chooses "Wallpaper Color" mode.
 * Never uploads images; caches extracted themes locally per wallpaper.
 */
object WallpaperThemeManager {

    private const val PREFS_NAME = "wallpaper_theme_prefs"
    private const val KEY_PREFIX_SAVED_THEME = "saved_theme_"
    private val paletteCache = mutableMapOf<String, List<TileTheme>>()
    private var previewTheme: TileTheme? = null

    fun getDefaultTheme(): TileTheme {
        return TileTheme(
            id = "default_acrylic",
            name = "Default Acrylic",
            tileBackgroundRgb = Color.rgb(9, 14, 26),
            focusedBackgroundRgb = Color.rgb(18, 26, 44),
            focusStrokeColor = Color.WHITE,
            focusGlowColor = Color.argb(45, 255, 255, 255),
            primaryTextColor = Color.WHITE,
            secondaryTextColor = Color.argb(204, 255, 255, 255)
        )
    }

    fun getActiveWallpaperKey(context: Context): String {
        val prefs = context.getSharedPreferences("custom_wallpaper_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("custom_wallpaper_path", null)
        if (path != null) {
            val f = File(path)
            if (f.exists() && f.length() > 0) {
                return "custom_${f.absolutePath}_${f.lastModified()}"
            }
        }
        return "default_bg_tv_wallpaper"
    }

    /**
     * Retrieves or generates 3-4 coordinated tile theme choices for the active wallpaper.
     * Caches palettes per wallpaper to prevent redundant extraction.
     */
    fun getOrGenerateThemes(context: Context): List<TileTheme> {
        val key = getActiveWallpaperKey(context)
        paletteCache[key]?.let { return it }

        val themes = extractThemesFromWallpaper(context)
        paletteCache[key] = themes
        return themes
    }

    fun getActiveTheme(context: Context): TileTheme {
        previewTheme?.let { return it }

        val key = getActiveWallpaperKey(context)
        val themes = getOrGenerateThemes(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_PREFIX_SAVED_THEME + key, null)

        return themes.firstOrNull { it.id == savedId } ?: themes.first()
    }

    fun setActiveTheme(context: Context, theme: TileTheme) {
        val key = getActiveWallpaperKey(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX_SAVED_THEME + key, theme.id)
            .apply()
        previewTheme = null
    }

    fun setPreviewTheme(theme: TileTheme) {
        previewTheme = theme
    }

    fun clearPreview() {
        previewTheme = null
    }

    /**
     * Called when the wallpaper changes while Wallpaper Color mode is already active.
     * Regenerates suggestions for the new wallpaper and restores its saved theme if available.
     */
    fun onWallpaperChanged(context: Context) {
        val key = getActiveWallpaperKey(context)
        val themes = getOrGenerateThemes(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_PREFIX_SAVED_THEME + key, null)
        if (savedId == null && themes.isNotEmpty()) {
            setActiveTheme(context, themes.first())
        }
    }

    private fun extractThemesFromWallpaper(context: Context): List<TileTheme> {
        val prefs = context.getSharedPreferences("custom_wallpaper_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("custom_wallpaper_path", null)

        val bitmap: Bitmap? = if (path != null && File(path).exists()) {
            CustomWallpaperManager.decodeSampledBitmap(path, 48, 48)
        } else {
            try {
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.bg_tv_wallpaper,
                    BitmapFactory.Options().apply { inSampleSize = 32 }
                )
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap == null) {
            return listOf(
                TileTheme("fallback_teal", "Dominant Teal", Color.rgb(8, 28, 34), Color.rgb(14, 42, 50), Color.WHITE, Color.argb(45, 0, 200, 220)),
                TileTheme("fallback_vibrant", "Vibrant Accent", Color.rgb(6, 20, 28), Color.rgb(10, 32, 44), Color.rgb(0, 210, 230), Color.argb(60, 0, 210, 230)),
                TileTheme("fallback_charcoal", "Neutral Charcoal", Color.rgb(18, 21, 25), Color.rgb(28, 33, 40), Color.WHITE, Color.argb(45, 255, 255, 255))
            )
        }

        return try {
            val hsv = FloatArray(3)
            val binCounts = IntArray(12)
            val binHueSums = FloatArray(12)
            val binSatSums = FloatArray(12)
            val binValSums = FloatArray(12)
            var totalValid = 0

            val width = bitmap.width
            val height = bitmap.height
            val stepX = (width / 16).coerceAtLeast(1)
            val stepY = (height / 16).coerceAtLeast(1)

            for (y in 0 until height step stepY) {
                for (x in 0 until width step stepX) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = (r * 299 + g * 587 + b * 114) / 1000
                    if (lum in 25..235) {
                        Color.colorToHSV(pixel, hsv)
                        val hue = hsv[0]
                        val sat = hsv[1]
                        val value = hsv[2]
                        if (sat >= 0.15f) {
                            val binIdx = ((hue / 30f).toInt()).coerceIn(0, 11)
                            binCounts[binIdx]++
                            binHueSums[binIdx] += hue
                            binSatSums[binIdx] += sat
                            binValSums[binIdx] += value
                            totalValid++
                        }
                    }
                }
            }

            if (totalValid < 5) {
                // Monochromatic or grayscale wallpaper -> 3 clean neutral graphite choices
                return listOf(
                    TileTheme("charcoal_deep", "Deep Slate", Color.rgb(14, 17, 22), Color.rgb(24, 29, 36), Color.WHITE, Color.argb(45, 255, 255, 255)),
                    TileTheme("charcoal_graphite", "Graphite", Color.rgb(20, 22, 26), Color.rgb(32, 35, 42), Color.WHITE, Color.argb(45, 255, 255, 255)),
                    TileTheme("charcoal_neutral", "Neutral Charcoal", Color.rgb(12, 14, 17), Color.rgb(20, 24, 28), Color.WHITE, Color.argb(45, 255, 255, 255))
                )
            }

            // Rank bins by pixel density
            val sortedBins = (0..11).sortedByDescending { binCounts[it] }
            val topBin1 = sortedBins[0]
            val h1 = binHueSums[topBin1] / binCounts[topBin1]
            val s1 = (binSatSums[topBin1] / binCounts[topBin1]).coerceIn(0.35f, 0.90f)
            val name1 = getHueName(h1)

            // Look for distinct secondary hue (distance >= 40 deg, count >= 15% of top bin)
            var topBin2: Int? = null
            for (i in 1..sortedBins.lastIndex) {
                val candidate = sortedBins[i]
                if (binCounts[candidate] < binCounts[topBin1] * 0.15) break
                val candHue = binHueSums[candidate] / binCounts[candidate]
                val diff = kotlin.math.abs(candHue - h1)
                val circularDiff = if (diff > 180f) 360f - diff else diff
                if (circularDiff >= 40f) {
                    topBin2 = candidate
                    break
                }
            }

            val list = mutableListOf<TileTheme>()

            // 1. Dominant Theme (Deep jewel tone)
            val bgDominant = Color.HSVToColor(floatArrayOf(h1, (s1 * 0.65f).coerceIn(0.28f, 0.52f), 0.13f))
            val focusedDominant = Color.HSVToColor(floatArrayOf(h1, (s1 * 0.65f).coerceIn(0.28f, 0.52f), 0.22f))
            val strokeDominant = Color.HSVToColor(floatArrayOf(h1, (s1 * 0.35f).coerceIn(0.12f, 0.32f), 1.0f))
            val glowDominant = Color.HSVToColor(50, floatArrayOf(h1, s1.coerceIn(0.5f, 1.0f), 1.0f))
            list.add(TileTheme("theme_dominant", "Dominant $name1", bgDominant, focusedDominant, strokeDominant, glowDominant))

            // 2. Vibrant Accent Theme
            val bgVibrant = Color.HSVToColor(floatArrayOf(h1, (s1 * 0.85f).coerceIn(0.38f, 0.70f), 0.09f))
            val focusedVibrant = Color.HSVToColor(floatArrayOf(h1, (s1 * 0.85f).coerceIn(0.38f, 0.70f), 0.17f))
            val strokeVibrant = Color.HSVToColor(floatArrayOf(h1, s1.coerceIn(0.65f, 1.0f), 1.0f))
            val glowVibrant = Color.HSVToColor(65, floatArrayOf(h1, s1.coerceIn(0.65f, 1.0f), 1.0f))
            list.add(TileTheme("theme_vibrant", "Vibrant $name1", bgVibrant, focusedVibrant, strokeVibrant, glowVibrant))

            // 3. Secondary / Harmony Theme (generated if distinct secondary hue exists)
            if (topBin2 != null) {
                val h2 = binHueSums[topBin2] / binCounts[topBin2]
                val s2 = (binSatSums[topBin2] / binCounts[topBin2]).coerceIn(0.35f, 0.90f)
                val name2 = getHueName(h2)

                val bgSecondary = Color.HSVToColor(floatArrayOf(h2, (s2 * 0.65f).coerceIn(0.28f, 0.52f), 0.13f))
                val focusedSecondary = Color.HSVToColor(floatArrayOf(h2, (s2 * 0.65f).coerceIn(0.28f, 0.52f), 0.22f))
                val strokeSecondary = Color.HSVToColor(floatArrayOf(h2, (s2 * 0.35f).coerceIn(0.12f, 0.32f), 1.0f))
                val glowSecondary = Color.HSVToColor(50, floatArrayOf(h2, s2.coerceIn(0.5f, 1.0f), 1.0f))
                list.add(TileTheme("theme_secondary", "Harmony $name2", bgSecondary, focusedSecondary, strokeSecondary, glowSecondary))
            }

            // 4. Neutral Charcoal Theme (Sophisticated graphite tinted with 6% wallpaper hue)
            val bgCharcoal = Color.HSVToColor(floatArrayOf(h1, 0.06f, 0.11f))
            val focusedCharcoal = Color.HSVToColor(floatArrayOf(h1, 0.06f, 0.20f))
            list.add(TileTheme("theme_charcoal", "Neutral Charcoal", bgCharcoal, focusedCharcoal, Color.WHITE, Color.argb(45, 255, 255, 255)))

            list
        } catch (e: Exception) {
            listOf(
                TileTheme("fallback_teal", "Dominant Teal", Color.rgb(8, 28, 34), Color.rgb(14, 42, 50), Color.WHITE, Color.argb(45, 0, 200, 220)),
                TileTheme("fallback_vibrant", "Vibrant Accent", Color.rgb(6, 20, 28), Color.rgb(10, 32, 44), Color.rgb(0, 210, 230), Color.argb(60, 0, 210, 230)),
                TileTheme("fallback_charcoal", "Neutral Charcoal", Color.rgb(18, 21, 25), Color.rgb(28, 33, 40), Color.WHITE, Color.argb(45, 255, 255, 255))
            )
        }
    }

    private fun getHueName(hue: Float): String {
        return when (hue) {
            in 0f..20f -> "Crimson"
            in 20f..45f -> "Amber"
            in 45f..70f -> "Gold"
            in 70f..150f -> "Emerald"
            in 150f..200f -> "Teal"
            in 200f..255f -> "Cobalt"
            in 255f..290f -> "Indigo"
            in 290f..330f -> "Plum"
            else -> "Rose"
        }
    }
}
