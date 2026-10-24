package com.metrolauncher.tv

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LauncherSettingsDialog(
    context: Context,
    private val onPerfModeChanged: (PerformanceMode) -> Unit,
    private val onNowPlayingToggled: (Boolean) -> Unit,
    private val onWeatherLocationChanged: (WeatherLocation) -> Unit,
    private val onTileAppearanceChanged: () -> Unit,
    private val onPickWallpaper: () -> Unit,
    private val onResetWallpaper: () -> Unit,
    private val onContinueWatchingToggled: (Boolean) -> Unit = {}
) : Dialog(context) {

    private lateinit var btnModePerf: TextView
    private lateinit var btnModeBalanced: TextView
    private lateinit var btnModeSmooth: TextView
    private lateinit var txtPerfDesc: TextView

    private lateinit var btnSelectorBreathing: TextView
    private lateinit var btnSelectorPulse: TextView
    private lateinit var btnSelectorStatic: TextView
    private lateinit var txtSelectorDesc: TextView

    private lateinit var btnTileColorDefault: TextView
    private lateinit var btnTileColorWallpaper: TextView
    private lateinit var containerWallpaperThemes: View
    private lateinit var rowWallpaperPalettes: LinearLayout
    private lateinit var btnTrans20: TextView
    private lateinit var btnTrans40: TextView
    private lateinit var btnTrans60: TextView
    private lateinit var btnTrans80: TextView
    private lateinit var btnTrans100: TextView

    private lateinit var btnToggleNowPlaying: View
    private lateinit var txtNpTitle: TextView
    private lateinit var txtNpDesc: TextView
    private lateinit var btnGrantNpPermission: View
    private lateinit var txtNpPermissionTitle: TextView
    private lateinit var txtNpPermissionDesc: TextView

    private lateinit var btnToggleContinueWatching: View
    private lateinit var txtCwTitle: TextView
    private lateinit var txtCwDesc: TextView

    private lateinit var txtWeatherDesc: TextView
    private lateinit var editWeatherCity: EditText
    private lateinit var btnSaveWeather: TextView

    private lateinit var chipAluva: TextView
    private lateinit var chipKochi: TextView
    private lateinit var chipLondon: TextView
    private lateinit var chipNyc: TextView
    private lateinit var chipTokyo: TextView

    private lateinit var btnWallpaper: TextView
    private lateinit var btnResetWallpaper: TextView
    private lateinit var btnSystemScreensaver: View
    private lateinit var btnClose: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_launcher_settings)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnModePerf = findViewById(R.id.btn_mode_perf)
        btnModeBalanced = findViewById(R.id.btn_mode_balanced)
        btnModeSmooth = findViewById(R.id.btn_mode_smooth)
        txtPerfDesc = findViewById(R.id.txt_perf_mode_desc)

        btnSelectorBreathing = findViewById(R.id.btn_selector_breathing)
        btnSelectorPulse = findViewById(R.id.btn_selector_pulse)
        btnSelectorStatic = findViewById(R.id.btn_selector_static)
        txtSelectorDesc = findViewById(R.id.txt_selector_desc)

        btnTileColorDefault = findViewById(R.id.btn_tile_color_default)
        btnTileColorWallpaper = findViewById(R.id.btn_tile_color_wallpaper)
        containerWallpaperThemes = findViewById(R.id.container_wallpaper_themes)
        rowWallpaperPalettes = findViewById(R.id.row_wallpaper_palettes)
        btnTrans20 = findViewById(R.id.btn_trans_20)
        btnTrans40 = findViewById(R.id.btn_trans_40)
        btnTrans60 = findViewById(R.id.btn_trans_60)
        btnTrans80 = findViewById(R.id.btn_trans_80)
        btnTrans100 = findViewById(R.id.btn_trans_100)

        btnToggleNowPlaying = findViewById(R.id.btn_toggle_now_playing)
        txtNpTitle = findViewById(R.id.txt_now_playing_title)
        txtNpDesc = findViewById(R.id.txt_now_playing_desc)
        btnGrantNpPermission = findViewById(R.id.btn_grant_np_permission)
        txtNpPermissionTitle = findViewById(R.id.txt_np_permission_title)
        txtNpPermissionDesc = findViewById(R.id.txt_np_permission_desc)

        btnToggleContinueWatching = findViewById(R.id.btn_toggle_continue_watching)
        txtCwTitle = findViewById(R.id.txt_continue_watching_title)
        txtCwDesc = findViewById(R.id.txt_continue_watching_desc)

        txtWeatherDesc = findViewById(R.id.txt_weather_city_desc)
        editWeatherCity = findViewById(R.id.edit_weather_location)
        btnSaveWeather = findViewById(R.id.btn_save_weather_location)

        chipAluva = findViewById(R.id.chip_aluva)
        chipKochi = findViewById(R.id.chip_kochi)
        chipLondon = findViewById(R.id.chip_london)
        chipNyc = findViewById(R.id.chip_nyc)
        chipTokyo = findViewById(R.id.chip_tokyo)

        btnWallpaper = findViewById(R.id.btn_config_wallpaper)
        btnResetWallpaper = findViewById(R.id.btn_config_reset_wallpaper)
        btnSystemScreensaver = findViewById(R.id.btn_config_system_screensaver)
        btnClose = findViewById(R.id.btn_config_close)

        val currentMode = LauncherConfig.getPerformanceMode(context)
        updatePerfModeDisplay(currentMode)
        updateSelectorAnimationDisplay(LauncherConfig.getSelectorAnimation(context))
        updateTileColorDisplay(LauncherConfig.getTileColorMode(context))
        updateTileTransparencyDisplay(LauncherConfig.getTileTransparency(context))
        updateNowPlayingDisplay(LauncherConfig.isHomeNowPlayingEnabled(context))
        updateContinueWatchingDisplay(LauncherConfig.isContinueWatchingEnabled(context))
        updateWeatherDisplay(LauncherConfig.getWeatherLocation(context))

        if (CustomWallpaperManager.hasCustomWallpaper(context)) {
            btnResetWallpaper.visibility = View.VISIBLE
        } else {
            btnResetWallpaper.visibility = View.GONE
        }

        // TV Remote focus highlight listener
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.bg_focus_border)
                v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(90).start()
            } else {
                v.setBackgroundResource(R.drawable.bg_header_button)
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
            }
        }

        val focusableItems = listOf(
            btnModePerf, btnModeBalanced, btnModeSmooth,
            btnSelectorBreathing, btnSelectorPulse, btnSelectorStatic,
            btnTileColorDefault, btnTileColorWallpaper,
            btnTrans20, btnTrans40, btnTrans60, btnTrans80, btnTrans100,
            btnToggleNowPlaying,
            editWeatherCity, btnSaveWeather,
            chipAluva, chipKochi, chipLondon, chipNyc, chipTokyo,
            btnWallpaper, btnResetWallpaper, btnSystemScreensaver, btnClose
        )

        focusableItems.forEach {
            it.onFocusChangeListener = focusListener
        }

        // Performance Mode selection
        btnModePerf.setOnClickListener { applyMode(PerformanceMode.PERFORMANCE) }
        btnModeBalanced.setOnClickListener { applyMode(PerformanceMode.BALANCED) }
        btnModeSmooth.setOnClickListener { applyMode(PerformanceMode.SMOOTH) }

        // Focus Selector Animation selection
        btnSelectorBreathing.setOnClickListener { applySelectorAnimation(SelectorAnimation.BREATHING_GLOW) }
        btnSelectorPulse.setOnClickListener { applySelectorAnimation(SelectorAnimation.PULSE) }
        btnSelectorStatic.setOnClickListener { applySelectorAnimation(SelectorAnimation.STATIC) }

        // Tile Color Mode
        btnTileColorDefault.setOnClickListener {
            LauncherConfig.setTileColorMode(context, TileColorMode.DEFAULT)
            WallpaperThemeManager.clearPreview()
            updateTileColorDisplay(TileColorMode.DEFAULT)
            onTileAppearanceChanged()
            Toast.makeText(context, "Tile Color: Default Acrylic", Toast.LENGTH_SHORT).show()
        }
        btnTileColorWallpaper.setOnClickListener {
            LauncherConfig.setTileColorMode(context, TileColorMode.WALLPAPER)
            updateTileColorDisplay(TileColorMode.WALLPAPER)
            onTileAppearanceChanged()
            Toast.makeText(context, "Tile Color: Wallpaper Color", Toast.LENGTH_SHORT).show()
        }

        // Transparency Options (20%, 40%, 60%, 80%, 100%)
        val transButtons = listOf(
            Pair(btnTrans20, 0.20f),
            Pair(btnTrans40, 0.40f),
            Pair(btnTrans60, 0.60f),
            Pair(btnTrans80, 0.80f),
            Pair(btnTrans100, 1.00f)
        )
        transButtons.forEach { (btn, value) ->
            btn.setOnClickListener {
                LauncherConfig.setTileTransparency(context, value)
                updateTileTransparencyDisplay(value)
                onTileAppearanceChanged()
                Toast.makeText(context, "Transparency: ${(value * 100).toInt()}%", Toast.LENGTH_SHORT).show()
            }
        }

        // Now Playing Toggle
        btnToggleNowPlaying.setOnClickListener {
            val nextState = !LauncherConfig.isHomeNowPlayingEnabled(context)
            LauncherConfig.setHomeNowPlayingEnabled(context, nextState)
            updateNowPlayingDisplay(nextState)
            onNowPlayingToggled(nextState)
            Toast.makeText(context, if (nextState) "Now Playing Tile: Enabled" else "Now Playing Tile: Disabled", Toast.LENGTH_SHORT).show()
        }

        // Continue Watching Toggle
        btnToggleContinueWatching.onFocusChangeListener = focusListener
        btnToggleContinueWatching.setOnClickListener {
            val nextState = !LauncherConfig.isContinueWatchingEnabled(context)
            LauncherConfig.setContinueWatchingEnabled(context, nextState)
            updateContinueWatchingDisplay(nextState)
            onContinueWatchingToggled(nextState)
            Toast.makeText(context, if (nextState) "Continue Watching: Enabled" else "Continue Watching: Disabled", Toast.LENGTH_SHORT).show()
        }

        // Notification Access Action
        btnGrantNpPermission.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Please open TV Settings > Apps > Special Access > Notification Access", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Weather Save/Apply Button
        btnSaveWeather.setOnClickListener {
            performCitySearch(editWeatherCity.text.toString().trim())
        }

        editWeatherCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performCitySearch(editWeatherCity.text.toString().trim())
                true
            } else {
                false
            }
        }

        // Weather Preset Chips
        chipAluva.setOnClickListener { setPresetLocation(WeatherLocation("Aluva, Kerala", 10.1076, 76.3516)) }
        chipKochi.setOnClickListener { setPresetLocation(WeatherLocation("Kochi, Kerala", 9.9312, 76.2673)) }
        chipLondon.setOnClickListener { setPresetLocation(WeatherLocation("London, UK", 51.5074, -0.1278)) }
        chipNyc.setOnClickListener { setPresetLocation(WeatherLocation("New York, USA", 40.7128, -74.0060)) }
        chipTokyo.setOnClickListener { setPresetLocation(WeatherLocation("Tokyo, Japan", 35.6762, 139.6503)) }

        // Wallpaper actions
        btnWallpaper.setOnClickListener {
            dismiss()
            onPickWallpaper()
        }

        btnResetWallpaper.setOnClickListener {
            dismiss()
            onResetWallpaper()
        }

        btnSystemScreensaver.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_DREAM_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_SETTINGS)
                        context.startActivity(intent)
                    } catch (e3: Exception) {
                        Toast.makeText(context, "Please open TV Settings > System > Screen Saver", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        btnClose.setOnClickListener {
            dismiss()
        }

        // Focus default mode button initially
        when (currentMode) {
            PerformanceMode.PERFORMANCE -> btnModePerf.requestFocus()
            PerformanceMode.BALANCED -> btnModeBalanced.requestFocus()
            PerformanceMode.SMOOTH -> btnModeSmooth.requestFocus()
        }
    }

    override fun onStart() {
        super.onStart()
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun dismiss() {
        WallpaperThemeManager.clearPreview()
        onTileAppearanceChanged()
        super.dismiss()
    }

    private fun applyMode(mode: PerformanceMode) {
        LauncherConfig.setPerformanceMode(context, mode)
        updatePerfModeDisplay(mode)
        onPerfModeChanged(mode)
        Toast.makeText(context, "Mode set to: ${mode.name}", Toast.LENGTH_SHORT).show()
    }

    private fun updatePerfModeDisplay(mode: PerformanceMode) {
        btnModePerf.text = if (mode == PerformanceMode.PERFORMANCE) "✓ Performance" else "Performance"
        btnModeBalanced.text = if (mode == PerformanceMode.BALANCED) "✓ Balanced" else "Balanced"
        btnModeSmooth.text = if (mode == PerformanceMode.SMOOTH) "✓ Smooth" else "Smooth"

        when (mode) {
            PerformanceMode.PERFORMANCE -> {
                txtPerfDesc.text = "Performance: All animations disabled for ultra-low latency and maximum FPS"
            }
            PerformanceMode.BALANCED -> {
                txtPerfDesc.text = "Balanced (Default): Standard lightweight TV transitions (240ms Turnstile, 280ms slide)"
            }
            PerformanceMode.SMOOTH -> {
                txtPerfDesc.text = "Smooth: Fluid polished motion with refined easing curves (300ms Turnstile, 350ms slide)"
            }
        }
    }

    private fun applySelectorAnimation(anim: SelectorAnimation) {
        LauncherConfig.setSelectorAnimation(context, anim)
        updateSelectorAnimationDisplay(anim)
        onTileAppearanceChanged()
        val name = when (anim) {
            SelectorAnimation.BREATHING_GLOW -> "Breathing Glow"
            SelectorAnimation.PULSE -> "Pulse"
            SelectorAnimation.STATIC -> "Static"
        }
        Toast.makeText(context, "Selector Animation: $name", Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectorAnimationDisplay(anim: SelectorAnimation) {
        btnSelectorBreathing.text = if (anim == SelectorAnimation.BREATHING_GLOW) "✓ Breathing Glow" else "Breathing Glow"
        btnSelectorPulse.text = if (anim == SelectorAnimation.PULSE) "✓ Pulse" else "Pulse"
        btnSelectorStatic.text = if (anim == SelectorAnimation.STATIC) "✓ Static" else "Static"

        when (anim) {
            SelectorAnimation.BREATHING_GLOW -> {
                txtSelectorDesc.text = "Breathing Glow: Gentle radiant glow and sinusoidal border pulsation"
            }
            SelectorAnimation.PULSE -> {
                txtSelectorDesc.text = "Pulse: Distinctive rhythmic brightness pulse inspired by terminal cursor"
            }
            SelectorAnimation.STATIC -> {
                txtSelectorDesc.text = "Static: Solid classic Metro focus border with zero animation"
            }
        }
    }

    private fun updateTileColorDisplay(mode: TileColorMode) {
        btnTileColorDefault.text = if (mode == TileColorMode.DEFAULT) "✓ Default Acrylic" else "Default Acrylic"
        btnTileColorWallpaper.text = if (mode == TileColorMode.WALLPAPER) "✓ Wallpaper Color" else "Wallpaper Color"

        if (mode == TileColorMode.WALLPAPER) {
            containerWallpaperThemes.visibility = View.VISIBLE
            populateWallpaperPalettes()
        } else {
            containerWallpaperThemes.visibility = View.GONE
            WallpaperThemeManager.clearPreview()
        }
    }

    private fun populateWallpaperPalettes() {
        rowWallpaperPalettes.removeAllViews()
        val themes = WallpaperThemeManager.getOrGenerateThemes(context)
        val activeTheme = WallpaperThemeManager.getActiveTheme(context)
        val inflater = layoutInflater

        themes.forEachIndexed { index, theme ->
            val card = inflater.inflate(R.layout.item_wallpaper_palette_preview, rowWallpaperPalettes, false)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
            if (index < themes.size - 1) {
                lp.marginEnd = (context.resources.displayMetrics.density * 6).toInt()
            }
            card.layoutParams = lp

            val swatch = card.findViewById<View>(R.id.palette_swatch)
            val txtName = card.findViewById<TextView>(R.id.txt_palette_name)
            val txtStatus = card.findViewById<TextView>(R.id.txt_palette_status)

            val swatchDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.resources.displayMetrics.density * 3
                setColor(theme.tileBackgroundRgb)
                setStroke((context.resources.displayMetrics.density * 2.5f).toInt(), theme.focusStrokeColor)
            }
            swatch.background = swatchDrawable

            txtName.text = theme.name
            val isSelected = (theme.id == activeTheme.id)
            if (isSelected) {
                txtStatus.text = "✓ Selected"
                txtStatus.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                txtStatus.text = "Enter to apply"
                txtStatus.setTextColor(Color.argb(153, 255, 255, 255))
            }

            card.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.setBackgroundResource(R.drawable.bg_focus_border)
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(90).start()
                    WallpaperThemeManager.setPreviewTheme(theme)
                    onTileAppearanceChanged()
                } else {
                    v.setBackgroundResource(R.drawable.bg_header_button)
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
                }
            }

            card.setOnClickListener {
                WallpaperThemeManager.setActiveTheme(context, theme)
                onTileAppearanceChanged()
                populateWallpaperPalettes()
                Toast.makeText(context, "Applied theme: ${theme.name}", Toast.LENGTH_SHORT).show()
            }

            rowWallpaperPalettes.addView(card)
        }
    }

    private fun updateTileTransparencyDisplay(transparency: Float) {
        btnTrans20.text = if (Math.abs(transparency - 0.20f) < 0.05f) "✓ 20%" else "20%"
        btnTrans40.text = if (Math.abs(transparency - 0.40f) < 0.05f) "✓ 40%" else "40%"
        btnTrans60.text = if (Math.abs(transparency - 0.60f) < 0.05f) "✓ 60%" else "60%"
        btnTrans80.text = if (Math.abs(transparency - 0.80f) < 0.05f) "✓ 80%" else "80%"
        btnTrans100.text = if (Math.abs(transparency - 1.00f) < 0.05f) "✓ 100%" else "100%"
    }

    private fun updateNowPlayingDisplay(enabled: Boolean) {
        if (enabled) {
            txtNpTitle.text = "Now Playing Tile: Enabled"
            txtNpDesc.text = "Displays active music track & artwork in the top tile row. Click to toggle."
        } else {
            txtNpTitle.text = "Now Playing Tile: Disabled"
            txtNpDesc.text = "Completely hidden and layout space reclaimed. Click to toggle."
        }
        updateNpPermissionDisplay()
    }

    private fun updateContinueWatchingDisplay(enabled: Boolean) {
        if (enabled) {
            txtCwTitle.text = "Continue Watching: Enabled"
            txtCwTitle.setTextColor(Color.WHITE)
            txtCwDesc.text = "Displays recent movies & shows with resume thumbnails below apps. Click to toggle."
        } else {
            txtCwTitle.text = "Continue Watching: Disabled"
            txtCwTitle.setTextColor(Color.parseColor("#FF6B68"))
            txtCwDesc.text = "Row is completely hidden. Click to toggle."
        }
    }

    private fun updateNpPermissionDisplay() {
        val granted = NowPlayingManager.isNotificationAccessGranted(context)
        if (granted) {
            txtNpPermissionTitle.text = "Notification Access: Granted ✓"
            txtNpPermissionTitle.setTextColor(Color.parseColor("#4CAF50"))
            txtNpPermissionDesc.text = "Metro TV has permission to detect Spotify and music media sessions."
        } else {
            txtNpPermissionTitle.text = "Notification Access: Required for Spotify"
            txtNpPermissionTitle.setTextColor(Color.parseColor("#FFCC00"))
            txtNpPermissionDesc.text = "Click to open Android Settings and grant Notification Access to Metro TV."
        }
    }

    private fun updateWeatherDisplay(location: WeatherLocation) {
        txtWeatherDesc.text = "Current: ${location.name} (${String.format("%.2f", location.latitude)}°, ${String.format("%.2f", location.longitude)}°)"
    }

    private fun setPresetLocation(location: WeatherLocation) {
        LauncherConfig.setWeatherLocation(context, location)
        updateWeatherDisplay(location)
        onWeatherLocationChanged(location)
        Toast.makeText(context, "Weather set to ${location.name}", Toast.LENGTH_SHORT).show()
    }

    private fun performCitySearch(query: String) {
        if (query.isEmpty()) {
            Toast.makeText(context, "Please enter a city name", Toast.LENGTH_SHORT).show()
            return
        }

        btnSaveWeather.text = "Searching..."
        btnSaveWeather.isEnabled = false

        Thread {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val jsonStr = reader.readText()
                    reader.close()

                    val json = JSONObject(jsonStr)
                    if (json.has("results")) {
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            val first = results.getJSONObject(0)
                            val name = first.getString("name")
                            val country = if (first.has("country")) first.getString("country") else ""
                            val lat = first.getDouble("latitude")
                            val lon = first.getDouble("longitude")

                            val displayName = if (country.isNotEmpty()) "$name, $country" else name
                            val location = WeatherLocation(displayName, lat, lon)

                            btnSaveWeather.post {
                                LauncherConfig.setWeatherLocation(context, location)
                                updateWeatherDisplay(location)
                                editWeatherCity.setText("")
                                btnSaveWeather.text = "Save"
                                btnSaveWeather.isEnabled = true
                                onWeatherLocationChanged(location)
                                Toast.makeText(context, "Location set to $displayName", Toast.LENGTH_SHORT).show()
                            }
                            return@Thread
                        }
                    }
                }

                btnSaveWeather.post {
                    btnSaveWeather.text = "Save"
                    btnSaveWeather.isEnabled = true
                    Toast.makeText(context, "City \"$query\" not found. Please try another.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                btnSaveWeather.post {
                    btnSaveWeather.text = "Save"
                    btnSaveWeather.isEnabled = true
                    Toast.makeText(context, "Network error searching city.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
