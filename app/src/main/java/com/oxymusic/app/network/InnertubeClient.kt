package com.oxymusic.app.network

import android.util.Log
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
 *
 * For STREAM URLS: tries multiple client types (WEB, ANDROID_VR, ANDROID_TESTSUITE)
 * because YouTube blocks some clients based on IP reputation. Residential IPs
 * (user's phone) typically work with WEB client.
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
                if (!resp.isSuccessful) {
                    Log.w(TAG, "search HTTP ${resp.code}")
                    return@withContext emptyList()
                }
                val raw = resp.body?.string() ?: return@withContext emptyList()
                parseSearchResults(raw)
            }
        } catch (e: Exception) {
            Log.e(TAG, "search error", e)
            emptyList()
        }
    }

    /**
     * Resolve stream URL via Innertube player endpoint.
     *
     * Strategy based on YouTube.js (LuanRT/YouTube.js) v17.2.0 (June 2026):
     * Uses LATEST client versions. Order:
     * 1. IOS v20.11.6 — direct URLs without poToken
     * 2. ANDROID_MUSIC v5.34.51 — YouTube Music Android client
     * 3. TVHTML5_SIMPLY_EMBEDDED_PLAYER v2.0 with thirdParty.embedUrl
     * 4. ANDROID v21.03.36 — full device config
     * 5. Fallbacks: ANDROID_VR, WEB
     *
     * @return ResolvedStream or null if all clients fail
     */
    suspend fun resolveStream(videoId: String): ResolvedStream? = withContext(Dispatchers.IO) {
        log("resolveStream START videoId=$videoId")

        // 1. IOS v20.11.6 (LuanRT/YouTube.js latest)
        var result = tryClient(
            videoId = videoId,
            clientName = "IOS",
            clientVersion = "20.11.6",
            apiKey = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
            userAgent = "com.google.ios.youtube/20.11.6 (iPhone10,4; U; CPU iOS 16_7_7 like Mac OS X)",
            extraClientFields = mapOf(
                "deviceMake" to "Apple",
                "deviceModel" to "iPhone10,4",
                "osName" to "iOS",
                "osVersion" to "16.7.7.20H330"
            )
        )
        if (result != null) {
            log("resolveStream SUCCESS with IOS")
            return@withContext result.copy(sourceLabel = "Innertube/IOS")
        }

        // 2. ANDROID_MUSIC v5.34.51 (YouTube Music Android)
        result = tryClient(
            videoId = videoId,
            clientName = "ANDROID_MUSIC",
            clientVersion = "5.34.51",
            apiKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI",
            userAgent = "com.google.android.apps.youtube.music/5.34.51 (Linux; U; Android 16; SM-S908E)",
            extraClientFields = mapOf(
                "osName" to "Android",
                "osVersion" to "16",
                "androidSdkVersion" to "36"
            )
        )
        if (result != null) {
            log("resolveStream SUCCESS with ANDROID_MUSIC")
            return@withContext result.copy(sourceLabel = "Innertube/ANDROID_MUSIC")
        }

        // 3. TVHTML5_SIMPLY_EMBEDDED_PLAYER v2.0 with embedUrl
        result = tryClient(
            videoId = videoId,
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            apiKey = "AIzaSyDCU8hByM-4DrUqRUYnGn-3llEO78bcxq8",
            userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version",
            useEmbedUrl = true,
            videoIdForEmbed = videoId
        )
        if (result != null) {
            log("resolveStream SUCCESS with TVHTML5")
            return@withContext result.copy(sourceLabel = "Innertube/TVHTML5")
        }

        // 4. ANDROID v21.03.36 (latest YouTube Android)
        result = tryClient(
            videoId = videoId,
            clientName = "ANDROID",
            clientVersion = "21.03.36",
            apiKey = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
            userAgent = "com.google.android.youtube/21.03.36 (Linux; U; Android 16; SM-S908E)",
            extraClientFields = mapOf(
                "osName" to "Android",
                "osVersion" to "16",
                "androidSdkVersion" to "36"
            )
        )
        if (result != null) {
            log("resolveStream SUCCESS with ANDROID")
            return@withContext result.copy(sourceLabel = "Innertube/ANDROID")
        }

        // 5. Fallbacks — NOTE: WEB removed because YouTube made it SABR-only in 2025/2026
        // (Server Adaptive Bitrate — returns DASH manifests instead of direct HTTP URLs that
        // ExoPlayer can play). Only ANDROID_VR and MWEB remain as fallbacks.
        // See: NewPipeExtractor release notes (issue #1297 — "do not use WEB client for stream URLs anymore")
        for ((clientName, clientVersion, ua) in listOf(
            Triple("ANDROID_VR", "1.65.10", "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; Quest 2 Build/SQ3A.220605.009.A1)"),
            Triple("MWEB", "2.20260205.04.01", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
        )) {
            try {
                val r = tryClient(videoId, clientName, clientVersion, userAgent = ua)
                if (r != null) {
                    log("resolveStream SUCCESS with $clientName (fallback)")
                    return@withContext r.copy(sourceLabel = "Innertube/$clientName")
                }
            } catch (e: Exception) {
                log("client $clientName failed: ${e.message}")
            }
        }

        log("resolveStream: ALL clients FAILED")
        null
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
    }

    /**
     * Get related tracks for a video via Innertube /next endpoint (free, no API key).
     * Used by OxyDJ for recommendations.
     */
    suspend fun getRelatedTracks(videoId: String, limit: Int = 10): List<Track> = withContext(Dispatchers.IO) {
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
                put("videoId", videoId)
            }.toString()

            val req = Request.Builder()
                .url("$baseUrl/next?key=$apiKey")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 13)")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "getRelatedTracks HTTP ${resp.code}")
                    return@withContext emptyList()
                }
                val raw = resp.body?.string() ?: return@withContext emptyList()
                // Walk the response looking for compactVideoRenderer (related videos)
                val tracks = mutableListOf<Track>()
                val seen = mutableSetOf<String>()
                val root = JSONObject(raw)
                walkForTracks(root, tracks, seen)
                if (tracks.size > limit) tracks.subList(0, limit) else tracks
            }
        } catch (e: Exception) {
            Log.e(TAG, "getRelatedTracks error", e)
            emptyList()
        }
    }

    /**
     * Validates that a stream URL is actually accessible (returns 2xx or 3xx).
     *
     * IMPORTANT: Uses GET with Range: bytes=0-1024 instead of HEAD.
     * Reason: googlevideo.com (YouTube CDN) often rejects or mishandles HEAD requests
     * even when the same URL works perfectly with GET. This was causing the app to
     * discard perfectly valid stream URLs and fall through to "all sources failed".
     *
     * With Range: bytes=0-1024, we download only 1KB to test the URL — fast and reliable.
     */
    suspend fun validateStreamUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(url)
                .get()
                .header("Range", "bytes=0-1024")
                .build()
            client.newCall(req).execute().use { resp ->
                val code = resp.code
                log("validateStreamUrl HTTP $code for ${url.take(80)}...")
                // 200, 206 (Partial Content), 2xx, 3xx are all valid
                code in 200..399
            }
        } catch (e: Exception) {
            log("validateStreamUrl error: ${e.message}")
            false
        }
    }

    private fun tryClient(
        videoId: String,
        clientName: String,
        clientVersion: String,
        apiKey: String = this.apiKey,
        userAgent: String = "com.google.android.youtube/20.10.38 (Linux; U; Android 13)",
        useEmbedUrl: Boolean = false,
        videoIdForEmbed: String? = null,
        extraClientFields: Map<String, String> = emptyMap()
    ): ResolvedStream? {
        val context = JSONObject().apply {
            put("client", JSONObject().apply {
                put("clientName", clientName)
                put("clientVersion", clientVersion)
                put("hl", "pt")
                put("gl", "BR")
                extraClientFields.forEach { (k, v) -> put(k, v) }
            })
            if (useEmbedUrl && videoIdForEmbed != null) {
                put("thirdParty", JSONObject().apply {
                    put("embedUrl", "https://www.youtube.com/watch?v=$videoIdForEmbed")
                })
            }
        }
        val body = JSONObject().apply {
            put("context", context)
            put("videoId", videoId)
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("html5Preference", "HTML5_PREF_WANTS")
                })
            })
        }.toString()

        val req = Request.Builder()
            .url("$baseUrl/player?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", userAgent)
            .header("Content-Type", "application/json")
            .build()

        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.w(TAG, "$clientName HTTP ${resp.code}")
                return null
            }
            val raw = resp.body?.string() ?: return null
            return parsePlayerResponse(raw, clientName)
        }
    }

    private fun parsePlayerResponse(raw: String, clientName: String): ResolvedStream? {
        try {
            val root = JSONObject(raw)
            val status = root.optJSONObject("playabilityStatus")?.optString("status", "") ?: ""
            if (status != "OK") {
                val reason = root.optJSONObject("playabilityStatus")?.optString("reason", "") ?: ""
                Log.w(TAG, "$clientName playabilityStatus=$status reason=$reason")
                return null
            }

            val sd = root.optJSONObject("streamingData") ?: return null
            val duration = sd.optLong("durationInSeconds", 0L) * 1000L
            val adaptive = sd.optJSONArray("adaptiveFormats") ?: JSONArray()
            val formats = sd.optJSONArray("formats") ?: JSONArray()

            // Find best audio-only stream with DIRECT url (no signatureCipher needed)
            // signatureCipher URLs require JS deciphering which we don't do — they fail with BAD_HTTP_STATUS
            var bestAudio: String? = null
            var bestBitrate = 0
            for (i in 0 until adaptive.length()) {
                val item = adaptive.optJSONObject(i) ?: continue
                val mime = item.optString("mimeType", "")
                if (!mime.contains("audio")) continue
                // SKIP signatureCipher URLs — they need deciphering and would fail
                if (item.has("signatureCipher") || item.has("cipher")) continue
                val bitrate = item.optInt("bitrate", 0)
                val url = item.optString("url", "").ifEmpty { null } ?: continue
                if (bitrate > bestBitrate) {
                    bestBitrate = bitrate
                    bestAudio = url
                }
            }

            // Fallback to muxed formats (video+audio in one file) — only direct URLs
            if (bestAudio == null) {
                for (i in 0 until formats.length()) {
                    val item = formats.optJSONObject(i) ?: continue
                    if (item.has("signatureCipher") || item.has("cipher")) continue
                    val url = item.optString("url", "").ifEmpty { null }
                    if (url != null) {
                        bestAudio = url
                        break
                    }
                }
            }

            if (bestAudio != null) {
                // Add &ratebypass=yes for YouTube URLs — without it, seeking past 30s returns 403
                val finalUrl = if (bestAudio.contains("googlevideo.com") && !bestAudio.contains("ratebypass")) {
                    "$bestAudio&ratebypass=yes"
                } else bestAudio
                return ResolvedStream(streamUrl = finalUrl, durationMs = duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "$clientName parse error", e)
        }
        return null
    }

    /**
     * Get trending videos via Innertube /browse endpoint.
     *
     * Note: FEtrending returns "Invalid argument" — YouTube changed the API.
     * We try FEmusic_home (YouTube Music home) which returns trending music.
     * Falls back to a curated search if that also fails.
     */
    suspend fun trending(region: String = "BR"): List<Track> = withContext(Dispatchers.IO) {
        // Try YouTube Music home — returns popular music
        try {
            val body = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", "7.16.53")
                        put("hl", "pt")
                        put("gl", region)
                    })
                })
                put("browseId", "FEmusic_home")
            }.toString()

            val req = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/browse?key=AIzaSyC9XL3ZjWddXxa6g74aFMFhNziO1lJ8MZw")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "com.google.android.apps.youtube.music/7.16.53 (Linux; U; Android 13)")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val raw = resp.body?.string() ?: ""
                    val tracks = parseMusicHome(raw)
                    if (tracks.isNotEmpty()) {
                        Log.i(TAG, "trending SUCCESS via YouTube Music home: ${tracks.size} tracks")
                        return@withContext tracks
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "trending YouTube Music home failed: ${e.message}")
        }

        // Fallback: search for popular music
        try {
            val popular = search("músicas populares 2026")
            if (popular.isNotEmpty()) {
                Log.i(TAG, "trending SUCCESS via search fallback: ${popular.size} tracks")
                return@withContext popular
            }
        } catch (e: Exception) {
            Log.w(TAG, "trending search fallback failed: ${e.message}")
        }

        emptyList()
    }

    /**
     * Parse YouTube Music home response. Walks the tree recursively looking for
     * musicTwoRowItemRenderer or any item with videoId + title.
     */
    private fun parseMusicHome(raw: String): List<Track> {
        val tracks = mutableListOf<Track>()
        val seen = mutableSetOf<String>()
        try {
            val root = JSONObject(raw)
            walkForTracks(root, tracks, seen)
        } catch (e: Exception) {
            Log.e(TAG, "parseMusicHome error", e)
        }
        return tracks
    }

    private fun walkForTracks(obj: Any, tracks: MutableList<Track>, seen: MutableSet<String>) {
        when (obj) {
            is JSONObject -> {
                // Check if this is a musicTwoRowItemRenderer or similar with videoId
                val track = extractTrackFromItem(obj)
                if (track != null && track.id !in seen) {
                    seen.add(track.id)
                    tracks.add(track)
                }
                // Recurse into all values
                obj.keys().forEach { key ->
                    try { walkForTracks(obj.get(key), tracks, seen) } catch (e: Exception) {}
                }
            }
            is JSONArray -> {
                for (i in 0 until obj.length()) {
                    try { walkForTracks(obj.get(i), tracks, seen) } catch (e: Exception) {}
                }
            }
        }
    }

    private fun extractTrackFromItem(obj: JSONObject): Track? {
        // Try various renderer types
        val renderer = obj.optJSONObject("musicTwoRowItemRenderer")
            ?: obj.optJSONObject("playlistPanelVideoRenderer")
            ?: obj.optJSONObject("compactVideoRenderer")
            ?: obj.optJSONObject("videoRenderer")
            ?: return null

        // Get videoId
        var videoId = renderer.optString("videoId", "").ifEmpty { null }
        if (videoId == null) {
            // Try navigationEndpoint.watchEndpoint.videoId
            videoId = renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId", "")
                ?.ifEmpty { null } ?: return null
        }
        if (videoId.length != 11) return null

        // Get title
        val title = StringBuilder()
        renderer.optJSONObject("title")?.optJSONArray("runs")?.let { runs ->
            for (i in 0 until runs.length()) {
                title.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
            }
        }
        if (title.isEmpty()) {
            title.append(renderer.optJSONObject("title")?.optString("simpleText", "") ?: "")
        }
        if (title.isEmpty()) return null

        // Get subtitle (artist)
        val subtitle = StringBuilder()
        renderer.optJSONObject("subtitle")?.optJSONArray("runs")?.let { runs ->
            for (i in 0 until runs.length()) {
                subtitle.append(runs.optJSONObject(i)?.optString("text", "") ?: "")
            }
        }
        val artist = if (subtitle.isNotEmpty()) subtitle.toString().trim() else "Unknown"

        // Get thumbnail
        val thumbs = renderer.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?: renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url", "") ?: ""
        } else "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        return Track(
            id = videoId,
            title = title.toString().trim(),
            artist = artist,
            thumbnailUrl = thumb,
            durationMs = 0L,
        )
    }

    private fun parseTrending(raw: String): List<Track> {
        // Kept for backwards compat, but parseMusicHome is used now
        return parseMusicHome(raw)
    }

    private fun parseVideoRenderer(v: JSONObject): Track? {
        return extractTrackFromItem(v)
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
            Log.e(TAG, "parseSearchResults error", e)
        }
        return tracks
    }

    private fun parseCompactVideo(v: JSONObject): Track? {
        val videoId = v.optString("videoId").ifEmpty { return null }

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

        val channel = v.optJSONObject("longBylineText")?.optJSONArray("runs")?.let { runs ->
            runs.optJSONObject(0)?.optString("text", "") ?: ""
        } ?: v.optJSONObject("shortBylineText")?.optJSONArray("runs")?.let { runs ->
            runs.optJSONObject(0)?.optString("text", "") ?: ""
        } ?: "Unknown"

        val durationStr = v.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""
        val durationMs = parseDuration(durationStr)

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

    companion object {
        private const val TAG = "InnertubeClient"
    }
}

data class ResolvedStream(
    val streamUrl: String,
    val durationMs: Long,
    val sourceLabel: String = "Innertube",
)
