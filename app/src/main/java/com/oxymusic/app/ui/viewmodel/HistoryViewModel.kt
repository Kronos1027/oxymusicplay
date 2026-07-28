package com.oxymusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.HistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: HistoryDao
) : ViewModel() {

    val history = dao.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun clear() = viewModelScope.launch { dao.clear() }
}
