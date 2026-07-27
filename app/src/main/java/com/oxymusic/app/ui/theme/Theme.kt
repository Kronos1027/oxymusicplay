package com.oxymusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OxyPurple = Color(0xFFA855F7)
val OxyPink = Color(0xFFEC4899)
val OxyMagenta = Color(0xFFF0ABFC)
val OxyBackground = Color(0xFF08060C)
val OxySurface = Color(0xFF0E0A14)
val OxySurfaceVariant = Color(0xFF1A1422)
val OxyOnSurface = Color(0xFFE4E4E7)
val OxyOnSurfaceMuted = Color(0xFF71717A)

val SakuraPrimary = Color(0xFFFF9EC7)
val SakuraSecondary = Color(0xFFFFD6E7)
val SakuraBackground = Color(0xFF2A1A2E)
val SakuraSurface = Color(0xFF1A1024)
val SakuraSurfaceVariant = Color(0xFF2D1F33)

private val OxyDarkScheme = darkColorScheme(
    primary = OxyPurple, onPrimary = Color.White,
    primaryContainer = OxyPurple.copy(alpha = 0.3f),
    secondary = OxyPink, onSecondary = Color.White,
    tertiary = OxyMagenta,
    background = OxyBackground, onBackground = OxyOnSurface,
    surface = OxySurface, onSurface = OxyOnSurface,
    surfaceVariant = OxySurfaceVariant, onSurfaceVariant = OxyOnSurfaceMuted,
)

private val SakuraScheme = darkColorScheme(
    primary = SakuraPrimary, onPrimary = Color(0xFF3A2540),
    primaryContainer = SakuraPrimary.copy(alpha = 0.25f),
    secondary = SakuraSecondary, onSecondary = Color(0xFF3A2540),
    tertiary = SakuraSecondary,
    background = SakuraBackground, onBackground = Color(0xFFFFE4F0),
    surface = SakuraSurface, onSurface = Color(0xFFFFE4F0),
    surfaceVariant = SakuraSurfaceVariant, onSurfaceVariant = Color(0xFFFFB6C1),
)

@Composable
fun OxyMusicTheme(animeMode: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (animeMode) SakuraScheme else OxyDarkScheme
    MaterialTheme(colorScheme = scheme, content = content)
}
