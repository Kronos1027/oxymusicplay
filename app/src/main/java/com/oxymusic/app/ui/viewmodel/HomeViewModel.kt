package com.oxymusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.HistoryDao
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.model.Track
import com.oxymusic.app.network.YouTubeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val youtube: YouTubeRepository,
    historyDao: HistoryDao,
) : ViewModel() {

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    val trending: StateFlow<List<Track>> = _trending.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val history: StateFlow<List<HistoryEntity>> = historyDao.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { loadTrending() }

    fun loadTrending() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _trending.value = youtube.trending("BR")
            } catch (e: Throwable) {} finally {
                _loading.value = false
            }
        }
    }
}
