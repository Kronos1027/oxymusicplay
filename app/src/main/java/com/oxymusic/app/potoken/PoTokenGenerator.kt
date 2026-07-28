package com.oxymusic.app.potoken

import android.content.Context
import java.io.Closeable

/**
 * Generates poTokens using the BotGuard integrity token obtained during initialization.
 * Simplified from NewPipe's PoTokenGenerator — uses Kotlin coroutines instead of RxJava.
 */
interface PoTokenGenerator : Closeable {

    /**
     * Generates a poToken for the provided identifier (videoId or visitorData).
     * Can be called multiple times.
     */
    suspend fun generatePoToken(identifier: String): String

    /** @return true if the integrityToken is expired (subsequent tokens will be invalid). */
    fun isExpired(): Boolean

    interface Factory {
        /**
         * Initializes a [PoTokenGenerator] by loading the BotGuard VM, running it, and obtaining
         * an `integrityToken`. Can then be used multiple times to generate poTokens.
         */
        suspend fun newPoTokenGenerator(context: Context): PoTokenGenerator
    }
}
