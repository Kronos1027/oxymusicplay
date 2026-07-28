package com.oxymusic.app.ui.viewmodel

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
                if (sid != 0) { visualizer.attach(sid); attached = true }
            }
        }
        // Forward playback errors to errorMessage
        viewModelScope.launch {
            playback.lastError.collect { e -> if (e != null) _errorMessage.value = e }
        }
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            _resolving.value = true
            _resolvingSource.value = "Innertube → NewPipe → Piped"
            _errorMessage.value = null
            _mascotMessage.value = "Resolvendo stream… ⏳"
            try {
                val result = youtube.resolveStream(track)
                if (!result.success || result.track.streamUrl.isNullOrEmpty()) {
                    _errorMessage.value = result.error ?: "Não consegui obter o stream. Tente outra música."
                    _mascotMessage.value = "Ops! 😢 Não consegui tocar essa"
                    _resolving.value = false
                    _resolvingSource.value = null
                    return@launch
                }
                _lastSource.value = result.source
                _mascotMessage.value = "Tocando via ${result.source}! 🎶"
                playback.playTrack(result.track)
                historyDao.insert(
                    HistoryEntity(
                        trackId = result.track.id, title = result.track.title, artist = result.track.artist,
                        thumbnailUrl = result.track.thumbnailUrl, durationMs = result.track.durationMs,
                        playedAt = System.currentTimeMillis(),
                    )
                )
            } catch (e: Throwable) {
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
}
