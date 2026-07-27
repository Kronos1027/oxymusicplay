package com.oxymusic.app.network

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class OxyHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val okBuilder = okhttp3.Request.Builder().url(request.url())
        if (request.httpMethod().equals("POST", ignoreCase = true)) {
            okBuilder.post(ByteArray(0).toRequestBody(null))
        } else {
            okBuilder.get()
        }
        for ((key, values) in request.headers()) {
            for (v in values) okBuilder.header(key, v)
        }
        client.newCall(okBuilder.build()).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (resp.code == 429) throw ReCaptchaException("Recaptcha", request.url())
            val respHeaders = mutableMapOf<String, List<String>>()
            for (name in resp.headers.names()) respHeaders[name] = resp.headers(name)
            return Response(resp.code, resp.message, respHeaders.toMap(), body, request.url())
        }
    }
}
