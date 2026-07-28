package com.oxymusic.app.network

import com.oxymusic.app.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct YouTube Innertube API client — no NewPipe, no Piped.
 *
 * This is the same approach InnerTune / RiMusic / ViMusic use:
 * - Uses YouTube's public internal API key (no auth needed)
 * - Anonymous access works for search (just needs proper client headers)
 * - Returns same data the YouTube app itself sees
 *
 * No poToken needed for search. Player endpoint may require poToken on some IPs,
 * but search always works.
 */
@Singleton
class InnertubeClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiKey = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
    private val baseUrl = "https://www.youtube.com/youtubei/v1"

    /**
     * Search YouTube videos. Returns list of tracks.
     * Uses ANDROID client — works reliably without poToken.
     */
    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID")
                        put("clientVersion", "20.10.38")
                        put("hl", "pt")
                        put("gl", "BR")
                    })
                })
                put("query", query)
            }.toString()

            val req = Request.Builder()
                .url("$baseUrl/search?key=$apiKey")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 13)")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val raw = resp.body?.string() ?: return@withContext emptyList()
                parseSearchResults(raw)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get trending videos in a region. Uses WEB client for browse endpoint.
     */
    suspend fun trending(region: String = "BR"): List<Track> = withContext(Dispatchers.IO) {
        try {
            // Use Piped for trending (more reliable than Innertube browse)
            // This is delegated to YouTubeRepository
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse Innertube search response. Walks the sectionListRenderer tree.
     */
    private fun parseSearchResults(raw: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(raw)
            val sections = root
                .optJSONObject("contents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return emptyList()

            for (i in 0 until sections.length()) {
                val section = sections.optJSONObject(i) ?: continue
                val itemSection = section.optJSONObject("itemSectionRenderer") ?: continue
                val items = itemSection.optJSONArray("contents") ?: continue

                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j) ?: continue
                    val video = item.optJSONObject("compactVideoRenderer") ?: continue
                    val track = parseCompactVideo(video) ?: continue
                    tracks.add(track)
                }
            }
        } catch (e: Exception) {
            // ignore parse errors
        }
        return tracks
    }

    private fun parseCompactVideo(v: JSONObject): Track? {
        val videoId = v.optString("videoId").ifEmpty { return null }

        // Title — runs joined
        val title = StringBuilder()
        v.optJSONObject("title")?.optJSONArray("runs")?.let { runs ->
            for (i in 0 until runs.length()) {
                title.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
            }
        }
        if (title.isEmpty()) {
            v.optJSONObject("title")?.optString("simpleText", "")?.let { title.append(it) }
        }
        if (title.isEmpty()) return null

        // Channel name
        val channel = v.optJSONObject("longBylineText")?.optJSONArray("runs")?.let { runs ->
            runs.optJSONObject(0)?.optString("text", "") ?: ""
        } ?: v.optJSONObject("shortBylineText")?.optJSONArray("runs")?.let { runs ->
            runs.optJSONObject(0)?.optString("text", "") ?: ""
        } ?: "Unknown"

        // Duration — lengthText.simpleText like "4:03"
        val durationStr = v.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
        val durationMs = parseDuration(durationStr)

        // Thumbnail — take the highest-quality one
        val thumbs = v.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
        } else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Track(
            id = videoId,
            title = title.toString().trim(),
            artist = channel,
            thumbnailUrl = thumb,
            durationMs = durationMs,
        )
    }

    private fun parseDuration(s: String): Long {
        if (s.isEmpty()) return 0L
        val parts = s.split(":")
        return try {
            when (parts.size) {
                2 -> parts[0].toLong() * 60 + parts[1].toLong()
                3 -> parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                else -> 0L
            } * 1000L
        } catch (e: Exception) { 0L }
    }
}
