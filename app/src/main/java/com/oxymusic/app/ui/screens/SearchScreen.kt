package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
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
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.primary.copy(alpha = 0.12f), colors.background)
                )
            )
            .padding(16.dp)
            .padding(top = 36.dp)
    ) {
        // Header
        Text(
            "Buscar",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Encontre qualquer música no YouTube",
            color = colors.onSurface.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Search field with clear button
        OutlinedTextField(
            value = query,
            onValueChange = searchVm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex: midnight city m83") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { searchVm.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = colors.onSurface.copy(alpha = 0.6f))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchVm.search() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                cursorColor = colors.primary,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f),
                focusedContainerColor = colors.surface.copy(alpha = 0.5f),
                unfocusedContainerColor = colors.surface.copy(alpha = 0.3f),
            )
        )

        Spacer(Modifier.height(16.dp))

        // Content
        when {
            loading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Buscando no YouTube…", color = colors.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
            error != null -> {
                ErrorState(message = error!!, colors = colors, onRetry = { searchVm.search() })
            }
            results.isEmpty() && query.isEmpty() -> {
                EmptyStateSearch(colors = colors, onSuggestion = { sug ->
                    searchVm.onQueryChange(sug)
                    searchVm.search()
                })
            }
            results.isEmpty() -> {
                NoResultsState(query = query, colors = colors, onRetry = { searchVm.search() })
            }
            else -> {
                Text(
                    "${results.size} resultado${if (results.size > 1) "s" else ""} encontrado${if (results.size > 1) "s" else ""}",
                    color = colors.onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results) { track ->
                        TrackRow(track) {
                            playerVm.playTrack(track)
                            onTrackClick()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with gradient fallback
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            Text(
                track.artist,
                color = colors.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (track.durationMs > 0) {
            val s = track.durationMs / 1000
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "%d:%02d".format(s / 60, s % 60),
                    color = colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun EmptyStateSearch(colors: ColorScheme, onSuggestion: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Busque sua música favorita",
            color = colors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Digite o nome da música, artista, ou cole um link do YouTube",
            color = colors.onSurface.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )
        // Suggestions
        Text("Sugestões", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val suggestions = listOf("lofi hip hop", "midnight city m83", "the weeknd", "anime op", "synthwave mix")
        suggestions.forEach { sug ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSuggestion(sug) },
                shape = RoundedCornerShape(10.dp),
                color = colors.surface.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(sug, color = colors.onSurface, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun NoResultsState(query: String, colors: ColorScheme, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔍", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Nenhum resultado para \"$query\"",
            color = colors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Verifique a ortografia ou tente termos diferentes",
            color = colors.onSurface.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun ErrorState(message: String, colors: ColorScheme, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Algo deu errado",
            color = colors.error,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            message,
            color = colors.onSurface.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = colors.primary)) {
            Text("Tentar novamente")
        }
    }
}
