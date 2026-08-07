package com.oxymusic.app.ui.viewmodel

import android.util.Log
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    fun onQueryChange(q: String) {
        _query.value = q
        // Clear error when user types
        _error.value = null
    }

    fun search() {
        val q = _query.value.trim()
        if (q.isEmpty()) {
            _error.value = "Digite algo para buscar"
            return
        }
        Log.i(TAG, "search: '$q'")
        _hasSearched.value = true
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val results = audius.search(q)
                Log.i(TAG, "search returned ${results.size} results")
                _results.value = results
                if (results.isEmpty()) {
                    _error.value = "Nenhuma música encontrada para \"$q\""
                }
            } catch (e: Exception) {
                Log.e(TAG, "search error", e)
                _error.value = "Erro na busca: ${e.message ?: "verifique sua conexão"}"
                _results.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    companion object { private const val TAG = "SearchViewModel" }
}
