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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oxymusic.app.model.AnimeTheme
import com.oxymusic.app.ui.components.AnimeMascotGif
import com.oxymusic.app.ui.components.GhibliDustOverlay
import com.oxymusic.app.ui.components.MorphingAlbumWithSpectrum
import com.oxymusic.app.ui.components.SakuraPetalsOverlay
import com.oxymusic.app.ui.components.SyncedLyricsView
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import com.oxymusic.app.ui.viewmodel.SettingsViewModel

@Composable
fun PlayerScreen(
    settingsVm: SettingsViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val settings by settingsVm.settings.collectAsState()
    val track by playerVm.currentTrack.collectAsState()
    val isPlaying by playerVm.isPlaying.collectAsState()
    val position by playerVm.positionMs.collectAsState()
    val duration by playerVm.durationMs.collectAsState()
    val lyrics by playerVm.lyrics.collectAsState()
    val resolving by playerVm.resolving.collectAsState()
    val buffering by playerVm.isBuffering.collectAsState()
    val mascotMsg by playerVm.mascotMessage.collectAsState()
    val errorMsg by playerVm.errorMessage.collectAsState()
    val magnitudes by playerVm.visualizer.magnitudes.collectAsState()
    val colors = MaterialTheme.colorScheme
    val isGhibli = settings.animeMode && settings.animeTheme == AnimeTheme.GHIBLI

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(colors.primary.copy(alpha = if (settings.animeMode) 0.25f else 0.18f), colors.background))
    )) {
        if (settings.animeMode) {
            if (isGhibli) GhibliDustOverlay(intensity = settings.animeIntensity, color = colors.secondary)
            else SakuraPetalsOverlay(intensity = settings.animeIntensity, color = colors.secondary)
        }

        Column(modifier = Modifier.fillMaxSize().padding(20.dp).padding(top = 36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TOCANDO AGORA", color = colors.onSurface.copy(alpha = 0.5f), fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 12.dp))

            if (settings.animeMode && settings.mascotEnabled && (track == null || resolving)) {
                AnimeMascotGif(animeTheme = settings.animeTheme, isPlaying = isPlaying, size = 220.dp, modifier = Modifier.padding(bottom = 16.dp))
            } else {
                MorphingAlbumWithSpectrum(
                    magnitudes = magnitudes,
                    primaryColor = colors.primary, secondaryColor = colors.secondary, tertiaryColor = colors.tertiary,
                    albumArtUrl = track?.thumbnailUrl, sizeDp = 280.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Text(track?.title ?: (if (settings.animeMode) "Vamos ouvir música? 🎵" else "Nada tocando"),
                color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(track?.artist ?: "—", color = colors.primary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

            if (buffering) {
                Text("⏳ Buffering…", color = colors.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(16.dp))
            SyncedLyricsView(lyrics = lyrics, positionMs = position, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Spacer(Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { v -> if (duration > 0) playerVm.seekTo((v * duration).toLong()) },
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.onSurface.copy(alpha = 0.15f))
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(position), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(fmtTime(duration), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { playerVm.previous() }) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", tint = colors.onSurface, modifier = Modifier.size(32.dp))
                }
                Box(modifier = Modifier.size(64.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary)))
                    .clickable { playerVm.togglePlayPause() }, contentAlignment = Alignment.Center) {
                    if (resolving) {
                        CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pausar" else "Tocar", tint = colors.onPrimary, modifier = Modifier.size(36.dp))
                    }
                }
                IconButton(onClick = { playerVm.next() }) {
                    Icon(Icons.Default.SkipNext, "Próxima", tint = colors.onSurface, modifier = Modifier.size(32.dp))
                }
            }

            // Test playback button (debug)
            if (track == null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { playerVm.playTestAudio() },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)) {
                    Text("🎵 Testar playback (sample MP3)", fontSize = 12.sp)
                }
            }
        }

        // Mascot speech bubble
        AnimatedVisibility(visible = settings.animeMode && settings.mascotEnabled && mascotMsg != null,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp))
                .background(colors.surface.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(mascotMsg ?: "", color = colors.onSurface, fontSize = 13.sp)
            }
        }

        // Error banner
        AnimatedVisibility(visible = errorMsg != null, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = colors.errorContainer,
                tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth().clickable { playerVm.clearError() }) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ Erro ao tocar", color = colors.onErrorContainer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(errorMsg ?: "", color = colors.onErrorContainer.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Toque para fechar · Tente outra música se persistir", color = colors.onErrorContainer.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
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
