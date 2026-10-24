package com.metrolauncher.tv

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * System notification and media session listener service.
 * Enables Android TV to receive real-time active MediaSession callbacks
 * from supported music apps (Spotify, YouTube Music, etc.).
 */
class NowPlayingListenerService : NotificationListenerService() {

    private var mediaSessionManager: MediaSessionManager? = null
    private var componentName: ComponentName? = null

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        NowPlayingManager.onActiveSessionsChanged(applicationContext, controllers)
    }

    override fun onCreate() {
        super.onCreate()
        componentName = ComponentName(this, NowPlayingListenerService::class.java)
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    }

    companion object {
        var instance: NowPlayingListenerService? = null
            private set

        fun requestSessionRefresh(context: Context) {
            val s = instance
            if (s != null) {
                try {
                    val notifs = s.activeNotifications
                    if (notifs != null) {
                        for (sbn in notifs) {
                            val extras = sbn?.notification?.extras
                            @Suppress("DEPRECATION")
                            val token = extras?.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
                            if (token != null) {
                                val directController = MediaController(s.applicationContext, token)
                                if (NowPlayingManager.isMusicSession(directController)) {
                                    NowPlayingManager.onActiveSessionsChanged(context, listOf(directController))
                                    return
                                }
                            }
                        }
                    }

                    val comp = ComponentName(s, NowPlayingListenerService::class.java)
                    val msm = s.mediaSessionManager ?: (s.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager)
                    val controllers = msm?.getActiveSessions(comp)
                    if (!controllers.isNullOrEmpty()) {
                        NowPlayingManager.onActiveSessionsChanged(context, controllers)
                    }
                } catch (e: Exception) {
                    // Safe fallback
                }
            } else {
                try {
                    val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    val comp = ComponentName(context, NowPlayingListenerService::class.java)
                    val controllers = msm?.getActiveSessions(comp)
                    if (!controllers.isNullOrEmpty()) {
                        NowPlayingManager.onActiveSessionsChanged(context, controllers)
                    }
                } catch (e: Exception) {
                    // Safe fallback
                }
            }
            NowPlayingManager.refreshFromCurrentController(context)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        try {
            val comp = componentName ?: ComponentName(this, NowPlayingListenerService::class.java)
            mediaSessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, comp)

            // 1. Check active notifications directly for media tokens
            val activeNotifs = activeNotifications
            var foundDirectMusic = false
            if (activeNotifs != null) {
                for (sbn in activeNotifs) {
                    val extras = sbn?.notification?.extras
                    @Suppress("DEPRECATION")
                    val token = extras?.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
                    if (token != null) {
                        try {
                            val directController = MediaController(applicationContext, token)
                            if (NowPlayingManager.isMusicSession(directController)) {
                                NowPlayingManager.onActiveSessionsChanged(applicationContext, listOf(directController))
                                foundDirectMusic = true
                                break
                            }
                        } catch (e: Exception) {
                            // Safe fallback
                        }
                    }
                }
            }

            // 2. Query MediaSessionManager for active sessions
            if (!foundDirectMusic) {
                val initialSessions = mediaSessionManager?.getActiveSessions(comp)
                NowPlayingManager.onActiveSessionsChanged(applicationContext, initialSessions)
            }
        } catch (e: SecurityException) {
            // Permission not yet enabled by user in settings
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        try {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            // 1. Direct extraction from MediaStyle notification token (captures YT Music & Spotify immediately)
            val extras = sbn?.notification?.extras
            @Suppress("DEPRECATION")
            val token = extras?.getParcelable<android.media.session.MediaSession.Token>(android.app.Notification.EXTRA_MEDIA_SESSION)
            if (token != null) {
                try {
                    val directController = MediaController(applicationContext, token)
                    if (NowPlayingManager.isMusicSession(directController)) {
                        NowPlayingManager.onActiveSessionsChanged(applicationContext, listOf(directController))
                        return
                    }
                } catch (e: Exception) {
                    // Safe fallback
                }
            }

            // 2. Query MediaSessionManager for all active sessions
            val comp = componentName ?: ComponentName(this, NowPlayingListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(comp)
            NowPlayingManager.onActiveSessionsChanged(applicationContext, controllers)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Refresh active media sessions if media notification is removed
        try {
            val comp = componentName ?: ComponentName(this, NowPlayingListenerService::class.java)
            val controllers = mediaSessionManager?.getActiveSessions(comp)
            NowPlayingManager.onActiveSessionsChanged(applicationContext, controllers)
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
