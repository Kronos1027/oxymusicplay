package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oxymusic.app.model.AnimeTheme
import com.oxymusic.app.model.MascotPersonality
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import com.oxymusic.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val s by vm.settings.collectAsState()
    val colors = MaterialTheme.colorScheme
    var cacheSizeMb by remember { mutableStateOf(playerVm.getCacheSizeMb()) }
    var cacheCount by remember { mutableStateOf(playerVm.getCacheCount()) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.12f), colors.background))
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(top = 36.dp)
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineLarge, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Text("Personalize seu OxyMusic", color = colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 20.dp))

        // Appearance
        SectionCard("🎨 Aparência", colors) {
            SwitchRow("Cores adaptativas da capa", "Extrai cores da capa do álbum em tempo real", s.adaptiveColors, vm::setAdaptive, colors)
            SwitchRow("Modo Anime", "Tema sakura/ghibli com partículas e mascote", s.animeMode, vm::setAnimeMode, colors)
            if (s.animeMode) {
                DividerRow(colors)
                Text("Tema anime", color = colors.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnimeTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = s.animeTheme == theme,
                            onClick = { vm.setAnimeTheme(theme) },
                            label = { Text(theme.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary)
                        )
                    }
                }
                SliderRow("Intensidade das partículas", s.animeIntensity.toFloat(), 4f..40f, { vm.setAnimeIntensity(it.toInt()) }, colors, "${s.animeIntensity} partículas")
                DividerRow(colors)
                SwitchRow("Mascote (GIF)", "Mostra chibi animada em GIF quando nada toca", s.mascotEnabled, vm::setMascot, colors)
                Text("Personalidade", color = colors.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MascotPersonality.entries.forEach { p ->
                        FilterChip(
                            selected = s.mascotPersonality == p,
                            onClick = { vm.setPersonality(p) },
                            label = { Text(p.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Playback
        SectionCard("🎵 Playback & Lyrics", colors) {
            SwitchRow("Karaoke mode", "Destaca palavra por palavra quando disponível", s.karaokeMode, vm::setKaraoke, colors)
            DividerRow(colors)
            Text(
                "Arquitetura: download-then-play (v2.1.0)",
                color = colors.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "As músicas são baixadas para o cache antes de tocar. Isso elimina o erro 403 causado por URLs expiradas do YouTube. Replays saem instantâneos do cache.",
                color = colors.onSurface.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Cache management (v2.1.0)
        SectionCard("💾 Cache Offline", colors) {
            Text("Faixas em cache", color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                "$cacheCount música(s) · $cacheSizeMb MB usados",
                color = colors.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            if (showClearConfirm) {
                Surface(
                    color = colors.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Limpar todo o cache?", color = colors.onErrorContainer, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("As músicas terão que ser baixadas de novo na próxima vez.", color = colors.onErrorContainer.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    playerVm.clearCache()
                                    cacheSizeMb = 0
                                    cacheCount = 0
                                    showClearConfirm = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                            ) { Text("Sim, limpar", fontSize = 12.sp) }
                            TextButton(
                                onClick = { showClearConfirm = false },
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface.copy(alpha = 0.6f))
                            ) { Text("Cancelar", fontSize = 12.sp) }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    enabled = cacheCount > 0,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (cacheCount > 0) colors.error else colors.onSurface.copy(alpha = 0.3f))
                ) {
                    Text("🧹 Limpar cache", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // About
        SectionCard("ℹ️ Sobre", colors) {
            Text("OxyMusic", color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("v2.1.0 — Download-then-play (resolve 403)", color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            Text(
                "Player de música adaptativo inspirado em Waybar Media e Caelestia Shell.\n\n" +
                "• Busca: Innertube API direta (sem API key)\n" +
                "• Stream: download-then-play (áudio baixado, depois tocado localmente)\n" +
                "• Fontes: NewPipe + PoToken WebView → Innertube → HTTP → Piped\n" +
                "• Cache: LRU automático com gerenciamento em Ajustes\n" +
                "• Lyrics: LRCLIB\n" +
                "• Cores: Palette + Material 3\n" +
                "• Spectrum: Visualizer nativo\n" +
                "• Anime: GIFs reais (sakura + ghibli)",
                color = colors.onSurface.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, colors: ColorScheme, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DividerRow(colors: ColorScheme) {
    HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, colors: ColorScheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary))
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, colors: ColorScheme, valueText: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(valueText, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.onSurface.copy(alpha = 0.15f)),
            modifier = Modifier.padding(top = 4.dp))
    }
}

