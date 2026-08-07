package com.oxymusic.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val streamUrl: String = "",
    val durationMs: Long = 0L,
    val genre: String = "",
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val artworkUrl: String = "",
    val trackCount: Int = 0,
)
