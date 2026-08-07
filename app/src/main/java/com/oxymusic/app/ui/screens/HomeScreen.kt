package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
    val loading by homeVm.loading.collectAsState()
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(Modifier.padding(16.dp, 36.dp, 16.dp, 8.dp)) {
                Text("OxyMusic", style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text("Audius · Streaming gratuito · 100% offline-capaz",
                    color = colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp,
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        if (loading && trending.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
        }

        if (trending.isNotEmpty()) {
            item {
                Text("🔥 Trending", color = colors.primary, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    trending.take(10).forEach { track ->
                        TrackCard(track, colors) {
                            playerVm.playQueue(trending, trending.indexOf(track))
                            onTrackClick()
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        items(trending.drop(10)) { track ->
            TrackRow(track, colors) {
                playerVm.playQueue(trending, trending.indexOf(track))
                onTrackClick()
            }
        }
    }
}

@Composable
private fun TrackCard(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).clickable(onClick = onClick)) {
        Box(Modifier.size(140.dp).clip(RoundedCornerShape(12.dp))
            .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary)))) {
            AsyncImage(model = track.artworkUrl, contentDescription = track.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
        }
        Text(track.title, color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        Text(track.artist, color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrackRow(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = track.artworkUrl, contentDescription = null,
            contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = colors.primary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.durationMs > 0) {
            val s = track.durationMs / 1000
            Text("%d:%02d".format(s / 60, s % 60), color = colors.onSurface.copy(alpha = 0.4f),
                fontSize = 11.sp, style = MaterialTheme.typography.labelSmall)
        }
    }
}
