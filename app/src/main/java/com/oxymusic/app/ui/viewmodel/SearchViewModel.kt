package com.oxymusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.AudiusClient
import com.oxymusic.app.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val audius: AudiusClient,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun onQueryChange(q: String) { _query.value = q }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _loading.value = true
            try { _results.value = audius.search(q) } catch (_: Exception) {}
            _loading.value = false
        }
    }
}
