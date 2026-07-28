package com.oxymusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

/**
 * Mini player that floats above the bottom navigation bar (Spotify-style).
 * Shows current track + play/pause + skip controls.
 * Tapping it navigates to the full Player screen.
 */
@Composable
fun MiniPlayer(
    onClick: () -> Unit,
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val track by playerVm.currentTrack.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val colors = MaterialTheme.colorScheme

    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val t = track ?: return@AnimatedVisibility
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface.copy(alpha = 0.95f))
                .clickable { onClick() }
        ) {
            // Progress bar at top (thin)
            LinearProgressIndicator(
                progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                color = colors.primary,
                trackColor = colors.onSurface.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )

            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    if (t.thumbnailUrl.isNotEmpty()) {
                        AsyncImage(
                            model = t.thumbnailUrl,
                            contentDescription = t.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                // Title + artist
                Column(Modifier.weight(1f)) {
                    Text(
                        t.title,
                        color = colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        t.artist,
                        color = colors.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                // Play/pause button
                IconButton(
                    onClick = { playerVm.togglePlayPause() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Tocar",
                        tint = colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                // Skip button
                IconButton(
                    onClick = { playerVm.next() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Próxima",
                        tint = colors.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
