package com.oxymusic.app.network

/**
 * Simple JSON value extractor — avoids kotlinx.serialization quirks for ad-hoc parsing.
 * Not a full JSON parser, but enough for Piped responses.
 */
object JsonExtractor {

    /** Extracts a top-level string value by key. Handles escaped chars minimally. */
    fun extractString(json: String, key: String): String? {
        val pattern = """"$key"\s*:\s*""""
        val keyIdx = json.indexOf(pattern.replace("\$", ""))
        if (keyIdx < 0) return null
        val startIdx = keyIdx + pattern.length - 1 // position of opening quote
        // Find closing quote (handle escapes)
        var i = startIdx + 1
        val sb = StringBuilder()
        while (i < json.length) {
            val c = json[i]
            when {
                c == '\\' && i + 1 < json.length -> {
                    val next = json[i + 1]
                    when (next) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'u' -> {
                            if (i + 5 < json.length) {
                                val hex = json.substring(i + 2, i + 6)
                                try { sb.append(hex.toInt(16).toChar()) } catch (e: Exception) {}
                                i += 4
                            }
                        }
                        else -> sb.append(next)
                    }
                    i += 2
                }
                c == '"' -> return sb.toString()
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString().ifEmpty { null }
    }

    fun extractLong(json: String, key: String): Long? {
        val pattern = """"$key"\s*:\s*"""
        val match = Regex(pattern).find(json) ?: return null
        val after = json.substring(match.range.last + 1)
        val numMatch = Regex("""-?\d+""").find(after) ?: return null
        return numMatch.value.toLongOrNull()
    }

    fun extractBool(json: String, key: String): Boolean {
        val pattern = """"$key"\s*:\s*"""
        val match = Regex(pattern).find(json) ?: return false
        val after = json.substring(match.range.last + 1).trim()
        return after.startsWith("true")
    }

    /** Extracts an array value by key. Returns list of raw JSON strings (each element). */
    fun extractArray(json: String, key: String): List<String>? {
        val pattern = """"$key"\s*:\s*\["""
        val match = Regex(pattern).find(json) ?: return null
        val startIdx = match.range.last
        return splitArray(json.substring(startIdx))
    }

    /** Splits a JSON array string (starting with [) into element strings. */
    fun splitArray(arrayJson: String): List<String> {
        if (!arrayJson.trimStart().startsWith("[")) return emptyList()
        val elements = mutableListOf<String>()
        var depth = 0
        var inString = false
        var escape = false
        var startIdx = -1
        for (i in arrayJson.indices) {
            val c = arrayJson[i]
            if (escape) { escape = false; continue }
            if (c == '\\' && inString) { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (inString) continue
            when (c) {
                '[' -> {
                    if (depth == 0) startIdx = i + 1
                    depth++
                }
                ']' -> {
                    depth--
                    if (depth == 0) {
                        // last element
                        if (startIdx in 0 until i) {
                            val elem = arrayJson.substring(startIdx, i).trim()
                            if (elem.isNotEmpty()) elements.add(elem)
                        }
                        return elements
                    }
                }
                ',' -> {
                    if (depth == 1 && startIdx in 0 until i) {
                        val elem = arrayJson.substring(startIdx, i).trim()
                        if (elem.isNotEmpty()) elements.add(elem)
                        startIdx = i + 1
                    }
                }
            }
        }
        return elements
    }
}
