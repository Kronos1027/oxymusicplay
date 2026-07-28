package com.oxymusic.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.HistoryDao
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.lyrics.LrclibClient
import com.oxymusic.app.media.CurrentAudioSessionId
import com.oxymusic.app.media.PlaybackController
import com.oxymusic.app.media.VisualizerManager
import com.oxymusic.app.model.Lyrics
import com.oxymusic.app.model.Track
import com.oxymusic.app.network.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val playback: PlaybackController,
    val visualizer: VisualizerManager,
    private val youtube: YouTubeRepository,
    private val lrclib: LrclibClient,
    private val historyDao: HistoryDao,
) : ViewModel() {

    val isPlaying = playback.isPlaying.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val currentTrack = playback.currentTrack.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val positionMs = playback.positionMs.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val durationMs = playback.durationMs.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val isBuffering = playback.isBuffering.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _resolving = MutableStateFlow(false)
    val resolving: StateFlow<Boolean> = _resolving.asStateFlow()

    private val _resolvingSource = MutableStateFlow<String?>(null)
    val resolvingSource: StateFlow<String?> = _resolvingSource.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _mascotMessage = MutableStateFlow<String?>(null)
    val mascotMessage: StateFlow<String?> = _mascotMessage.asStateFlow()

    private val _lastSource = MutableStateFlow<String?>(null)
    val lastSource: StateFlow<String?> = _lastSource.asStateFlow()

    private val _debugLog = MutableStateFlow<String>("")
    val debugLog: StateFlow<String> = _debugLog.asStateFlow()

    private fun log(msg: String) {
        Log.i(TAG, msg)
        _debugLog.value = _debugLog.value + "\n" + msg
    }

    init {
        // Position ticker
        viewModelScope.launch {
            while (true) { playback.tickPosition(); delay(250) }
        }
        // Load lyrics when track changes
        viewModelScope.launch {
            currentTrack.collect { t -> if (t != null) loadLyrics(t) }
        }
        // Auto-attach visualizer when audio session ID becomes available
        viewModelScope.launch {
            var attached = false
            while (!attached) {
                delay(500)
                val sid = CurrentAudioSessionId.value
                if (sid != 0) { visualizer.attach(sid); attached = true; log("Visualizer attached to session $sid") }
            }
        }
        // Forward playback errors to errorMessage
        viewModelScope.launch {
            playback.lastError.collect { e ->
                if (e != null) {
                    log("PLAYBACK ERROR: $e")
                    _errorMessage.value = e
                }
            }
        }
    }

    fun playTrack(track: Track) {
        log("▶ playTrack: ${track.title} - ${track.artist} (id=${track.id})")
        viewModelScope.launch {
            _resolving.value = true
            _resolvingSource.value = "Innertube → NewPipe → Piped"
            _errorMessage.value = null
            _mascotMessage.value = "Resolvendo stream… ⏳"
            try {
                log("Calling youtube.resolveStream...")
                val result = youtube.resolveStream(track)
                log("resolveStream result: success=${result.success} source=${result.source} streamUrl=${if (result.track.streamUrl.isNullOrEmpty()) "NULL" else "OK len=${result.track.streamUrl!!.length}"}")

                if (!result.success || result.track.streamUrl.isNullOrEmpty()) {
                    val errMsg = result.error ?: "Não consegui obter stream URL. Tente outra música."
                    log("ERROR: $errMsg")
                    _errorMessage.value = errMsg
                    _mascotMessage.value = "Ops! 😢 Não consegui tocar essa"
                    _resolving.value = false
                    _resolvingSource.value = null
                    return@launch
                }

                _lastSource.value = result.source
                _mascotMessage.value = "Tocando via ${result.source}! 🎶"
                log("Calling playback.playTrack with streamUrl=${result.track.streamUrl!!.take(80)}...")
                playback.playTrack(result.track)
                log("playback.playTrack returned, waiting for state change...")

                historyDao.insert(
                    HistoryEntity(
                        trackId = result.track.id, title = result.track.title, artist = result.track.artist,
                        thumbnailUrl = result.track.thumbnailUrl, durationMs = result.track.durationMs,
                        playedAt = System.currentTimeMillis(),
                    )
                )

                // Watchdog: if after 10 seconds nothing is playing AND no error, show error
                launch {
                    delay(10000)
                    if (!isPlaying.value && _errorMessage.value == null && currentTrack.value?.id == track.id) {
                        log("WATCHDOG: 10s elapsed, not playing, no error — showing timeout message")
                        _errorMessage.value = "Timeout: o stream não começou em 10s. Talvez esteja bloqueado pelo YouTube. Tente outra música."
                    }
                }
            } catch (e: Throwable) {
                log("EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
                _errorMessage.value = "Erro: ${e.message ?: "desconhecido"}"
                _mascotMessage.value = "Ops! 😢"
            } finally {
                _resolving.value = false
                _resolvingSource.value = null
            }
        }
    }

    /** Plays a known-good test MP3 to isolate playback issues. */
    fun playTestAudio() {
        log("▶ playTestAudio: Google sample MP3")
        viewModelScope.launch {
            _errorMessage.value = null
            _mascotMessage.value = "Teste de playback… 🎵"
            _lastSource.value = "test-mp3"
            val testTrack = Track(
                id = "test-sample",
                title = "Test Playback (sample audio)",
                artist = "OxyMusic",
                thumbnailUrl = "",
                streamUrl = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3",
            )
            playback.playTrack(testTrack)
            log("Test MP3 submitted to ExoPlayer")
        }
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            playback.pause()
            _mascotMessage.value = "Pausa rápida? Ok! ⏸️"
        } else {
            playback.play()
            _mascotMessage.value = "Toca essa! 🎶"
        }
    }

    fun next() { playback.next(); _mascotMessage.value = "Próxima! ✨" }
    fun previous() = playback.previous()
    fun seekTo(ms: Long) = playback.seekTo(ms)
    fun clearError() { _errorMessage.value = null }
    fun clearDebugLog() { _debugLog.value = "" }

    private suspend fun loadLyrics(track: Track) {
        _lyrics.value = null
        val cleanTitle = track.title
            .replace(Regex("""\s*[\(\[][^)\]]*[\)\]]"""), "")
            .replace(Regex("""\s*-\s*(Official|Music Video|MV|Lyrics?|Audio|Visualizer|Lyric Video).*""", RegexOption.IGNORE_CASE), "")
            .trim()
        val cleanArtist = track.artist
            .replace(Regex("""\s*-\s*Topic"""), "")
            .replace(Regex("""\s*VEVO""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(.*?\)"""), "")
            .trim()
        val lyrics = lrclib.fetch(
            trackName = cleanTitle,
            artistName = cleanArtist,
            durationSec = if (track.durationMs > 0) track.durationMs / 1000 else null
        )
        _lyrics.value = lyrics
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}
