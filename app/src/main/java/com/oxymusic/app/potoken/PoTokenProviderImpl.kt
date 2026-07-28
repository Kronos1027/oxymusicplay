/*
 * Copyright (C) NewPipe Contributors
 * Licensed under GPLv3 — see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Vendored from https://github.com/TeamNewPipe/NewPipe (PR #11955)
 * Adapted for OxyMusic:
 *   - package renamed
 *   - object → class (takes Context via constructor, since OxyMusic has no App.instance singleton)
 *   - RxJava3 Single → Kotlin coroutines (runBlocking on extractor thread)
 *   - DeviceUtils.supportsWebView() → inline implementation using CookieManager
 *   - App.instance → appContext passed via constructor
 *
 * LOGIC PRESERVED VERBATIM:
 *   - Uses InnertubeClientRequestInfo.ofWebClient() and YoutubeParsingHelper.getVisitorDataFromInnertube()
 *     to get visitorData (same as NewPipe)
 *   - Creates PoTokenWebView.newPoTokenGenerator(appContext) on main thread
 *   - Generates streaming poToken first, then player poToken per videoId
 *   - Caches webPoTokenGenerator + visitorData + streamingPot until expired
 *   - Recreates generator on error (forceRecreate=true) with one retry
 */
package com.oxymusic.app.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PoTokenProviderImpl(private val appContext: Context) : PoTokenProvider {

    private val webViewSupported by lazy { supportsWebView() }
    private var webViewBadImpl = false

    private val webPoTokenGenLock = ReentrantLock()
    private var webPoTokenVisitorData: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenGenerator? = null

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        if (!webViewSupported || webViewBadImpl) {
            Log.w(TAG, "WebView not supported or marked bad — skipping poToken")
            return null
        }

        return try {
            getWebClientPoToken(videoId = videoId, forceRecreate = false)
        } catch (e: RuntimeException) {
            when (val cause = e.cause) {
                is BadWebViewException -> {
                    Log.e(TAG, "Could not obtain poToken because WebView is broken", e)
                    webViewBadImpl = true
                    null
                }
                null -> {
                    Log.e(TAG, "poToken generation failed (no cause): ${e.message}", e)
                    null
                }
                else -> {
                    Log.e(TAG, "poToken generation failed: ${cause.message}", cause)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getWebClientPoToken failed: ${e.message}", e)
            null
        }
    }

    private fun getWebClientPoToken(videoId: String, forceRecreate: Boolean): PoTokenResult {
        data class Quadruple<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4)

        val (poTokenGenerator, visitorData, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate = webPoTokenGenerator == null || forceRecreate ||
                    webPoTokenGenerator!!.isExpired()

                if (shouldRecreate) {
                    Log.i(TAG, "Creating new PoTokenGenerator (forceRecreate=$forceRecreate)")

                    // Get visitorData using NewPipe's YoutubeParsingHelper (same as NewPipe app)
                    val innertubeClientRequestInfo = InnertubeClientRequestInfo.ofWebClient()
                    innertubeClientRequestInfo.clientInfo.clientVersion =
                        YoutubeParsingHelper.getClientVersion()

                    webPoTokenVisitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
                        innertubeClientRequestInfo,
                        NewPipe.getPreferredLocalization(),
                        NewPipe.getPreferredContentCountry(),
                        YoutubeParsingHelper.getYouTubeHeaders(),
                        YoutubeParsingHelper.YOUTUBEI_V1_URL,
                        null,
                        false
                    )
                    Log.i(TAG, "Got visitorData (len=${webPoTokenVisitorData!!.length})")

                    // close the current webPoTokenGenerator on the main thread
                    webPoTokenGenerator?.let { Handler(Looper.getMainLooper()).post { it.close() } }

                    // create a new webPoTokenGenerator (must be on main thread — PoTokenWebView handles that)
                    webPoTokenGenerator = runBlocking {
                        PoTokenWebView.newPoTokenGenerator(appContext)
                    }

                    // The streaming poToken needs to be generated exactly once before generating
                    // any other (player) tokens.
                    webPoTokenStreamingPot = runBlocking {
                        webPoTokenGenerator!!.generatePoToken(webPoTokenVisitorData!!)
                    }
                    Log.i(TAG, "Generated streaming poToken (len=${webPoTokenStreamingPot!!.length})")
                }

                return@withLock Quadruple(
                    webPoTokenGenerator!!,
                    webPoTokenVisitorData!!,
                    webPoTokenStreamingPot!!,
                    shouldRecreate
                )
            }

        val playerPot = try {
            // Not using synchronized here, since poTokenGenerator would be able to generate
            // multiple poTokens in parallel if needed. The only important thing is for exactly one
            // visitorData/streaming poToken to be generated before anything else.
            runBlocking { poTokenGenerator.generatePoToken(videoId) }
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                // the poTokenGenerator has just been recreated (and possibly this is already the
                // second time we try), so there is likely nothing we can do
                throw throwable
            } else {
                // retry, this time recreating the [webPoTokenGenerator] from scratch;
                // this might happen for example if NewPipe goes in the background and the WebView
                // content is lost
                Log.e(TAG, "Failed to obtain poToken, retrying with fresh generator", throwable)
                return getWebClientPoToken(videoId = videoId, forceRecreate = true)
            }
        }

        Log.i(TAG, "poToken generated for videoId=$videoId (playerPot len=${playerPot.length}, streamingPot len=${streamingPot.length})")
        return PoTokenResult(visitorData, playerPot, streamingPot)
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null
    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null
    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null

    private fun supportsWebView(): Boolean {
        return try {
            CookieManager.getInstance()
            true
        } catch (t: Throwable) {
            Log.w(TAG, "WebView not supported: ${t.message}")
            false
        }
    }

    companion object {
        private const val TAG = "PoTokenProviderImpl"
    }
}
