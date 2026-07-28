/*
 * Copyright (C) NewPipe Contributors
 * Licensed under GPLv3 — see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Vendored from https://github.com/TeamNewPipe/NewPipe (PR #11955)
 * Adapted for OxyMusic: package renamed, otherwise verbatim.
 */
package com.oxymusic.app.potoken

class PoTokenException(message: String) : Exception(message)

// to be thrown if the WebView provided by the system is broken
class BadWebViewException(message: String) : Exception(message)

fun buildExceptionForJsError(error: String): Exception {
    return if (error.contains("SyntaxError")) {
        BadWebViewException(error)
    } else {
        PoTokenException(error)
    }
}
