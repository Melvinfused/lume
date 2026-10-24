package com.metrolauncher.tv

import android.graphics.Bitmap

/**
 * Model representing a unified Continue Watching / Watch Next item.
 * Strictly video/viewing content; excludes music sessions.
 */
data class WatchNextItem(
    val id: Long,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val episodeTitle: String?,
    val posterArtUri: String?,
    var cachedArtwork: Bitmap? = null,
    val lastPlaybackPositionMillis: Long,
    val durationMillis: Long,
    val lastEngagementTimeUtcMillis: Long,
    val intentUri: String?
) {
    /**
     * Percentage progress clamped between 0 and 100.
     */
    val progressPercent: Int
        get() {
            if (durationMillis <= 0 || lastPlaybackPositionMillis <= 0) return 0
            val pct = ((lastPlaybackPositionMillis.toDouble() / durationMillis.toDouble()) * 100).toInt()
            return pct.coerceIn(0, 100)
        }
}
