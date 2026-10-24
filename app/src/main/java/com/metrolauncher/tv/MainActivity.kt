package com.metrolauncher.tv

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
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

    private lateinit var imgWallpaper: ImageView
    private lateinit var wallpaperDimOverlay: View
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var mainContentLayer: LinearLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var headerGradientScrim: View
    private lateinit var bottomGradientScrim: View
    private lateinit var focusSelectorOverlay: FocusSelectorOverlayView
    private lateinit var tvGreeting: TextView
    private lateinit var btnWifi: TextView
    private lateinit var btnAmbient: TextView
    private lateinit var btnSettings: TextView
    private lateinit var gridRow1: HorizontalGridView
    private lateinit var gridRow2: HorizontalGridView
    // 3 Metro Information Tiles (Row above App Grid)
    private lateinit var rowWidgetTiles: LinearLayout
    private lateinit var tileClock: FrameLayout
    private lateinit var tvClockTime: TextView
    private lateinit var tvClockWeekday: TextView
    private lateinit var tvClockDate: TextView

    private lateinit var tileWeather: FrameLayout
    private lateinit var weatherFront: RelativeLayout
    private lateinit var weatherBack: LinearLayout
    private lateinit var tvWeatherTemp: TextView
    private lateinit var imgWeatherIcon: ImageView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvWeatherLocation: TextView
    private lateinit var tvForecastLine1: TextView
    private lateinit var tvForecastLine2: TextView
    private lateinit var tvForecastLine3: TextView
    private var isWeatherFlipped = false
    private var weatherFlipAnimator: AnimatorSet? = null

    private val weatherAutoFlipHandler = Handler(Looper.getMainLooper())
    private val weatherAutoFlipRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed && !isAmbientScreensaverActive && !tileWeather.hasFocus()) {
                flipWeatherTile(!isWeatherFlipped)
            }
            val delay = if (isWeatherFlipped) 6000L else 10000L
            weatherAutoFlipHandler.postDelayed(this, delay)
        }
    }

    private lateinit var tileNowPlaying: FrameLayout
    private lateinit var npFront: LinearLayout
    private lateinit var npBack: LinearLayout
    private lateinit var npControls: LinearLayout
    private lateinit var tvNpWidgetTitle: TextView
    private lateinit var tvNpWidgetArtist: TextView
    private lateinit var tvNpWidgetAlbum: TextView
    private lateinit var imgNpWidgetArtwork: ImageView
    private lateinit var tvNpWidgetRelease: TextView
    private lateinit var tvNpWidgetReleaseType: TextView
    private lateinit var tvNpWidgetSource: TextView
    private lateinit var tvNpCtrlSource: TextView
    private lateinit var tvNpCtrlTitle: TextView
    private lateinit var tvNpCtrlArtist: TextView
    private lateinit var btnTileNpPrev: TextView
    private lateinit var btnTileNpPlayPause: TextView
    private lateinit var btnTileNpNext: TextView
    private var isNowPlayingAlternated = false
    private var isNowPlayingControlsActive = false
    private var npAnimatorFront: ViewPropertyAnimator? = null
    private var npAnimatorBack: ViewPropertyAnimator? = null

    // Configuration button (in top nav, accessible by pressing Right beyond TV Settings)
    private lateinit var configButtonContainer: FrameLayout
    private lateinit var btnLauncherConfig: TextView
    private var isLauncherConfigRevealed = false

    private val nowPlayingAutoSlideHandler = Handler(Looper.getMainLooper())
    private val nowPlayingAutoSlideRunnable = object : Runnable {
        override fun run() {
            if (!isFinishing && !isDestroyed && !isAmbientScreensaverActive &&
                tileNowPlaying.visibility == View.VISIBLE && !tileNowPlaying.hasFocus()) {
                slideNowPlayingTile(!isNowPlayingAlternated)
            }
            val delay = if (isNowPlayingAlternated) 5000L else 7000L
            nowPlayingAutoSlideHandler.postDelayed(this, delay)
        }
    }

    // Continue Watching Widget Tile
    private lateinit var tileContinueWatching: FrameLayout
    private lateinit var imgCwThumb: ImageView
    private lateinit var tvCwSource: TextView
    private lateinit var tvCwTitle: TextView
    private lateinit var tvCwSubtitle: TextView
    private lateinit var pbCwProgress: ProgressBar

    // Ambient Screensaver views
    private lateinit var ambientScreensaverLayer: FrameLayout
    private lateinit var imgAmbientWallpaper: ImageView
    private lateinit var ambientNowPlayingContainer: LinearLayout
    private lateinit var imgAmbientNpArt: ImageView
    private lateinit var tvAmbientNpTitle: TextView
    private lateinit var tvAmbientNpArtist: TextView
    private lateinit var tvAmbientTitle: TextView
    private lateinit var tvAmbientCopyright: TextView
    private lateinit var tvAmbientTime: TextView
    private lateinit var tvAmbientDate: TextView
    private lateinit var tvAmbientWeather: TextView
    private lateinit var imgAmbientWeatherIcon: ImageView
    private var scrollAnimator: ValueAnimator? = null
    private var isAmbientScreensaverActive = false
    private var isAmbientAnimating = false
    private var ambientAnimator: ViewPropertyAnimator? = null

    // Launch animation guard to prevent duplicate launches
    private var isLaunchingApp = false

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
            fetchWeather()
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

    // Shared listener for Now Playing music state (feeds Home & Ambient)
    private val nowPlayingListener: (NowPlayingData?) -> Unit = { data ->
        runOnUiThread {
            updateNowPlayingUi(data)
        }
    }

    private val ambientBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.metrolauncher.tv.ACTION_SHOW_AMBIENT" -> showAmbientScreensaver()
                "com.metrolauncher.tv.ACTION_HIDE_AMBIENT" -> hideAmbientScreensaver()
            }
        }
    }

    // Activity result launcher for custom wallpaper (JPG/PNG/GIF)
    private val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val success = CustomWallpaperManager.setCustomWallpaperFromUri(this, uri)
            if (success) {
                CustomWallpaperManager.applyWallpaper(this, imgWallpaper)
                if (LauncherConfig.getTileColorMode(this) == TileColorMode.WALLPAPER) {
                    WallpaperThemeManager.onWallpaperChanged(this)
                }
                applyTileAppearances()
                Toast.makeText(this, "Custom wallpaper applied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to load custom wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgWallpaper = findViewById(R.id.img_wallpaper)
        wallpaperDimOverlay = findViewById(R.id.wallpaper_dim_overlay)
        mainScrollView = findViewById(R.id.main_scroll_view)
        mainContentLayer = findViewById(R.id.main_content_layer)
        headerLayout = findViewById(R.id.header_layout)
        headerGradientScrim = findViewById(R.id.header_gradient_scrim)
        bottomGradientScrim = findViewById(R.id.bottom_gradient_scrim)
        focusSelectorOverlay = findViewById(R.id.focus_selector_overlay)
        tvGreeting = findViewById(R.id.tv_greeting)
        btnWifi = findViewById(R.id.btn_wifi)
        btnAmbient = findViewById(R.id.btn_ambient)
        btnSettings = findViewById(R.id.btn_settings)
        gridRow1 = findViewById(R.id.grid_row_1)
        gridRow2 = findViewById(R.id.grid_row_2)

        // Metro Information Tiles
        rowWidgetTiles = findViewById(R.id.row_widget_tiles)
        tileClock = findViewById(R.id.widget_tile_clock)
        tvClockTime = findViewById(R.id.tv_widget_clock_time)
        tvClockWeekday = findViewById(R.id.tv_widget_clock_weekday)
        tvClockDate = findViewById(R.id.tv_widget_clock_date)

        tileWeather = findViewById(R.id.widget_tile_weather)
        weatherFront = findViewById(R.id.weather_tile_front)
        weatherBack = findViewById(R.id.weather_tile_back)
        tvWeatherTemp = findViewById(R.id.tv_widget_weather_temp)
        imgWeatherIcon = findViewById(R.id.img_widget_weather_icon)
        tvWeatherCondition = findViewById(R.id.tv_widget_weather_condition)
        tvWeatherLocation = findViewById(R.id.tv_widget_weather_location)
        tvForecastLine1 = findViewById(R.id.tv_forecast_line1)
        tvForecastLine2 = findViewById(R.id.tv_forecast_line2)
        tvForecastLine3 = findViewById(R.id.tv_forecast_line3)

        tileNowPlaying = findViewById(R.id.widget_tile_now_playing)
        npFront = findViewById(R.id.np_tile_front)
        npBack = findViewById(R.id.np_tile_back)
        npControls = findViewById(R.id.np_tile_controls)
        tvNpWidgetTitle = findViewById(R.id.tv_widget_np_title)
        tvNpWidgetArtist = findViewById(R.id.tv_widget_np_artist)
        tvNpWidgetAlbum = findViewById(R.id.tv_widget_np_album)
        imgNpWidgetArtwork = findViewById(R.id.img_widget_np_artwork)
        tvNpWidgetRelease = findViewById(R.id.tv_widget_np_release)
        tvNpWidgetReleaseType = findViewById(R.id.tv_widget_np_release_type)
        tvNpWidgetSource = findViewById(R.id.tv_widget_np_source)
        tvNpCtrlSource = findViewById(R.id.tv_widget_np_ctrl_source)
        tvNpCtrlTitle = findViewById(R.id.tv_widget_np_ctrl_title)
        tvNpCtrlArtist = findViewById(R.id.tv_widget_np_ctrl_artist)
        btnTileNpPrev = findViewById(R.id.btn_tile_np_prev)
        btnTileNpPlayPause = findViewById(R.id.btn_tile_np_play_pause)
        btnTileNpNext = findViewById(R.id.btn_tile_np_next)

        // Config button
        configButtonContainer = findViewById(R.id.config_button_container)
        btnLauncherConfig = findViewById(R.id.btn_launcher_config)



        // Continue Watching Widget Tile
        tileContinueWatching = findViewById(R.id.widget_tile_continue_watching)
        imgCwThumb = findViewById(R.id.img_widget_cw_thumb)
        tvCwSource = findViewById(R.id.tv_widget_cw_source)
        tvCwTitle = findViewById(R.id.tv_widget_cw_title)
        tvCwSubtitle = findViewById(R.id.tv_widget_cw_subtitle)
        pbCwProgress = findViewById(R.id.pb_widget_cw_progress)

        // Ambient screensaver views
        ambientScreensaverLayer = findViewById(R.id.ambient_screensaver_layer)
        imgAmbientWallpaper = findViewById(R.id.img_ambient_wallpaper)
        ambientNowPlayingContainer = findViewById(R.id.ambient_now_playing_container)
        imgAmbientNpArt = findViewById(R.id.img_ambient_np_art)
        tvAmbientNpTitle = findViewById(R.id.tv_ambient_np_title)
        tvAmbientNpArtist = findViewById(R.id.tv_ambient_np_artist)
        tvAmbientTitle = findViewById(R.id.tv_ambient_title)
        tvAmbientCopyright = findViewById(R.id.tv_ambient_copyright)
        tvAmbientTime = findViewById(R.id.tv_ambient_time)
        tvAmbientDate = findViewById(R.id.tv_ambient_date)
        tvAmbientWeather = findViewById(R.id.tv_ambient_weather)
        imgAmbientWeatherIcon = findViewById(R.id.img_ambient_weather_icon)

        mainScrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            focusSelectorOverlay.invalidate()
        }

        val rootLayout = findViewById<View>(R.id.root_layout)
        rootLayout.viewTreeObserver.addOnGlobalFocusChangeListener { _: View?, newFocus: View? ->
            if (newFocus != null && (newFocus == btnTileNpPrev || newFocus == btnTileNpPlayPause || newFocus == btnTileNpNext)) {
                focusSelectorOverlay.bindFocusedView(tileNowPlaying)
            } else {
                if (isNowPlayingControlsActive && newFocus != null && newFocus != tileNowPlaying) {
                    showNowPlayingControls(false)
                }
                if (newFocus != null && isTileView(newFocus)) {
                    focusSelectorOverlay.bindFocusedView(newFocus)
                } else {
                    focusSelectorOverlay.bindFocusedView(null)
                }
            }
        }

        setupGridViews()
        setupWidgetTiles()
        applyTileAppearances()
        setupHeaderButtons()
        setupLauncherConfigButton()
        loadInstalledApps()
        loadContinueWatching()
        updateClockAndGreeting()
        updateWifiStatus()
        fetchWeather()

        // Apply wallpaper (custom or default)
        CustomWallpaperManager.applyWallpaper(this, imgWallpaper)

        // Sync 7-day Bing wallpapers locally for Ambient mode
        AmbientWallpaperManager.syncWallpapers(this)

        @Suppress("DEPRECATION")
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        val ambientFilter = IntentFilter().apply {
            addAction("com.metrolauncher.tv.ACTION_SHOW_AMBIENT")
            addAction("com.metrolauncher.tv.ACTION_HIDE_AMBIENT")
        }
        ContextCompat.registerReceiver(this, ambientBroadcastReceiver, ambientFilter, ContextCompat.RECEIVER_EXPORTED)

        // Register shared music playback listener and Spotify receiver
        NowPlayingManager.addListener(nowPlayingListener)
        NowPlayingManager.registerSpotifyReceiver(this)
        WatchNextRepository.addListener { item ->
            bindContinueWatchingWidget(item)
        }
        NowPlayingListenerService.requestSessionRefresh(this)

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
        resetTurnstileState()
        clockHandler.post(clockRunnable)
        weatherHandler.post(weatherRunnable)
        startWeatherAutoFlip()
        startNowPlayingAutoSlide()
        updateWifiStatus()
        resetInactivityTimer()
        CustomWallpaperManager.applyWallpaper(this, imgWallpaper)
        loadContinueWatching()
        NowPlayingManager.registerSpotifyReceiver(this)
        NowPlayingListenerService.requestSessionRefresh(this)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        clockHandler.removeCallbacks(clockRunnable)
        weatherHandler.removeCallbacks(weatherRunnable)
        stopWeatherAutoFlip()
        stopNowPlayingAutoSlide()
        inactivityHandler.removeCallbacks(inactivityRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWeatherAutoFlip()
        stopNowPlayingAutoSlide()
        weatherFlipAnimator?.removeAllListeners()
        weatherFlipAnimator?.cancel()
        weatherFlipAnimator = null
        ambientAnimator?.cancel()
        NowPlayingManager.removeListener(nowPlayingListener)
        try {
            unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
        try {
            unregisterReceiver(ambientBroadcastReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    /**
     * Resets inactivity countdown timer for the ambient screensaver.
     */
    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        if (!isAmbientScreensaverActive) {
            inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS)
        }
    }

    /**
     * Displays the Ambient Mode screensaver with a GPU-friendly slide-down transition.
     */
    fun showAmbientScreensaver() {
        if (isFinishing || isDestroyed || isAmbientScreensaverActive) return
        stopWeatherAutoFlip()
        stopNowPlayingAutoSlide()
        isAmbientScreensaverActive = true
        isAmbientAnimating = true
        ambientAnimator?.cancel()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateAmbientClock()
        loadActiveWallpaperIntoAmbient()
        updateNowPlayingUi(NowPlayingManager.currentTrack)

        // Ensure greeting, top header controls and focus overlay are hidden in Ambient Mode
        if (::focusSelectorOverlay.isInitialized) {
            focusSelectorOverlay.visibility = View.GONE
            focusSelectorOverlay.bindFocusedView(null)
        }
        if (::headerLayout.isInitialized) {
            headerLayout.animate().cancel()
            headerLayout.visibility = View.GONE
        }
        if (::headerGradientScrim.isInitialized) {
            headerGradientScrim.animate().cancel()
            headerGradientScrim.visibility = View.GONE
        }
        if (::bottomGradientScrim.isInitialized) {
            bottomGradientScrim.animate().cancel()
            bottomGradientScrim.visibility = View.GONE
        }

        val duration = LauncherConfig.getAmbientSlideDuration(this)
        val height = if (ambientScreensaverLayer.height > 0) ambientScreensaverLayer.height.toFloat() else resources.displayMetrics.heightPixels.toFloat()

        if (duration <= 0L) {
            ambientScreensaverLayer.translationY = 0f
            ambientScreensaverLayer.alpha = 1f
            ambientScreensaverLayer.visibility = View.VISIBLE
            isAmbientAnimating = false
            return
        }

        ambientScreensaverLayer.translationY = -height
        ambientScreensaverLayer.alpha = 1f
        ambientScreensaverLayer.visibility = View.VISIBLE

        val interpolator = if (LauncherConfig.getPerformanceMode(this) == PerformanceMode.SMOOTH) {
            DecelerateInterpolator(1.4f)
        } else {
            DecelerateInterpolator()
        }

        ambientAnimator = ambientScreensaverLayer.animate()
            .translationY(0f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAmbientAnimating = false
                }
            })
        ambientAnimator?.start()
    }

    /**
     * Dismisses the ambient screensaver with an upward slide animation and returns to the home screen.
     */
    fun hideAmbientScreensaver() {
        if (!isAmbientScreensaverActive) return
        isAmbientScreensaverActive = false
        isAmbientAnimating = true
        ambientAnimator?.cancel()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val duration = LauncherConfig.getAmbientSlideDuration(this)
        val height = if (ambientScreensaverLayer.height > 0) ambientScreensaverLayer.height.toFloat() else resources.displayMetrics.heightPixels.toFloat()

        fun restoreHeaderViews() {
            if (::focusSelectorOverlay.isInitialized) {
                focusSelectorOverlay.visibility = View.VISIBLE
            }
            if (::headerLayout.isInitialized) {
                headerLayout.visibility = View.VISIBLE
                headerLayout.alpha = 1f
            }
            if (::headerGradientScrim.isInitialized) {
                headerGradientScrim.visibility = View.VISIBLE
                headerGradientScrim.alpha = 1f
            }
            if (::bottomGradientScrim.isInitialized) {
                bottomGradientScrim.visibility = View.VISIBLE
                bottomGradientScrim.alpha = 1f
            }
        }

        if (duration <= 0L) {
            ambientScreensaverLayer.visibility = View.GONE
            ambientScreensaverLayer.translationY = 0f
            isAmbientAnimating = false
            restoreHeaderViews()
            restorePreviousFocus()
            resetInactivityTimer()
            startWeatherAutoFlip()
            startNowPlayingAutoSlide()
            return
        }

        val interpolator = if (LauncherConfig.getPerformanceMode(this) == PerformanceMode.SMOOTH) {
            AccelerateInterpolator(1.4f)
        } else {
            AccelerateInterpolator()
        }

        ambientAnimator = ambientScreensaverLayer.animate()
            .translationY(-height)
            .setDuration((duration * 0.9f).toLong())
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ambientScreensaverLayer.visibility = View.GONE
                    ambientScreensaverLayer.translationY = 0f
                    isAmbientAnimating = false
                    restoreHeaderViews()
                    restorePreviousFocus()
                    startWeatherAutoFlip()
                    startNowPlayingAutoSlide()
                }
            })
        ambientAnimator?.start()
        resetInactivityTimer()
    }

    private fun restorePreviousFocus() {
        if (!gridRow1.hasFocus() && !gridRow2.hasFocus()) {
            gridRow1.requestFocus()
        }
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
            tvAmbientTitle.text = "Lume Ambient"
            tvAmbientCopyright.text = "Lume"
        }
    }

    private fun updateAmbientClock() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        tvAmbientTime.text = timeFormat.format(calendar.time)

        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.US)
        tvAmbientDate.text = dateFormat.format(calendar.time)
    }

    /**
     * Updates both the Home-screen compact music widget and the Ambient Mode
     * Now Playing indicator using the single shared playback state.
     */
    private fun updateNowPlayingUi(data: NowPlayingData?) {
        val isHomeEnabled = LauncherConfig.isHomeNowPlayingEnabled(this)

        if (data != null && (data.isPlaying || data.title.isNotEmpty())) {
            if (isHomeEnabled) {
                tvNpWidgetTitle.text = data.title
                tvNpWidgetArtist.text = if (data.artist.isNotEmpty()) data.artist else data.appLabel
                tvNpWidgetAlbum.text = if (data.album.isNotEmpty()) data.album else ""

                if (data.artwork != null) {
                    imgNpWidgetArtwork.setImageBitmap(data.artwork)
                } else {
                    imgNpWidgetArtwork.setImageResource(R.drawable.ic_music_note)
                }
                tvNpWidgetRelease.text = if (data.album.isNotEmpty()) data.album else data.title
                tvNpWidgetReleaseType.text = if (data.album.isNotEmpty()) "Album • ${data.appLabel}" else data.appLabel

                val sourceText = if (data.appLabel.isNotEmpty()) data.appLabel.uppercase() else "MUSIC"
                tvNpWidgetSource.text = sourceText
                tvNpCtrlSource.text = sourceText
                tvNpCtrlTitle.text = if (data.title.isNotEmpty()) data.title else "Track information unavailable"
                tvNpCtrlArtist.text = if (data.artist.isNotEmpty()) data.artist else data.album
                tvNpCtrlArtist.visibility = if (data.artist.isNotEmpty() || data.album.isNotEmpty()) View.VISIBLE else View.GONE
                btnTileNpPlayPause.text = if (data.isPlaying) "\uE769" else "\uE768"

                if (isNowPlayingControlsActive) {
                    updateNowPlayingControlsText()
                }
            } else {
                if (isNowPlayingControlsActive) {
                    showNowPlayingControls(false)
                }
            }

            ambientNowPlayingContainer.visibility = View.VISIBLE
            tvAmbientNpTitle.text = data.title
            tvAmbientNpArtist.text = if (data.artist.isNotEmpty()) data.artist else data.appLabel
            if (data.artwork != null) {
                imgAmbientNpArt.setImageBitmap(data.artwork)
            } else {
                imgAmbientNpArt.setImageResource(R.drawable.ic_music_note)
            }
        } else {
            if (isNowPlayingControlsActive) {
                showNowPlayingControls(false)
            }
            ambientNowPlayingContainer.visibility = View.GONE
        }

        updateWidgetRowVisibility()
    }

    /**
     * Configure HorizontalGridViews for smooth TV scrolling and PS3-style edge-triggered navigation.
     */
    private fun configureGrid(grid: HorizontalGridView, offsetPx: Int) {
        grid.focusScrollStrategy = BaseGridView.FOCUS_SCROLL_ITEM
        grid.windowAlignment = BaseGridView.WINDOW_ALIGN_BOTH_EDGE
        grid.windowAlignmentOffset = offsetPx
        grid.windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        grid.itemAlignmentOffset = 0
        grid.itemAlignmentOffsetPercent = 0f
        grid.isWindowAlignmentPreferKeyLineOverLowEdge = false
        grid.isWindowAlignmentPreferKeyLineOverHighEdge = false
        val perf = LauncherConfig.getPerformanceMode(this)
        grid.setAnimateChildLayout(perf != PerformanceMode.PERFORMANCE && LauncherConfig.isAnimationEnabled(this))
    }

    private fun setupGridViews() {
        val offsetPx = (64 * resources.displayMetrics.density).toInt()
        configureGrid(gridRow1, offsetPx)
        configureGrid(gridRow2, offsetPx)
    }

    /**
     * Loads the single most recently watched video from supported video apps.
     */
    private fun loadContinueWatching() {
        if (!LauncherConfig.isContinueWatchingEnabled(this)) {
            if (::tileContinueWatching.isInitialized) {
                tileContinueWatching.visibility = View.GONE
            }
            updateWidgetRowVisibility()
            return
        }

        WatchNextRepository.loadLatestSingleVideo(this) { item ->
            bindContinueWatchingWidget(item)
        }
    }

    private fun bindContinueWatchingWidget(item: WatchNextItem?) {
        if (!::tileContinueWatching.isInitialized) return
        if (item == null) {
            updateWidgetRowVisibility()
            return
        }

        val sourceText = if (item.appLabel.isNotEmpty()) item.appLabel.uppercase() else "VIDEO"
        tvCwSource.text = sourceText
        tvCwTitle.text = item.title
        if (!item.episodeTitle.isNullOrEmpty() && item.episodeTitle != item.title) {
            tvCwSubtitle.text = item.episodeTitle
            tvCwSubtitle.visibility = View.VISIBLE
        } else {
            tvCwSubtitle.visibility = View.GONE
        }

        if (item.durationMillis > 0L && item.lastPlaybackPositionMillis > 0L) {
            val progressPercent = ((item.lastPlaybackPositionMillis.toFloat() / item.durationMillis.toFloat()) * 100).toInt().coerceIn(1, 100)
            pbCwProgress.progress = progressPercent
            pbCwProgress.visibility = View.VISIBLE
        } else {
            pbCwProgress.visibility = View.GONE
        }

        val artUri = item.posterArtUri
        if (!artUri.isNullOrEmpty()) {
            WatchNextRepository.loadArtwork(this, artUri) { bitmap ->
                if (bitmap != null) {
                    imgCwThumb.setImageBitmap(bitmap)
                    imgCwThumb.visibility = View.VISIBLE
                } else {
                    imgCwThumb.setImageDrawable(null)
                    imgCwThumb.visibility = View.GONE
                }
            }
        } else {
            imgCwThumb.setImageDrawable(null)
            imgCwThumb.visibility = View.GONE
        }

        updateWidgetRowVisibility()
    }

    private fun updateWidgetRowVisibility() {
        if (!::tileNowPlaying.isInitialized || !::tileContinueWatching.isInitialized) return

        val isNpEnabled = LauncherConfig.isHomeNowPlayingEnabled(this)
        val hasNpSession = NowPlayingManager.currentTrack != null
        val showNp = isNpEnabled && hasNpSession

        val isCwEnabled = LauncherConfig.isContinueWatchingEnabled(this)
        val hasCwRecord = WatchNextRepository.getLatestSavedVideo(this) != null
        val showCw = isCwEnabled && hasCwRecord

        val npWasFocused = tileNowPlaying.hasFocus() || isNowPlayingControlsActive
        val cwWasFocused = tileContinueWatching.hasFocus()

        tileNowPlaying.visibility = if (showNp) View.VISIBLE else View.GONE
        tileContinueWatching.visibility = if (showCw) View.VISIBLE else View.GONE

        if (showNp) {
            startNowPlayingAutoSlide()
        } else {
            stopNowPlayingAutoSlide()
        }

        if (npWasFocused && !showNp) {
            if (showCw) {
                tileContinueWatching.requestFocus()
            } else {
                tileWeather.requestFocus()
            }
        } else if (cwWasFocused && !showCw) {
            if (showNp) {
                tileNowPlaying.requestFocus()
            } else {
                tileWeather.requestFocus()
            }
        }
    }

    /**
     * Checks network connection and dynamically updates the Wi-Fi icon.
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
     * Configures the raw Wi-Fi, Ambient Mode, and Settings symbols with TV glow.
     */
    private fun setupHeaderButtons() {
        val rawGlyphFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            val tv = v as? TextView
            val duration = if (LauncherConfig.isAnimationEnabled(this)) 120L else 0L
            if (hasFocus) {
                tv?.setTextColor(Color.WHITE)
                tv?.setShadowLayer(16f, 0f, 0f, Color.WHITE)
                if (duration > 0L) {
                    v.animate().scaleX(1.22f).scaleY(1.22f).translationZ(6f).setDuration(duration).start()
                } else {
                    v.scaleX = 1.22f
                    v.scaleY = 1.22f
                    v.translationZ = 6f
                }
            } else {
                tv?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white_80))
                tv?.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                if (duration > 0L) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(duration).start()
                } else {
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    v.translationZ = 0f
                }
            }
        }

        btnWifi.onFocusChangeListener = rawGlyphFocusListener
        btnAmbient.onFocusChangeListener = rawGlyphFocusListener
        btnSettings.onFocusChangeListener = rawGlyphFocusListener

        btnWifi.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            }
        }

        btnAmbient.setOnClickListener {
            showAmbientScreensaver()
        }

        btnSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                try {
                    startActivity(Intent("android.settings.SETTINGS"))
                } catch (e2: Exception) {
                    Toast.makeText(this, "Cannot open TV settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Sets up the hidden Launcher Configuration button in the top nav,
     * revealed when navigating Right beyond the normal TV Settings button.
     */
    private fun setupLauncherConfigButton() {
        val configFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            val duration = if (LauncherConfig.isAnimationEnabled(this)) 100L else 0L
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.bg_focus_border)
                if (duration > 0L) {
                    v.animate().scaleX(1.15f).scaleY(1.15f).translationZ(8f).setDuration(duration).start()
                } else {
                    v.scaleX = 1.15f
                    v.scaleY = 1.15f
                    v.translationZ = 8f
                }
            } else {
                v.setBackgroundResource(R.drawable.bg_acrylic_tile_dark)
                if (duration > 0L) {
                    v.animate().scaleX(1.0f).scaleY(1.0f).translationZ(0f).setDuration(duration).start()
                } else {
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                    v.translationZ = 0f
                }
                // Automatically slide out and hide whenever losing focus
                hideLauncherConfigButton()
            }
        }

        btnLauncherConfig.onFocusChangeListener = configFocusListener

        btnLauncherConfig.setOnClickListener {
            openLauncherSettings()
        }

        btnLauncherConfig.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        hideLauncherConfigButton {
                            btnSettings.requestFocus()
                        }
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        hideLauncherConfigButton {
                            if (tileNowPlaying.visibility == View.VISIBLE) {
                                tileNowPlaying.requestFocus()
                            } else {
                                tileWeather.requestFocus()
                            }
                        }
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        hideLauncherConfigButton {
                            btnSettings.requestFocus()
                        }
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun revealLauncherConfigButton() {
        if (isLauncherConfigRevealed && configButtonContainer.visibility == View.VISIBLE) return
        isLauncherConfigRevealed = true

        val slideDistance = 60f * resources.displayMetrics.density
        val duration = if (LauncherConfig.isAnimationEnabled(this)) 180L else 0L

        configButtonContainer.animate().cancel()
        configButtonContainer.visibility = View.VISIBLE
        configButtonContainer.translationX = slideDistance
        configButtonContainer.alpha = 0f

        btnLauncherConfig.requestFocus()

        if (duration > 0L) {
            configButtonContainer.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator())
                .setListener(null)
                .start()
        } else {
            configButtonContainer.translationX = 0f
            configButtonContainer.alpha = 1f
        }
    }

    private fun hideLauncherConfigButton(onHidden: (() -> Unit)? = null) {
        if (!isLauncherConfigRevealed && configButtonContainer.visibility == View.GONE) {
            onHidden?.invoke()
            return
        }
        isLauncherConfigRevealed = false

        val slideDistance = 60f * resources.displayMetrics.density
        val duration = if (LauncherConfig.isAnimationEnabled(this)) 160L else 0L

        configButtonContainer.animate().cancel()

        if (duration > 0L && configButtonContainer.visibility == View.VISIBLE) {
            configButtonContainer.animate()
                .translationX(slideDistance)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(AccelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        configButtonContainer.visibility = View.GONE
                        configButtonContainer.translationX = 0f
                        onHidden?.invoke()
                    }
                    override fun onAnimationCancel(animation: Animator) {
                        configButtonContainer.visibility = View.GONE
                        configButtonContainer.translationX = 0f
                    }
                })
                .start()
        } else {
            configButtonContainer.visibility = View.GONE
            configButtonContainer.translationX = 0f
            configButtonContainer.alpha = 0f
            onHidden?.invoke()
        }
    }

    /**
     * Configures the 3 Metro-style information tiles (Clock, Weather, Now Playing).
     * These widgets are focusable via D-pad but non-clickable and never launch applications.
     */
    private fun setupWidgetTiles() {
        val focusDuration = if (LauncherConfig.isAnimationEnabled(this)) {
            if (LauncherConfig.getPerformanceMode(this) == PerformanceMode.SMOOTH) 140L else 100L
        } else 0L

        // Common stationary focus listener: tile remains stationary underneath selector overlay
        val widgetFocusListener = { view: View, _: Boolean ->
            view.scaleX = 1.0f
            view.scaleY = 1.0f
            view.translationZ = 0f
        }

        // TILE 1: CLOCK (Static - no flip/slide)
        tileClock.isClickable = true
        tileClock.isFocusable = true
        tileClock.setOnClickListener { /* Informational widget - non-clickable */ }
        tileClock.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            widgetFocusListener(v, hasFocus)
        }

        // TILE 2: WEATHER (Instant front/back switch on focus/hover)
        tileWeather.isClickable = true
        tileWeather.isFocusable = true
        tileWeather.setOnClickListener { /* Informational widget - non-clickable */ }
        tileWeather.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            widgetFocusListener(v, hasFocus)
            if (hasFocus) {
                stopWeatherAutoFlip()
                showWeatherFace(showBack = true)
            } else {
                showWeatherFace(showBack = false)
                startWeatherAutoFlip()
            }
        }
        tileWeather.setOnHoverListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    stopWeatherAutoFlip()
                    showWeatherFace(showBack = true)
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    if (!tileWeather.hasFocus()) {
                        showWeatherFace(showBack = false)
                        startWeatherAutoFlip()
                    }
                }
            }
            false
        }

        // TILE 3: NOW PLAYING (Instant metadata/artwork face switch on focus/hover)
        tileNowPlaying.clipChildren = true
        tileNowPlaying.clipToPadding = true
        tileNowPlaying.clipToOutline = true
        tileNowPlaying.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 4f * resources.displayMetrics.density)
            }
        }
        tileNowPlaying.isClickable = true
        tileNowPlaying.isFocusable = true
        tileNowPlaying.setOnClickListener {
            showNowPlayingControls(!isNowPlayingControlsActive)
        }
        tileNowPlaying.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            widgetFocusListener(v, hasFocus)
            if (!isNowPlayingControlsActive) {
                if (hasFocus) {
                    stopNowPlayingAutoSlide()
                    showNowPlayingFace(showAlternate = true)
                } else {
                    showNowPlayingFace(showAlternate = false)
                    startNowPlayingAutoSlide()
                }
            }
        }
        tileNowPlaying.setOnHoverListener { _, event ->
            if (!isNowPlayingControlsActive) {
                when (event.action) {
                    MotionEvent.ACTION_HOVER_ENTER -> {
                        stopNowPlayingAutoSlide()
                        showNowPlayingFace(showAlternate = true)
                    }
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        if (!tileNowPlaying.hasFocus()) {
                            showNowPlayingFace(showAlternate = false)
                            startNowPlayingAutoSlide()
                        }
                    }
                }
            }
            false
        }

        // Setup transport controls within music tile
        val buttonFocusListener = View.OnFocusChangeListener { v, hasFocus ->
            val tv = v as? TextView ?: return@OnFocusChangeListener
            val duration = if (LauncherConfig.isAnimationEnabled(this)) 100L else 0L
            if (hasFocus) {
                tv.setTextColor(Color.WHITE)
                tv.setShadowLayer(16f, 0f, 0f, Color.WHITE)
                if (duration > 0L) {
                    tv.animate().scaleX(1.22f).scaleY(1.22f).setDuration(duration).start()
                } else {
                    tv.scaleX = 1.22f
                    tv.scaleY = 1.22f
                }
            } else {
                tv.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white_80))
                tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                if (duration > 0L) {
                    tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(duration).start()
                } else {
                    tv.scaleX = 1.0f
                    tv.scaleY = 1.0f
                }
            }
        }
        btnTileNpPrev.onFocusChangeListener = buttonFocusListener
        btnTileNpPlayPause.onFocusChangeListener = buttonFocusListener
        btnTileNpNext.onFocusChangeListener = buttonFocusListener

        btnTileNpPrev.setOnClickListener {
            btnTileNpPrev.animate().scaleX(1.35f).scaleY(1.35f).setDuration(80).withEndAction {
                btnTileNpPrev.animate().scaleX(1.22f).scaleY(1.22f).setDuration(80).start()
            }.start()
            tvNpCtrlTitle.animate().alpha(0.45f).setDuration(120).withEndAction {
                tvNpCtrlTitle.animate().alpha(1.0f).setDuration(220).start()
            }.start()
            tvNpCtrlArtist.animate().alpha(0.45f).setDuration(120).withEndAction {
                tvNpCtrlArtist.animate().alpha(1.0f).setDuration(220).start()
            }.start()
            NowPlayingManager.skipToPrevious(this)
        }
        btnTileNpPlayPause.setOnClickListener {
            btnTileNpPlayPause.animate().scaleX(1.35f).scaleY(1.35f).setDuration(80).withEndAction {
                btnTileNpPlayPause.animate().scaleX(1.22f).scaleY(1.22f).setDuration(80).start()
            }.start()
            NowPlayingManager.togglePlayPause(this)
        }
        btnTileNpNext.setOnClickListener {
            btnTileNpNext.animate().scaleX(1.35f).scaleY(1.35f).setDuration(80).withEndAction {
                btnTileNpNext.animate().scaleX(1.22f).scaleY(1.22f).setDuration(80).start()
            }.start()
            tvNpCtrlTitle.animate().alpha(0.45f).setDuration(120).withEndAction {
                tvNpCtrlTitle.animate().alpha(1.0f).setDuration(220).start()
            }.start()
            tvNpCtrlArtist.animate().alpha(0.45f).setDuration(120).withEndAction {
                tvNpCtrlArtist.animate().alpha(1.0f).setDuration(220).start()
            }.start()
            NowPlayingManager.skipToNext(this)
        }

        // TILE 4: CONTINUE WATCHING (Single latest watched video)
        tileContinueWatching.clipChildren = true
        tileContinueWatching.clipToPadding = true
        tileContinueWatching.clipToOutline = true
        tileContinueWatching.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 4f * resources.displayMetrics.density)
            }
        }
        tileContinueWatching.isClickable = true
        tileContinueWatching.isFocusable = true
        tileContinueWatching.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            widgetFocusListener(v, hasFocus)
        }
        tileContinueWatching.setOnClickListener {
            val item = WatchNextRepository.getLatestSavedVideo(this)
            if (item != null) {
                val launched = WatchNextRepository.resumePlayback(this, item)
                if (!launched) {
                    val launchIntent = packageManager.getLaunchIntentForPackage(item.packageName)
                    if (launchIntent != null) {
                        try {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Could not open app", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun applyTileAppearances() {
        tileClock.background = LauncherConfig.createTileBackgroundDrawable(this)
        tileWeather.background = LauncherConfig.createTileBackgroundDrawable(this)
        tileNowPlaying.background = LauncherConfig.createTileBackgroundDrawable(this)
        if (::tileContinueWatching.isInitialized) {
            tileContinueWatching.background = LauncherConfig.createTileBackgroundDrawable(this)
        }
        if (::focusSelectorOverlay.isInitialized) {
            focusSelectorOverlay.refreshAnimationState()
        }
        (gridRow1.adapter as? AppsAdapter)?.notifyDataSetChanged()
        (gridRow2.adapter as? AppsAdapter)?.notifyDataSetChanged()
    }

    private fun startWeatherAutoFlip() {
        stopWeatherAutoFlip()
        weatherAutoFlipHandler.postDelayed(weatherAutoFlipRunnable, 10000L)
    }

    private fun stopWeatherAutoFlip() {
        weatherAutoFlipHandler.removeCallbacks(weatherAutoFlipRunnable)
    }

    private fun startNowPlayingAutoSlide() {
        stopNowPlayingAutoSlide()
        if (tileNowPlaying.visibility == View.VISIBLE) {
            nowPlayingAutoSlideHandler.postDelayed(nowPlayingAutoSlideRunnable, 7000L)
        }
    }

    private fun stopNowPlayingAutoSlide() {
        nowPlayingAutoSlideHandler.removeCallbacks(nowPlayingAutoSlideRunnable)
    }

    /**
     * Vertical-axis 3D flip animation for Weather Tile using ObjectAnimator.
     * Guarded with layout and attachment checks, cancel-before-restart safety,
     * and mid-point view swaps so text is never mirrored.
     */
    private fun flipWeatherTile(showBack: Boolean) {
        if (isWeatherFlipped == showBack) return

        if (!::tileWeather.isInitialized || !::weatherFront.isInitialized || !::weatherBack.isInitialized) {
            return
        }

        if (!tileWeather.isAttachedToWindow || tileWeather.width == 0 || tileWeather.height == 0) {
            tileWeather.post {
                if (tileWeather.isAttachedToWindow && tileWeather.width > 0 && tileWeather.height > 0) {
                    flipWeatherTile(showBack)
                }
            }
            return
        }

        isWeatherFlipped = showBack

        val duration = LauncherConfig.getWidgetFlipDuration(this)
        if (duration <= 0L || !LauncherConfig.isAnimationEnabled(this)) {
            weatherFlipAnimator?.removeAllListeners()
            weatherFlipAnimator?.cancel()
            weatherFlipAnimator = null
            tileWeather.rotationY = 0f
            weatherFront.visibility = if (showBack) View.GONE else View.VISIBLE
            weatherBack.visibility = if (showBack) View.VISIBLE else View.GONE
            return
        }

        // Cancel previous animator and remove listeners cleanly
        weatherFlipAnimator?.removeAllListeners()
        weatherFlipAnimator?.cancel()
        weatherFlipAnimator = null

        tileWeather.cameraDistance = 8000f * resources.displayMetrics.density
        val halfDuration = duration / 2

        val isCurrentlyShowingBack = (weatherBack.visibility == View.VISIBLE)
        if (isCurrentlyShowingBack == showBack) {
            if (tileWeather.rotationY != 0f) {
                val restoreAnim = ObjectAnimator.ofFloat(tileWeather, View.ROTATION_Y, tileWeather.rotationY, 0f).apply {
                    this.duration = halfDuration
                    interpolator = DecelerateInterpolator()
                }
                val set = AnimatorSet()
                set.play(restoreAnim)
                set.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        tileWeather.rotationY = 0f
                        weatherFlipAnimator = null
                    }
                })
                weatherFlipAnimator = set
                set.start()
            }
            return
        }

        val firstHalf = ObjectAnimator.ofFloat(tileWeather, View.ROTATION_Y, tileWeather.rotationY, 90f).apply {
            this.duration = halfDuration
            interpolator = AccelerateInterpolator()
        }

        val secondHalf = ObjectAnimator.ofFloat(tileWeather, View.ROTATION_Y, -90f, 0f).apply {
            this.duration = halfDuration
            interpolator = DecelerateInterpolator()
        }

        firstHalf.addListener(object : AnimatorListenerAdapter() {
            private var canceled = false

            override fun onAnimationCancel(animation: Animator) {
                canceled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (canceled || !tileWeather.isAttachedToWindow) return
                if (showBack) {
                    weatherFront.visibility = View.GONE
                    weatherBack.visibility = View.VISIBLE
                } else {
                    weatherBack.visibility = View.GONE
                    weatherFront.visibility = View.VISIBLE
                }
                tileWeather.rotationY = -90f
            }
        })

        val set = AnimatorSet()
        set.playSequentially(firstHalf, secondHalf)
        set.addListener(object : AnimatorListenerAdapter() {
            private var canceled = false

            override fun onAnimationCancel(animation: Animator) {
                canceled = true
            }

            override fun onAnimationEnd(animation: Animator) {
                if (canceled) return
                tileWeather.rotationY = 0f
                weatherFlipAnimator = null
            }
        })

        weatherFlipAnimator = set
        set.start()
    }

    /**
     * Horizontal slide transition for Now Playing Tile.
     */
    private fun slideNowPlayingTile(showAlternate: Boolean) {
        if (isNowPlayingAlternated == showAlternate) return
        isNowPlayingAlternated = showAlternate

        val duration = LauncherConfig.getWidgetSlideDuration(this)
        val width = if (tileNowPlaying.width > 0) tileNowPlaying.width.toFloat() else 250f * resources.displayMetrics.density

        if (duration <= 0L) {
            npFront.visibility = if (showAlternate) View.GONE else View.VISIBLE
            npBack.visibility = if (showAlternate) View.VISIBLE else View.GONE
            npFront.translationX = 0f
            npBack.translationX = 0f
            npFront.alpha = 1f
            npBack.alpha = 1f
            return
        }

        npAnimatorFront?.cancel()
        npAnimatorBack?.cancel()

        val interpolator = if (LauncherConfig.getPerformanceMode(this) == PerformanceMode.SMOOTH) {
            DecelerateInterpolator(1.3f)
        } else {
            DecelerateInterpolator()
        }

        val slideOffset = width * 0.75f

        if (showAlternate) {
            npBack.visibility = View.VISIBLE
            npBack.translationX = slideOffset
            npBack.alpha = 0f

            npAnimatorFront = npFront.animate()
                .translationX(-slideOffset)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        npFront.visibility = View.GONE
                    }
                })
            npAnimatorFront?.start()

            npAnimatorBack = npBack.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .setListener(null)
            npAnimatorBack?.start()
        } else {
            npFront.visibility = View.VISIBLE
            npFront.translationX = -slideOffset
            npFront.alpha = 0f

            npAnimatorBack = npBack.animate()
                .translationX(slideOffset)
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        npBack.visibility = View.GONE
                    }
                })
            npAnimatorBack?.start()

            npAnimatorFront = npFront.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .setListener(null)
            npAnimatorFront?.start()
        }
    }

    private fun showWeatherFace(showBack: Boolean) {
        if (!::tileWeather.isInitialized || !::weatherFront.isInitialized || !::weatherBack.isInitialized) return
        weatherFlipAnimator?.removeAllListeners()
        weatherFlipAnimator?.cancel()
        weatherFlipAnimator = null
        tileWeather.rotationY = 0f
        weatherFront.visibility = if (showBack) View.GONE else View.VISIBLE
        weatherBack.visibility = if (showBack) View.VISIBLE else View.GONE
        weatherFront.alpha = 1f
        weatherBack.alpha = 1f
        isWeatherFlipped = showBack
    }

    private fun showNowPlayingFace(showAlternate: Boolean) {
        if (!::tileNowPlaying.isInitialized || !::npFront.isInitialized || !::npBack.isInitialized) return
        npAnimatorFront?.cancel()
        npAnimatorBack?.cancel()
        npFront.translationX = 0f
        npBack.translationX = 0f
        npFront.alpha = 1f
        npBack.alpha = 1f
        npFront.visibility = if (showAlternate) View.GONE else View.VISIBLE
        npBack.visibility = if (showAlternate) View.VISIBLE else View.GONE
        isNowPlayingAlternated = showAlternate
    }

    private fun updateNowPlayingControlsText() {
        val track = NowPlayingManager.currentTrack
        if (track != null) {
            val sourceText = if (track.appLabel.isNotEmpty()) track.appLabel.uppercase() else "MUSIC"
            tvNpCtrlSource.text = sourceText
            tvNpCtrlTitle.text = if (track.title.isNotEmpty()) track.title else "Track information unavailable"
            tvNpCtrlArtist.text = if (track.artist.isNotEmpty()) track.artist else track.album
            tvNpCtrlArtist.visibility = if (track.artist.isNotEmpty() || track.album.isNotEmpty()) View.VISIBLE else View.GONE
            btnTileNpPlayPause.text = if (track.isPlaying) "\uE769" else "\uE768"
        } else {
            tvNpCtrlSource.text = "MUSIC"
            tvNpCtrlTitle.text = "Nothing playing"
            tvNpCtrlArtist.visibility = View.GONE
            btnTileNpPlayPause.text = "\uE768"
        }
    }

    /**
     * Toggles playback controls face within the music widget tile instantly.
     */
    private fun showNowPlayingControls(show: Boolean) {
        if (isNowPlayingControlsActive == show) return
        isNowPlayingControlsActive = show

        npAnimatorFront?.cancel()
        npAnimatorBack?.cancel()

        if (show) {
            stopNowPlayingAutoSlide()
            updateNowPlayingControlsText()
            npFront.visibility = View.GONE
            npBack.visibility = View.GONE
            npControls.visibility = View.VISIBLE
            npControls.alpha = 1f
            npControls.translationY = 0f
            btnTileNpPlayPause.requestFocus()
        } else {
            npControls.visibility = View.GONE
            if (isNowPlayingAlternated) {
                npBack.visibility = View.VISIBLE
                npBack.alpha = 1f
                npBack.translationX = 0f
            } else {
                npFront.visibility = View.VISIBLE
                npFront.alpha = 1f
                npFront.translationX = 0f
            }
            if (tileNowPlaying.visibility == View.VISIBLE) {
                startNowPlayingAutoSlide()
            }
        }
    }

    private fun openLauncherSettings() {
        hideLauncherConfigButton()
        val dialog = LauncherSettingsDialog(
            context = this,
            onPerfModeChanged = { _ -> },
            onNowPlayingToggled = { _ ->
                updateNowPlayingUi(NowPlayingManager.currentTrack)
            },
            onWeatherLocationChanged = { _ ->
                fetchWeather()
            },
            onTileAppearanceChanged = {
                applyTileAppearances()
            },
            onPickWallpaper = {
                wallpaperPickerLauncher.launch("image/*")
            },
            onResetWallpaper = {
                CustomWallpaperManager.clearCustomWallpaper(this)
                CustomWallpaperManager.applyWallpaper(this, imgWallpaper)
                if (LauncherConfig.getTileColorMode(this) == TileColorMode.WALLPAPER) {
                    WallpaperThemeManager.onWallpaperChanged(this)
                }
                applyTileAppearances()
                Toast.makeText(this, "Wallpaper reset to default", Toast.LENGTH_SHORT).show()
            },
            onContinueWatchingToggled = { _ ->
                loadContinueWatching()
            }
        )
        dialog.setOnDismissListener {
            hideLauncherConfigButton()
            btnSettings.requestFocus()
        }
        dialog.show()
    }



    /**
     * Dynamically queries all device applications and separates them:
     * - Top Row: User pinned apps + Primary media apps (YouTube, Prime Video, Netflix, Music app on device)
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

        val topRowApps = mutableListOf<AppItem>()
        val bottomRowApps = mutableListOf<AppItem>()

        // 1. Pinned apps from user configuration
        val pinnedPackages = LauncherConfig.getPinnedTopRowApps(this)
        for (pkg in pinnedPackages) {
            val pinned = allApps.firstOrNull { it.packageName == pkg }
            if (pinned != null && !topRowApps.any { it.packageName == pinned.packageName }) {
                topRowApps.add(pinned)
            }
        }

        // 2. Default favorites if not already in topRowApps
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

        if (ytApp != null && !topRowApps.any { it.packageName == ytApp.packageName }) topRowApps.add(ytApp)
        if (primeApp != null && !topRowApps.any { it.packageName == primeApp.packageName }) topRowApps.add(primeApp)
        if (netflixApp != null && !topRowApps.any { it.packageName == netflixApp.packageName }) topRowApps.add(netflixApp)
        if (musicApp != null && !topRowApps.any { it.packageName == musicApp.packageName }) topRowApps.add(musicApp)

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

        val openContextMenuForApp: (AppItem, Boolean) -> Unit = { appItem, isSecondRow ->
            TileContextMenuDialog(
                context = this,
                appItem = appItem,
                isSecondRow = isSecondRow,
                onPinToTopRow = {
                    LauncherConfig.pinAppToTopRow(this, appItem.packageName)
                    loadInstalledApps()
                    Toast.makeText(this, "${appItem.label} added to Top Row", Toast.LENGTH_SHORT).show()
                }
            ).show()
        }

        gridRow1.adapter = AppsAdapter(
            apps = topRowApps,
            isTopRow = true,
            onNavigateToHeader = { position ->
                scrollToRow(0)
                if (position == 0) {
                    tileClock.requestFocus()
                } else if (position == 1) {
                    tileWeather.requestFocus()
                } else if (position == 2) {
                    if (tileNowPlaying.visibility == View.VISIBLE) {
                        tileNowPlaying.requestFocus()
                    } else if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                        tileContinueWatching.requestFocus()
                    } else {
                        tileWeather.requestFocus()
                    }
                } else {
                    if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                        tileContinueWatching.requestFocus()
                    } else if (tileNowPlaying.visibility == View.VISIBLE) {
                        tileNowPlaying.requestFocus()
                    } else {
                        tileWeather.requestFocus()
                    }
                }
            },
            onNavigateLeft = null,
            onItemClick = { app -> launchApp(app) },
            onItemLongClick = { app -> openContextMenuForApp(app, false) }
        )

        gridRow2.adapter = AppsAdapter(
            apps = bottomRowApps,
            isTopRow = false,
            onNavigateToHeader = null,
            onNavigateLeft = null,
            onItemClick = { app -> launchApp(app) },
            onItemLongClick = { app -> openContextMenuForApp(app, true) }
        )

        gridRow1.post {
            gridRow1.requestFocus()
        }
    }

    /**
     * Launches external apps with a Windows Phone Turnstile 3D rotation transition.
     * Keeps wallpaper and dimming overlay stationary while foreground UI rotates away.
     */
    private fun launchApp(app: AppItem) {
        if (isLaunchingApp) return

        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
            return
        }

        if (::focusSelectorOverlay.isInitialized) {
            focusSelectorOverlay.visibility = View.GONE
            focusSelectorOverlay.bindFocusedView(null)
        }

        val duration = LauncherConfig.getTurnstileDuration(this)
        if (duration <= 0L) {
            // Performance mode: immediate launch without animation
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        isLaunchingApp = true
        mainScrollView.cameraDistance = 8000f * resources.displayMetrics.density
        mainScrollView.pivotX = 0f
        mainScrollView.pivotY = mainScrollView.height * 0.5f

        val interpolator = if (LauncherConfig.getPerformanceMode(this) == PerformanceMode.SMOOTH) {
            AccelerateDecelerateInterpolator()
        } else {
            AccelerateInterpolator(1.2f)
        }

        mainScrollView.animate()
            .rotationY(-26f)
            .translationX(-60f * resources.displayMetrics.density)
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    try {
                        startActivity(launchIntent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
                        resetTurnstileState()
                    }
                }
            })
            .start()
    }

    private fun resetTurnstileState() {
        isLaunchingApp = false
        mainScrollView.animate().cancel()
        mainScrollView.rotationY = 0f
        mainScrollView.translationX = 0f
        mainScrollView.alpha = 1f
        mainScrollView.scaleX = 1f
        mainScrollView.scaleY = 1f
        val current = currentFocus
        if (::focusSelectorOverlay.isInitialized && current != null && isTileView(current)) {
            focusSelectorOverlay.bindFocusedView(current)
        }
    }

    /**
     * Updates greeting dynamically based on current time of the day.
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

        // Widget Tile Clock (12-hour format with AM/PM uppercase, full weekday, full date)
        val clockTimeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val clockWeekdayFormat = SimpleDateFormat("EEEE", Locale.US)
        val clockDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.US)

        tvClockTime.text = clockTimeFormat.format(calendar.time).uppercase(Locale.US)
        tvClockWeekday.text = clockWeekdayFormat.format(calendar.time)
        tvClockDate.text = clockDateFormat.format(calendar.time)
    }

    /**
     * Maps Open-Meteo weather codes to crisp monochrome static Meteocons vector drawables.
     */
    private fun getMeteoconResId(code: Int, isDay: Boolean = true): Int {
        return when (code) {
            0 -> if (isDay) R.drawable.ic_meteo_clear_day else R.drawable.ic_meteo_clear_night
            1, 2 -> R.drawable.ic_meteo_partly_cloudy
            3 -> R.drawable.ic_meteo_cloudy
            45, 48 -> R.drawable.ic_meteo_fog
            51, 53, 55 -> R.drawable.ic_meteo_drizzle
            56, 57, 61, 63, 65, 80, 81, 82 -> R.drawable.ic_meteo_rain
            66, 67, 71, 73, 75, 77, 85, 86 -> R.drawable.ic_meteo_snow
            95, 96, 99 -> R.drawable.ic_meteo_thunderstorm
            else -> R.drawable.ic_meteo_partly_cloudy
        }
    }

    /**
     * Fetches real-time weather and today's hourly/daily forecast from Open-Meteo.
     */
    private fun fetchWeather() {
        val location = LauncherConfig.getWeatherLocation(this)
        Thread {
            try {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}&current_weather=true&hourly=temperature_2m,precipitation_probability,weathercode&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto")
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
                    val isDay = current.optInt("is_day", 1) == 1

                    val condition = getWeatherCondition(code)
                    val meteoResId = getMeteoconResId(code, isDay)
                    val tempFormatted = "${Math.round(temp)}°C"

                    var line1 = ""
                    var line2 = ""
                    var line3 = ""

                    try {
                        if (json.has("hourly")) {
                            val hourly = json.getJSONObject("hourly")
                            val times = hourly.getJSONArray("time")
                            val temps = hourly.getJSONArray("temperature_2m")
                            val pops = if (hourly.has("precipitation_probability")) hourly.getJSONArray("precipitation_probability") else null

                            val nowCal = Calendar.getInstance()
                            val curHour = nowCal.get(Calendar.HOUR_OF_DAY)
                            val nextHour1 = (curHour + 1).coerceAtMost(times.length() - 1)
                            val nextHour2 = (curHour + 2).coerceAtMost(times.length() - 1)

                            fun formatHour(h: Int): String {
                                val cal = Calendar.getInstance()
                                cal.set(Calendar.HOUR_OF_DAY, h)
                                val sdf = SimpleDateFormat("h a", Locale.US)
                                return sdf.format(cal.time)
                            }

                            val t1 = Math.round(temps.getDouble(nextHour1))
                            val pop1 = pops?.optInt(nextHour1, 0) ?: 0
                            line1 = "${formatHour(nextHour1)} · $t1°C · $pop1% rain"

                            val t2 = Math.round(temps.getDouble(nextHour2))
                            val pop2 = pops?.optInt(nextHour2, 0) ?: 0
                            line2 = "${formatHour(nextHour2)} · $t2°C · $pop2% rain"
                        }

                        if (json.has("daily")) {
                            val daily = json.getJSONObject("daily")
                            val maxTemp = Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0))
                            val minTemp = Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0))
                            line3 = "Today · High $maxTemp° · Low $minTemp°"
                        }
                    } catch (e: Exception) {
                        // ignore forecast parse errors
                    }

                    runOnUiThread {
                        tvAmbientWeather.text = "${Math.round(temp)}°C • ${condition.second} • ${location.name}"
                        imgAmbientWeatherIcon.setImageResource(meteoResId)

                        tvWeatherTemp.text = tempFormatted
                        imgWeatherIcon.setImageResource(meteoResId)
                        tvWeatherCondition.text = condition.second
                        tvWeatherLocation.text = location.name

                        if (line1.isNotEmpty()) tvForecastLine1.text = line1
                        if (line2.isNotEmpty()) tvForecastLine2.text = line2
                        if (line3.isNotEmpty()) tvForecastLine3.text = line3
                    }
                }
            } catch (e: Exception) {
                // Silently fallback without fabricating
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

    private fun updateRowFading(scrollY: Int = 0) {
        // Rows remain stationary and 100% opaque. Progressive vertical fade is applied
        // by MaskedScrollContainer per-pixel without entire-tile disappearances.
    }

    private fun isTileView(v: View): Boolean {
        if (!v.isFocusable) return false
        if (v == btnTileNpPrev || v == btnTileNpPlayPause || v == btnTileNpNext) return false
        var p = v.parent
        while (p != null) {
            if (p == mainScrollView) return true
            p = p.parent
        }
        return false
    }

    private fun scrollToRow(targetY: Int) {
        val mode = LauncherConfig.getPerformanceMode(this)
        val duration = when (mode) {
            PerformanceMode.PERFORMANCE -> 0L
            PerformanceMode.BALANCED -> 200L
            PerformanceMode.SMOOTH -> 280L
        }

        scrollAnimator?.cancel()
        if (duration <= 0L || !LauncherConfig.isAnimationEnabled(this)) {
            mainScrollView.scrollTo(0, targetY)
            return
        }

        scrollAnimator = ValueAnimator.ofInt(mainScrollView.scrollY, targetY).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                mainScrollView.scrollTo(0, animator.animatedValue as Int)
            }
            start()
        }
    }

    /**
     * Determines whether a child tile is currently fully visible within the grid's visible viewport.
     */
    private fun isChildFullyVisible(grid: HorizontalGridView, child: View): Boolean {
        if (!child.isAttachedToWindow || !child.isShown) return false
        val childLeft = child.left
        val childRight = child.right
        val viewportLeft = grid.paddingStart
        val viewportRight = grid.width - grid.paddingEnd
        return childLeft >= viewportLeft && childRight <= viewportRight
    }

    /**
     * Navigates into an app row at the specified logical column index.
     * Preserves destination row's horizontal scroll position if the target tile is already visible.
     * Scrolls only the minimum distance required if the tile is outside the viewport.
     */
    private fun navigateGridToLogicalColumn(grid: HorizontalGridView, targetCol: Int): Boolean {
        val adapter = grid.adapter ?: return false
        val itemCount = adapter.itemCount
        if (itemCount <= 0) return false

        val targetPos = targetCol.coerceIn(0, itemCount - 1)
        val vh = grid.findViewHolderForAdapterPosition(targetPos)
        val targetView = vh?.itemView

        if (targetView != null && isChildFullyVisible(grid, targetView)) {
            grid.selectedPosition = targetPos
            targetView.requestFocus()
        } else {
            val perf = LauncherConfig.getPerformanceMode(this)
            if (perf == PerformanceMode.PERFORMANCE || !LauncherConfig.isAnimationEnabled(this)) {
                grid.selectedPosition = targetPos
                grid.post {
                    grid.findViewHolderForAdapterPosition(targetPos)?.itemView?.requestFocus() ?: grid.requestFocus()
                }
            } else {
                grid.setSelectedPositionSmooth(targetPos)
                grid.post {
                    grid.findViewHolderForAdapterPosition(targetPos)?.itemView?.requestFocus() ?: grid.requestFocus()
                }
            }
        }
        return true
    }

    /**
     * Navigates between adjacent app rows preserving the logical column index.
     * If the destination row is shorter, selects its last available tile.
     * Does not scroll the destination row merely to align horizontally with the source tile.
     * Preserves existing destination scroll position if the destination tile is already visible.
     */
    private fun navigateBetweenAppRows(sourceGrid: HorizontalGridView, destGrid: HorizontalGridView): Boolean {
        val destAdapter = destGrid.adapter ?: return false
        val destCount = destAdapter.itemCount
        if (destCount <= 0) return false

        var sourcePos = sourceGrid.selectedPosition
        if (sourcePos < 0) {
            val focused = sourceGrid.findFocus()
            if (focused != null) {
                val vh = sourceGrid.findContainingViewHolder(focused)
                if (vh != null) {
                    sourcePos = vh.adapterPosition
                }
            }
        }
        if (sourcePos < 0) sourcePos = 0

        val targetPos = sourcePos.coerceIn(0, destCount - 1)
        val vh = destGrid.findViewHolderForAdapterPosition(targetPos)
        val targetView = vh?.itemView

        if (targetView != null && isChildFullyVisible(destGrid, targetView)) {
            destGrid.selectedPosition = targetPos
            targetView.requestFocus()
        } else {
            val perf = LauncherConfig.getPerformanceMode(this)
            if (perf == PerformanceMode.PERFORMANCE || !LauncherConfig.isAnimationEnabled(this)) {
                destGrid.selectedPosition = targetPos
                destGrid.post {
                    destGrid.findViewHolderForAdapterPosition(targetPos)?.itemView?.requestFocus() ?: destGrid.requestFocus()
                }
            } else {
                destGrid.setSelectedPositionSmooth(targetPos)
                destGrid.post {
                    destGrid.findViewHolderForAdapterPosition(targetPos)?.itemView?.requestFocus() ?: destGrid.requestFocus()
                }
            }
        }
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Any user interaction resets the ambient screensaver timer
        resetInactivityTimer()

        // If ambient screensaver is active or animating, any remote key press dismisses it immediately
        if (isAmbientScreensaverActive || isAmbientAnimating) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                hideAmbientScreensaver()
            }
            return true // Strictly consume key event so it never triggers underlying home tiles!
        }

        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (gridRow2.hasFocus()) {
                        scrollToRow(0)
                        navigateBetweenAppRows(gridRow2, gridRow1)
                        return true
                    }
                    if (gridRow1.hasFocus()) {
                        scrollToRow(0)
                        val sourcePos = gridRow1.selectedPosition.coerceAtLeast(0)
                        if (sourcePos == 0) {
                            tileClock.requestFocus()
                        } else if (sourcePos == 1) {
                            tileWeather.requestFocus()
                        } else if (sourcePos == 2) {
                            if (tileNowPlaying.visibility == View.VISIBLE) {
                                tileNowPlaying.requestFocus()
                            } else if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                                tileContinueWatching.requestFocus()
                            } else {
                                tileWeather.requestFocus()
                            }
                        } else {
                            if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                                tileContinueWatching.requestFocus()
                            } else if (tileNowPlaying.visibility == View.VISIBLE) {
                                tileNowPlaying.requestFocus()
                            } else {
                                tileWeather.requestFocus()
                            }
                        }
                        return true
                    }
                    if (btnTileNpPrev.hasFocus() || btnTileNpPlayPause.hasFocus() || btnTileNpNext.hasFocus()) {
                        showNowPlayingControls(false)
                        scrollToRow(0)
                        btnSettings.requestFocus()
                        return true
                    }
                    if (tileClock.hasFocus() || tileWeather.hasFocus() || tileNowPlaying.hasFocus() || (::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus())) {
                        scrollToRow(0)
                        if (tileClock.hasFocus()) {
                            btnWifi.requestFocus()
                        } else if (tileWeather.hasFocus()) {
                            btnAmbient.requestFocus()
                        } else {
                            btnSettings.requestFocus()
                        }
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (btnTileNpPrev.hasFocus() || btnTileNpPlayPause.hasFocus() || btnTileNpNext.hasFocus()) {
                        showNowPlayingControls(false)
                        scrollToRow(0)
                        navigateGridToLogicalColumn(gridRow1, 2)
                        return true
                    }
                    if (btnWifi.hasFocus() || btnAmbient.hasFocus() || btnSettings.hasFocus() || btnLauncherConfig.hasFocus()) {
                        if (btnLauncherConfig.hasFocus() || isLauncherConfigRevealed) {
                            hideLauncherConfigButton()
                        }
                        scrollToRow(0)
                        if (btnWifi.hasFocus()) {
                            tileClock.requestFocus()
                        } else if (btnAmbient.hasFocus()) {
                            tileWeather.requestFocus()
                        } else {
                            if (tileNowPlaying.visibility == View.VISIBLE) {
                                tileNowPlaying.requestFocus()
                            } else if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                                tileContinueWatching.requestFocus()
                            } else {
                                tileWeather.requestFocus()
                            }
                        }
                        return true
                    }
                    if (tileClock.hasFocus() || tileWeather.hasFocus() || tileNowPlaying.hasFocus() || (::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus())) {
                        scrollToRow(0)
                        val widgetCol = when {
                            tileClock.hasFocus() -> 0
                            tileWeather.hasFocus() -> 1
                            tileNowPlaying.hasFocus() || btnTileNpPrev.hasFocus() || btnTileNpPlayPause.hasFocus() || btnTileNpNext.hasFocus() -> 2
                            ::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus() -> {
                                if (tileNowPlaying.visibility == View.VISIBLE) 3 else 2
                            }
                            else -> 0
                        }
                        navigateGridToLogicalColumn(gridRow1, widgetCol)
                        return true
                    }
                    if (gridRow1.hasFocus()) {
                        val targetY = (gridRow2.top - gridRow1.top).coerceAtLeast(0)
                        scrollToRow(targetY)
                        navigateBetweenAppRows(gridRow1, gridRow2)
                        return true
                    }
                    if (gridRow2.hasFocus()) {
                        return true // Consume so focus doesn't wander down
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (btnTileNpPrev.hasFocus()) {
                        btnTileNpPlayPause.requestFocus()
                        return true
                    }
                    if (btnTileNpPlayPause.hasFocus()) {
                        btnTileNpNext.requestFocus()
                        return true
                    }
                    if (btnTileNpNext.hasFocus()) {
                        return true
                    }
                    if (btnLauncherConfig.hasFocus()) {
                        return true
                    }
                    if (tileClock.hasFocus()) {
                        tileWeather.requestFocus()
                        return true
                    }
                    if (tileWeather.hasFocus()) {
                        if (tileNowPlaying.visibility == View.VISIBLE) {
                            tileNowPlaying.requestFocus()
                        } else if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                            tileContinueWatching.requestFocus()
                        }
                        return true
                    }
                    if (tileNowPlaying.hasFocus()) {
                        if (::tileContinueWatching.isInitialized && tileContinueWatching.visibility == View.VISIBLE) {
                            tileContinueWatching.requestFocus()
                        }
                        return true
                    }
                    if (::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus()) {
                        return true
                    }
                    if (btnWifi.hasFocus()) {
                        btnAmbient.requestFocus()
                        return true
                    }
                    if (btnAmbient.hasFocus()) {
                        btnSettings.requestFocus()
                        return true
                    }
                    if (btnSettings.hasFocus()) {
                        revealLauncherConfigButton()
                        return true
                    }
                    if (gridRow1.hasFocus() && gridRow1.selectedPosition >= (gridRow1.adapter?.itemCount ?: 0) - 1) {
                        return true
                    }
                    if (gridRow2.hasFocus() && gridRow2.selectedPosition >= (gridRow2.adapter?.itemCount ?: 0) - 1) {
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (btnTileNpNext.hasFocus()) {
                        btnTileNpPlayPause.requestFocus()
                        return true
                    }
                    if (btnTileNpPlayPause.hasFocus()) {
                        btnTileNpPrev.requestFocus()
                        return true
                    }
                    if (btnTileNpPrev.hasFocus()) {
                        showNowPlayingControls(false)
                        tileWeather.requestFocus()
                        return true
                    }
                    if (btnLauncherConfig.hasFocus()) {
                        hideLauncherConfigButton()
                        btnSettings.requestFocus()
                        return true
                    }
                    if (::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus()) {
                        if (tileNowPlaying.visibility == View.VISIBLE) {
                            tileNowPlaying.requestFocus()
                        } else {
                            tileWeather.requestFocus()
                        }
                        return true
                    }
                    if (tileNowPlaying.hasFocus()) {
                        tileWeather.requestFocus()
                        return true
                    }
                    if (tileWeather.hasFocus()) {
                        tileClock.requestFocus()
                        return true
                    }
                    if (tileClock.hasFocus()) {
                        return true
                    }
                    if (gridRow1.hasFocus() && gridRow1.selectedPosition == 0) {
                        return true
                    }
                    if (gridRow2.hasFocus() && gridRow2.selectedPosition == 0) {
                        return true
                    }
                    if (btnSettings.hasFocus()) {
                        btnAmbient.requestFocus()
                        return true
                    }
                    if (btnAmbient.hasFocus()) {
                        btnWifi.requestFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (tileClock.hasFocus() || tileWeather.hasFocus()) {
                        return true
                    }
                    if (tileNowPlaying.hasFocus()) {
                        showNowPlayingControls(true)
                        return true
                    }
                    if (::tileContinueWatching.isInitialized && tileContinueWatching.hasFocus()) {
                        tileContinueWatching.performClick()
                        return true
                    }
                    if (btnTileNpPrev.hasFocus()) {
                        btnTileNpPrev.performClick()
                        return true
                    }
                    if (btnTileNpPlayPause.hasFocus()) {
                        btnTileNpPlayPause.performClick()
                        return true
                    }
                    if (btnTileNpNext.hasFocus()) {
                        btnTileNpNext.performClick()
                        return true
                    }
                    if (btnWifi.hasFocus()) {
                        btnWifi.performClick()
                        return true
                    } else if (btnAmbient.hasFocus()) {
                        btnAmbient.performClick()
                        return true
                    } else if (btnSettings.hasFocus()) {
                        btnSettings.performClick()
                        return true
                    } else if (btnLauncherConfig.hasFocus()) {
                        btnLauncherConfig.performClick()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isNowPlayingControlsActive) {
                showNowPlayingControls(false)
                tileNowPlaying.requestFocus()
                return true
            }
            if (isAmbientScreensaverActive || isAmbientAnimating) {
                hideAmbientScreensaver()
                return true
            }
            if (configButtonContainer.visibility == View.VISIBLE || btnLauncherConfig.hasFocus()) {
                hideLauncherConfigButton()
                btnSettings.requestFocus()
                return true
            }
            if (!gridRow1.hasFocus() && !tileClock.hasFocus()) {
                gridRow1.requestFocus()
                return true
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
