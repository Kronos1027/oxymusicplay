package com.oxymusic.app.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.oxymusic.app.MainActivity
import com.oxymusic.app.R
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * PlaybackService — Media3 MediaSessionService.
 *
 * Creates ExoPlayer + MediaSession. Android AUTOMATICALLY generates:
 * - Lock screen controls (play/pause/next/prev + artwork)
 * - Notification with MediaStyle (custom layout, artwork, controls)
 * - Media button handling (headphones, Bluetooth, Android Auto)
 *
 * No manual notification code needed — Media3 handles it all.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    companion object {
        @Volatile var audioSessionId: Int = 0
    }

    override fun onCreate() {
        super.onCreate()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        val httpDataSource = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("OxyMusic/3.0 (Android)")
            .setDefaultRequestProperties(mapOf("Accept" to "*/*"))

        val dataSourceFactory = DefaultDataSource.Factory(this, httpDataSource)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        // Expose audio session ID for equalizer/visualizer
        audioSessionId = player.audioSessionId
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(sessionId: Int) {
                audioSessionId = sessionId
            }
        })

        // PendingIntent for notification click → opens app
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
