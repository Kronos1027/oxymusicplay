package com.oxymusic.app.potoken

import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64

/**
 * Parses the raw challenge data obtained from the Create endpoint and returns a JSON object
 * string that can be embedded in a JavaScript snippet.
 *
 * Ported from NewPipe's JavaScriptUtil.kt, using org.json (Android built-in) instead of nanojson.
 */
fun parseChallengeData(rawChallengeData: String): String {
    val scrambled = JSONArray(rawChallengeData)

    val challengeData = if (scrambled.length() > 1 && scrambled.opt(1) is String) {
        val descrambled = descramble(scrambled.getString(1))
        JSONArray(descrambled)
    } else {
        scrambled.getJSONArray(0)
    }

    val messageId = challengeData.getString(0)
    val interpreterHash = challengeData.getString(3)
    val program = challengeData.getString(4)
    val globalName = challengeData.getString(5)
    val clientExperimentsStateBlob = challengeData.optString(7, "")

    val privateDoNotAccessOrElseSafeScriptWrappedValue = challengeData.optJSONArray(1)?.let { arr ->
        (0 until arr.length()).map { arr.opt(it) }.firstOrNull { it is String } as? String
    }
    val privateDoNotAccessOrElseTrustedResourceUrlWrappedValue = challengeData.optJSONArray(2)?.let { arr ->
        (0 until arr.length()).map { arr.opt(it) }.firstOrNull { it is String } as? String
    }

    val obj = JSONObject()
    obj.put("messageId", messageId)
    val interpreterJs = JSONObject()
    interpreterJs.put("privateDoNotAccessOrElseSafeScriptWrappedValue", privateDoNotAccessOrElseSafeScriptWrappedValue ?: JSONObject.NULL)
    interpreterJs.put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", privateDoNotAccessOrElseTrustedResourceUrlWrappedValue ?: JSONObject.NULL)
    obj.put("interpreterJavascript", interpreterJs)
    obj.put("interpreterHash", interpreterHash)
    obj.put("program", program)
    obj.put("globalName", globalName)
    obj.put("clientExperimentsStateBlob", clientExperimentsStateBlob)
    return obj.toString()
}

/**
 * Parses the raw integrity token data and returns (Uint8Array JS string, duration in seconds).
 */
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val integrityTokenData = JSONArray(rawIntegrityTokenData)
    return base64ToU8(integrityTokenData.getString(0)) to integrityTokenData.getLong(1)
}

/** Converts a string identifier to a JavaScript `Uint8Array` literal. */
fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

/** Converts "97,98,99" → "abc" with base64-url-encoding for poTokens. */
fun u8ToBase64(poToken: String): String {
    val bytes = poToken.split(",")
        .map { it.toUByte().toByte() }
        .toByteArray()
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        .replace("+", "-")
        .replace("/", "_")
}

/** Scrambled challenge: decode base64, add 97 to each byte. */
private fun descramble(scrambledChallenge: String): String {
    return base64ToByteString(scrambledChallenge)
        .map { (it + 97).toByte() }
        .toByteArray()
        .decodeToString()
}

/** Decodes base64 (YouTube variant) → JS Uint8Array literal. */
private fun base64ToU8(base64: String): String = newUint8Array(base64ToByteString(base64))

private fun newUint8Array(contents: ByteArray): String =
    "new Uint8Array([" + contents.joinToString(separator = ",") { it.toUByte().toString() } + "])"

private fun base64ToByteString(base64: String): ByteArray {
    val base64Mod = base64
        .replace('-', '+')
        .replace('_', '/')
        .replace('.', '=')
    return Base64.decode(base64Mod, Base64.DEFAULT)
}
