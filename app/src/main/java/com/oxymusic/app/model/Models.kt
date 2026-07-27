package com.oxymusic.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val streamUrl: String? = null,
)

data class SearchResults(val query: String, val tracks: List<Track>)

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val trackId: String,
    val lines: List<LyricLine>,
    val synced: Boolean,
    val plain: String? = null,
)

data class Settings(
    val adaptiveColors: Boolean = true,
    val animeMode: Boolean = false,
    val animeTheme: AnimeTheme = AnimeTheme.SAKURA,
    val animeIntensity: Int = 14,
    val karaokeMode: Boolean = true,
    val lockScreenLyrics: Boolean = true,
    val crossfadeSeconds: Float = 0f,
    val skipOnError: Boolean = true,
    val cacheSizeMb: Int = 500,
    val mascotEnabled: Boolean = true,
    val mascotPersonality: MascotPersonality = MascotPersonality.CUTE,
)

enum class AnimeTheme(val label: String, val displayName: String) {
    SAKURA("Sakura", "🌸 Sakura"),
    GHIBLI("Ghibli", "🍃 Ghibli"),
}

enum class MascotPersonality(val label: String) {
    CUTE("Fofa"), SHY("Tímida"), SARCASTIC("Sarcástica"),
}
