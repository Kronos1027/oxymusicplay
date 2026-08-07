package com.oxymusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import coil.compose.AsyncImage
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

@Composable
fun MiniPlayer(
    playerVm: PlayerViewModel,
    onClick: () -> Unit,
) {
    val track by playerVm.currentTrack.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val colors = MaterialTheme.colorScheme

    val t = track ?: return
    val progress = if (duration > 0) (position.toFloat() / duration) else 0f

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Column {
            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = colors.primary,
                trackColor = colors.onSurface.copy(alpha = 0.1f),
            )
            Row(
                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork
                AsyncImage(
                    model = t.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(10.dp))
                // Title + artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.title, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(t.artist, color = colors.primary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Play/pause button
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary)))
                        .clickable { playerVm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Tocar",
                        tint = colors.onPrimary, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
