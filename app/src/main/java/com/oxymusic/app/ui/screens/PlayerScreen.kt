package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.oxymusic.app.ui.components.ParticleBackground
import com.oxymusic.app.ui.components.SpectrumVisualizer
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val track by playerVm.currentTrack.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val buffering by playerVm.isBuffering.collectAsState()
    val magnitudes by playerVm.visualizer.magnitudes.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(0f to colors.primary.copy(alpha = 0.18f), 0.5f to colors.background, 1f to colors.background)
    )) {
        ParticleBackground(
            primaryColor = colors.primary, secondaryColor = colors.secondary,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Now playing label with pulsing dot
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape)
                    .background(if (isPlaying) colors.primary else colors.onSurface.copy(alpha = 0.3f)))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isPlaying) "TOCANDO AGORA" else if (buffering) "CARREGANDO…" else "PRONTO",
                    color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Album art + spectrum visualizer
            Box(contentAlignment = Alignment.Center) {
                SpectrumVisualizer(
                    magnitudes = magnitudes,
                    primaryColor = colors.primary,
                    secondaryColor = colors.secondary,
                    size = 340
                )
                AsyncImage(
                    model = track?.artworkUrl ?: "",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(210.dp).clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(Modifier.height(28.dp))

            // Title + artist
            Text(
                track?.title ?: "Nenhuma música tocando",
                color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.artist ?: "Busque na aba Buscar",
                color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "via Audius · streaming gratuito",
                color = colors.onSurface.copy(alpha = 0.3f), fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelSmall
            )

            Spacer(Modifier.weight(1f))

            // Progress bar
            Column(Modifier.fillMaxWidth()) {
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { v -> if (duration > 0) playerVm.seekTo((v * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.onSurface.copy(alpha = 0.15f)
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(position), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall)
                    Text(fmtTime(duration), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp,
                        style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Controls — bigger, more polished
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerVm.previous() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", tint = colors.onSurface, modifier = Modifier.size(38.dp))
                }
                Box(
                    Modifier.size(76.dp).clip(CircleShape).background(
                        Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))
                    ).clickable { playerVm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (buffering) {
                        CircularProgressIndicator(
                            color = colors.onPrimary, strokeWidth = 3.dp,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pausar" else "Tocar",
                            tint = colors.onPrimary, modifier = Modifier.size(42.dp)
                        )
                    }
                }
                IconButton(onClick = { playerVm.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.SkipNext, "Próxima", tint = colors.onSurface, modifier = Modifier.size(38.dp))
                }
            }
        }
    }
}

private fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
