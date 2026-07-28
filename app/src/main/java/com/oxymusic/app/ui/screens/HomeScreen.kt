package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.HomeViewModel
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

@Composable
fun HomeScreen(
    onTrackClick: () -> Unit,
    homeVm: HomeViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val trending by homeVm.trending.collectAsState()
    val history by homeVm.history.collectAsState()
    val loading by homeVm.loading.collectAsState()
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.18f), colors.background)))
            .padding(top = 36.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Greeting
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(greeting(), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                Text(
                    "OxyMusic",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Recently played (horizontal scroll)
        if (history.isNotEmpty()) {
            item {
                SectionHeader("Tocadas recentemente", colors)
                HorizontalTrackRow(
                    tracks = history.distinctBy { it.trackId }.take(10).map { it.toTrack() },
                    onTrackClick = { track ->
                        playerVm.playTrack(track)
                        onTrackClick()
                    },
                    colors = colors
                )
            }
        }

        // Trending
        item {
            SectionHeader(if (history.isEmpty()) "🔥 Em alta no Brasil" else "🔥 Trending BR", colors)
        }
        if (loading && trending.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary, modifier = Modifier.size(36.dp))
                }
            }
        } else if (trending.isEmpty()) {
            item {
                Surface(
                    Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface.copy(alpha = 0.5f)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Sem conexão com trending", color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Use a aba Buscar para procurar músicas", color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        TextButton(onClick = { homeVm.loadTrending() }, colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)) {
                            Text("Tentar de novo", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            item {
                HorizontalTrackRow(
                    tracks = trending.take(15),
                    onTrackClick = { track ->
                        playerVm.playTrack(track)
                        onTrackClick()
                    },
                    colors = colors
                )
            }
        }

        // Quick suggestions
        item {
            SectionHeader("🎵 Sugestões de busca", colors)
            Row(
                Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("🌙 Lofi", "⚡ Rock", "😔 Sad", "📚 Focus", "🌸 Anime OP", "🎧 EDM").forEach { mood ->
                    AssistChip(
                        onClick = {},
                        label = { Text(mood, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = colors.onSurface
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, colors: ColorScheme) {
    Text(
        title,
        color = colors.onBackground,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun HorizontalTrackRow(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    colors: ColorScheme,
) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tracks.forEach { track ->
            TrackCard(track, colors) { onTrackClick(track) }
        }
    }
}

@Composable
private fun TrackCard(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(140.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            if (track.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                )
            }
        }
        Text(
            track.title,
            color = colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, end = 4.dp)
        )
        Text(
            track.artist,
            color = colors.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}

private fun HistoryEntity.toTrack() = Track(
    id = trackId,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
)
