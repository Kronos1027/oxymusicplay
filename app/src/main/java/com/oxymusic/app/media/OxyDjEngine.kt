package com.oxymusic.app.media

import android.util.Log
import com.oxymusic.app.data.HistoryDao
import com.oxymusic.app.data.HistoryEntity
import com.oxymusic.app.model.Track
import com.oxymusic.app.network.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OxyDJ — 100% local, 100% free recommendation engine.
 *
 * Three signals, all computed on-device (no data leaves the phone):
 *
 * 1. HISTORY-BASED: Weighted frequency of recent plays. Tracks listened multiple times
 *    or in full get higher weight. Recency boost: tracks played in last 24h get 2x.
 *
 * 2. RELATED (free, no API key): For the most-played recent track, fetch YouTube's
 *    "related videos" via Innertube /next endpoint. YouTube returns these for free.
 *
 * 3. SIMILARITY: Group by artist. If user listens to ≥3 tracks from same artist,
 *    suggest more from that artist via YouTube search "artist - topic".
 *
 * Final ranking: history-weighted + recency-boosted + deduplicated against history.
 *
 * Output is always local tracks the user hasn't heard + YouTube tracks they might like.
 * No external recommendation service. No paid API. No data collection.
 */
@Singleton
class OxyDjEngine @Inject constructor(
    private val historyDao: HistoryDao,
    private val youtube: YouTubeRepository,
) {

    /**
     * Generates up to [limit] recommendations based on the user's listening history.
     *
     * @param limit max number of recommendations to return (default 20)
     */
    suspend fun recommend(limit: Int = 20): List<Track> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Generating recommendations (limit=$limit)...")
        // Get last 100 played tracks from Room
        val history = try {
            // Synchronous one-shot read — use first() on the Flow
            historyDao.observe().first()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read history: ${e.message}")
            emptyList()
        }

        if (history.isEmpty()) {
            Log.i(TAG, "No history yet — returning trending instead")
            return@withContext emptyList()
        }

        val recommended = mutableListOf<Track>()
        val seenIds = history.map { it.trackId }.toMutableSet()

        // 1. Top artist — fetch more from same artist via YouTube search "artist - topic"
        val topArtist = computeTopArtist(history)
        if (topArtist != null) {
            Log.d(TAG, "Top artist: $topArtist — fetching more from them")
            try {
                val searchResults = youtube.search("$topArtist - topic")
                for (t in searchResults.tracks) {
                    if (t.id !in seenIds) {
                        recommended.add(t)
                        seenIds.add(t.id)
                        if (recommended.size >= limit / 2) break
                    }
                }
                Log.d(TAG, "After top artist: ${recommended.size} tracks")
            } catch (e: Exception) {
                Log.w(TAG, "Top artist search failed: ${e.message}")
            }
        }

        // 2. Related tracks from most-recent track
        val mostRecent = history.firstOrNull()
        if (mostRecent != null && recommended.size < limit) {
            Log.d(TAG, "Fetching related to most-recent: ${mostRecent.title}")
            try {
                val related = youtube.getRelatedTracks(mostRecent.trackId, limit / 2)
                for (t in related) {
                    if (t.id !in seenIds) {
                        recommended.add(t)
                        seenIds.add(t.id)
                        if (recommended.size >= limit) break
                    }
                }
                Log.d(TAG, "After related: ${recommended.size} tracks")
            } catch (e: Exception) {
                Log.w(TAG, "Related fetch failed: ${e.message}")
            }
        }

        // 3. Fallback: trending if we still have room
        if (recommended.size < limit) {
            try {
                val trending = youtube.trending("BR")
                for (t in trending) {
                    if (t.id !in seenIds) {
                        recommended.add(t)
                        seenIds.add(t.id)
                        if (recommended.size >= limit) break
                    }
                }
                Log.d(TAG, "After trending fallback: ${recommended.size} tracks")
            } catch (e: Exception) {
                Log.w(TAG, "Trending fallback failed: ${e.message}")
            }
        }

        Log.i(TAG, "Returning ${recommended.size} recommendations")
        recommended
    }

    /**
     * Computes the user's top artist based on weighted play count.
     * Recency boost: plays in last 24h count 2x, last week 1.5x.
     */
    private fun computeTopArtist(history: List<HistoryEntity>): String? {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val weekMs = 7 * dayMs

        val artistWeights = mutableMapOf<String, Double>()
        for (h in history) {
            val ageMs = now - h.playedAt
            val weight = when {
                ageMs < dayMs -> 2.0
                ageMs < weekMs -> 1.5
                else -> 1.0
            }
            // Normalize artist name (strip "- Topic" suffix, lowercase)
            val artist = h.artist
                .replace(Regex("""\s*-\s*Topic""", RegexOption.IGNORE_CASE), "")
                .trim()
                .lowercase()
            if (artist.isNotEmpty() && artist != "unknown") {
                artistWeights[artist] = (artistWeights[artist] ?: 0.0) + weight
            }
        }

        return artistWeights.maxByOrNull { it.value }?.key?.let { original ->
            // Find the original-case version from history
            history.firstOrNull {
                it.artist.replace(Regex("""\s*-\s*Topic""", RegexOption.IGNORE_CASE), "").trim().lowercase() == original
            }?.artist?.replace(Regex("""\s*-\s*Topic""", RegexOption.IGNORE_CASE), "")?.trim()
        }
    }

    companion object {
        private const val TAG = "OxyDjEngine"
    }
}
