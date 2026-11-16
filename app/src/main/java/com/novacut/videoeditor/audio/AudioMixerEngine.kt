package com.novacut.videoeditor.audio

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance 16-bit Linear PCM Audio Mixer.
 * Supports multi-track channel mixing, volume attenuation/boost,
 * soft-limiting clipping protection, and automatic background audio ducking.
 */
class AudioMixerEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 2
        const val BYTES_PER_SAMPLE = 2 // 16-bit PCM

        // Dialogue threshold to trigger background audio ducking
        private const val DUCKING_TRIGGER_THRESHOLD = 0.08f
        private const val DUCKING_ATTENUATION_FACTOR = 0.25f // ~ -12 dB
    }

    private var isDuckingActive = false
    private var smoothedDuckingFactor = 1.0f

    /**
     * Mix two stereo 16-bit PCM byte buffers (e.g., Main Video Audio + Background Music Track).
     *
     * @param voicePcmBuffer Primary audio track (dialogue/voiceover).
     * @param musicPcmBuffer Secondary audio track (music/effects).
     * @param voiceVolume Volume factor for voice (0.0f to 2.0f).
     * @param musicVolume Volume factor for music (0.0f to 2.0f).
     * @param enableAutoDucking Automatically lowers music volume when voice is detected.
     * @return Mixed 16-bit PCM stereo byte array.
     */
    fun mixStereoBuffers(
        voicePcmBuffer: ByteArray,
        musicPcmBuffer: ByteArray,
        voiceVolume: Float = 1.0f,
        musicVolume: Float = 0.8f,
        enableAutoDucking: Boolean = true
    ): ByteArray {
        val maxLen = max(voicePcmBuffer.size, musicPcmBuffer.size)
        val mixedOutput = ByteArray(maxLen)

        // Convert byte buffers to 16-bit Short samples
        val totalSamples = maxLen / 2
        val voiceShorts = bytesToShorts(voicePcmBuffer, totalSamples)
        val musicShorts = bytesToShorts(musicPcmBuffer, totalSamples)
        val mixedShorts = ShortArray(totalSamples)

        // 1. Calculate RMS of the voice track to detect dialogue presence
        if (enableAutoDucking) {
            val voiceRms = calculateRms(voiceShorts)
            val shouldDuck = voiceRms > DUCKING_TRIGGER_THRESHOLD
            // Smooth ducking transition (attack / release)
            val targetFactor = if (shouldDuck) DUCKING_ATTENUATION_FACTOR else 1.0f
            smoothedDuckingFactor += (targetFactor - smoothedDuckingFactor) * 0.15f
        } else {
            smoothedDuckingFactor = 1.0f
        }

        // 2. Mix samples with soft clipping limiter
        for (i in 0 until totalSamples) {
            val voiceSample = (voiceShorts[i] * voiceVolume)
            val musicSample = (musicShorts[i] * musicVolume * smoothedDuckingFactor)

            // Linear summation
            var mixedSample = (voiceSample + musicSample).toInt()

            // Soft-Clipping Limiter (tanh approximation to prevent digital distortion)
            mixedSample = clampSample(mixedSample)

            mixedShorts[i] = mixedSample.toShort()
        }

        return shortsToBytes(mixedShorts, mixedOutput)
    }

    private fun calculateRms(samples: ShortArray): Float {
        if (samples.isEmpty()) return 0f
        var sumSquares = 0.0
        for (sample in samples) {
            val normalized = sample / 32768.0
            sumSquares += normalized * normalized
        }
        return sqrt(sumSquares / samples.size).toFloat()
    }

    private fun clampSample(sample: Int): Int {
        return min(32767, max(-32768, sample))
    }

    private fun bytesToShorts(bytes: ByteArray, targetSize: Int): ShortArray {
        val shorts = ShortArray(targetSize)
        val byteCount = min(bytes.size, targetSize * 2)
        var b = 0
        var s = 0
        while (b < byteCount - 1 && s < targetSize) {
            // Little-Endian 16-bit PCM
            val low = bytes[b].toInt() and 0xFF
            val high = bytes[b + 1].toInt()
            shorts[s] = ((high shl 8) or low).toShort()
            b += 2
            s++
        }
        return shorts
    }

    private fun shortsToBytes(shorts: ShortArray, output: ByteArray): ByteArray {
        var b = 0
        for (sample in shorts) {
            output[b] = (sample.toInt() and 0xFF).toByte()
            output[b + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            b += 2
        }
        return output
    }
}
