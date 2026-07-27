package com.oxymusic.app.lyrics

import com.oxymusic.app.model.LyricLine
import com.oxymusic.app.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LrclibClient @Inject constructor() {
    private val client = OkHttpClient.Builder().build()

    suspend fun fetch(trackName: String, artistName: String, durationSec: Long? = null): Lyrics? =
        withContext(Dispatchers.IO) {
            val url = buildString {
                append("https://lrclib.net/api/get?track_name=")
                append(java.net.URLEncoder.encode(trackName, "UTF-8"))
                append("&artist_name=")
                append(java.net.URLEncoder.encode(artistName, "UTF-8"))
                if (durationSec != null) { append("&duration="); append(durationSec) }
            }
            val req = Request.Builder().url(url).header("User-Agent", "OxyMusic/1.0").build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string() ?: return@withContext null
                    val synced = com.oxymusic.app.network.JsonExtractor.extractString(body, "syncedLyrics")
                    val plain = com.oxymusic.app.network.JsonExtractor.extractString(body, "plainLyrics")
                    if (!synced.isNullOrBlank()) {
                        val lines = LrcParser.parseLines(synced)
                        if (lines.isNotEmpty()) return@withContext Lyrics("$trackName|$artistName", lines, true, plain)
                    }
                    if (!plain.isNullOrBlank()) {
                        return@withContext Lyrics(
                            "$trackName|$artistName",
                            plain.lines().mapIndexed { i, l -> LyricLine(i * 4000L, l) },
                            false, plain
                        )
                    }
                    null
                }
            } catch (e: Exception) { null }
        }
}

object LrcParser {
    private val lineRegex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{2,3}))?]""")
    fun parseLines(lrc: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        lrc.lines().forEach { rawLine ->
            val matches = lineRegex.findAll(rawLine).toList()
            if (matches.isEmpty()) return@forEach
            val text = rawLine.substring(matches.last().range.last + 1).trim()
            matches.forEach { m ->
                val (min, sec, msStr) = m.destructured
                val timeMs = min.toLong() * 60_000L + sec.toLong() * 1000L +
                    (msStr.takeIf { it.isNotEmpty() }?.padEnd(3, '0')?.toLong() ?: 0L)
                lines.add(LyricLine(timeMs, text))
            }
        }
        return lines.sortedBy { it.timeMs }
    }
}
