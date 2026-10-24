package com.metrolauncher.tv

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.LruCache
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Immutable state model for currently playing music.
 */
data class NowPlayingData(
    val title: String,
    val artist: String,
    val album: String = "",
    val artwork: Bitmap? = null,
    val isPlaying: Boolean = false,
    val packageName: String = "",
    val appLabel: String = "Music"
)

/**
 * Shared singleton managing active music playback state.
 * Feeds both the Home-screen Now Playing widget and Ambient Mode indicator.
 * Strictly filters out video playback (YouTube, Netflix, Prime Video, MX Player, etc.)
 * to only show verified music streaming and audio playback.
 */
object NowPlayingManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<WeakReference<(NowPlayingData?) -> Unit>>()
    private val artworkCache = object : LruCache<String, Bitmap>(20) {}
    private val imageExecutor = Executors.newSingleThreadExecutor()

    private var appContext: Context? = null

    var currentTrack: NowPlayingData? = null
        private set

    private var activeController: MediaController? = null
    private var isSpotifyReceiverRegistered = false

    // Video package blacklist: exclude pure movie/video streaming apps from music display
    private val VIDEO_PACKAGES = setOf(
        "com.netflix.ninja",
        "com.netflix.mediaclient",
        "com.amazon.amazonvideo.livingroom",
        "com.amazon.avod",
        "in.startv.hotstar.dplus.tv",
        "in.startv.hotstar",
        "com.jio.media.ondemand",
        "com.jio.jiocinema",
        "com.sonyliv",
        "com.sonyliv.tv",
        "com.graymatrix.did",
        "com.zee5.tv",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro",
        "com.mxtech.videoplayer.television",
        "org.videolan.vlc",
        "com.plexapp.android",
        "com.plexapp.mediaserver.smb",
        "org.jellyfin.androidtv"
    )

    // Known music packages that are verified audio sources (Spotify, YT Music, JioSaavn, etc.)
    private val MUSIC_PACKAGES = setOf(
        "com.spotify.music",
        "com.spotify.tv.android",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube.tvmusic",
        "com.google.android.youtube.music",
        "com.vanced.android.apps.youtube.music",
        "app.revanced.android.apps.youtube.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.aspiro.tidal",
        "deezer.android.app",
        "com.pandora.android",
        "tunein.player",
        "com.jio.media.jiobeats",
        "com.saavn.android",
        "com.gaana",
        "com.auramusic",
        "com.android.music"
    )

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateFromActiveController(playbackState = state)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateFromActiveController(metadata = metadata)
        }

        override fun onSessionDestroyed() {
            activeController = null
            setTrack(null)
        }
    }

    private val spotifyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            appContext = context.applicationContext
            val action = intent.action ?: return
            when {
                action.endsWith(".playbackstatechanged") -> {
                    val isPlaying = intent.getBooleanExtra("playing", false)
                    currentTrack?.let {
                        setTrack(it.copy(isPlaying = isPlaying))
                    }
                    scheduleSessionRefreshes(context)
                }
                action.endsWith(".metadatachanged") -> {
                    val track = intent.getStringExtra("track")
                    val artist = intent.getStringExtra("artist")
                    val album = intent.getStringExtra("album")
                    val isPlaying = intent.getBooleanExtra("playing", true)
                    if (!track.isNullOrEmpty()) {
                        setTrack(
                            NowPlayingData(
                                title = track,
                                artist = artist ?: "",
                                album = album ?: "",
                                artwork = currentTrack?.takeIf { it.title == track }?.artwork,
                                isPlaying = isPlaying,
                                packageName = intent.`package` ?: currentTrack?.packageName ?: "com.spotify.tv.android",
                                appLabel = "Spotify"
                            )
                        )
                    }
                    scheduleSessionRefreshes(context)
                }
            }
        }
    }

    fun registerSpotifyReceiver(context: Context) {
        appContext = context.applicationContext
        if (isSpotifyReceiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction("com.spotify.music.playbackstatechanged")
                addAction("com.spotify.music.metadatachanged")
                addAction("com.spotify.music.queuechanged")
                addAction("com.spotify.mobile.android.playbackstatechanged")
                addAction("com.spotify.mobile.android.metadatachanged")
                addAction("com.spotify.mobile.android.queuechanged")
            }
            ContextCompat.registerReceiver(
                context.applicationContext,
                spotifyReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            isSpotifyReceiverRegistered = true
        } catch (e: Exception) {
            // Ignore if registration fails
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val pkg = context.packageName
        if (flat.contains(pkg)) return true
        val myComponent = ComponentName(context, NowPlayingListenerService::class.java).flattenToString()
        return flat.contains(myComponent)
    }

    /**
     * Registers a listener for now playing updates.
     */
    fun addListener(listener: (NowPlayingData?) -> Unit) {
        listeners.add(WeakReference(listener))
        // Immediately notify current state
        listener(currentTrack)
    }

    /**
     * Removes a listener.
     */
    fun removeListener(listener: (NowPlayingData?) -> Unit) {
        val iterator = listeners.iterator()
        while (iterator.hasNext()) {
            val ref = iterator.next()
            if (ref.get() == null || ref.get() == listener) {
                listeners.remove(ref)
            }
        }
    }

    /**
     * Called by NotificationListenerService or Activity when active media sessions change.
     */
    fun onActiveSessionsChanged(context: Context, controllers: List<MediaController>?) {
        appContext = context.applicationContext
        val musicControllers = controllers?.filter { isMusicSession(it) } ?: emptyList()
        // 1. Prioritize any session that is actively PLAYING
        val activePlaying = musicControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        }
        // 2. Otherwise pick any music controller (e.g. paused)
        val musicController = activePlaying ?: musicControllers.firstOrNull()

        if (musicController != null) {
            if (activeController?.sessionToken != musicController.sessionToken || activeController == null) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = musicController
                musicController.registerCallback(controllerCallback, mainHandler)
            }
            updateFromActiveController(context)
        } else {
            if (activeController != null) {
                activeController?.unregisterCallback(controllerCallback)
                activeController = null
                setTrack(null)
            }
        }

        // 3. Track latest video sessions (VLC, streaming apps) for Continue Watching
        val videoControllers = controllers?.filter { isVideoSession(it) } ?: emptyList()
        val activeVideo = videoControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: videoControllers.firstOrNull()
        if (activeVideo != null) {
            recordVideoSession(context, activeVideo)
        }
    }

    /**
     * Refreshes metadata directly from the currently active controller if one exists.
     */
    fun refreshFromCurrentController(context: Context? = null) {
        val controller = activeController ?: return
        mainHandler.post {
            updateFromActiveController(context ?: appContext, controller.metadata, controller.playbackState)
        }
    }


    /**
     * Determines whether a MediaController represents a supported music session.
     */
    fun isMusicSession(controller: MediaController): Boolean {
        val pkg = controller.packageName.lowercase()

        // 1. Whitelisted music packages & audio keywords (Always allowed)
        if (MUSIC_PACKAGES.contains(pkg) ||
            pkg.contains("music") ||
            pkg.contains("spotify") ||
            pkg.contains("audio") ||
            pkg.contains("jiosaavn") ||
            pkg.contains("saavn") ||
            pkg.contains("gaana") ||
            pkg.contains("wynk") ||
            pkg.contains("tidal") ||
            pkg.contains("deezer") ||
            pkg.contains("tunein") ||
            pkg.contains("soundcloud") ||
            pkg.contains("podcast") ||
            pkg.contains("radio")) {
            return true
        }

        // 2. Reject pure video streaming apps (Netflix, Prime, Hotstar, SonyLIV, Zee5, etc.)
        if (VIDEO_PACKAGES.contains(pkg) ||
            pkg.contains("netflix") ||
            pkg.contains("hotstar") ||
            pkg.contains("sonyliv") ||
            pkg.contains("zee5") ||
            pkg.contains("prime") ||
            pkg.contains("amazonvideo") ||
            pkg.contains("disney") ||
            pkg.contains("jiocinema")) {
            return false
        }

        // 3. YouTube TV & YouTube player: Accept if it has track title/artist (audio/music video playing)
        if (pkg.contains("youtube")) {
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            if (!title.isNullOrEmpty() || !artist.isNullOrEmpty()) {
                return true
            }
        }

        // 4. Check playback state: active or has valid media title
        val state = controller.playbackState?.state
        val hasTitle = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotEmpty() == true ||
                controller.metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.isNotEmpty() == true
        return state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_PAUSED ||
                state == PlaybackState.STATE_BUFFERING ||
                hasTitle
    }

    private fun updateFromActiveController(
        context: Context? = null,
        metadata: MediaMetadata? = null,
        playbackState: PlaybackState? = null
    ) {
        val effectiveContext = context ?: appContext
        val controller = activeController ?: run {
            setTrack(null)
            return
        }

        val meta = metadata ?: controller.metadata
        val pbState = playbackState ?: controller.playbackState

        val isPlaying = pbState?.state == PlaybackState.STATE_PLAYING

        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""

        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""

        val album = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        var artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artUriString = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        if (artwork == null && !artUriString.isNullOrEmpty()) {
            val cached = artworkCache.get(artUriString)
            if (cached != null) {
                artwork = cached
            } else {
                loadArtworkFromUri(effectiveContext, artUriString, title) { loadedBmp ->
                    if (currentTrack?.title == title) {
                        setTrack(currentTrack?.copy(artwork = loadedBmp))
                    }
                }
            }
        }

        if (title.isNotEmpty() || isPlaying) {
            val appLabel = if (effectiveContext != null) {
                try {
                    val appInfo = effectiveContext.packageManager.getApplicationInfo(controller.packageName, 0)
                    effectiveContext.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    when {
                        controller.packageName.contains("spotify") -> "Spotify"
                        controller.packageName.contains("youtube") -> "YouTube Music"
                        controller.packageName.contains("saavn") -> "JioSaavn"
                        controller.packageName.contains("gaana") -> "Gaana"
                        controller.packageName.contains("apple") -> "Apple Music"
                        controller.packageName.contains("amazon") -> "Amazon Music"
                        else -> "Music"
                    }
                }
            } else when {
                controller.packageName.contains("spotify") -> "Spotify"
                controller.packageName.contains("youtube") -> "YouTube Music"
                controller.packageName.contains("saavn") -> "JioSaavn"
                controller.packageName.contains("gaana") -> "Gaana"
                controller.packageName.contains("apple") -> "Apple Music"
                controller.packageName.contains("amazon") -> "Amazon Music"
                else -> "Music"
            }

            setTrack(
                NowPlayingData(
                    title = if (title.isNotEmpty()) title else "Playing",
                    artist = artist,
                    album = album,
                    artwork = artwork,
                    isPlaying = isPlaying,
                    packageName = controller.packageName,
                    appLabel = appLabel
                )
            )
        } else {
            setTrack(null)
        }
    }

    private fun loadArtworkFromUri(
        context: Context?,
        uriString: String,
        expectedTitle: String,
        onLoaded: (Bitmap) -> Unit
    ) {
        imageExecutor.execute {
            try {
                val bitmap: Bitmap? = if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    val url = URL(uriString)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.doInput = true
                    conn.connect()
                    conn.inputStream.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                } else if (context != null) {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply {
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }
                        BitmapFactory.decodeStream(stream, null, options)
                    }
                } else null

                if (bitmap != null) {
                    artworkCache.put(uriString, bitmap)
                    mainHandler.post {
                        if (currentTrack?.title == expectedTitle) {
                            onLoaded(bitmap)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore network/decode errors
            }
        }
    }

    private fun setTrack(data: NowPlayingData?) {
        currentTrack = data
        mainHandler.post {
            for (ref in listeners) {
                ref.get()?.invoke(data)
            }
        }
    }

    /**
     * Determines whether a MediaController represents an active video playback session.
     */
    fun isVideoSession(controller: MediaController): Boolean {
        val pkg = controller.packageName.lowercase()
        if (isMusicSession(controller)) return false

        if (VIDEO_PACKAGES.contains(pkg) ||
            pkg.contains("vlc") ||
            pkg.contains("netflix") ||
            pkg.contains("hotstar") ||
            pkg.contains("sonyliv") ||
            pkg.contains("zee5") ||
            pkg.contains("prime") ||
            pkg.contains("amazonvideo") ||
            pkg.contains("disney") ||
            pkg.contains("jiocinema") ||
            pkg.contains("mxplayer") ||
            pkg.contains("plex") ||
            pkg.contains("jellyfin") ||
            pkg.contains("stremio") ||
            pkg.contains("youtube") ||
            pkg.contains("video")) {
            return true
        }
        return false
    }

    private fun recordVideoSession(context: Context, controller: MediaController) {
        try {
            val metadata = controller.metadata ?: return
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: return
            if (title.isBlank()) return

            val subtitle = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_AUTHOR)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)

            val artUri = metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

            val pkg = controller.packageName
            val pm = context.packageManager
            val appLabel = try {
                val ai = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                pkg
            }

            val pos = controller.playbackState?.position ?: 0L
            val dur = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

            val item = WatchNextItem(
                id = System.currentTimeMillis(),
                packageName = pkg,
                appLabel = appLabel,
                title = title,
                episodeTitle = subtitle,
                posterArtUri = artUri,
                lastPlaybackPositionMillis = pos,
                durationMillis = dur,
                lastEngagementTimeUtcMillis = System.currentTimeMillis(),
                intentUri = null
            )
            WatchNextRepository.saveLatestVideo(context, item)
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return !flat.isNullOrEmpty() && flat.contains(pkgName)
    }

    fun hasActiveSession(): Boolean {
        return activeController != null && currentTrack != null
    }

    private var lastCommandTimestamp = 0L

    /**
     * Transport control: Toggle play/pause
     */
    fun togglePlayPause(context: Context? = null) {
        val effectiveContext = context ?: appContext
        val now = SystemClock.uptimeMillis()
        if (now - lastCommandTimestamp < 150L) return
        lastCommandTimestamp = now

        val isCurrentlyPlaying = (activeController?.playbackState?.state == PlaybackState.STATE_PLAYING) || (currentTrack?.isPlaying == true)
        val newPlayingState = !isCurrentlyPlaying

        // 1. Optimistically update local track state so button icon toggles immediately
        currentTrack?.let { track ->
            setTrack(track.copy(isPlaying = newPlayingState))
        }

        // 2. Dispatch via active MediaController if available
        val controller = activeController
        if (controller != null) {
            try {
                if (isCurrentlyPlaying) {
                    controller.transportControls.pause()
                } else {
                    controller.transportControls.play()
                }
            } catch (e: Exception) {
                val key = if (isCurrentlyPlaying) KeyEvent.KEYCODE_MEDIA_PAUSE else KeyEvent.KEYCODE_MEDIA_PLAY
                try {
                    controller.dispatchMediaButtonEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0))
                    controller.dispatchMediaButtonEvent(KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, key, 0))
                } catch (e2: Exception) {}
            }
        } else {
            // 3. Fallback only when no active controller exists
            val targetKey = if (isCurrentlyPlaying) KeyEvent.KEYCODE_MEDIA_PAUSE else KeyEvent.KEYCODE_MEDIA_PLAY
            dispatchMediaKeyFallback(effectiveContext, targetKey)
        }

        scheduleSessionRefreshes(effectiveContext)
    }

    /**
     * Transport control: Skip to next track
     */
    fun skipToNext(context: Context? = null) {
        val effectiveContext = context ?: appContext
        val now = SystemClock.uptimeMillis()
        if (now - lastCommandTimestamp < 250L) return
        lastCommandTimestamp = now

        val controller = activeController
        if (controller != null) {
            try {
                controller.transportControls.skipToNext()
            } catch (e: Exception) {
                try {
                    controller.dispatchMediaButtonEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0))
                    controller.dispatchMediaButtonEvent(KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0))
                } catch (e2: Exception) {}
            }
        } else {
            dispatchMediaKeyFallback(effectiveContext, KeyEvent.KEYCODE_MEDIA_NEXT)
        }

        scheduleSessionRefreshes(effectiveContext)
    }

    /**
     * Transport control: Skip to previous track
     */
    fun skipToPrevious(context: Context? = null) {
        val effectiveContext = context ?: appContext
        val now = SystemClock.uptimeMillis()
        if (now - lastCommandTimestamp < 250L) return
        lastCommandTimestamp = now

        val controller = activeController
        if (controller != null) {
            try {
                controller.transportControls.skipToPrevious()
            } catch (e: Exception) {
                try {
                    controller.dispatchMediaButtonEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0))
                    controller.dispatchMediaButtonEvent(KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0))
                } catch (e2: Exception) {}
            }
        } else {
            dispatchMediaKeyFallback(effectiveContext, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }

        scheduleSessionRefreshes(effectiveContext)
    }

    private fun scheduleSessionRefreshes(context: Context?) {
        val ctx = context?.applicationContext ?: appContext ?: return
        mainHandler.postDelayed({
            NowPlayingListenerService.requestSessionRefresh(ctx)
            refreshFromCurrentController(ctx)
        }, 250)
        mainHandler.postDelayed({
            NowPlayingListenerService.requestSessionRefresh(ctx)
            refreshFromCurrentController(ctx)
        }, 600)
        mainHandler.postDelayed({
            NowPlayingListenerService.requestSessionRefresh(ctx)
            refreshFromCurrentController(ctx)
        }, 1200)
        mainHandler.postDelayed({
            NowPlayingListenerService.requestSessionRefresh(ctx)
            refreshFromCurrentController(ctx)
        }, 2000)
    }

    private fun dispatchMediaKeyFallback(context: Context?, keyCode: Int) {
        if (context == null) return
        val now = SystemClock.uptimeMillis()

        val targetPkg = currentTrack?.packageName?.takeIf { it.isNotEmpty() }

        // 1. Dedicated Spotify widget broadcasts if Spotify is source or installed
        val isSpotify = targetPkg == null || targetPkg.contains("spotify")
        if (isSpotify) {
            val spotifyAction = when (keyCode) {
                KeyEvent.KEYCODE_MEDIA_NEXT -> "com.spotify.mobile.android.ui.widget.NEXT"
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "com.spotify.mobile.android.ui.widget.PREVIOUS"
                KeyEvent.KEYCODE_MEDIA_PLAY -> "com.spotify.mobile.android.ui.widget.PLAY"
                KeyEvent.KEYCODE_MEDIA_PAUSE -> "com.spotify.mobile.android.ui.widget.PAUSE"
                else -> null
            }
            if (spotifyAction != null) {
                for (spPkg in listOf("com.spotify.tv.android", "com.spotify.music")) {
                    try {
                        val spotIntent = Intent(spotifyAction).apply {
                            setPackage(spPkg)
                            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                        }
                        context.sendBroadcast(spotIntent)
                    } catch (e: Exception) {}
                }
            }
        }

        // 2. Explicit Component Broadcasts to MediaButtonReceivers across target packages
        val pkgsToTry = mutableListOf<String>()
        if (!targetPkg.isNullOrEmpty()) {
            pkgsToTry.add(targetPkg)
        } else {
            pkgsToTry.addAll(listOf("com.spotify.tv.android", "com.spotify.music", "com.google.android.youtube.tvmusic", "com.google.android.apps.youtube.music"))
        }

        val pm = context.packageManager
        for (pkg in pkgsToTry) {
            try {
                val queryIntent = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(pkg)
                val receivers = pm.queryBroadcastReceivers(queryIntent, 0)
                for (ri in receivers) {
                    val comp = ComponentName(ri.activityInfo.packageName, ri.activityInfo.name)
                    val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                        component = comp
                        putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    context.sendBroadcast(downIntent)

                    val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                        component = comp
                        putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
                        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    }
                    context.sendBroadcast(upIntent)
                }
            } catch (e: Exception) {}
        }

        // 3. AudioManager system-wide dispatch
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager?.dispatchMediaKeyEvent(KeyEvent(now, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
        } catch (e: Exception) {}
    }

    /**
     * Sets sample or mock music track for verification on devices/emulators
     * where external music streaming apps are not actively playing.
     */
    fun setMockTrack(mock: NowPlayingData?) {
        setTrack(mock)
    }
}

