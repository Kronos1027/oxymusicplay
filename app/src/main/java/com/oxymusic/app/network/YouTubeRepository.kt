package com.oxymusic.app.network

import android.util.Log
import com.oxymusic.app.model.SearchResults
import com.oxymusic.app.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Multi-source YouTube client.
 *
 * Priority for SEARCH:
 * 1. Innertube API direct (most reliable, no poToken needed)
 * 2. NewPipeExtractor (fallback)
 * 3. Piped API (last resort)
 *
 * Priority for STREAM URL:
 * 1. Innertube player endpoint with multiple client types (WEB, ANDROID_VR, etc.)
 * 2. NewPipeExtractor (uses user's residential IP)
 * 3. Piped API (4 instances, fallback)
 *
 * Same approach as InnerTune / RiMusic / ViMusic.
 * NO API key needed.
 */
@Singleton
class YouTubeRepository @Inject constructor(
    private val innertube: InnertubeClient,
) {

    private val client = OkHttpClient.Builder().build()
    private val service: YoutubeService = ServiceList.YouTube as YoutubeService

    init {
        try { NewPipe.init(OxyHttpDownloader()) } catch (e: Throwable) {}
    }

    private val pipedInstances = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.r4fo.com",
    )

    /** Search YouTube via Innertube (primary), NewPipe + Piped fallback. */
    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        Log.i(TAG, "search: '$query'")
        // 1. Innertube (primary, most reliable)
        try {
            val tracks = innertube.search(query)
            if (tracks.isNotEmpty()) {
                Log.i(TAG, "search SUCCESS via Innertube: ${tracks.size} tracks")
                return@withContext SearchResults(query, tracks)
            }
        } catch (e: Exception) {
            Log.w(TAG, "search Innertube failed: ${e.message}")
        }

        // 2. NewPipe fallback
        try {
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            val items = extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .filter {
                    it.streamType == org.schabi.newpipe.extractor.stream.StreamType.AUDIO_STREAM ||
                    it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM
                }
                .map { item ->
                    Track(
                        id = extractVideoId(item.url) ?: item.url,
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                        durationMs = (item.duration ?: 0L) * 1000L,
                    )
                }
            if (items.isNotEmpty()) {
                Log.i(TAG, "search SUCCESS via NewPipe: ${items.size} tracks")
                return@withContext SearchResults(query, items)
            }
        } catch (e: Exception) {
            Log.w(TAG, "search NewPipe failed: ${e.message}")
        }

        // 3. Piped fallback
        for (instance in pipedInstances) {
            val r = tryPipedSearch(instance, query)
            if (r.tracks.isNotEmpty()) {
                Log.i(TAG, "search SUCCESS via Piped: ${r.tracks.size} tracks")
                return@withContext r
            }
        }
        Log.w(TAG, "search: ALL sources failed for '$query'")
        SearchResults(query, emptyList())
    }

    /** Trending via Piped (most reliable). */
    suspend fun trending(region: String = "BR"): List<Track> = withContext(Dispatchers.IO) {
        for (instance in pipedInstances) {
            val url = "$instance/trending?region=$region"
            val raw = httpGet(url) ?: continue
            try {
                val items = JsonExtractor.splitArray(raw.ifEmpty { "[]" })
                val tracks = items.mapNotNull { itemStr ->
                    val u = JsonExtractor.extractString(itemStr, "url") ?: return@mapNotNull null
                    val t = JsonExtractor.extractString(itemStr, "title") ?: return@mapNotNull null
                    Track(
                        id = extractVideoId(u) ?: u,
                        title = t,
                        artist = JsonExtractor.extractString(itemStr, "uploaderName") ?: "Unknown",
                        thumbnailUrl = JsonExtractor.extractString(itemStr, "thumbnail") ?: "",
                        durationMs = (JsonExtractor.extractLong(itemStr, "duration") ?: 0L) * 1000L,
                    )
                }
                if (tracks.isNotEmpty()) return@withContext tracks
            } catch (e: Exception) { continue }
        }
        emptyList()
    }

    /**
     * Resolve stream URL. Returns ResolveResult with details about which source worked.
     */
    suspend fun resolveStream(track: Track): ResolveResult = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(track.id) ?: track.id
        Log.i(TAG, "resolveStream: videoId=$videoId title='${track.title}'")

        // 1. Innertube player endpoint (try multiple client types)
        try {
            val result = innertube.resolveStream(videoId)
            if (result != null) {
                Log.i(TAG, "resolveStream SUCCESS via ${result.sourceLabel}")
                return@withContext ResolveResult(
                    track = track.copy(
                        streamUrl = result.streamUrl,
                        durationMs = if (result.durationMs > 0) result.durationMs else track.durationMs,
                    ),
                    source = result.sourceLabel,
                    success = true,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveStream Innertube failed: ${e.message}")
        }

        // 2. NewPipe (uses user's residential IP)
        try {
            val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
            extractor.fetchPage()
            val info = StreamInfo.getInfo(extractor)
            val audio = info.audioStreams
                .filter { it.url != null }
                .maxByOrNull { it.bitrate }
            if (audio?.url != null) {
                Log.i(TAG, "resolveStream SUCCESS via NewPipe")
                return@withContext ResolveResult(
                    track = track.copy(
                        streamUrl = audio.url,
                        durationMs = info.duration * 1000L,
                        thumbnailUrl = track.thumbnailUrl.ifEmpty { info.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "" },
                        artist = track.artist.ifEmpty { info.uploaderName ?: "Unknown" },
                        title = track.title.ifEmpty { info.name ?: track.title },
                    ),
                    source = "NewPipe",
                    success = true,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "resolveStream NewPipe failed: ${e.message}")
        }

        // 3. Piped fallback (4 instances)
        for (instance in pipedInstances) {
            try {
                val url = "$instance/streams/$videoId"
                val raw = httpGet(url) ?: continue
                val audioStr = JsonExtractor.extractArray(raw, "audioStreams") ?: emptyList()
                val bestAudio = audioStr.mapNotNull { itemStr ->
                    val u = JsonExtractor.extractString(itemStr, "url") ?: return@mapNotNull null
                    val mime = JsonExtractor.extractString(itemStr, "mimeType") ?: ""
                    if (!mime.contains("audio")) return@mapNotNull null
                    val bitrate = JsonExtractor.extractLong(itemStr, "bitrate") ?: 0L
                    Triple(u, bitrate, itemStr)
                }.maxByOrNull { it.second }

                if (bestAudio != null) {
                    val (audioUrl, _, _) = bestAudio
                    Log.i(TAG, "resolveStream SUCCESS via Piped ($instance)")
                    return@withContext ResolveResult(
                        track = track.copy(
                            streamUrl = audioUrl,
                            durationMs = (JsonExtractor.extractLong(raw, "duration") ?: 0L) * 1000L,
                            thumbnailUrl = track.thumbnailUrl.ifEmpty { JsonExtractor.extractString(raw, "thumbnailUrl") ?: "" },
                            artist = track.artist.ifEmpty { JsonExtractor.extractString(raw, "uploader") ?: "Unknown" },
                            title = track.title.ifEmpty { JsonExtractor.extractString(raw, "title") ?: track.title },
                        ),
                        source = "Piped/${instance.substringAfterLast("/")}",
                        success = true,
                    )
                }

                // Fallback: video stream with audio (LBRY)
                val videoStr = JsonExtractor.extractArray(raw, "videoStreams") ?: emptyList()
                val videoWithAudio = videoStr.firstOrNull { vs ->
                    JsonExtractor.extractString(vs, "url") != null &&
                    JsonExtractor.extractBool(vs, "videoOnly") == false
                }
                if (videoWithAudio != null) {
                    Log.i(TAG, "resolveStream SUCCESS via Piped video ($instance)")
                    return@withContext ResolveResult(
                        track = track.copy(
                            streamUrl = JsonExtractor.extractString(videoWithAudio, "url"),
                            durationMs = (JsonExtractor.extractLong(raw, "duration") ?: 0L) * 1000L,
                            thumbnailUrl = track.thumbnailUrl.ifEmpty { JsonExtractor.extractString(raw, "thumbnailUrl") ?: "" },
                        ),
                        source = "Piped-video/${instance.substringAfterLast("/")}",
                        success = true,
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "resolveStream Piped $instance failed: ${e.message}")
            }
        }

        Log.w(TAG, "resolveStream: ALL sources FAILED for videoId=$videoId")
        ResolveResult(
            track = track,
            source = "nenhuma",
            success = false,
            error = "Todas as fontes falharam (Innertube, NewPipe, Piped). YouTube pode estar bloqueando seu IP. Tente outra música."
        )
    }

    private fun tryPipedSearch(instance: String, query: String): SearchResults {
        val url = "$instance/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=videos"
        val raw = httpGet(url) ?: return SearchResults(query, emptyList())
        return try {
            val itemsArr = JsonExtractor.extractArray(raw, "items") ?: emptyList()
            val tracks = itemsArr.mapNotNull { itemStr ->
                val u = JsonExtractor.extractString(itemStr, "url") ?: return@mapNotNull null
                val t = JsonExtractor.extractString(itemStr, "title") ?: return@mapNotNull null
                Track(
                    id = extractVideoId(u) ?: u,
                    title = t,
                    artist = JsonExtractor.extractString(itemStr, "uploaderName") ?: "Unknown",
                    thumbnailUrl = JsonExtractor.extractString(itemStr, "thumbnail") ?: "",
                    durationMs = (JsonExtractor.extractLong(itemStr, "duration") ?: 0L) * 1000L,
                )
            }
            SearchResults(query, tracks)
        } catch (e: Exception) { SearchResults(query, emptyList()) }
    }

    private fun httpGet(url: String): String? {
        return try {
            val req = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) { null }
    }

    private fun extractVideoId(url: String): String? {
        Regex("""[?&]v=([\w-]{11})""").find(url)?.let { return it.groupValues[1] }
        Regex("""youtu\.be/([\w-]{11})""").find(url)?.let { return it.groupValues[1] }
        Regex("""/embed/([\w-]{11})""").find(url)?.let { return it.groupValues[1] }
        if (url.length == 11 && url.all { it.isLetterOrDigit() || it == '_' || it == '-' }) return url
        return null
    }

    companion object {
        private const val TAG = "YouTubeRepository"
    }
}

data class ResolveResult(
    val track: Track,
    val source: String,
    val success: Boolean,
    val error: String? = null,
)
