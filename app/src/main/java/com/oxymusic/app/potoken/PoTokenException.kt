package com.oxymusic.app.potoken

/** Thrown when something goes wrong with poToken generation. */
class PoTokenException(message: String) : Exception(message)

/** Thrown when the system WebView is broken (e.g. old version that doesn't support needed JS). */
class BadWebViewException(message: String) : Exception(message)

/** Builds the appropriate exception for a JS error. */
fun buildExceptionForJsError(error: String): Exception =
    if (error.contains("SyntaxError")) BadWebViewException(error) else PoTokenException(error)
