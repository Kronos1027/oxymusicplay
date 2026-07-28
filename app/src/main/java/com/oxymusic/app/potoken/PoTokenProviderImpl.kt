package com.oxymusic.app.potoken

import android.content.Context
import android.util.Log
import android.webkit.WebView
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implementation of NewPipeExtractor's [PoTokenProvider] that generates poTokens via
 * [PoTokenWebView] (BotGuard in a system WebView).
 *
 * Ported from NewPipe's PoTokenProviderImpl.kt — uses Kotlin coroutines instead of RxJava.
 *
 * Tokens are cached per visitorData (one generator per visitorData, reused across videos).
 * If WebView is unavailable or broken, all methods return null and the extractor falls back
 * to no-poToken mode (URLs may still work, or may get 403).
 */
class PoTokenProviderImpl(private val appContext: Context) : PoTokenProvider {

    private val webViewSupported by lazy {
        try {
            WebView.getCurrentWebViewPackage() != null
        } catch (e: Throwable) {
            // WebView might not be present on some AOSP-based ROMs
            Log.w(TAG, "WebView not available: ${e.message}")
            false
        }
    }
    private var webViewBadImpl = false

    private val webPoTokenGenLock = ReentrantLock()
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (!webViewSupported || webViewBadImpl) return null

        return try {
            getWebClientPoToken(videoId, forceRecreate = false)
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is BadWebViewException -> {
                    Log.e(TAG, "WebView is broken — disabling poToken provider", e)
                    webViewBadImpl = true
                    null
                }
                null -> { Log.e(TAG, "poToken generation failed (no cause)", e); null }
                else -> { Log.e(TAG, "poToken generation failed: ${cause.message}", cause); null }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getWebClientPoToken failed: ${e.message}", e)
            null
        }
    }

    private fun getWebClientPoToken(videoId: String, forceRecreate: Boolean): PoTokenResult {
        data class Quad<T1, T2, T3, T4>(val first: T1, val second: T2, val third: T3, val fourth: T4)
        val (poTokenGenerator, visitorData, streamingPot, hasBeenRecreated) = webPoTokenGenLock.withLock {
            val shouldRecreate = webPoTokenGenerator == null || forceRecreate || webPoTokenGenerator!!.isExpired()

            if (shouldRecreate) {
                // Get visitorData by hitting the YouTube homepage
                webPoTokenVisitorData = fetchVisitorData()
                if (webPoTokenVisitorData == null) {
                    throw PoTokenException("Could not fetch visitorData")
                }

                // Close old generator on main thread
                webPoTokenGenerator?.let { old ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post { old.close() }
                }

                // Create new generator — must run on main thread (WebView requirement)
                webPoTokenGenerator = kotlinx.coroutines.runBlocking {
                    PoTokenWebView.newPoTokenGenerator(appContext)
                }

                // Generate the streaming poToken first (one-time)
                webPoTokenStreamingPot = kotlinx.coroutines.runBlocking {
                    webPoTokenGenerator!!.generatePoToken(webPoTokenVisitorData!!)
                }
            }

            Quad(
                webPoTokenGenerator!!,
                webPoTokenVisitorData!!,
                webPoTokenStreamingPot!!,
                shouldRecreate
            )
        }

        val playerPot = try {
            kotlinx.coroutines.runBlocking { poTokenGenerator.generatePoToken(videoId) }
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                throw throwable
            } else {
                Log.e(TAG, "Failed to obtain poToken, retrying with fresh generator", throwable)
                return getWebClientPoToken(videoId, forceRecreate = true)
            }
        }

        Log.i(TAG, "poToken generated for $videoId (len=${playerPot.length})")
        return PoTokenResult(visitorData, playerPot, streamingPot)
    }

    /** Fetches visitorData by hitting youtube.com — used as minter for streaming poToken. */
    private fun fetchVisitorData(): String? {
        return try {
            val req = okhttp3.Request.Builder()
                .url("https://www.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0.0.0 Safari/537.36")
                .build()
            okhttp3.OkHttpClient().newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: return null
                Regex(""""visitorData":"([^"]+)"""").find(body)?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchVisitorData failed: ${e.message}")
            null
        }
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null
    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null
    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null

    companion object {
        private const val TAG = "PoTokenProviderImpl"
    }
}
