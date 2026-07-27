package com.oxymusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MorphingAlbumWithSpectrum(
    magnitudes: FloatArray,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    albumArtUrl: String?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 280.dp,
) {
    val infinite = rememberInfiniteTransition(label = "morph")
    val rotation by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )
    val borderRadiusFraction by infinite.animateFloat(
        initialValue = 0.30f, targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(7_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "radius"
    )
    val albumSizeDp = sizeDp * 0.62f

    Box(modifier = modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            val innerR = this.size.width * 0.32f
            val maxBarLen = this.size.width * 0.13f
            val barCount = magnitudes.size.coerceAtLeast(32)
            val sweepBrush = Brush.sweepGradient(listOf(tertiaryColor, primaryColor, secondaryColor, tertiaryColor))
            rotate(degrees = rotation) {
                for (i in 0 until barCount) {
                    val angle = (i.toFloat() / barCount) * 2f * PI.toFloat()
                    val mag = if (i < magnitudes.size) magnitudes[i] else 0f
                    val barLen = (maxBarLen * (0.18f + mag * 0.95f)).coerceAtLeast(2f)
                    val x1 = cx + cos(angle) * innerR
                    val y1 = cy + sin(angle) * innerR
                    val x2 = cx + cos(angle) * (innerR + barLen)
                    val y2 = cy + sin(angle) * (innerR + barLen)
                    drawLine(brush = sweepBrush, start = Offset(x1, y1), end = Offset(x2, y2),
                        strokeWidth = 4f, cap = StrokeCap.Round,
                        alpha = (0.45f + mag * 0.55f).coerceIn(0f, 1f))
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx, cy), radius = innerR
                ),
                radius = innerR, center = Offset(cx, cy)
            )
        }
        val cornerPx = albumSizeDp.value * borderRadiusFraction * 2.5f
        val shape = RoundedCornerShape(cornerPx.dp)
        Box(
            modifier = Modifier.size(albumSizeDp).clip(shape)
                .background(Brush.sweepGradient(listOf(tertiaryColor, primaryColor, secondaryColor, tertiaryColor))),
            contentAlignment = Alignment.Center
        ) {
            if (!albumArtUrl.isNullOrEmpty()) {
                AsyncImage(model = albumArtUrl, contentDescription = "Album art",
                    modifier = Modifier.size(albumSizeDp).clip(shape))
            }
        }
    }
}
