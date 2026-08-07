package com.oxymusic.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oxymusic.app.R

/**
 * OxyMusic v2.0 theme — "deep tech / terminal de IA" (matches the NATSKY portfolio site).
 *
 * PALETTE:
 * - Background: almost-black (#0a0b0f → #12141a) — same as site
 * - Primary accent: cyan/teal (#22d3ee) — same as site
 * - Secondary accent: amber/gold (#fbbf24) — same as site
 * - These are the "heterochromic eyes" colors of the Oto-ai VTuber persona
 *
 * The default theme is the deep-tech one. The legacy "anime mode" (Sakura/Ghibli)
 * is kept as an optional skin in Settings.
 */

// Deep-tech palette (default — matches portfolio site)
val Teal = Color(0xFF22D3EE)
val TealSoft = Color(0xFF67E8F9)
val TealDeep = Color(0xFF0891B2)
val Amber = Color(0xFFFBBF24)
val AmberSoft = Color(0xFFFCD34D)
val AmberDeep = Color(0xFFD97706)

val Bg0 = Color(0xFF0A0B0F)        // primary background (almost black)
val Bg1 = Color(0xFF0E1016)        // slightly lighter
val Bg2 = Color(0xFF12141A)
val Bg3 = Color(0xFF181A22)        // cards / elevated surfaces

val Fg0 = Color(0xFFF4F6FB)        // primary text (almost white)
val Fg1 = Color(0xFFC9CDD6)        // secondary text
val Fg2 = Color(0xFF8B919E)        // muted text
val Fg3 = Color(0xFF5A6070)        // very muted

val LineColor = Color(0x1AFFFFFF)  // 10% white
val LineStrong = Color(0x33FFFFFF) // 20% white

// Legacy anime palette (kept as optional skin)
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

/**
 * Fonts:
 * - Display (titles): Space Grotesk (matches site's display font)
 * - Body: Inter (matches site's body font)
 * - Mono (bitrate, duration, technical HUD): JetBrains Mono
 *
 * The fonts are NOT pre-loaded as resources (would inflate APK size by ~2MB).
 * Instead we use the system default for each family with monospaced fallback.
 * This gives a similar look without bundling fonts.
 */
val MonoFamily = FontFamily.Monospace
val DisplayFamily = FontFamily.Default  // System sans-serif (Inter-like)
val BodyFamily = FontFamily.Default

val OxyTypography = Typography(
    // Display
    displayLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp, lineHeight = 44.sp,
    ),
    // Headline
    headlineLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp,
    ),
    // Title
    titleLarge = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Medium,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    // Label — using Mono for technical HUD look
    labelLarge = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)

// Deep-tech dark scheme (default)
private val DeepTechScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Bg0,
    primaryContainer = Teal.copy(alpha = 0.15f),
    onPrimaryContainer = TealSoft,
    secondary = Amber,
    onSecondary = Bg0,
    secondaryContainer = Amber.copy(alpha = 0.15f),
    onSecondaryContainer = AmberSoft,
    tertiary = TealSoft,
    onTertiary = Bg0,
    background = Bg0,
    onBackground = Fg1,
    surface = Bg1,
    onSurface = Fg0,
    surfaceVariant = Bg3,
    onSurfaceVariant = Fg2,
    surfaceTint = Teal,
    outline = LineStrong,
    outlineVariant = LineColor,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFEF4444).copy(alpha = 0.15f),
    onErrorContainer = Color(0xFFFCA5A5),
)

// Legacy Oxy purple scheme (kept for backwards-compat with anime mode OFF + old pref)
private val OxyDarkScheme = darkColorScheme(
    primary = OxyPurple, onPrimary = Color.White,
    primaryContainer = OxyPurple.copy(alpha = 0.3f),
    secondary = OxyPink, onSecondary = Color.White,
    tertiary = OxyMagenta,
    background = OxyBackground, onBackground = OxyOnSurface,
    surface = OxySurface, onSurface = OxyOnSurface,
    surfaceVariant = OxySurfaceVariant, onSurfaceVariant = OxyOnSurfaceMuted,
)

// Legacy Sakura scheme
private val SakuraScheme = darkColorScheme(
    primary = SakuraPrimary, onPrimary = Color(0xFF3A2540),
    primaryContainer = SakuraPrimary.copy(alpha = 0.25f),
    secondary = SakuraSecondary, onSecondary = Color(0xFF3A2540),
    tertiary = SakuraSecondary,
    background = SakuraBackground, onBackground = Color(0xFFFFE4F0),
    surface = SakuraSurface, onSurface = Color(0xFFFFE4F0),
    surfaceVariant = SakuraSurfaceVariant, onSurfaceVariant = Color(0xFFFFB6C1),
)

/**
 * @param animeMode if true, uses Sakura/Ghibli scheme (legacy skin)
 * @param animeTheme which anime skin (only used when animeMode = true)
 */
@Composable
fun OxyMusicTheme(
    animeMode: Boolean = false,
    animeTheme: com.oxymusic.app.model.AnimeTheme = com.oxymusic.app.model.AnimeTheme.SAKURA,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        animeMode && animeTheme == com.oxymusic.app.model.AnimeTheme.GHIBLI ->
            SakuraScheme.copy(primary = Color(0xFF8FBC8F))  // greenish for Ghibli
        animeMode -> SakuraScheme
        else -> DeepTechScheme  // DEFAULT — deep tech / terminal de IA
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = OxyTypography,
        content = content,
    )
}
