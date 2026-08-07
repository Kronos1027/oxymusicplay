package com.oxymusic.app.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.oxymusic.app.data.AudiusClient
import com.oxymusic.app.media.EqualizerManager
import com.oxymusic.app.media.PlaybackService
import com.oxymusic.app.media.VisualizerManager
import com.oxymusic.app.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audius: AudiusClient,
) : ViewModel() {

    val equalizer = EqualizerManager()
    val visualizer = VisualizerManager()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    fun connect() {
        if (controller != null) return
        val sessionToken = androidx.media3.session.SessionToken(
            context, ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
            future.addListener({
                try {
                    controller = future.get()
                    setupController()
                } catch (e: Exception) {
                    Log.e(TAG, "Controller connection failed", e)
                }
            }, androidx.core.content.ContextCompat.getMainExecutor(context))
        }
    }

    private fun setupController() {
        controller?.let { c ->
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { _isPlaying.value = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    _isBuffering.value = state == Player.STATE_BUFFERING
                    _durationMs.value = c.duration.takeIf { it > 0 } ?: 0L
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaMetadata?.let { meta ->
                        _currentTrack.value = Track(
                            id = mediaItem.mediaId,
                            title = meta.title?.toString() ?: "Unknown",
                            artist = meta.artist?.toString() ?: "Unknown",
                            artworkUrl = meta.artworkUri?.toString() ?: "",
                        )
                    }
                }
            })
            // Attach equalizer + visualizer
            val sid = com.oxymusic.app.media.PlaybackService.audioSessionId
            if (sid != 0) {
                equalizer.attach(sid)
                visualizer.attach(sid)
            }
            // Position ticker
            viewModelScope.launch {
                while (true) {
                    _positionMs.value = c.currentPosition
                    _durationMs.value = c.duration.takeIf { it > 0 } ?: 0L
                    delay(250)
                }
            }
        }
    }

    fun playTrack(track: Track) {
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(Uri.parse(track.artworkUrl))
                    .build()
            )
            .build()
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
        _currentTrack.value = track
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _queue.value = tracks
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setArtworkUri(Uri.parse(track.artworkUrl))
                        .build()
                )
                .build()
        }
        controller?.setMediaItems(items, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        _currentTrack.value = tracks[startIndex]
    }

    fun togglePlayPause() {
        if (controller?.isPlaying == true) controller?.pause() else controller?.play()
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }

    override fun onCleared() {
        super.onCleared()
        equalizer.release()
        visualizer.release()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    companion object { private const val TAG = "PlayerViewModel" }
}
