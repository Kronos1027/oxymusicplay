package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.outlined.BugReport
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
    val resolvingSource by playerVm.resolvingSource.collectAsState()
    val buffering by playerVm.isBuffering.collectAsState()
    val mascotMsg by playerVm.mascotMessage.collectAsState()
    val errorMsg by playerVm.errorMessage.collectAsState()
    val lastSource by playerVm.lastSource.collectAsState()
    val magnitudes by playerVm.visualizer.magnitudes.collectAsState()
    val colors = MaterialTheme.colorScheme
    val isGhibli = settings.animeMode && settings.animeTheme == AnimeTheme.GHIBLI

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to colors.primary.copy(alpha = if (settings.animeMode) 0.30f else 0.22f),
                0.5f to colors.background,
                1f to colors.background
            )
        )
    ) {
        // Particles
        if (settings.animeMode) {
            if (isGhibli) GhibliDustOverlay(intensity = settings.animeIntensity, color = colors.secondary)
            else SakuraPetalsOverlay(intensity = settings.animeIntensity, color = colors.secondary)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Now playing label with animated dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) colors.primary else colors.onSurface.copy(alpha = 0.3f)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isPlaying) "TOCANDO AGORA" else if (track != null) "PAUSADO" else "PRONTO PARA TOCAR",
                    color = colors.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }

            // Album / Mascot
            // Show chibi mascot when anime mode + (no track OR resolving), OR show album always
            if (settings.animeMode && settings.mascotEnabled && track == null) {
                AnimeMascotGif(
                    animeTheme = settings.animeTheme,
                    isPlaying = isPlaying,
                    size = 220.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                MorphingAlbumWithSpectrum(
                    magnitudes = magnitudes,
                    primaryColor = colors.primary,
                    secondaryColor = colors.secondary,
                    tertiaryColor = colors.tertiary,
                    albumArtUrl = track?.thumbnailUrl,
                    sizeDp = 280.dp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // Title + artist
            Text(
                text = track?.title ?: (if (settings.animeMode) "Vamos ouvir música? 🎵" else "Nenhuma música tocando"),
                color = colors.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 28.sp
            )
            Text(
                text = track?.artist ?: "Busque uma música na aba Buscar",
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
            )

            // Buffering indicator
            AnimatedVisibility(visible = buffering, enter = fadeIn(), exit = fadeOut()) {
                Text("⏳ Carregando…", color = colors.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            // Resolving indicator with source info
            AnimatedVisibility(visible = resolving && !buffering, enter = fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                    Text("🔍 Resolvendo stream…", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "Tentando: $resolvingSource",
                        color = colors.onSurface.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            // Active source indicator (when playing)
            AnimatedVisibility(visible = !resolving && !buffering && lastSource != null && track != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "via $lastSource",
                    color = colors.onSurface.copy(alpha = 0.35f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // Lyrics
            SyncedLyricsView(
                lyrics = lyrics,
                positionMs = position,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(Modifier.weight(1f))

            // Progress bar
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { v -> if (duration > 0) playerVm.seekTo((v * duration).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.onSurface.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(position), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Text(fmtTime(duration), color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { playerVm.previous() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", tint = colors.onSurface, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary, colors.tertiary)))
                        .clickable { playerVm.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (resolving) {
                        CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pausar" else "Tocar",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = { playerVm.next() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, "Próxima", tint = colors.onSurface, modifier = Modifier.size(36.dp))
                }
            }

            // Test playback button (when nothing is playing)
            AnimatedVisibility(
                visible = track == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = { playerVm.playTestAudio() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Testar playback (MP3 sample)", fontSize = 12.sp)
                    }
                    Text(
                        "Use isto para isolar problemas de playback",
                        color = colors.onSurface.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Mascot speech bubble
        AnimatedVisibility(
            visible = settings.animeMode && settings.mascotEnabled && mascotMsg != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface.copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(mascotMsg ?: "", color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Error banner
        AnimatedVisibility(
            visible = errorMsg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.errorContainer,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().clickable { playerVm.clearError() }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("⚠️ Não consegui tocar", color = colors.onErrorContainer, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(errorMsg ?: "", color = colors.onErrorContainer.copy(alpha = 0.85f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    Text("Toque para fechar · Tente outra música ou use o teste de playback", color = colors.onErrorContainer.copy(alpha = 0.6f), fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
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
