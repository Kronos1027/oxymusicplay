package com.oxymusic.app.media

import android.media.audiofx.Equalizer
import android.util.Log

/**
 * EqualizerManager — wraps Android's AudioEffect Equalizer.
 *
 * Attached to ExoPlayer's audio session ID. Provides:
 * - 5-band equalizer (60Hz, 230Hz, 910Hz, 3600Hz, 14000Hz)
 * - 8 presets: Flat, Bass Boost, Treble Boost, Vocal, Rock, Pop, Jazz, Electronic
 * - Custom band level control
 */
class EqualizerManager {

    private var equalizer: Equalizer? = null

    data class Band(val index: Int, val freq: String, val level: Int, val minLevel: Int, val maxLevel: Int)

    val presets = listOf(
        "Flat", "Bass Boost", "Treble Boost", "Vocal",
        "Rock", "Pop", "Jazz", "Electronic"
    )

    // Preset definitions (millibel levels for 5 bands)
    private val presetValues = mapOf(
        "Flat"         to shortArrayOf(0, 0, 0, 0, 0),
        "Bass Boost"   to shortArrayOf(800, 600, 200, 0, 0),
        "Treble Boost" to shortArrayOf(0, 0, 200, 600, 800),
        "Vocal"        to shortArrayOf(-200, 0, 600, 400, 0),
        "Rock"         to shortArrayOf(500, 300, -200, 300, 500),
        "Pop"          to shortArrayOf(-200, 300, 500, 300, -200),
        "Jazz"         to shortArrayOf(300, 0, 0, 200, 300),
        "Electronic"   to shortArrayOf(600, 200, -100, 200, 600),
    )

    fun attach(audioSessionId: Int) {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
            Log.i(TAG, "Equalizer attached to session $audioSessionId, bands=${equalizer?.numberOfBands}")
        } catch (e: Exception) {
            Log.e(TAG, "Equalizer attach failed", e)
        }
    }

    fun setPreset(name: String) {
        val values = presetValues[name] ?: return
        val eq = equalizer ?: return
        try {
            for (i in values.indices) {
                eq.setBandLevel(i.toShort(), values[i])
            }
            Log.i(TAG, "Preset applied: $name")
        } catch (e: Exception) {
            Log.e(TAG, "Preset failed", e)
        }
    }

    fun setBandLevel(band: Int, level: Short) {
        try {
            equalizer?.setBandLevel(band.toShort(), level)
        } catch (e: Exception) {
            Log.e(TAG, "Set band failed", e)
        }
    }

    fun getBands(): List<Band> {
        val eq = equalizer ?: return emptyList()
        val bands = mutableListOf<Band>()
        try {
            val n = eq.numberOfBands.toInt()
            for (i in 0 until n) {
                val freq = eq.getCenterFreq(i.toShort()) / 1000 // Hz
                val freqStr = if (freq >= 1000) "${freq / 1000}kHz" else "${freq}Hz"
                bands.add(Band(
                    index = i,
                    freq = freqStr,
                    level = eq.getBandLevel(i.toShort()).toInt(),
                    minLevel = eq.bandLevelRange[0].toInt(),
                    maxLevel = eq.bandLevelRange[1].toInt(),
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getBands failed", e)
        }
        return bands
    }

    fun release() {
        equalizer?.release()
        equalizer = null
    }

    companion object {
        private const val TAG = "EqualizerManager"
    }
}
