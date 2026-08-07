package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.oxymusic.app.media.OxyDjEngine
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OxyDjViewModel @Inject constructor(
    private val oxyDj: OxyDjEngine,
) : ViewModel() {
    private val _recommendations = MutableStateFlow<List<Track>>(emptyList())
    val recommendations: StateFlow<List<Track>> = _recommendations.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _recommendations.value = oxyDj.recommend(20)
                if (_recommendations.value.isEmpty()) {
                    _error.value = "Sem histórico suficiente ainda. Toque algumas músicas e volte aqui."
                }
            } catch (e: Throwable) {
                _error.value = "Erro: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}

@Composable
fun OxyDjScreen(
    onTrackClick: () -> Unit,
    oxyDjVm: OxyDjViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val recs by oxyDjVm.recommendations.collectAsState()
    val loading by oxyDjVm.loading.collectAsState()
    val error by oxyDjVm.error.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "OxyDJ",
                    color = colors.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Recomendações 100% locais · grátis · sem coletar seus dados",
                    color = colors.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = { oxyDjVm.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = colors.primary)
            }
        }

        // Loading
        AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Analisando seu histórico…",
                        color = colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                }
            }
        }

        // Error
        AnimatedVisibility(visible = error != null && !loading, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { oxyDjVm.refresh() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nada ainda",
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = error ?: "",
                        color = colors.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Recommendations list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            items(recs, key = { it.id }) { track ->
                RecommendationRow(
                    track = track,
                    onClick = {
                        playerVm.playTrack(track)
                        onTrackClick()
                    },
                )
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun RecommendationRow(track: Track, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (track.thumbnailUrl.isNotEmpty()) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.primary)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = colors.primary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (track.durationMs > 0) {
            Text(
                text = fmtTime(track.durationMs),
                color = colors.onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
