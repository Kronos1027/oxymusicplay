package com.oxymusic.app.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.HistoryDao
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.media.LocalMediaRepository
import com.oxymusic.app.model.Track
import com.oxymusic.app.model.TrackSource
import com.oxymusic.app.network.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Unified Library: combines local MediaStore tracks + YouTube search results in one screen.
 *
 * Two tabs (no separate screens):
 * - "Meu aparelho" — local tracks (no internet needed, instant playback via content:// URI)
 * - "Online" — YouTube search/trending (needs internet, stream URL resolved on play)
 *
 * Both kinds of tracks go into the same Media3 queue — mixed playback supported
 * (local track followed by YouTube track in the same queue).
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val app: Application,
    private val localMedia: LocalMediaRepository,
    private val youtube: YouTubeRepository,
    historyDao: HistoryDao,
) : ViewModel() {

    /** Currently selected tab — "local" or "online". */
    private val _selectedTab = MutableStateFlow("local")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    /** Local tracks (from MediaStore). */
    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    /** Local scan status. */
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** Permission granted flag. */
    private val _permissionGranted = MutableStateFlow(hasReadMediaPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    /** YouTube search/trending tracks (online tab). */
    private val _onlineTracks = MutableStateFlow<List<Track>>(emptyList())
    val onlineTracks: StateFlow<List<Track>> = _onlineTracks.asStateFlow()

    /** YouTube search query. */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Pending query — when set, LibraryScreen will auto-switch to "online" tab
     * and trigger the search. Used by HomeScreen's "Sugestões de busca" chips.
     */
    private val _pendingQuery = MutableStateFlow<String?>(null)
    val pendingQuery: StateFlow<String?> = _pendingQuery.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = historyDao.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Auto-load trending on first show of online tab
        viewModelScope.launch {
            if (_onlineTracks.value.isEmpty()) loadTrending()
        }
    }

    fun selectTab(tab: String) {
        _selectedTab.value = tab
        if (tab == "local" && _permissionGranted.value && _localTracks.value.isEmpty()) {
            scanLocal()
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    /**
     * Triggers a search from outside the Library screen (e.g. HomeScreen chips).
     * Sets the query, switches to "online" tab, and runs the search.
     * The screen observes pendingQuery and clears it after consuming.
     */
    fun searchFromOutside(query: String) {
        _query.value = query
        _selectedTab.value = "online"
        _pendingQuery.value = query
        search()
    }

    /** Called by LibraryScreen when it consumes the pending query. */
    fun consumePendingQuery() {
        _pendingQuery.value = null
    }

    fun refreshPermission() {
        _permissionGranted.value = hasReadMediaPermission()
        if (_permissionGranted.value && _localTracks.value.isEmpty()) {
            scanLocal()
        }
    }

    /** Scans the device's MediaStore for local audio files. */
    fun scanLocal() {
        if (!hasReadMediaPermission()) {
            _error.value = "Permissão de áudio negada. Conceda acesso em Configurações."
            return
        }
        viewModelScope.launch {
            _scanning.value = true
            _error.value = null
            try {
                _localTracks.value = localMedia.scanLocalMusic()
                if (_localTracks.value.isEmpty()) {
                    _error.value = "Nenhuma música encontrada no aparelho."
                }
            } catch (e: Throwable) {
                _error.value = "Erro ao escanear: ${e.message}"
            } finally {
                _scanning.value = false
            }
        }
    }

    /** Searches YouTube (online tab). */
    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _searching.value = true
            _error.value = null
            try {
                val r = youtube.search(q)
                _onlineTracks.value = r.tracks
                if (r.tracks.isEmpty()) _error.value = "Nenhuma faixa encontrada."
            } catch (e: Throwable) {
                _error.value = "Erro ao buscar: ${e.message}"
            } finally {
                _searching.value = false
            }
        }
    }

    /** Loads trending (online tab, default view). */
    fun loadTrending() {
        viewModelScope.launch {
            _searching.value = true
            try {
                _onlineTracks.value = youtube.trending("BR")
            } catch (e: Throwable) {} finally {
                _searching.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun hasReadMediaPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            app.checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            app.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
