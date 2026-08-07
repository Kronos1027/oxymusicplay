package com.oxymusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * ParticleBackground — animated particles on dark background.
 * Matches the portfolio site's particle aesthetic.
 */
@Composable
fun ParticleBackground(
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    particleCount: Int = 40,
) {
    val transition = rememberInfiniteTransition(label = "particles")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    val particles = List(particleCount) { i ->
        ParticleData(
            x = Random(i).nextFloat(),
            y = Random(i + 100).nextFloat(),
            speed = 0.0001f + Random(i + 200).nextFloat() * 0.0003f,
            size = 1f + Random(i + 300).nextFloat() * 3f,
            isAmber = Random(i + 400).nextFloat() > 0.85f,
        )
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val x = (p.x + phase * p.speed * 1000) % 1f * w
            val y = (p.y + phase * p.speed * 500) % 1f * h
            val color = if (p.isAmber) secondaryColor else primaryColor
            drawCircle(
                color = color.copy(alpha = 0.3f + 0.4f * (0.5f + 0.5f * sin(phase * 6.28f + p.x * 10))),
                radius = p.size,
                center = Offset(x, y),
            )
        }
    }
}

private data class ParticleData(
    val x: Float, val y: Float, val speed: Float, val size: Float, val isAmber: Boolean
)

private fun sin(x: Float): Float = kotlin.math.sin(x)
