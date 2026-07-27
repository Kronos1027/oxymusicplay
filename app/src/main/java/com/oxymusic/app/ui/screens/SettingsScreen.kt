package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oxymusic.app.model.AnimeTheme
import com.oxymusic.app.model.MascotPersonality
import com.oxymusic.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val s by vm.settings.collectAsState()
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(16.dp).padding(top = 32.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(20.dp))
        SectionHeader("Aparência", colors)
        SwitchRow("Cores adaptativas da capa", "Extrai cores da capa em tempo real", s.adaptiveColors, vm::setAdaptive, colors)
        SwitchRow("Modo Anime", "Tema sakura/ghibli com partículas e mascote", s.animeMode, vm::setAnimeMode, colors)
        if (s.animeMode) {
            Text("Tema anime", color = colors.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimeTheme.entries.forEach { theme ->
                    FilterChip(selected = s.animeTheme == theme, onClick = { vm.setAnimeTheme(theme) },
                        label = { Text(theme.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary))
                }
            }
            SliderRow("Intensidade das partículas", s.animeIntensity.toFloat(), 4f..40f, { vm.setAnimeIntensity(it.toInt()) }, colors, "${s.animeIntensity}")
            SwitchRow("Mascote (GIF)", "Mostra chibi animada em GIF", s.mascotEnabled, vm::setMascot, colors)
            Text("Personalidade", color = colors.onSurface.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MascotPersonality.entries.forEach { p ->
                    FilterChip(selected = s.mascotPersonality == p, onClick = { vm.setPersonality(p) },
                        label = { Text(p.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primary))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionHeader("Playback & Lyrics", colors)
        SwitchRow("Karaoke mode", "Destaca palavra por palavra quando disponível", s.karaokeMode, vm::setKaraoke, colors)
        Spacer(Modifier.height(20.dp))
        SectionHeader("Sobre", colors)
        Surface(shape = RoundedCornerShape(12.dp), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("OxyMusic", color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("v1.3.0 — fix de playback + GIFs reais", color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text("Player de música adaptativo inspirado em Waybar Media e Caelestia Shell.\n\n" +
                    "• YouTube via NewPipeExtractor + Piped (sem API key)\n" +
                    "• Lyrics via LRCLIB\n" +
                    "• Cores adaptativas via Palette\n" +
                    "• Spectrum via Visualizer nativo\n" +
                    "• Modo Anime com GIFs reais (sakura/ghibli)",
                    color = colors.onSurface.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, colors: ColorScheme) {
    Text(title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
    HorizontalDivider(color = colors.onSurface.copy(alpha = 0.1f))
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, colors: ColorScheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp)
            Text(subtitle, color = colors.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary))
    }
}

@Composable
private fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, colors: ColorScheme, valueText: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = colors.onSurface, fontSize = 14.sp)
            Text(valueText, color = colors.primary, fontSize = 13.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.onSurface.copy(alpha = 0.15f)))
    }
}
