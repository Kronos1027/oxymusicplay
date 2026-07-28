/*
 * Copyright (C) NewPipe Contributors
 * Licensed under GPLv3 — see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Vendored from https://github.com/TeamNewPipe/NewPipe (PR #11955)
 * Adapted for OxyMusic:
 *   - package renamed
 *   - RxJava3 Single → Kotlin CompletableDeferred (coroutines)
 *   - DownloaderImpl → OkHttpClient (already used in project)
 *   - BuildConfig.DEBUG → always-on logging (no BuildConfig dependency)
 *
 * NETWORK LOGIC PRESERVED VERBATIM:
 *   - URLs: https://www.youtube.com/api/jnn/v1/Create and /GenerateIT
 *   - Headers: User-Agent, Accept, Content-Type, x-goog-api-key, x-user-agent
 *   - Request body format: [ "REQUEST_KEY" ] and [ "REQUEST_KEY", "botguardResponse" ]
 *   - Response parsing via parseChallengeData() and parseIntegrityTokenData()
 *   - JS interface methods: downloadAndRunBotguard, onRunBotguardResult, onObtainPoTokenResult, etc.
 *   - po_token.html asset loaded with base URL https://www.youtube.com
 */
package com.oxymusic.app.potoken

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.MainThread
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.concurrent.TimeUnit

class PoTokenWebView private constructor(
    context: Context,
    // to be used exactly once only during initialization!
    private val generatorDeferred: CompletableDeferred<PoTokenGenerator>
) : PoTokenGenerator {
    private val webView = WebView(context)
    private val poTokenEmitters = mutableListOf<Pair<String, CompletableDeferred<String>>>()
    private lateinit var expirationInstant: Instant

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    //region Initialization
    init {
        val webViewSettings = webView.settings
        //noinspection SetJavaScriptEnabled we want to use JavaScript!
        webViewSettings.javaScriptEnabled = true
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webViewSettings, false)
        }
        webViewSettings.userAgentString = USER_AGENT
        webViewSettings.blockNetworkLoads = true // the WebView does not need internet access

        // so that we can run async functions and get back the result
        webView.addJavascriptInterface(this, JS_INTERFACE)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    // There should not be any uncaught errors while executing the code, because
                    // everything that can fail is guarded by try-catch. Therefore, this likely
                    // indicates that there was a syntax error in the code, i.e. the WebView only
                    // supports a really old version of JS.

                    val fmt = "\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"
                    val exception = BadWebViewException(fmt)
                    Log.e(TAG, "This WebView implementation is broken: $fmt")

                    onInitializationErrorCloseAndCancel(exception)
                    popAllPoTokenEmitters().forEach { (_, deferred) -> deferred.completeExceptionally(exception) }
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    /**
     * Must be called right after instantiating [PoTokenWebView] to perform the actual
     * initialization. This will asynchronously go through all the steps needed to load BotGuard,
     * run it, and obtain an `integrityToken`.
     */
    private fun loadHtmlAndObtainBotguard(context: Context) {
        Log.d(TAG, "loadHtmlAndObtainBotguard() called")

        try {
            val html = context.assets.open("po_token.html").bufferedReader()
                .use { it.readText() }

            // must be on main thread
            Handler(Looper.getMainLooper()).post {
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    html.replaceFirst(
                        "</script>",
                        // calls downloadAndRunBotguard() when the page has finished loading
                        "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"
                    ),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        } catch (e: Throwable) {
            onInitializationErrorCloseAndCancel(e)
        }
    }

    /**
     * Called during initialization by the JavaScript snippet appended to the HTML page content in
     * [loadHtmlAndObtainBotguard] after the WebView content has been loaded.
     *
     * Makes a POST request to /Create endpoint, then runs BotGuard in JS with the challenge data.
     */
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        Log.d(TAG, "downloadAndRunBotguard() called")

        // Run network request on background thread (JavascriptInterface methods are called on a
        // background thread already, but OkHttp needs to not block the WebView's thread pool)
        Thread {
            try {
                val responseBody = makeBotguardServiceRequest(
                    "https://www.youtube.com/api/jnn/v1/Create",
                    "[ \"$REQUEST_KEY\" ]"
                )
                val parsedChallengeData = parseChallengeData(responseBody)
                Log.d(TAG, "Got challenge data, running BotGuard in JS...")

                // Run BotGuard in the WebView (must be main thread)
                Handler(Looper.getMainLooper()).post {
                    webView.evaluateJavascript(
                        """try {
                            data = $parsedChallengeData
                            runBotGuard(data).then(function (result) {
                                this.webPoSignalOutput = result.webPoSignalOutput
                                $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                            }, function (error) {
                                $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                            })
                        } catch (error) {
                            $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                        }""",
                        null
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "downloadAndRunBotguard failed", e)
                onInitializationErrorCloseAndCancel(e)
            }
        }.start()
    }

    /**
     * Called during initialization by the JavaScript snippet from [downloadAndRunBotguard] or
     * [onRunBotguardResult].
     */
    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Log.e(TAG, "Initialization error from JavaScript: $error")
        onInitializationErrorCloseAndCancel(buildExceptionForJsError(error))
    }

    /**
     * Called during initialization by the JavaScript snippet from [downloadAndRunBotguard] after
     * obtaining the BotGuard execution output [botguardResponse].
     *
     * Makes a POST request to /GenerateIT endpoint, then sets the integrityToken in JS.
     */
    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        Log.d(TAG, "botguardResponse received (len=${botguardResponse.length})")

        Thread {
            try {
                val responseBody = makeBotguardServiceRequest(
                    "https://www.youtube.com/api/jnn/v1/GenerateIT",
                    "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]"
                )
                Log.d(TAG, "GenerateIT response: $responseBody")

                val (integrityToken, expirationTimeInSeconds) = parseIntegrityTokenData(responseBody)

                // leave 10 minutes of margin just to be sure
                expirationInstant = Instant.now().plusSeconds(expirationTimeInSeconds - 600)

                // Set integrityToken in JS (must be main thread)
                Handler(Looper.getMainLooper()).post {
                    webView.evaluateJavascript(
                        "this.integrityToken = $integrityToken"
                    ) {
                        Log.d(TAG, "initialization finished, expiration=${expirationTimeInSeconds}s")
                        generatorDeferred.complete(this@PoTokenWebView)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onRunBotguardResult failed", e)
                onInitializationErrorCloseAndCancel(e)
            }
        }.start()
    }
    //endregion

    //region Obtaining poTokens
    override suspend fun generatePoToken(identifier: String): String {
        Log.d(TAG, "generatePoToken() called with identifier $identifier")
        val emitter = CompletableDeferred<String>()
        addPoTokenEmitter(identifier, emitter)
        val u8Identifier = stringToU8(identifier)

        // must be on main thread
        val posted = Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(
                """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
                null
            )
        }
        if (!posted) {
            throw PoTokenException("Could not run on main thread")
        }
        return emitter.await()
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] when an error occurs in calling the
     * JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        Log.e(TAG, "obtainPoToken error from JavaScript: $error")
        popPoTokenEmitter(identifier)?.completeExceptionally(buildExceptionForJsError(error))
    }

    /**
     * Called by the JavaScript snippet from [generatePoToken] with the original identifier and the
     * result of the JavaScript `obtainPoToken()` function.
     */
    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        Log.d(TAG, "Generated poToken (before decoding): identifier=$identifier poTokenU8=$poTokenU8")
        val poToken = try {
            u8ToBase64(poTokenU8)
        } catch (t: Throwable) {
            popPoTokenEmitter(identifier)?.completeExceptionally(t)
            return
        }

        Log.d(TAG, "Generated poToken: identifier=$identifier poToken=$poToken")
        popPoTokenEmitter(identifier)?.complete(poToken)
    }

    override fun isExpired(): Boolean {
        return Instant.now().isAfter(expirationInstant)
    }
    //endregion

    //region Handling multiple emitters
    private fun addPoTokenEmitter(identifier: String, emitter: CompletableDeferred<String>) {
        synchronized(poTokenEmitters) {
            poTokenEmitters.add(Pair(identifier, emitter))
        }
    }

    private fun popPoTokenEmitter(identifier: String): CompletableDeferred<String>? {
        return synchronized(poTokenEmitters) {
            poTokenEmitters.indexOfFirst { it.first == identifier }.takeIf { it >= 0 }?.let {
                poTokenEmitters.removeAt(it).second
            }
        }
    }

    private fun popAllPoTokenEmitters(): List<Pair<String, CompletableDeferred<String>>> {
        return synchronized(poTokenEmitters) {
            val result = poTokenEmitters.toList()
            poTokenEmitters.clear()
            result
        }
    }
    //endregion

    //region Utils
    /**
     * Makes a POST request to [url] with the given [data] by setting the correct headers.
     * Returns the response body string.
     * Throws PoTokenException on non-200 status or network error.
     *
     * NETWORK LOGIC PRESERVED EXACTLY FROM NEWPIPE:
     * - URL: https://www.youtube.com/api/jnn/v1/Create or /GenerateIT
     * - Headers: User-Agent, Accept: application/json, Content-Type: application/json+protobuf,
     *   x-goog-api-key, x-user-agent: grpc-web-javascript/0.1
     * - Body: [ "REQUEST_KEY" ] or [ "REQUEST_KEY", "botguardResponse" ]
     */
    private fun makeBotguardServiceRequest(url: String, data: String): String {
        Log.d(TAG, "makeBotguardServiceRequest: POST $url")

        val req = Request.Builder()
            .url(url)
            .post(data.toByteArray().toRequestBody("application/json+protobuf".toMediaType()))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json+protobuf")
            .header("x-goog-api-key", GOOGLE_API_KEY)
            .header("x-user-agent", "grpc-web-javascript/0.1")
            .build()

        httpClient.newCall(req).execute().use { response ->
            val httpCode = response.code
            if (httpCode != 200) {
                throw PoTokenException("Invalid response code: $httpCode")
            }
            return response.body?.string() ?: throw PoTokenException("Empty response body")
        }
    }

    private fun onInitializationErrorCloseAndCancel(error: Throwable) {
        Log.e(TAG, "Initialization error", error)
        // close on main thread
        Handler(Looper.getMainLooper()).post {
            try { close() } catch (_: Exception) {}
        }
        generatorDeferred.completeExceptionally(error)
    }

    @MainThread
    override fun close() {
        webView.clearHistory()
        // clears RAM cache and disk cache (globally for all WebViews)
        webView.clearCache(true)
        // ensures that the WebView isn't doing anything when destroying it
        webView.loadUrl("about:blank")
        webView.onPause()
        webView.removeAllViews()
        webView.destroy()
    }
    //endregion

    companion object : PoTokenGenerator.Factory {
        private val TAG = PoTokenWebView::class.simpleName

        // Public API key used by BotGuard, which has been got by looking at BotGuard requests
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"
        private const val JS_INTERFACE = "PoTokenWebView"

        override suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator {
            val deferred = CompletableDeferred<PoTokenGenerator>()
            // WebView must be created on main thread
            val posted = Handler(Looper.getMainLooper()).post {
                val potWv = PoTokenWebView(context, deferred)
                potWv.loadHtmlAndObtainBotguard(context)
            }
            if (!posted) {
                throw PoTokenException("Could not run on main thread")
            }
            return deferred.await()
        }
    }
}
