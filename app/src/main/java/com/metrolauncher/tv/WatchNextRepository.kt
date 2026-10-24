package com.metrolauncher.tv

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

object WatchNextRepository {

    private const val PREFS_WATCH_NEXT = "metro_latest_watch_next"
    private const val KEY_ID = "video_id"
    private const val KEY_PACKAGE = "video_package"
    private const val KEY_APP_LABEL = "video_app_label"
    private const val KEY_TITLE = "video_title"
    private const val KEY_EPISODE_TITLE = "video_episode_title"
    private const val KEY_ART_URI = "video_art_uri"
    private const val KEY_POSITION = "video_position"
    private const val KEY_DURATION = "video_duration"
    private const val KEY_TIME = "video_time"
    private const val KEY_INTENT_URI = "video_intent_uri"

    private const val MAX_ENTRIES = 10
    private val WATCH_NEXT_URI = Uri.parse("content://android.media.tv/watch_next_program")
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<WeakReference<(WatchNextItem?) -> Unit>>()

    var latestVideoItem: WatchNextItem? = null
        private set

    fun addListener(listener: (WatchNextItem?) -> Unit) {
        listeners.add(WeakReference(listener))
        listener(latestVideoItem)
    }

    fun saveLatestVideo(context: Context, item: WatchNextItem) {
        latestVideoItem = item
        try {
            context.getSharedPreferences(PREFS_WATCH_NEXT, Context.MODE_PRIVATE).edit().apply {
                putLong(KEY_ID, item.id)
                putString(KEY_PACKAGE, item.packageName)
                putString(KEY_APP_LABEL, item.appLabel)
                putString(KEY_TITLE, item.title)
                putString(KEY_EPISODE_TITLE, item.episodeTitle)
                putString(KEY_ART_URI, item.posterArtUri)
                putLong(KEY_POSITION, item.lastPlaybackPositionMillis)
                putLong(KEY_DURATION, item.durationMillis)
                putLong(KEY_TIME, item.lastEngagementTimeUtcMillis)
                putString(KEY_INTENT_URI, item.intentUri)
                apply()
            }
        } catch (e: Exception) {}

        mainHandler.post {
            for (ref in listeners) {
                ref.get()?.invoke(item)
            }
        }
    }

    fun getLatestSavedVideo(context: Context): WatchNextItem? {
        if (!LauncherConfig.isContinueWatchingEnabled(context)) return null
        if (latestVideoItem != null) return latestVideoItem

        try {
            val sp = context.getSharedPreferences(PREFS_WATCH_NEXT, Context.MODE_PRIVATE)
            val title = sp.getString(KEY_TITLE, null) ?: return null
            val pkg = sp.getString(KEY_PACKAGE, null) ?: return null

            val item = WatchNextItem(
                id = sp.getLong(KEY_ID, 1L),
                packageName = pkg,
                appLabel = sp.getString(KEY_APP_LABEL, "Video") ?: "Video",
                title = title,
                episodeTitle = sp.getString(KEY_EPISODE_TITLE, null),
                posterArtUri = sp.getString(KEY_ART_URI, null),
                lastPlaybackPositionMillis = sp.getLong(KEY_POSITION, 0L),
                durationMillis = sp.getLong(KEY_DURATION, 0L),
                lastEngagementTimeUtcMillis = sp.getLong(KEY_TIME, System.currentTimeMillis()),
                intentUri = sp.getString(KEY_INTENT_URI, null)
            )
            latestVideoItem = item
            return item
        } catch (e: Exception) {
            return null
        }
    }

    // Image memory cache: max 15MB
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024 * 4)
    private val artworkCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    // Packages to exclude from video Continue Watching (music streaming)
    private val EXCLUDED_MUSIC_PACKAGES = setOf(
        "com.spotify.music",
        "com.spotify.tv.android",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.aspiro.tidal",
        "deezer.android.app",
        "com.pandora.android",
        "tunein.player",
        "com.auramusic",
        "com.android.music"
    )

    /**
     * Loads the single most recently watched video across supported video apps.
     */
    fun loadLatestSingleVideo(context: Context, callback: (WatchNextItem?) -> Unit) {
        val saved = getLatestSavedVideo(context)
        if (saved != null) {
            callback(saved)
        }

        Thread {
            val pm = context.packageManager
            var latestItem: WatchNextItem? = null

            try {
                val projection = arrayOf(
                    "_id",
                    "package_name",
                    "title",
                    "episode_title",
                    "poster_art_uri",
                    "thumbnail_uri",
                    "last_playback_position_millis",
                    "duration_millis",
                    "last_engagement_time_utc_millis",
                    "intent_uri"
                )

                val cursor = try {
                    context.contentResolver.query(
                        WATCH_NEXT_URI,
                        projection,
                        null,
                        null,
                        "last_engagement_time_utc_millis DESC"
                    )
                } catch (e: Exception) {
                    null
                }

                if (cursor != null) {
                    val idCol = cursor.getColumnIndex("_id")
                    val pkgCol = cursor.getColumnIndex("package_name")
                    val titleCol = cursor.getColumnIndex("title")
                    val epCol = cursor.getColumnIndex("episode_title")
                    val posterCol = cursor.getColumnIndex("poster_art_uri")
                    val thumbCol = cursor.getColumnIndex("thumbnail_uri")
                    val posCol = cursor.getColumnIndex("last_playback_position_millis")
                    val durCol = cursor.getColumnIndex("duration_millis")
                    val timeCol = cursor.getColumnIndex("last_engagement_time_utc_millis")
                    val intentCol = cursor.getColumnIndex("intent_uri")

                    while (cursor.moveToNext()) {
                        val pkg = if (pkgCol != -1) cursor.getString(pkgCol) ?: "" else ""
                        val title = if (titleCol != -1) cursor.getString(titleCol) ?: "" else ""
                        if (pkg.isEmpty() || title.isEmpty()) continue
                        if (EXCLUDED_MUSIC_PACKAGES.contains(pkg.lowercase()) || pkg.lowercase().contains("music")) continue

                        val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                        val epTitle = if (epCol != -1) cursor.getString(epCol) else null
                        val posterUri = if (posterCol != -1) cursor.getString(posterCol) else null
                        val thumbUri = if (thumbCol != -1) cursor.getString(thumbCol) else null
                        val bestArtUri = when {
                            !thumbUri.isNullOrEmpty() -> thumbUri
                            !posterUri.isNullOrEmpty() -> posterUri
                            id > 0L -> "content://android.media.tv/watch_next_program/$id/thumbnail"
                            else -> null
                        }
                        val pos = if (posCol != -1) cursor.getLong(posCol) else 0L
                        val dur = if (durCol != -1) cursor.getLong(durCol) else 0L
                        val time = if (timeCol != -1) cursor.getLong(timeCol) else System.currentTimeMillis()
                        val intentUri = if (intentCol != -1) cursor.getString(intentCol) else null

                        val appLabel = try {
                            val ai = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(ai).toString()
                        } catch (e: Exception) {
                            pkg
                        }

                        latestItem = WatchNextItem(
                            id = id,
                            packageName = pkg,
                            appLabel = appLabel,
                            title = title,
                            episodeTitle = epTitle,
                            posterArtUri = bestArtUri,
                            cachedArtwork = bestArtUri?.let { artworkCache.get(it) },
                            lastPlaybackPositionMillis = pos,
                            durationMillis = dur,
                            lastEngagementTimeUtcMillis = time,
                            intentUri = intentUri
                        )
                        break
                    }
                    cursor.close()
                }
            } catch (e: Exception) {}

            if (latestItem != null) {
                val currentSaved = getLatestSavedVideo(context)
                if (currentSaved == null || latestItem.lastEngagementTimeUtcMillis >= currentSaved.lastEngagementTimeUtcMillis) {
                    saveLatestVideo(context, latestItem)
                }
            }

            mainHandler.post {
                callback(getLatestSavedVideo(context))
            }
        }.start()
    }

    /**
     * Loads unified continue watching items from Android TV WatchNextPrograms content provider.
     * Guaranteed: max 10 entries, newest first, deduplicated, excluding music sessions.
     */
    fun loadWatchNext(context: Context, callback: (List<WatchNextItem>) -> Unit) {
        Thread {
            val items = mutableListOf<WatchNextItem>()
            val pm = context.packageManager

            try {
                val projection = arrayOf(
                    "_id",
                    "package_name",
                    "title",
                    "episode_title",
                    "poster_art_uri",
                    "thumbnail_uri",
                    "last_playback_position_millis",
                    "duration_millis",
                    "last_engagement_time_utc_millis",
                    "intent_uri"
                )

                val cursor = try {
                    context.contentResolver.query(
                        WATCH_NEXT_URI,
                        projection,
                        null,
                        null,
                        "last_engagement_time_utc_millis DESC"
                    )
                } catch (e: Exception) {
                    null
                }

                if (cursor != null) {
                    val idCol = cursor.getColumnIndex("_id")
                    val pkgCol = cursor.getColumnIndex("package_name")
                    val titleCol = cursor.getColumnIndex("title")
                    val epCol = cursor.getColumnIndex("episode_title")
                    val posterCol = cursor.getColumnIndex("poster_art_uri")
                    val thumbCol = cursor.getColumnIndex("thumbnail_uri")
                    val posCol = cursor.getColumnIndex("last_playback_position_millis")
                    val durCol = cursor.getColumnIndex("duration_millis")
                    val timeCol = cursor.getColumnIndex("last_engagement_time_utc_millis")
                    val intentCol = cursor.getColumnIndex("intent_uri")

                    val seenKeys = HashSet<String>()

                    while (cursor.moveToNext()) {
                        val pkg = if (pkgCol != -1) cursor.getString(pkgCol) ?: "" else ""
                        val title = if (titleCol != -1) cursor.getString(titleCol) ?: "" else ""

                        if (pkg.isEmpty() || title.isEmpty()) continue

                        // Filter out music sessions from viewing history
                        if (EXCLUDED_MUSIC_PACKAGES.contains(pkg.lowercase()) || pkg.lowercase().contains("music")) {
                            continue
                        }

                        // Deduplication key: package + title
                        val dedupeKey = "$pkg::$title"
                        if (seenKeys.contains(dedupeKey)) continue
                        seenKeys.add(dedupeKey)

                        val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                        val epTitle = if (epCol != -1) cursor.getString(epCol) else null
                        val posterUri = if (posterCol != -1) cursor.getString(posterCol) else null
                        val thumbUri = if (thumbCol != -1) cursor.getString(thumbCol) else null
                        val bestArtUri = when {
                            !thumbUri.isNullOrEmpty() -> thumbUri
                            !posterUri.isNullOrEmpty() -> posterUri
                            id > 0L -> "content://android.media.tv/watch_next_program/$id/thumbnail"
                            else -> null
                        }
                        val pos = if (posCol != -1) cursor.getLong(posCol) else 0L
                        val dur = if (durCol != -1) cursor.getLong(durCol) else 0L
                        val time = if (timeCol != -1) cursor.getLong(timeCol) else System.currentTimeMillis()
                        val intentUri = if (intentCol != -1) cursor.getString(intentCol) else null

                        val appLabel = try {
                            val ai = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(ai).toString()
                        } catch (e: Exception) {
                            pkg
                        }

                        items.add(
                            WatchNextItem(
                                id = id,
                                packageName = pkg,
                                appLabel = appLabel,
                                title = title,
                                episodeTitle = epTitle,
                                posterArtUri = bestArtUri,
                                cachedArtwork = bestArtUri?.let { artworkCache.get(it) },
                                lastPlaybackPositionMillis = pos,
                                durationMillis = dur,
                                lastEngagementTimeUtcMillis = time,
                                intentUri = intentUri
                            )
                        )

                        if (items.size >= MAX_ENTRIES) break
                    }
                    cursor.close()
                }
            } catch (e: Exception) {}

            Handler(Looper.getMainLooper()).post {
                callback(items)
            }
        }.start()
    }

    /**
     * Loads artwork for an item asynchronously and caches it in memory.
     */
    fun loadArtwork(context: Context, uriStr: String, callback: (Bitmap?) -> Unit) {
        val cached = artworkCache.get(uriStr)
        if (cached != null) {
            callback(cached)
            return
        }

        Thread {
            var bitmap: Bitmap? = null
            try {
                if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                    val conn = URL(uriStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 6000
                    conn.readTimeout = 6000
                    conn.requestMethod = "GET"
                    if (conn.responseCode == 200) {
                        bitmap = BitmapFactory.decodeStream(conn.inputStream)
                    }
                } else {
                    val uri = Uri.parse(uriStr)
                    var inputStream: InputStream? = try {
                        context.contentResolver.openInputStream(uri)
                    } catch (e: Exception) {
                        null
                    }
                    if (inputStream == null && uriStr.endsWith("/thumbnail")) {
                        val fallbackUri = Uri.parse(uriStr.removeSuffix("/thumbnail") + "/poster_art")
                        inputStream = try {
                            context.contentResolver.openInputStream(fallbackUri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (inputStream != null) {
                        bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                    }
                }

                if (bitmap != null) {
                    artworkCache.put(uriStr, bitmap)
                }
            } catch (e: Exception) {}

            Handler(Looper.getMainLooper()).post {
                callback(bitmap)
            }
        }.start()
    }

    /**
     * Resumes playback across compatible viewing apps.
     */
    fun resumePlayback(context: Context, item: WatchNextItem): Boolean {
        // Try intent URI first
        if (!item.intentUri.isNullOrEmpty()) {
            try {
                val intent = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {}
        }

        // Fallback to package launch intent
        val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName)
        if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }
}
