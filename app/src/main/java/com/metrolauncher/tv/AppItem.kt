package com.metrolauncher.tv

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils

data class AppItem(
    val packageName: String,
    val activityName: String?,
    val label: String,
    val systemIcon: Drawable,
    val brandLogoResId: Int?,
    val solidColor: Int
) {
    /**
     * Contrasting text color based on background luminance:
     * Dark text (#212121) on light tiles (YouTube, Netflix, Google Play),
     * Light text (#FFFFFF) on dark tiles (Prime Video, VLC, Music, Files, etc.)
     */
    val textColor: Int
        get() {
            val luminance = ColorUtils.calculateLuminance(solidColor)
            return if (luminance > 0.5) Color.parseColor("#212121") else Color.WHITE
        }

    companion object {
        private val METRO_SOLID_PALETTE = intArrayOf(
            Color.parseColor("#1E5799"), // Prime Blue
            Color.parseColor("#FA5B0F"), // VLC Orange
            Color.parseColor("#008299"), // Music Cyan
            Color.parseColor("#008A00"), // Files Green
            Color.parseColor("#5A4582"), // Stremio Purple
            Color.parseColor("#00695C"), // Teal
            Color.parseColor("#0078D7"), // Cobalt Blue
            Color.parseColor("#D83B01"), // Coral Orange
            Color.parseColor("#107C41"), // Xbox Green
            Color.parseColor("#68217A"), // Purple
            Color.parseColor("#1B2A4A")  // Navy
        )

        fun resolveSolidColor(packageName: String): Int {
            val lower = packageName.lowercase()
            return when {
                lower.contains("youtube") -> Color.parseColor("#FFFFFF") // Solid White as specified
                lower.contains("netflix") -> Color.parseColor("#FFFFFF") // Solid White as specified
                lower.contains("amazon") || lower.contains("prime") -> Color.parseColor("#1E5799") // Solid Blue
                lower.contains("vlc") -> Color.parseColor("#FA5B0F") // Solid Orange
                lower.contains("vending") || lower.contains("play") -> Color.parseColor("#FFFFFF") // Solid White for Google Play
                lower.contains("auramusic") || lower.contains("music") -> Color.parseColor("#008299") // Solid Cyan
                lower.contains("file") || lower.contains("storage") -> Color.parseColor("#008A00") // Solid Emerald
                lower.contains("stremio") -> Color.parseColor("#5A4582") // Solid Purple
                lower.contains("tunein") -> Color.parseColor("#1B2A4A")
                lower.contains("yablio") || lower.contains("sendfiles") -> Color.parseColor("#00695C")
                else -> {
                    val idx = Math.abs(packageName.hashCode()) % METRO_SOLID_PALETTE.size
                    METRO_SOLID_PALETTE[idx]
                }
            }
        }

        fun resolveBrandLogo(packageName: String): Int? {
            val lower = packageName.lowercase()
            return when {
                // User requirement: use apps default icon for VLC, Stremio, and Aura Music
                lower.contains("vlc") -> null
                lower.contains("stremio") -> null
                lower.contains("auramusic") -> null

                lower.contains("youtube") && lower.contains("music") -> R.drawable.ic_projectivy_yt_music
                lower.contains("youtube") -> R.drawable.ic_projectivy_youtube
                lower.contains("netflix") -> R.drawable.ic_projectivy_netflix
                lower.contains("amazon") && lower.contains("music") -> R.drawable.ic_projectivy_amazon_music
                lower.contains("amazon") || lower.contains("prime") -> R.drawable.ic_projectivy_prime
                lower.contains("spotify") -> R.drawable.ic_projectivy_spotify
                lower.contains("sendfiles") || lower.contains("yablio") -> R.drawable.ic_projectivy_sendfiles
                lower.contains("vending") -> R.drawable.ic_projectivy_playstore
                lower.contains("play.games") -> R.drawable.ic_projectivy_playgames
                lower.contains("aurora") -> R.drawable.ic_projectivy_aurora_store
                lower.contains("mxplayer") || lower.contains("videoplayer.ad") -> R.drawable.ic_projectivy_mx_player
                lower.contains("novavideo") -> R.drawable.ic_projectivy_nova_player
                lower.contains("appletv") -> R.drawable.ic_projectivy_apple_tv
                lower.contains("apple") && lower.contains("music") -> R.drawable.ic_projectivy_apple_music
                lower.contains("mitv.videoplayer") || lower.contains("gallery") -> R.drawable.ic_projectivy_xiaomi_player
                lower.contains("file") || lower.contains("storage") || lower.contains("mediaexplorer") -> R.drawable.ic_projectivy_files
                lower.contains("browser") || lower.contains("chrome") -> R.drawable.ic_projectivy_browser
                else -> null // Uses default system icon
            }
        }
    }
}
