package com.oxymusic.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.oxymusic.app.ui.viewmodel.PlayerViewModel

@Composable
fun EqualizerScreen(
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val presets = playerVm.equalizer.presets
    val bands = remember { mutableStateOf(playerVm.equalizer.getBands()) }
    var selectedPreset by remember { mutableStateOf("Flat") }

    LaunchedEffect(Unit) {
        bands.value = playerVm.equalizer.getBands()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp).padding(top = 40.dp)
    ) {
        Text("Equalizador", style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground, fontWeight = FontWeight.Bold)
        Text("8 presets · 5 bandas · AudioEffect nativo",
            color = colors.onSurface.copy(alpha = 0.5f), fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 20.dp), style = MaterialTheme.typography.labelSmall)

        // Presets header
        Text("Presets", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp), style = MaterialTheme.typography.labelLarge)

        // Presets list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(presets, key = { it }) { preset ->
                Surface(
                    onClick = {
                        selectedPreset = preset
                        playerVm.equalizer.setPreset(preset)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (preset == selectedPreset) {
                        colors.primary.copy(alpha = 0.15f)
                    } else {
                        colors.surfaceVariant.copy(alpha = 0.3f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (preset == selectedPreset) colors.primary else colors.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            preset,
                            color = if (preset == selectedPreset) colors.primary else colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = if (preset == selectedPreset) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.weight(1f))
                        if (preset == selectedPreset) {
                            Text("●", color = colors.primary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Active bands display
        AnimatedVisibility(visible = bands.value.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("Bandas ativas", color = colors.primary, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.labelLarge)
                bands.value.forEach { band ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(band.freq, color = colors.onSurface, fontSize = 12.sp,
                            style = MaterialTheme.typography.labelSmall)
                        Text("${band.level / 100}dB", color = colors.secondary, fontSize = 12.sp,
                            style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "O equalizador conecta automaticamente ao player quando uma música toca.",
            color = colors.onSurface.copy(alpha = 0.4f), fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
