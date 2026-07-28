package com.oxymusic.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
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
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.model.Track
import com.oxymusic.app.ui.viewmodel.HistoryViewModel
import com.oxymusic.app.ui.viewmodel.PlayerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onTrackClick: () -> Unit,
    historyVm: HistoryViewModel = hiltViewModel(),
    playerVm: PlayerViewModel = hiltViewModel(),
) {
    val items by historyVm.history.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.12f), colors.background)))
            .padding(16.dp)
            .padding(top = 36.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Histórico",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${items.size} faixa${if (items.size != 1) "s" else ""} tocada${if (items.size != 1) "s" else ""}",
                    color = colors.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            if (items.isNotEmpty()) {
                IconButton(onClick = { historyVm.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Limpar histórico", tint = colors.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            EmptyHistoryState(colors = colors)
        } else {
            // Group by date
            val grouped = items.groupBy { formatDate(it.playedAt) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                grouped.forEach { (dateLabel, tracks) ->
                    item {
                        Text(
                            dateLabel,
                            color = colors.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(tracks) { item ->
                        HistoryRow(item) {
                            val track = Track(
                                id = item.trackId,
                                title = item.title,
                                artist = item.artist,
                                thumbnailUrl = item.thumbnailUrl,
                                durationMs = item.durationMs,
                            )
                            playerVm.playTrack(track)
                            onTrackClick()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryEntity, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            if (item.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.artist,
                color = colors.onSurface.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            formatTimeOfDay(item.playedAt),
            color = colors.onSurface.copy(alpha = 0.4f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun EmptyHistoryState(colors: ColorScheme) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp))
                .background(Brush.sweepGradient(listOf(colors.tertiary, colors.primary, colors.secondary))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Nenhuma música tocada ainda",
            color = colors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "As músicas que você ouvir aparecerão aqui",
            color = colors.onSurface.copy(alpha = 0.5f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60 * 60 * 1000L -> "Agora"
        diff < 24 * 60 * 60 * 1000L -> "Hoje"
        diff < 48 * 60 * 60 * 1000L -> "Ontem"
        diff < 7 * 24 * 60 * 60 * 1000L -> "Esta semana"
        else -> SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")).format(Date(timestamp))
    }
}

private fun formatTimeOfDay(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(timestamp))
}
