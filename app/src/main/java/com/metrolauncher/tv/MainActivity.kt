package com.metrolauncher.tv

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.HorizontalGridView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var btnWifi: TextView
    private lateinit var btnSettings: TextView
    private lateinit var gridRow1: HorizontalGridView
    private lateinit var gridRow2: HorizontalGridView

    // Ambient Screensaver views
    private lateinit var ambientScreensaverLayer: FrameLayout
    private lateinit var imgAmbientWallpaper: ImageView
    private lateinit var tvAmbientTitle: TextView
    private lateinit var tvAmbientCopyright: TextView
    private lateinit var tvAmbientTime: TextView
    private lateinit var tvAmbientDate: TextView
    private var isAmbientScreensaverActive = false

    private val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes
    private val inactivityHandler = Handler(Looper.getMainLooper())
    private val inactivityRunnable = Runnable {
        showAmbientScreensaver()
    }

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClockAndGreeting()
            updateAmbientClock()
            clockHandler.postDelayed(this, 10000)
        }
    }

    private val weatherHandler = Handler(Looper.getMainLooper())
    private val weatherRunnable = object : Runnable {
        override fun run() {
            fetchAluvaWeather()
            // Refresh weather every 15 minutes
            weatherHandler.postDelayed(this, 15 * 60 * 1000)
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateWifiStatus()
            if (isNetworkConnected()) {
                AmbientWallpaperManager.syncWallpapers(this@MainActivity) {
                    if (isAmbientScreensaverActive) {
                        loadActiveWallpaperIntoAmbient()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvGreeting = findViewById(R.id.tv_greeting)
        tvWeather = findViewById(R.id.tv_weather)
        tvDateTime = findViewById(R.id.tv_datetime)
        btnWifi = findViewById(R.id.btn_wifi)
        btnSettings = findViewById(R.id.btn_settings)
        gridRow1 = findViewById(R.id.grid_row_1)
        gridRow2 = findViewById(R.id.grid_row_2)

        // Ambient screensaver views
        ambientScreensaverLayer = findViewById(R.id.ambient_screensaver_layer)
        imgAmbientWallpaper = findViewById(R.id.img_ambient_wallpaper)
        tvAmbientTitle = findViewById(R.id.tv_ambient_title)
        tvAmbientCopyright = findViewById(R.id.tv_ambient_copyright)
        tvAmbientTime = findViewById(R.id.tv_ambient_time)
        tvAmbientDate = findViewById(R.id.tv_ambient_date)

        setupGridViews()
        setupHeaderButtons()
        loadInstalledApps()
        updateClockAndGreeting()
        updateWifiStatus()
        fetchAluvaWeather()

        // Sync 7-day Bing wallpapers locally
        AmbientWallpaperManager.syncWallpapers(this)

        @Suppress("DEPRECATION")
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        if (intent?.getBooleanExtra("show_ambient", false) == true) {
            showAmbientScreensaver()
        }

        resetInactivityTimer()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.getBooleanExtra("show_ambient", false) == true) {
            showAmbientScreensaver()
        }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
        weatherHandler.post(weatherRunnable)
        updateWifiStatus()
        resetInactivityTimer()
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
        weatherHandler.removeCallbacks(weatherRunnable)
        inactivityHandler.removeCallbacks(inactivityRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    /**
     * Resets the 60-second inactivity countdown timer for the ambient screensaver.
     */
    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        if (!isAmbientScreensaverActive) {
            inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS)
        }
    }

    /**
     * Displays the Windows 10 lock-screen aesthetic screensaver.
     */
    fun showAmbientScreensaver() {
        if (isFinishing || isDestroyed) return
        isAmbientScreensaverActive = true

        updateAmbientClock()
        loadActiveWallpaperIntoAmbient()

        ambientScreensaverLayer.alpha = 0f
        ambientScreensaverLayer.visibility = View.VISIBLE
        ambientScreensaverLayer.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
    }

    /**
     * Dismisses the ambient screensaver with a smooth fade-out and returns to the home screen.
     */
    fun hideAmbientScreensaver() {
        if (!isAmbientScreensaverActive) return
        isAmbientScreensaverActive = false

        ambientScreensaverLayer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                ambientScreensaverLayer.visibility = View.GONE
                gridRow1.requestFocus()
            }
            .start()

        resetInactivityTimer()
    }

    private fun loadActiveWallpaperIntoAmbient() {
        val wallpaperPair = AmbientWallpaperManager.resolveActiveWallpaper(this)
        if (wallpaperPair != null) {
            val entry = wallpaperPair.first
            val bitmap = wallpaperPair.second
            imgAmbientWallpaper.setImageBitmap(bitmap)
            tvAmbientTitle.text = entry.title
            tvAmbientCopyright.text = if (entry.copyright.isNotEmpty()) entry.copyright else "Bing Daily Wallpaper"
        } else {
            imgAmbientWallpaper.setImageResource(R.drawable.bg_tv_wallpaper)
            tvAmbientTitle.text = "Windows 10 Ambient"
            tvAmbientCopyright.text = "Metro TV Launcher"
        }
    }

    private fun updateAmbientClock() {
        val calendar = Calendar.getInstance()
        // 12-hour clock with AM/PM (e.g., 1:45 PM)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        tvAmbientTime.text = timeFormat.format(calendar.time)

        // Readable date format (e.g., Wednesday, 21 October)
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.US)
        tvAmbientDate.text = dateFormat.format(calendar.time)
    }

    /**
     * Configure HorizontalGridViews for smooth TV scrolling.
     */
    private fun setupGridViews() {
        val offsetPx = (64 * resources.displayMetrics.density).toInt()

        gridRow1.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        gridRow1.windowAlignmentOffset = offsetPx
        gridRow1.windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        gridRow1.itemAlignmentOffsetPercent = 0f

        gridRow2.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        gridRow2.windowAlignmentOffset = offsetPx
        gridRow2.windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        gridRow2.itemAlignmentOffsetPercent = 0f
    }

    /**
     * Checks network connection and dynamically updates the Wi-Fi icon:
     * - Connected: \uE701 (Wi-Fi connected from segmdl2)
     * - Disconnected: \uEB57 (Wi-Fi not connected from segmdl2)
     */
    private fun updateWifiStatus() {
        val connected = isNetworkConnected()
        btnWifi.text = if (connected) "\uE701" else "\uEB57"
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Configures the raw Wi-Fi and Settings symbols:
     * - No background box
     * - Lights up with bright white glow and scale when selected
     */
    private fun setupHeaderButtons() {
        val rawGlyphFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            val tv = v as? TextView
            if (hasFocus) {
                tv?.setTextColor(Color.WHITE)
                tv?.setShadowLayer(16f, 0f, 0f, Color.WHITE)
                v.animate().scaleX(1.22f).scaleY(1.22f).translationZ(6f).setDuration(120).start()
            } else {
                tv?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white_80))
                tv?.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(120).start()
            }
        }

        btnWifi.onFocusChangeListener = rawGlyphFocusListener
        btnSettings.onFocusChangeListener = rawGlyphFocusListener

        btnWifi.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /**
     * Dynamically queries all device applications and separates them:
     * - Top Row: Primary media apps (YouTube, Prime Video, Netflix, Music app on device)
     * - Bottom Row: All other installed apps on the TV
     */
    private fun loadInstalledApps() {
        val pm = packageManager

        val tvIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val tvApps = pm.queryIntentActivities(tvIntent, 0)
        val standardApps = pm.queryIntentActivities(standardIntent, 0)

        val allResolveInfos = mutableListOf<ResolveInfo>()
        allResolveInfos.addAll(tvApps)
        allResolveInfos.addAll(standardApps)

        val seenPackages = mutableSetOf<String>()
        seenPackages.add(packageName)

        val allApps = mutableListOf<AppItem>()

        for (resolveInfo in allResolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (!seenPackages.contains(pkg)) {
                seenPackages.add(pkg)
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val color = AppItem.resolveSolidColor(pkg)
                val brandLogo = AppItem.resolveBrandLogo(pkg)

                allApps.add(
                    AppItem(
                        packageName = pkg,
                        activityName = resolveInfo.activityInfo.name,
                        label = label,
                        systemIcon = icon,
                        brandLogoResId = brandLogo,
                        solidColor = color
                    )
                )
            }
        }

        // Top Row: YouTube, Prime Video, Netflix, Music app
        val topRowApps = mutableListOf<AppItem>()
        val bottomRowApps = mutableListOf<AppItem>()

        val ytApp = allApps.firstOrNull { it.packageName.lowercase().contains("youtube") }
        val primeApp = allApps.firstOrNull {
            val p = it.packageName.lowercase()
            p.contains("amazon") || p.contains("prime")
        }
        val netflixApp = allApps.firstOrNull { it.packageName.lowercase().contains("netflix") }
        val musicApp = allApps.firstOrNull {
            val p = it.packageName.lowercase()
            p.contains("auramusic") || p.contains("music") || p.contains("tunein") || p.contains("spotify")
        }

        if (ytApp != null) topRowApps.add(ytApp)
        if (primeApp != null) topRowApps.add(primeApp)
        if (netflixApp != null) topRowApps.add(netflixApp)
        if (musicApp != null) topRowApps.add(musicApp)

        // All remaining apps go into Bottom Row
        for (app in allApps) {
            if (!topRowApps.any { it.packageName == app.packageName }) {
                bottomRowApps.add(app)
            }
        }

        if (topRowApps.size < 4 && bottomRowApps.isNotEmpty()) {
            val needed = 4 - topRowApps.size
            val fill = bottomRowApps.take(needed)
            topRowApps.addAll(fill)
            bottomRowApps.removeAll(fill)
        }

        gridRow1.adapter = AppsAdapter(
            apps = topRowApps,
            isTopRow = true,
            onNavigateToHeader = { position ->
                if (position <= 1) {
                    btnWifi.requestFocus()
                } else {
                    btnSettings.requestFocus()
                }
            },
            onItemClick = { app -> launchApp(app) }
        )

        gridRow2.adapter = AppsAdapter(
            apps = bottomRowApps,
            isTopRow = false,
            onNavigateToHeader = null,
            onItemClick = { app -> launchApp(app) }
        )

        gridRow1.post {
            gridRow1.requestFocus()
        }
    }

    private fun launchApp(app: AppItem) {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent != null) {
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Updates greeting dynamically based on current time of the day:
     * - 05:00 - 11:59 -> Good morning
     * - 12:00 - 16:59 -> Good afternoon
     * - 17:00 - 20:59 -> Good evening
     * - 21:00 - 04:59 -> Good night
     */
    private fun updateClockAndGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        tvGreeting.text = when (hour) {
            in 5..11 -> getString(R.string.greeting_morning)
            in 12..16 -> getString(R.string.greeting_afternoon)
            in 17..20 -> getString(R.string.greeting_evening)
            else -> getString(R.string.greeting_night)
        }

        val timeFormat = SimpleDateFormat("h:mm a | EEEE, MMM d", Locale.US)
        tvDateTime.text = timeFormat.format(calendar.time)
    }

    /**
     * Fetches real-time weather in Aluva, Kerala (Lat 10.1076, Lon 76.3516).
     */
    private fun fetchAluvaWeather() {
        Thread {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=10.1076&longitude=76.3516&current_weather=true")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val current = json.getJSONObject("current_weather")
                    val temp = current.getDouble("temperature")
                    val code = current.getInt("weathercode")

                    val condition = getWeatherCondition(code)
                    val weatherDisplay = "${condition.first} ${Math.round(temp)}°C • ${condition.second} • Aluva, Kerala"

                    runOnUiThread {
                        tvWeather.text = weatherDisplay
                    }
                }
            } catch (e: Exception) {
                // Silently fallback
            }
        }.start()
    }

    private fun getWeatherCondition(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("☀️", "Clear Sky")
            1 -> Pair("🌤️", "Mainly Clear")
            2 -> Pair("⛅", "Partly Cloudy")
            3 -> Pair("☁️", "Overcast")
            45, 48 -> Pair("🌫️", "Foggy")
            51, 53, 55 -> Pair("🌦️", "Drizzle")
            56, 57 -> Pair("🌨️", "Freezing Drizzle")
            61, 63, 65 -> Pair("🌧️", "Rain")
            66, 67 -> Pair("🌨️", "Freezing Rain")
            71, 73, 75, 77 -> Pair("❄️", "Snow")
            80, 81, 82 -> Pair("🌧️", "Rain Showers")
            85, 86 -> Pair("🌨️", "Snow Showers")
            95, 96, 99 -> Pair("⛈️", "Thunderstorm")
            else -> Pair("⛅", "Partly Cloudy")
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Any user interaction resets the ambient screensaver timer
        resetInactivityTimer()

        // If ambient screensaver is active, any remote key press dismisses it immediately
        if (isAmbientScreensaverActive) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                hideAmbientScreensaver()
            }
            return true
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (gridRow1.hasFocus()) {
                        val pos = gridRow1.selectedPosition
                        if (pos <= 1) {
                            btnWifi.requestFocus()
                        } else {
                            btnSettings.requestFocus()
                        }
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (btnWifi.hasFocus() || btnSettings.hasFocus()) {
                        gridRow1.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (btnWifi.hasFocus()) {
                        btnSettings.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (btnSettings.hasFocus()) {
                        btnWifi.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (btnWifi.hasFocus()) {
                        btnWifi.performClick()
                        return true
                    } else if (btnSettings.hasFocus()) {
                        btnSettings.performClick()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isAmbientScreensaverActive) {
                hideAmbientScreensaver()
                return true
            }
            if (!gridRow1.hasFocus()) {
                gridRow1.requestFocus()
                return true
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
