package com.oxymusic.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oxymusic.app.model.Lyrics

@Composable
fun SyncedLyricsView(
    lyrics: Lyrics?,
    positionMs: Long,
    modifier: Modifier = Modifier,
    visibleLines: Int = 5,
) {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        Box(modifier = modifier.height(80.dp), contentAlignment = Alignment.Center) {
            Text(
                text = if (lyrics == null) "Carregando lyrics…" else "Sem lyrics",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 13.sp
            )
        }
        return
    }
    val activeIndex = remember(lyrics, positionMs) {
        var idx = -1
        lyrics.lines.forEachIndexed { i, line -> if (line.timeMs <= positionMs) idx = i }
        idx.coerceAtLeast(0)
    }
    val window = (activeIndex - visibleLines / 2).coerceAtLeast(0)
    val end = (window + visibleLines).coerceAtMost(lyrics.lines.size)
    Column(
        modifier = modifier.fillMaxWidth().height((visibleLines * 30).dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        lyrics.lines.subList(window, end).forEachIndexed { i, line ->
            val realIndex = window + i
            val isActive = realIndex == activeIndex
            val isNext = realIndex == activeIndex + 1
            val alpha by animateFloatAsState(
                targetValue = when { isActive -> 1f; isNext -> 0.55f; else -> 0.3f },
                animationSpec = tween(300), label = "alpha"
            )
            val sizeByAnim by animateFloatAsState(
                targetValue = if (isActive) 17f else 14f,
                animationSpec = tween(300), label = "size"
            )
            Text(
                text = line.text.ifEmpty { "♪" },
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = sizeByAnim.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(alpha).padding(vertical = 4.dp)
            )
        }
    }
}
