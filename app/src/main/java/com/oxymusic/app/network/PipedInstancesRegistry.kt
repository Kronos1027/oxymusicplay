package com.oxymusic.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamic registry of public Piped instances with real health-check.
 *
 * Before v2.0.0: hardcoded list of 4 instances, several of which are dead.
 * Now: probes each candidate instance's /healthcheck endpoint, keeps only the live ones,
 * and rotates between them. Falls back to the hardcoded list if health-check fails entirely.
 *
 * The list of candidate instances is itself updateable — when the public Piped instance
 * list (https://piped-instances.kavin.rocks/) is reachable, we use that as the source of truth.
 */
@Singleton
class PipedInstancesRegistry @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    /** Hardcoded fallback list (used if the public registry is unreachable). */
    private val fallbackInstances = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.r4fo.com",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.reallyaweso.me",
        "https://pipedapi.nosebs.ru",
        "https://pipedapi.ducks.party",
        "https://pipedapi.smnz.de",
        "https://pipedapi.darkness.services",
        "https://pipedapi.drus.rs",
        "https://pipedapi.r2.va",
        "https://api.piped.yt",
        "https://pipedapi.csb.dev",
        "https://pipedapi.hostux.net",
    )

    @Volatile
    private var liveInstances: List<String> = emptyList()

    @Volatile
    private var lastRefreshMs: Long = 0

    /**
     * Returns the current list of healthy Piped instances.
     * Refreshes at most once every 10 minutes.
     */
    suspend fun getLiveInstances(): List<String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (liveInstances.isNotEmpty() && (now - lastRefreshMs) < REFRESH_INTERVAL_MS) {
            return@withContext liveInstances
        }
        refresh()
        liveInstances.ifEmpty { fallbackInstances }
    }

    /**
     * Forces a refresh of the live instances list.
     * Tries the public registry first; if that fails, probes the fallback list directly.
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Refreshing Piped instances...")
        val fromRegistry = tryFetchFromRegistry()
        val candidates = fromRegistry.ifEmpty { fallbackInstances }

        val live = mutableListOf<String>()
        for (instance in candidates) {
            if (probeHealth(instance)) {
                live.add(instance)
                Log.d(TAG, "  ✓ live: $instance")
            } else {
                Log.d(TAG, "  ✗ dead: $instance")
            }
        }

        if (live.isNotEmpty()) {
            liveInstances = live
            lastRefreshMs = System.currentTimeMillis()
            Log.i(TAG, "Found ${live.size} live Piped instances")
        } else {
            Log.w(TAG, "All Piped instances failed health-check; keeping fallback list")
        }
    }

    /** Fetches the public list of Piped instances from kavin.rocks. */
    private fun tryFetchFromRegistry(): List<String> {
        return try {
            val req = Request.Builder()
                .url("https://piped-instances.kavin.rocks/")
                .header("User-Agent", "OxyMusic/2.0 (Android)")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val raw = resp.body?.string() ?: return emptyList()
                // Parse JSON array: [{"api_url": "https://...", ...}, ...]
                val arr = org.json.JSONArray(raw)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val url = obj.optString("api_url", "").ifEmpty { obj.optString("apiUrl", "") }
                    if (url.startsWith("http")) list.add(url.trimEnd('/'))
                }
                Log.d(TAG, "Registry returned ${list.size} instances")
                list
            }
        } catch (e: Exception) {
            Log.w(TAG, "Registry fetch failed: ${e.message}")
            emptyList()
        }
    }

    /** Probes /healthcheck on an instance. Returns true if it responds 200 within 5s. */
    private fun probeHealth(instance: String): Boolean {
        return try {
            val req = Request.Builder()
                .url("$instance/healthcheck")
                .header("User-Agent", "OxyMusic/2.0 (Android)")
                .build()
            client.newCall(req).execute().use { resp ->
                resp.code == 200
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "PipedInstancesRegistry"
        private const val REFRESH_INTERVAL_MS = 10 * 60 * 1000L  // 10 min
    }
}
