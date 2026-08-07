package com.oxymusic.app.media

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VisualizerManager — captures FFT data from ExoPlayer's audio output.
 *
 * Provides real-time frequency magnitude data (0-1 normalized) for the
 * SpectrumVisualizer Canvas component.
 */
class VisualizerManager {

    private var visualizer: Visualizer? = null

    private val _magnitudes = MutableStateFlow(FloatArray(64) { 0f })
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    fun attach(audioSessionId: Int) {
        try {
            visualizer?.release()
            val range = Visualizer.getCaptureSizeRange()
            val captureSize = range[1].coerceAtMost(1024)
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                    override fun onFftDataCapture(visualizer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft == null) return
                        val n = fft.size / 2
                        val mags = FloatArray(64)
                        for (i in 0 until 64) {
                            if (i < n) {
                                val re = fft[i * 2].toFloat()
                                val im = fft[i * 2 + 1].toFloat()
                                val mag = Math.sqrt((re * re + im * im).toDouble()).toFloat()
                                mags[i] = (mag / 128f).coerceIn(0f, 1f)
                            }
                        }
                        _magnitudes.value = mags
                    }
                }, captureSize, false, true)
                enabled = true
            }
            Log.i(TAG, "Visualizer attached to session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Visualizer attach failed", e)
        }
    }

    fun release() {
        visualizer?.apply { enabled = false; release() }
        visualizer = null
        _magnitudes.value = FloatArray(64) { 0f }
    }

    companion object {
        private const val TAG = "VisualizerManager"
    }
}
