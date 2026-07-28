package com.oxymusic.app.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.oxymusic.app.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ExoPlayer direto — sem MediaController async, sem service.
 *
 * Configurado com HTTP DataSource que:
 * - Usa User-Agent "com.google.android.youtube" (algumas fontes exigem)
 * - Permite redirects cross-protocol (HTTP→HTTPS)
 * - Permite cross-origin redirects (YouTube usa CDN com redirect)
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
        // HTTP DataSource with proper User-Agent and cross-protocol redirects
        // (YouTube CDN uses HTTP→HTTPS redirects that ExoPlayer blocks by default)
        val httpDataSource = DefaultHttpDataSource.Factory()
            .setUserAgent("com.google.android.youtube/20.10.38 (Linux; U; Android 13)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        // Combine with file DataSource for local files
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSource)

        // Build ExoPlayer with custom MediaSourceFactory that uses our DataSource
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
                _lastError.value = "Erro ExoPlayer: ${error.errorCodeName} — ${error.message}"
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                CurrentAudioSessionId.value = audioSessionId
            }
        })

        mediaSession = try {
            MediaSession.Builder(context, player).build()
        } catch (e: Exception) { null }
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
        // Sanitize stream URL — remove trailing & if any
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
}

object CurrentAudioSessionId { @Volatile var value: Int = 0 }
