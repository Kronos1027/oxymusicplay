package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    val hasSearched by searchVm.hasSearched.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        // Search bar with explicit search button
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = searchVm::onQueryChange,
                placeholder = { Text("Buscar músicas, artistas…", color = colors.onSurface.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = colors.primary) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            searchVm.onQueryChange("")
                            searchVm.clearError()
                        }) {
                            Icon(Icons.Default.Close, "Limpar", tint = colors.onSurface.copy(alpha = 0.5f))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { searchVm.search() }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f),
                    cursorColor = colors.primary,
                )
            )
            Spacer(Modifier.width(8.dp))
            // Explicit search button (always visible, always clickable)
            Button(
                onClick = { searchVm.search() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                modifier = Modifier.height(56.dp),
                enabled = !loading && query.isNotBlank()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                        color = colors.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Search, "Buscar")
                }
            }
        }

        // Loading state
        AnimatedVisibility(visible = loading, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Buscando…", color = colors.primary, fontSize = 13.sp, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Error / empty state
        AnimatedVisibility(visible = error != null && !loading, enter = fadeIn(), exit = fadeOut()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { searchVm.clearError() }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(error ?: "", color = colors.onSurface, fontSize = 13.sp)
                    if (hasSearched) {
                        Text("Toque para fechar", color = colors.onSurface.copy(alpha = 0.4f), fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Results
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp, top = 4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.id }) { track ->
                TrackResultRow(track, colors) {
                    playerVm.playQueue(results, results.indexOf(track))
                    onTrackClick()
                }
            }
        }
    }
}

@Composable
private fun TrackResultRow(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
            .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary)))) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = colors.primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall)
            if (track.genre.isNotEmpty()) {
                Text(track.genre, color = colors.onSurface.copy(alpha = 0.3f), fontSize = 10.sp,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (track.durationMs > 0) {
            val s = track.durationMs / 1000
            Text("%d:%02d".format(s / 60, s % 60), color = colors.onSurface.copy(alpha = 0.4f),
                fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
        }
    }
}
