package com.oxymusic.app.media

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisualizerManager @Inject constructor() {
    private var visualizer: Visualizer? = null

    private val _magnitudes = MutableStateFlow(FloatArray(BARS) { 0f })
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    fun attach(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            release()
            val v = Visualizer(audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(visualizer: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    if (waveform.isEmpty()) return
                    val bars = BARS
                    val out = FloatArray(bars)
                    val step = (waveform.size / bars).coerceAtLeast(1)
                    for (i in 0 until bars) {
                        var sum = 0
                        for (j in 0 until step) {
                            val idx = i * step + j
                            if (idx >= waveform.size) break
                            val b = waveform[idx].toInt() and 0xFF
                            sum += kotlin.math.abs(b - 128)
                        }
                        out[i] = (sum.toFloat() / step) / 128f
                    }
                    _magnitudes.value = out
                }
                override fun onFftDataCapture(visualizer: Visualizer, fft: ByteArray, samplingRate: Int) {
                    if (fft.isEmpty()) return
                    val n = fft.size / 2
                    val bars = BARS
                    val out = FloatArray(bars)
                    val step = (n / bars).coerceAtLeast(1)
                    for (i in 0 until bars) {
                        var max = 0
                        for (j in 0 until step) {
                            val idx = 2 * (i * step + j)
                            if (idx + 1 >= fft.size) break
                            val re = fft[idx].toInt()
                            val im = fft[idx + 1].toInt()
                            val mag = kotlin.math.sqrt((re.toDouble() * re + im.toDouble() * im)).toInt()
                            if (mag > max) max = mag
                        }
                        out[i] = (max.toFloat() / 128f).coerceIn(0f, 1f)
                    }
                    _magnitudes.value = out
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            v.enabled = true
            visualizer = v
        } catch (e: Exception) {
            Log.w("Visualizer", "attach failed: ${e.message}")
        }
    }

    fun release() {
        try { visualizer?.enabled = false; visualizer?.release() } catch (e: Exception) {}
        visualizer = null
        _magnitudes.value = FloatArray(BARS) { 0f }
    }

    companion object { const val BARS = 64 }
}
