package com.oxymusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxymusic.app.data.SettingsRepository
import com.oxymusic.app.model.AnimeTheme
import com.oxymusic.app.model.MascotPersonality
import com.oxymusic.app.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<Settings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun setAnimeMode(v: Boolean) = viewModelScope.launch { repo.setAnimeMode(v) }
    fun setAnimeTheme(v: AnimeTheme) = viewModelScope.launch { repo.setAnimeTheme(v) }
    fun setAnimeIntensity(v: Int) = viewModelScope.launch { repo.setAnimeIntensity(v) }
    fun setKaraoke(v: Boolean) = viewModelScope.launch { repo.setKaraoke(v) }
    fun setAdaptive(v: Boolean) = viewModelScope.launch { repo.setAdaptive(v) }
    fun setMascot(v: Boolean) = viewModelScope.launch { repo.setMascot(v) }
    fun setPersonality(p: MascotPersonality) = viewModelScope.launch { repo.setPersonality(p) }
}
