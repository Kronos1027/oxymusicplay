package com.oxymusic.app.model

import kotlinx.serialization.Serializable

/**
 * Source of a Track.
 * - LOCAL: scanned from device MediaStore (content://media/external/audio/media/N)
 * - YOUTUBE: from YouTube search/trending/related (resolved to googlevideo URL at play time)
 */
enum class TrackSource {
    LOCAL,
    YOUTUBE,
}

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationMs: Long = 0L,
    val streamUrl: String? = null,
    val source: TrackSource = TrackSource.YOUTUBE,
    // Local-only metadata (null for YouTube tracks)
    val album: String? = null,
    val bitrate: Long = 0L,        // bits per second (e.g. 320000 = 320 kbps)
    val mimeType: String? = null,  // e.g. "audio/mpeg"
    val fileSize: Long = 0L,       // bytes
    val year: Int = 0,
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
