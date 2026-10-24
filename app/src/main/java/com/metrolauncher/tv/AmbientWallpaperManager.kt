package com.metrolauncher.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WallpaperEntry(
    val date: String,
    val title: String,
    val copyright: String,
    val localFileName: String,
    val url: String
)

object AmbientWallpaperManager {

    private const val PREFS_NAME = "ambient_wallpaper_prefs"
    private const val KEY_METADATA = "wallpapers_metadata"
    private const val KEY_OFFLINE_INDEX = "offline_cycle_index"
    private const val KEY_LAST_OFFLINE_DAY = "last_offline_day"
    private const val MAX_WALLPAPERS = 7

    /**
     * Syncs with Bing Daily Wallpaper API asynchronously.
     * Retains a rolling 7-day cache, stores images and metadata offline.
     */
    fun syncWallpapers(context: Context, onComplete: (() -> Unit)? = null) {
        Thread {
            try {
                if (!isOnline(context)) {
                    Handler(Looper.getMainLooper()).post { onComplete?.invoke() }
                    return@Thread
                }

                val apiUrl = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=7&mkt=en-US"
                val conn = URL(apiUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonStr)
                    val imagesArr = json.getJSONArray("images")

                    val existingList = loadMetadata(context).toMutableList()
                    val wallpaperDir = File(context.filesDir, "ambient_wallpapers")
                    if (!wallpaperDir.exists()) wallpaperDir.mkdirs()

                    val newlyAdded = mutableListOf<WallpaperEntry>()

                    for (i in 0 until imagesArr.length()) {
                        val imgObj = imagesArr.getJSONObject(i)
                        val date = imgObj.optString("enddate")
                        val title = imgObj.optString("title", "Bing Daily Wallpaper")
                        val copyright = imgObj.optString("copyright", "")
                        val relUrl = imgObj.optString("url")
                        val fullUrl = if (relUrl.startsWith("http")) relUrl else "https://www.bing.com$relUrl"

                        val fileName = "bing_$date.jpg"
                        val localFile = File(wallpaperDir, fileName)

                        // Download only if not already cached
                        if (!localFile.exists() || localFile.length() == 0L) {
                            try {
                                downloadFile(fullUrl, localFile)
                            } catch (e: Exception) {
                                continue
                            }
                        }

                        if (localFile.exists() && localFile.length() > 0L) {
                            newlyAdded.add(
                                WallpaperEntry(
                                    date = date,
                                    title = title,
                                    copyright = copyright,
                                    localFileName = fileName,
                                    url = fullUrl
                                )
                            )
                        }
                    }

                    // Merge and deduplicate by date
                    val mergedMap = LinkedHashMap<String, WallpaperEntry>()
                    for (entry in newlyAdded) {
                        mergedMap[entry.date] = entry
                    }
                    for (entry in existingList) {
                        if (!mergedMap.containsKey(entry.date)) {
                            val f = File(wallpaperDir, entry.localFileName)
                            if (f.exists() && f.length() > 0L) {
                                mergedMap[entry.date] = entry
                            }
                        }
                    }

                    // Sort descending by date (newest first) and keep up to 7
                    val sortedEntries = mergedMap.values.sortedByDescending { it.date }
                    val trimmed = sortedEntries.take(MAX_WALLPAPERS)

                    // Remove files outside the 7-day rolling window
                    val activeFileNames = trimmed.map { it.localFileName }.toSet()
                    wallpaperDir.listFiles()?.forEach { file ->
                        if (!activeFileNames.contains(file.name)) {
                            file.delete()
                        }
                    }

                    saveMetadata(context, trimmed)
                }
            } catch (e: Exception) {
                // Fail gracefully without crashing
            } finally {
                Handler(Looper.getMainLooper()).post { onComplete?.invoke() }
            }
        }.start()
    }

    /**
     * Resolves the wallpaper to display:
     * - When online: returns newest cached daily wallpaper.
     * - When offline: cycles reverse-chronologically through the 7-day cache
     *   (newest to oldest, 1 per day, then wrap around).
     * - Returns null if cache is empty (caller falls back to bundled background).
     */
    fun resolveActiveWallpaper(context: Context): Pair<WallpaperEntry, Bitmap>? {
        val list = loadMetadata(context)
        if (list.isEmpty()) return null

        val wallpaperDir = File(context.filesDir, "ambient_wallpapers")
        val online = isOnline(context)

        val entryToUse: WallpaperEntry = if (online) {
            list[0]
        } else {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val lastDay = prefs.getString(KEY_LAST_OFFLINE_DAY, "")
            var idx = prefs.getInt(KEY_OFFLINE_INDEX, 0)

            if (lastDay != todayDateStr) {
                // Next day offline: move backward in history (newest -> oldest)
                idx = (idx + 1) % list.size
                prefs.edit()
                    .putString(KEY_LAST_OFFLINE_DAY, todayDateStr)
                    .putInt(KEY_OFFLINE_INDEX, idx)
                    .apply()
            }

            val safeIdx = if (idx in list.indices) idx else 0
            list[safeIdx]
        }

        val file = File(wallpaperDir, entryToUse.localFileName)
        if (file.exists() && file.length() > 0L) {
            val bitmap = decodeOptimizedBitmap(file.absolutePath)
            if (bitmap != null) {
                return Pair(entryToUse, bitmap)
            }
        }

        // Fallback to any valid cached image
        for (item in list) {
            val f = File(wallpaperDir, item.localFileName)
            if (f.exists() && f.length() > 0L) {
                val b = decodeOptimizedBitmap(f.absolutePath)
                if (b != null) return Pair(item, b)
            }
        }

        return null
    }

    private fun decodeOptimizedBitmap(path: String): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        var sample = 1
        if (options.outWidth > 1920 || options.outHeight > 1080) {
            sample = 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return try {
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun downloadFile(urlStr: String, dest: File) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        if (conn.responseCode == 200) {
            val tempFile = File(dest.parentFile, "${dest.name}.tmp")
            conn.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 0L) {
                tempFile.renameTo(dest)
            }
        }
    }

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    private fun loadMetadata(context: Context): List<WallpaperEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_METADATA, null) ?: return emptyList()
        val result = mutableListOf<WallpaperEntry>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    WallpaperEntry(
                        date = obj.getString("date"),
                        title = obj.getString("title"),
                        copyright = obj.optString("copyright", ""),
                        localFileName = obj.getString("localFileName"),
                        url = obj.optString("url", "")
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore format errors
        }
        return result
    }

    private fun saveMetadata(context: Context, entries: List<WallpaperEntry>) {
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject()
            obj.put("date", entry.date)
            obj.put("title", entry.title)
            obj.put("copyright", entry.copyright)
            obj.put("localFileName", entry.localFileName)
            obj.put("url", entry.url)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_METADATA, arr.toString())
            .apply()
    }
}
