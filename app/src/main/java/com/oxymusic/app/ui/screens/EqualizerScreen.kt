package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        // Refresh bands when screen opens
        bands.value = playerVm.equalizer.getBands()
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(16.dp).padding(top = 36.dp)) {
        Text("Equalizador", style = MaterialTheme.typography.headlineLarge, color = colors.onBackground, fontWeight = FontWeight.Bold)
        Text("Presets personalizados · 5 bandas", color = colors.onSurface.copy(alpha = 0.5f), fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 20.dp), style = MaterialTheme.typography.labelSmall)

        // Presets
        Text("Presets", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(presets) { preset ->
                Surface(
                    onClick = {
                        selectedPreset = preset
                        playerVm.equalizer.setPreset(preset)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (preset == selectedPreset) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, if (preset == selectedPreset) colors.primary else colors.onSurface.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(preset, color = if (preset == selectedPreset) colors.primary else colors.onSurface,
                            fontSize = 14.sp, fontWeight = if (preset == selectedPreset) FontWeight.Bold else FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        if (preset == selectedPreset) {
                            Text("●", color = colors.primary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Band info
        if (bands.value.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Bandas ativas", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            bands.value.forEach { band ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(band.freq, color = colors.onSurface, fontSize = 12.sp, style = MaterialTheme.typography.labelSmall)
                    Text("${band.level / 100}dB", color = colors.secondary, fontSize = 12.sp, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("O equalizador se conecta automaticamente ao player quando uma música toca.",
            color = colors.onSurface.copy(alpha = 0.4f), fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall)
    }
}
