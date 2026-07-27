package com.oxymusic.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import java.util.Random
import kotlin.math.sin

@Composable
fun SakuraPetalsOverlay(intensity: Int = 14, color: Color = Color(0xFFFFC8DD), modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "petals")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "t"
    )
    val petals = remember(intensity) {
        val random = Random(42)
        List(intensity) {
            PetalData(
                x = random.nextFloat(), startY = random.nextFloat() * -1f,
                speed = 0.4f + random.nextFloat() * 0.8f,
                size = 6f + random.nextFloat() * 10f,
                phase = random.nextFloat() * 6.28f,
            )
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = this.size.width; val h = this.size.height
        petals.forEach { p ->
            val cycle = ((t * p.speed) + p.startY) % 1f
            val y = cycle * h
            val x = (p.x + 0.08f * sin(cycle * 6.28f + p.phase)) * w
            drawCircle(color = color.copy(alpha = 0.85f), radius = p.size, center = Offset(x, y))
        }
    }
}

private data class PetalData(val x: Float, val startY: Float, val speed: Float, val size: Float, val phase: Float)

@Composable
fun GhibliDustOverlay(intensity: Int = 12, color: Color = Color(0xFFE0F2E0), modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "dust")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(15_000, easing = LinearEasing)), label = "t"
    )
    val particles = remember(intensity) {
        val random = Random(7)
        List(intensity) {
            DustParticle(
                x = random.nextFloat(), startY = random.nextFloat(),
                speed = 0.3f + random.nextFloat() * 0.6f,
                size = 2f + random.nextFloat() * 4f,
                phase = random.nextFloat() * 6.28f,
                opacity = 0.4f + random.nextFloat() * 0.5f,
            )
        }
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = this.size.width; val h = this.size.height
        particles.forEach { p ->
            val cycle = ((t * p.speed) + p.startY) % 1f
            val y = (1f - cycle) * h
            val x = (p.x + 0.05f * sin(cycle * 6.28f + p.phase)) * w
            drawCircle(color = color.copy(alpha = p.opacity), radius = p.size, center = Offset(x, y))
        }
    }
}

private data class DustParticle(val x: Float, val startY: Float, val speed: Float, val size: Float, val phase: Float, val opacity: Float)
