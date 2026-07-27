package com.oxymusic.app.network

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
 * Priority:
 * 1. NewPipeExtractor — uses user's residential IP. URLs signed for user's IP = playable.
 * 2. Piped API (4 instances) — fallback when NewPipe fails.
 *
 * Same approach as InnerTune / RiMusic / ViMusic.
 * NO API key needed.
 */
@Singleton
class YouTubeRepository @Inject constructor() {

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

    /** Search via NewPipe (primary), fallback Piped. */
    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        // Try NewPipe first
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
                        id = item.url,
                        title = item.name,
                        artist = item.uploaderName ?: "Unknown",
                        thumbnailUrl = item.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "",
                        durationMs = (item.duration ?: 0L) * 1000L,
                    )
                }
            if (items.isNotEmpty()) return@withContext SearchResults(query, items)
        } catch (e: Exception) {
            // fall through to Piped
        }

        // Fallback: Piped
        for (instance in pipedInstances) {
            val r = tryPipedSearch(instance, query)
            if (r.tracks.isNotEmpty()) return@withContext r
        }
        SearchResults(query, emptyList())
    }

    private fun tryPipedSearch(instance: String, query: String): SearchResults {
        val url = "$instance/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=videos"
        val raw = httpGet(url) ?: return SearchResults(query, emptyList())
        return try {
            val resp = parseJson<PipedSearchResponse>(raw)
            val tracks = resp.items.mapNotNull { item ->
                if (item.url.isNullOrEmpty() || item.title.isNullOrEmpty()) null
                else Track(
                    id = extractVideoId(item.url) ?: item.url,
                    title = item.title,
                    artist = item.uploaderName ?: "Unknown",
                    thumbnailUrl = item.thumbnail ?: "",
                    durationMs = (item.duration ?: 0L) * 1000L,
                )
            }
            SearchResults(query, tracks)
        } catch (e: Exception) { SearchResults(query, emptyList()) }
    }

    /** Trending via Piped (NewPipe doesn't expose trending easily). */
    suspend fun trending(region: String = "BR"): List<Track> = withContext(Dispatchers.IO) {
        for (instance in pipedInstances) {
            val url = "$instance/trending?region=$region"
            val raw = httpGet(url) ?: continue
            try {
                val items = parseJson<List<PipedTrendingItem>>(raw)
                val tracks = items.mapNotNull { item ->
                    if (item.url.isNullOrEmpty() || item.title.isNullOrEmpty()) null
                    else Track(
                        id = extractVideoId(item.url) ?: item.url,
                        title = item.title,
                        artist = item.uploaderName ?: "Unknown",
                        thumbnailUrl = item.thumbnail ?: "",
                        durationMs = (item.duration ?: 0L) * 1000L,
                    )
                }
                if (tracks.isNotEmpty()) return@withContext tracks
            } catch (e: Exception) { continue }
        }
        emptyList()
    }

    /**
     * Resolve stream URL. NewPipe first (user IP), Piped fallback.
     * Returns null if no source worked.
     */
    suspend fun resolveStream(track: Track): Track? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(track.id) ?: track.id

        // 1. NewPipe (primary — uses user's residential IP, URLs signed for user)
        try {
            val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId")
            extractor.fetchPage()
            val info = StreamInfo.getInfo(extractor)
            val audio = info.audioStreams
                .filter { it.url != null }
                .maxByOrNull { it.bitrate }
            if (audio?.url != null) {
                return@withContext track.copy(
                    streamUrl = audio.url,
                    durationMs = info.duration * 1000L,
                    thumbnailUrl = track.thumbnailUrl.ifEmpty { info.thumbnails.maxByOrNull { it.height * it.width }?.url ?: "" },
                    artist = track.artist.ifEmpty { info.uploaderName ?: "Unknown" },
                    title = track.title.ifEmpty { info.name ?: track.title },
                )
            }
        } catch (e: Exception) {
            // fall through to Piped
        }

        // 2. Piped fallback
        for (instance in pipedInstances) {
            val url = "$instance/streams/$videoId"
            val raw = httpGet(url) ?: continue
            try {
                val resp = parseJson<PipedStreamsResponse>(raw)
                val audio = resp.audioStreams
                    .filter { it.url != null && it.mimeType?.contains("audio") == true }
                    .maxByOrNull { it.bitrate ?: 0 }
                if (audio?.url != null) {
                    return@withContext track.copy(
                        streamUrl = audio.url,
                        durationMs = (resp.duration ?: 0L) * 1000L,
                        thumbnailUrl = track.thumbnailUrl.ifEmpty { resp.thumbnailUrl ?: "" },
                        artist = track.artist.ifEmpty { resp.uploader ?: "Unknown" },
                        title = track.title.ifEmpty { resp.title ?: track.title },
                    )
                }
                // Fallback: video stream with audio (LBRY)
                val video = resp.videoStreams.firstOrNull { !it.videoOnly && it.url != null }
                if (video?.url != null) {
                    return@withContext track.copy(
                        streamUrl = video.url,
                        durationMs = (resp.duration ?: 0L) * 1000L,
                        thumbnailUrl = track.thumbnailUrl.ifEmpty { resp.thumbnailUrl ?: "" },
                    )
                }
            } catch (e: Exception) { continue }
        }

        null
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

    // JSON parsing helpers (manual to avoid kotlinx.serialization complexity)
    private inline fun <reified T> parseJson(s: String): T {
        return when (T::class) {
            PipedSearchResponse::class -> PipedSearchResponse.parse(s) as T
            PipedStreamsResponse::class -> PipedStreamsResponse.parse(s) as T
            List::class -> PipedTrendingItem.parseList(s) as T
            else -> throw IllegalArgumentException("Unsupported type")
        }
    }
}

// ===== Piped DTOs with manual JSON parsing =====
data class PipedSearchResponse(val items: List<PipedSearchItem>) {
    companion object {
        fun parse(s: String): PipedSearchResponse {
            val items = mutableListOf<PipedSearchItem>()
            val itemsArray = JsonExtractor.extractArray(s, "items") ?: return PipedSearchResponse(emptyList())
            for (itemStr in itemsArray) {
                items.add(PipedSearchItem(
                    url = JsonExtractor.extractString(itemStr, "url"),
                    title = JsonExtractor.extractString(itemStr, "title"),
                    uploaderName = JsonExtractor.extractString(itemStr, "uploaderName"),
                    thumbnail = JsonExtractor.extractString(itemStr, "thumbnail"),
                    duration = JsonExtractor.extractLong(itemStr, "duration"),
                ))
            }
            return PipedSearchResponse(items)
        }
    }
}

data class PipedSearchItem(
    val url: String?, val title: String?, val uploaderName: String?,
    val thumbnail: String?, val duration: Long?,
)

data class PipedTrendingItem(
    val url: String?, val title: String?, val uploaderName: String?,
    val thumbnail: String?, val duration: Long?,
) {
    companion object {
        fun parseList(s: String): List<PipedTrendingItem> {
            val items = mutableListOf<PipedTrendingItem>()
            // s should be a JSON array
            val arrayContent = if (s.trim().startsWith("[")) s.trim() else "[]"
            val elements = JsonExtractor.splitArray(arrayContent)
            for (itemStr in elements) {
                items.add(PipedTrendingItem(
                    url = JsonExtractor.extractString(itemStr, "url"),
                    title = JsonExtractor.extractString(itemStr, "title"),
                    uploaderName = JsonExtractor.extractString(itemStr, "uploaderName"),
                    thumbnail = JsonExtractor.extractString(itemStr, "thumbnail"),
                    duration = JsonExtractor.extractLong(itemStr, "duration"),
                ))
            }
            return items
        }
    }
}

data class PipedStreamsResponse(
    val title: String?, val uploader: String?, val duration: Long?,
    val thumbnailUrl: String?,
    val audioStreams: List<PipedStream>, val videoStreams: List<PipedStream>,
) {
    companion object {
        fun parse(s: String): PipedStreamsResponse {
            val audioStr = JsonExtractor.extractArray(s, "audioStreams") ?: emptyList()
            val videoStr = JsonExtractor.extractArray(s, "videoStreams") ?: emptyList()
            return PipedStreamsResponse(
                title = JsonExtractor.extractString(s, "title"),
                uploader = JsonExtractor.extractString(s, "uploader"),
                duration = JsonExtractor.extractLong(s, "duration"),
                thumbnailUrl = JsonExtractor.extractString(s, "thumbnailUrl"),
                audioStreams = audioStr.map { PipedStream.parse(it) },
                videoStreams = videoStr.map { PipedStream.parse(it) },
            )
        }
    }
}

data class PipedStream(
    val url: String?, val mimeType: String?, val bitrate: Long?,
    val quality: String?, val format: String?, val videoOnly: Boolean,
) {
    companion object {
        fun parse(s: String): PipedStream {
            return PipedStream(
                url = JsonExtractor.extractString(s, "url"),
                mimeType = JsonExtractor.extractString(s, "mimeType"),
                bitrate = JsonExtractor.extractLong(s, "bitrate"),
                quality = JsonExtractor.extractString(s, "quality"),
                format = JsonExtractor.extractString(s, "format"),
                videoOnly = JsonExtractor.extractBool(s, "videoOnly"),
            )
        }
    }
}
