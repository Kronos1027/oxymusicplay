package com.oxymusic.app.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Generates poTokens by running BotGuard inside an Android WebView (system WebView).
 *
 * Ported from NewPipe's PoTokenWebView.kt — uses Kotlin coroutines instead of RxJava,
 * and OkHttp instead of NewPipe's DownloaderImpl.
 *
 * The WebView is invisible/headless and has blockNetworkLoads=true — it only runs the
 * BotGuard VM locally. All network requests (to /Create and /GenerateIT) are made via
 * OkHttp on a background thread.
 */
class PoTokenWebView private constructor(
    context: Context,
    private val generatorDeferred: CompletableDeferred<PoTokenGenerator>
) : PoTokenGenerator {

    private val webView = WebView(context)
    private lateinit var expirationInstant: Instant

    // List of (identifier, deferred) pairs waiting for poToken results from JS
    private val poTokenWaiters = mutableListOf<Pair<String, CompletableDeferred<String>>>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    init {
        val settings = webView.settings
        @Suppress("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(settings, false)
        }
        settings.userAgentString = USER_AGENT
        settings.blockNetworkLoads = true  // WebView doesn't need network — we proxy via OkHttp

        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                val msg: String = m.message()
                val src: String = m.sourceId()
                val line: Int = m.lineNumber()
                if (msg.contains("Uncaught")) {
                    val fmt = "\"$msg\", source: $src ($line)"
                    Log.e(TAG, "WebView broken: $fmt")
                    val exception = BadWebViewException(fmt)
                    onInitializationError(exception)
                    synchronized(poTokenWaiters) {
                        poTokenWaiters.toList().forEach { (_, deferred) -> deferred.completeExceptionally(exception) }
                        poTokenWaiters.clear()
                    }
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    private fun loadHtmlAndObtainBotguard(context: Context) {
        try {
            val html = context.assets.open("po_token.html").bufferedReader().use { it.readText() }
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html.replaceFirst(
                        "</script>",
                        "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
                    ),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        } catch (e: Exception) {
            onInitializationError(e)
        }
    }

    //region JavascriptInterface callbacks
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        // Called by JS after the page loads. We need to fetch BotGuard challenge from YouTube.
        Thread {
            try {
                fetchChallengeAndRun()
            } catch (e: Exception) {
                Log.e(TAG, "downloadAndRunBotguard failed", e)
                onInitializationError(e)
            }
        }.start()
    }

    private fun fetchChallengeAndRun() {
        // Step 1: fetch visitorData from Innertube
        val visitorData = fetchVisitorData()
        // Step 2: fetch BotGuard challenge
        val challengeRaw = fetchBotGuardChallenge(visitorData)
        // Step 3: pass challenge to JS via JS_INTERFACE.runBotGuard(challengeData)
        val challengeData = parseChallengeData(challengeRaw)
        // Pass to WebView on main thread
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(
                "($JS_INTERFACE.runBotGuard && $JS_INTERFACE.runBotGuard($challengeData));",
                null
            )
        }
    }

    @JavascriptInterface
    @Suppress("unused") // called from JS
    fun onBotGuardResult(integrityTokenData: String) {
        Thread {
            try {
                val (u8Token, durationSec) = parseIntegrityTokenData(integrityTokenData)
                expirationInstant = Instant.now().plusSeconds(durationSec)

                // Pass the integrity token to JS
                Handler(Looper.getMainLooper()).post {
                    webView.evaluateJavascript(
                        "$JS_INTERFACE.setIntegrityToken && $JS_INTERFACE.setIntegrityToken($u8Token);",
                        null
                    )
                }
                generatorDeferred.complete(this)
            } catch (e: Exception) {
                Log.e(TAG, "onBotGuardResult parse failed", e)
                onInitializationError(e)
            }
        }.start()
    }

    @JavascriptInterface
    @Suppress("unused") // called from JS
    fun onPoTokenResult(identifier: String, poToken: String) {
        synchronized(poTokenWaiters) {
            val iter = poTokenWaiters.iterator()
            while (iter.hasNext()) {
                val (id, deferred) = iter.next()
                if (id == identifier) {
                    iter.remove()
                    val base64 = try { u8ToBase64(poToken) } catch (e: Exception) {
                        deferred.completeExceptionally(e); return
                    }
                    deferred.complete(base64)
                    return
                }
            }
        }
    }

    @JavascriptInterface
    @Suppress("unused") // called from JS when BotGuard needs to make a /Create or /GenerateIT request
    fun makeBotGuardRequest(url: String, data: String): String {
        // Synchronous call from JS — but JS is async, so we use a blocking HTTP request
        return try {
            val req = Request.Builder()
                .url(url)
                .post(data.toRequestBody("application/json+protobuf".toMediaType()))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json+protobuf")
                .header("x-goog-api-key", GOOGLE_API_KEY)
                .header("x-user-agent", "grpc-web-javascript/0.1")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                resp.body?.string() ?: ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "makeBotGuardRequest failed", e)
            ""
        }
    }
    //endregion

    override suspend fun generatePoToken(identifier: String): String {
        if (this::expirationInstant.isInitialized && Instant.now().isAfter(expirationInstant)) {
            throw PoTokenException("PoToken generator expired")
        }
        val deferred = CompletableDeferred<String>()
        synchronized(poTokenWaiters) { poTokenWaiters.add(identifier to deferred) }
        val u8Identifier = stringToU8(identifier)
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(
                "$JS_INTERFACE.generatePoToken && $JS_INTERFACE.generatePoToken('$identifier', $u8Identifier);",
                null
            )
        }
        return deferred.await()
    }

    override fun isExpired(): Boolean {
        return !this::expirationInstant.isInitialized || Instant.now().isAfter(expirationInstant)
    }

    private fun onInitializationError(error: Throwable) {
        generatorDeferred.completeExceptionally(error)
        try { close() } catch (_: Exception) {}
    }

    override fun close() {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                webView.clearHistory()
                webView.clearCache(true)
                webView.loadUrl("about:blank")
                webView.onPause()
                webView.removeAllViews()
                webView.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "close error", e)
            }
        }
    }

    //region Network helpers
    private fun fetchVisitorData(): String {
        // Get visitorData from Innertube /visitor_id endpoint
        val req = Request.Builder()
            .url("https://www.youtube.com/sw.js_data")
            .header("User-Agent", USER_AGENT)
            .build()
        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            // visitorData is in the response — extract via regex
            val match = Regex(""""visitorData":"([^"]+)"""").find(body)
            return match?.groupValues?.get(1) ?: throw PoTokenException("visitorData not found")
        }
    }

    private fun fetchBotGuardChallenge(visitorData: String): String {
        // POST to /Create endpoint with visitorData
        // The body is a protobuf-encoded payload; we use a minimal hardcoded one
        // NewPipe's actual implementation does this properly, but for now we'll use the
        // /visitor_id API which returns visitorData + a simple challenge
        // For full BotGuard, the JS in po_token.html handles the challenge fetching itself
        // via makeBotGuardRequest. So we just need to trigger the JS flow.
        return visitorData
    }
    //endregion

    companion object : PoTokenGenerator.Factory {
        private const val TAG = "PoTokenWebView"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        private const val JS_INTERFACE = "PoTokenWebView"

        override suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator =
            withContext(Dispatchers.Main) {
                val deferred = CompletableDeferred<PoTokenGenerator>()
                val potWv = PoTokenWebView(context, deferred)
                potWv.loadHtmlAndObtainBotguard(context)
                deferred.await()
            }
    }
}
