package com.oxymusic.app.data

import android.util.Log
import com.oxymusic.app.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audius API client — free, decentralized music streaming API.
 *
 * No authentication required. No API key. No rate limits.
 * 100+ million tracks from independent artists.
 *
 * Endpoints used:
 * - GET https://api.audius.co → returns list of available hosts
 * - GET {host}/v1/tracks/trending?app_name=OxyMusic → trending tracks
 * - GET {host}/v1/tracks/search?query=X&app_name=OxyMusic → search
 * - GET {host}/v1/tracks/{id}/stream?app_name=OxyMusic → stream audio (redirects to CDN)
 *
 * Tested and validated: trending returns 100 tracks, search works,
 * stream returns HTTP 200 with valid mp3 (validated with ffprobe).
 */
@Singleton
class AudiusClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var host: String = "https://api.audius.co"

    private val APP_NAME = "OxyMusic"

    init {
        // Discover best host in background
        Thread {
            try {
                val req = Request.Builder().url("https://api.audius.co").build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: return@Thread
                        val parsed = json.parseToJsonElement(body).jsonObject
                        val data = parsed["data"]?.jsonArray
                        if (data != null && data.isNotEmpty()) {
                            host = data[0].jsonPrimitive.contentOrNull ?: host
                            Log.i(TAG, "Audius host: $host")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Host discovery failed, using default: $host")
            }
        }.start()
    }

    suspend fun trending(): List<Track> = withContext(Dispatchers.IO) {
        try {
            val url = "$host/v1/tracks/trending?app_name=$APP_NAME"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray ?: return@withContext emptyList()
                data.mapNotNull { it.jsonObject.toTrack() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "trending error", e)
            emptyList()
        }
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "$host/v1/tracks/search?query=$encoded&app_name=$APP_NAME"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val root = json.parseToJsonElement(body).jsonObject
                val data = root["data"]?.jsonArray ?: return@withContext emptyList()
                data.mapNotNull { it.jsonObject.toTrack() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "search error", e)
            emptyList()
        }
    }

    /** Returns the stream URL for a track (ExoPlayer follows the redirect to CDN). */
    fun streamUrl(trackId: String): String {
        return "$host/v1/tracks/$trackId/stream?app_name=$APP_NAME"
    }

    private fun JsonObject.toTrack(): Track? {
        val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = this["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val user = this["user"]?.jsonObject
        val artist = user?.get("name")?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val artwork = this["artwork"]?.jsonObject?.get("480x480")?.jsonPrimitive?.contentOrNull ?: ""
        val duration = this["duration"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        val genre = this["genre"]?.jsonPrimitive?.contentOrNull ?: ""
        return Track(
            id = id,
            title = title,
            artist = artist,
            artworkUrl = artwork,
            streamUrl = streamUrl(id),
            durationMs = duration * 1000,
            genre = genre,
        )
    }

    companion object {
        private const val TAG = "AudiusClient"
    }
}
