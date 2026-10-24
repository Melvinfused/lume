# Optimization and obfuscation rules for Lume

# Keep Leanback widgets and attributes
-keep class androidx.leanback.** { *; }
-keep interface androidx.leanback.** { *; }

# Keep App Models
-keep class com.metrolauncher.tv.AppItem { *; }
-keep class com.metrolauncher.tv.NowPlayingData { *; }
-keep class com.metrolauncher.tv.WatchNextItem { *; }
-keep class com.metrolauncher.tv.WallpaperEntry { *; }

# Keep custom managers and services
-keep class com.metrolauncher.tv.NowPlayingListenerService { *; }

# Keep AndroidX core, recyclerview, cardview
-keep class androidx.recyclerview.widget.** { *; }
-keep class androidx.cardview.widget.** { *; }
-dontwarn androidx.leanback.**
