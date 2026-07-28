package com.oxymusic.app.media

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.oxymusic.app.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer direto — sem MediaController async, sem service.
 *
 * Uses OkHttpDataSource (more robust than DefaultHttpDataSource):
 * - Proper redirect handling (cross-protocol, cross-origin)
 * - Configurable User-Agent
 * - Better error reporting
 *
 * Reports detailed playback errors including HTTP status code and cause.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val player: ExoPlayer
    private var mediaSession: MediaSession?

    init {
        // OkHttp client with permissive redirect handling
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        // OkHttpDataSource — much better than DefaultHttpDataSource
        val httpDataSource = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("com.google.android.youtube/20.10.38 (Linux; U; Android 13)")
            .setDefaultRequestProperties(mapOf(
                "Accept" to "*/*",
                "Accept-Language" to "pt-BR,pt;q=0.9,en;q=0.8"
            ))

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSource)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()

        try { CurrentAudioSessionId.value = player.audioSessionId } catch (e: Exception) {}

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentTrack.value = mediaItem?.toTrack()
                _currentIndex.value = player.currentMediaItemIndex
            }
            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING
                _durationMs.value = player.duration.takeIf { it > 0 } ?: 0L
            }
            override fun onPlayerError(error: PlaybackException) {
                val detailedMsg = buildDetailedErrorMessage(error)
                Log.e(TAG, "Player error: $detailedMsg", error)
                _lastError.value = detailedMsg
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                CurrentAudioSessionId.value = audioSessionId
            }
        })

        mediaSession = try {
            MediaSession.Builder(context, player).build()
        } catch (e: Exception) { null }
    }

    /**
     * Builds a detailed error message including HTTP status code and root cause.
     */
    private fun buildDetailedErrorMessage(error: PlaybackException): String {
        val sb = StringBuilder()
        sb.append("Erro ExoPlayer: ${error.errorCodeName}")

        // Extract HTTP status code if available
        val cause = error.cause
        if (cause != null) {
            sb.append("\nCausa: ${cause.javaClass.simpleName}: ${cause.message ?: "sem mensagem"}")

            // For HttpDataSource.InvalidResponseCodeException, get the status code
            val innerCause = cause.cause
            if (innerCause != null) {
                sb.append("\nCausa interna: ${innerCause.javaClass.simpleName}: ${innerCause.message ?: ""}")
            }

            // Try to extract HTTP code from message
            val httpCodeMatch = Regex("""response code:\s*(\d+)""").find(cause.message ?: "")
            if (httpCodeMatch != null) {
                val code = httpCodeMatch.groupValues[1]
                sb.append("\nHTTP status: $code")
                when (code) {
                    "403" -> sb.append(" (Forbidden — URL expirada ou IP rejeitado)")
                    "404" -> sb.append(" (Not Found — URL inválida)")
                    "429" -> sb.append(" (Too Many Requests — rate limit)")
                    "5" + code.substring(1) -> sb.append(" (Server Error)")
                }
            }
        }

        sb.append("\nSource error: ${error.errorCodeName}")
        return sb.toString()
    }

    fun tickPosition() {
        try {
            _positionMs.value = player.currentPosition
            _durationMs.value = player.duration.takeIf { it > 0 } ?: 0L
        } catch (e: Exception) {}
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val items = tracks.map { it.toMediaItem() }
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        player.play()
        _queue.value = tracks
        _currentIndex.value = startIndex
        _currentTrack.value = tracks[startIndex]
        _lastError.value = null
    }

    fun playTrack(track: Track) { setQueue(listOf(track), 0) }

    fun play() { player.play() }
    fun pause() { player.pause() }
    fun next() { if (player.hasNextMediaItem()) player.seekToNextMediaItem() }
    fun previous() { if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { player.seekTo(positionMs) }

    fun audioSessionId(): Int = CurrentAudioSessionId.value

    private fun Track.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(thumbnailUrl.takeIf { it.isNotEmpty() }?.let { android.net.Uri.parse(it) })
            .build()
        // Sanitize stream URL — remove trailing & if any, ensure proper URL encoding
        val cleanUrl = streamUrl?.trim()?.let { if (it.endsWith("&")) it.dropLast(1) else it }
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(cleanUrl ?: id)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun MediaItem.toTrack(): Track {
        val m = mediaMetadata
        return Track(
            id = mediaId,
            title = m.title?.toString() ?: "Unknown",
            artist = m.artist?.toString() ?: "Unknown",
            thumbnailUrl = m.artworkUri?.toString() ?: "",
            streamUrl = playbackProperties?.uri?.toString(),
        )
    }

    companion object {
        private const val TAG = "PlaybackController"
    }
}

object CurrentAudioSessionId { @Volatile var value: Int = 0 }
