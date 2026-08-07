package com.oxymusic.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * SpectrumVisualizer — circular FFT spectrum bars around album art.
 * Driven by VisualizerManager's real-time magnitudes (0-1 normalized).
 */
@Composable
fun SpectrumVisualizer(
    magnitudes: FloatArray,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    size: Int = 280,
) {
    Canvas(modifier = modifier.size(size.dp)) {
        val center = Offset(size / 2f, size / 2f)
        val baseRadius = size * 0.38f
        val barCount = magnitudes.size.coerceAtMost(64)
        val angleStep = (2 * Math.PI / barCount).toFloat()

        for (i in 0 until barCount) {
            val angle = i * angleStep - (Math.PI / 2).toFloat()
            val mag = magnitudes[i].coerceIn(0f, 1f)
            val barLength = baseRadius * 0.05f + mag * baseRadius * 0.35f
            val startRadius = baseRadius
            val endRadius = baseRadius + barLength

            val startX = center.x + cos(angle) * startRadius
            val startY = center.y + sin(angle) * startRadius
            val endX = center.x + cos(angle) * endRadius
            val endY = center.y + sin(angle) * endRadius

            val color = if (i % 3 == 0) secondaryColor else primaryColor
            drawLine(
                color = color.copy(alpha = 0.4f + mag * 0.6f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 3f,
            )
        }

        // Outer glow ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.08f),
            radius = baseRadius + 2f,
            center = center,
            style = Stroke(width = 1f),
        )
    }
}
