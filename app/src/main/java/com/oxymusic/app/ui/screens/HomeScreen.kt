package com.oxymusic.app.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
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
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to colors.primary.copy(alpha = 0.10f), 0.3f to colors.background)
        ),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Column(Modifier.padding(16.dp, 40.dp, 16.dp, 8.dp)) {
                Text("OxyMusic", style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text("Audius · Streaming gratuito · 100% funcional",
                    color = colors.onSurface.copy(alpha = 0.5f), fontSize = 11.sp,
                    style = MaterialTheme.typography.labelSmall)
            }
        }

        if (loading && trending.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Carregando trending…", color = colors.primary, fontSize = 12.sp,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (trending.isNotEmpty()) {
            // Horizontal trending row
            item {
                Text("🔥 Trending", color = colors.primary, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                    style = MaterialTheme.typography.labelLarge)
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    trending.take(10).forEach { track ->
                        TrendingCard(track, colors) {
                            playerVm.playQueue(trending, trending.indexOf(track))
                            onTrackClick()
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                    thickness = 1.dp,
                    color = colors.onSurface.copy(alpha = 0.08f)
                )
            }

            // Track list
            item {
                Text("🎵 Todas as faixas", color = colors.primary, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 4.dp),
                    style = MaterialTheme.typography.labelLarge)
            }
            items(trending.drop(10), key = { it.id }) { track ->
                TrackListRow(track, colors) {
                    playerVm.playQueue(trending, trending.indexOf(track))
                    onTrackClick()
                }
            }
        }
    }
}

@Composable
private fun TrendingCard(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Column(modifier = Modifier.width(144.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.size(144.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary)))
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
            )
        }
        Text(track.title, color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, end = 4.dp))
        Text(track.artist, color = colors.onSurface.copy(alpha = 0.5f), fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TrackListRow(track: Track, colors: ColorScheme, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
