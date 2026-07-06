package com.example.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import java.util.Random

class DrumSynth(private val instrumentType: Int) {
    // 0: Kick, 1: Snare, 2: HiHat, 3: Clap
    
    private var sampleRate: Int = 44100
    private var sampleIndex: Long = -1
    private var isActive: Boolean = false
    private var volume: Float = 1.0f

    // Synthesis phase variables
    private var phase: Float = 0f
    private val random = Random()
    
    // High-pass filter memory for HiHat
    private var lastNoiseInput: Float = 0f
    private var lastFilterOutput: Float = 0f

    fun trigger(vol: Float) {
        this.volume = vol
        this.sampleIndex = 0
        this.phase = 0f
        this.isActive = true
        
        // Reset filter
        lastNoiseInput = 0f
        lastFilterOutput = 0f
    }

    fun nextSample(): Float {
        if (!isActive) return 0f

        val t = sampleIndex.toFloat() / sampleRate
        
        val sample = when (instrumentType) {
            0 -> renderKick(t)
            1 -> renderSnare(t)
            2 -> renderHiHat(t)
            3 -> renderClap(t)
            else -> 0f
        }

        sampleIndex++
        
        // Safety bounds
        if (sampleIndex > sampleRate * 0.5f) { // All drum hits are shorter than 0.5 seconds
            isActive = false
        }

        return sample * volume
    }

    private fun renderKick(t: Float): Float {
        // Kick Pitch Sweep (starts at 150 Hz, rapidly drops to 45 Hz)
        val startFreq = 150f
        val endFreq = 45f
        val decayRate = 80f // sweeping rate
        val freq = endFreq + (startFreq - endFreq) * exp(-t * decayRate)
        
        // Integrate frequency to get phase
        val phaseIncrement = (2f * PI.toFloat() * freq) / sampleRate
        phase += phaseIncrement
        
        // Sine wave fundamental
        val sineWave = sin(phase)
        
        // Amplitude Envelope
        val ampEnvelope = exp(-t * 25f) // drops off quick
        
        // Add a small click burst at the beginning for transient punch
        val clickNoise = if (t < 0.005f) {
            (random.nextFloat() * 2f - 1f) * 0.15f * exp(-t * 1000f)
        } else {
            0f
        }
        
        val output = (sineWave * ampEnvelope) + clickNoise
        
        if (ampEnvelope < 0.001f) {
            isActive = false
        }
        
        return output
    }

    private fun renderSnare(t: Float): Float {
        // Snare consists of a decaying 180 Hz body + decaying noise
        val bodyFreq = 180f
        val bodyPhaseIncrement = (2f * PI.toFloat() * bodyFreq) / sampleRate
        phase += bodyPhaseIncrement
        
        val bodySine = sin(phase)
        val bodyEnvelope = exp(-t * 45f)
        
        val noiseVal = random.nextFloat() * 2f - 1f
        val noiseEnvelope = exp(-t * 15f)
        
        // Simple bandpass filter for snare noise (around 1000Hz - 2000Hz)
        // We'll simulate it by scaling down sub bass frequencies in noise
        val snareSample = (bodySine * bodyEnvelope * 0.3f) + (noiseVal * noiseEnvelope * 0.7f)
        
        if (noiseEnvelope < 0.001f && bodyEnvelope < 0.001f) {
            isActive = false
        }
        
        return snareSample
    }

    private fun renderHiHat(t: Float): Float {
        // Hihat consists of high-passed white noise with a very sharp decay envelope
        val noiseVal = random.nextFloat() * 2f - 1f
        val envelope = exp(-t * 85f) // sharp, metallic decay
        
        // First-order digital high-pass filter: y[n] = x[n] - x[n-1] + 0.85 * y[n-1]
        val filterOutput = noiseVal - lastNoiseInput + 0.85f * lastFilterOutput
        lastNoiseInput = noiseVal
        lastFilterOutput = filterOutput
        
        if (envelope < 0.001f) {
            isActive = false
        }
        
        return filterOutput * envelope * 0.4f
    }

    private fun renderClap(t: Float): Float {
        // Handclap is 3 rapid impulse bursts (spaced 12ms apart), then a decaying noise tail
        // 12ms is 0.012s, 24ms is 0.024s, 36ms is 0.036s
        val noiseVal = random.nextFloat() * 2f - 1f
        var envelope = 0f
        
        if (t < 0.012f) {
            // Burst 1
            envelope = exp(-t * 300f) * 0.7f
        } else if (t < 0.024f) {
            // Burst 2
            val tLocal = t - 0.012f
            envelope = exp(-tLocal * 300f) * 0.7f
        } else if (t < 0.036f) {
            // Burst 3
            val tLocal = t - 0.024f
            envelope = exp(-tLocal * 300f) * 0.7f
        } else {
            // Main Decay Tail
            val tLocal = t - 0.036f
            envelope = exp(-tLocal * 16f) * 0.8f
        }
        
        if (t > 0.036f && envelope < 0.001f) {
            isActive = false
        }
        
        return noiseVal * envelope
    }
}
