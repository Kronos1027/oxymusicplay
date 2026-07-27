package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import com.oxymusic.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onTrackClick: () -> Unit,
    searchVm: SearchViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val query by searchVm.query.collectAsState()
    val results by searchVm.results.collectAsState()
    val loading by searchVm.loading.collectAsState()
    val error by searchVm.error.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp).padding(top = 36.dp)) {
        Text("Buscar", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = query, onValueChange = searchVm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar no YouTube (ex: midnight city m83)") },
            leadingIcon = { Text("🔍") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { searchVm.search() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground, unfocusedTextColor = colors.onBackground,
                cursorColor = colors.primary, focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f),
            )
        )
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
            error != null -> Text(error!!, color = colors.error)
            results.isEmpty() -> Text("Busque por artista, música, álbum.", color = colors.onSurface.copy(alpha = 0.6f))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { track ->
                    TrackRow(track) { playerVm.playTrack(track); onTrackClick() }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotEmpty()) {
                AsyncImage(model = track.thumbnailUrl, contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = colors.onSurface, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.durationMs > 0) {
            val s = track.durationMs / 1000
            Text("%d:%02d".format(s / 60, s % 60), color = colors.onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
        }
    }
}
