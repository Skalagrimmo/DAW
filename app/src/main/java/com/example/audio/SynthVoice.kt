package com.example.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SynthVoice {
    // Synth Properties
    var frequency: Float = 440f
    private var phase: Float = 0f
    
    // Waveform Type: 0: Sine, 1: Triangle, 2: Sawtooth, 3: Square
    var waveformType: Int = 0
    
    // Low-pass filter state
    private var lastOutput: Float = 0f
    var cutoff: Float = 0.8f // 0.0f (fully closed) to 1.0f (fully open)
    var resonance: Float = 0.3f // Extra gain peak (simple feedback)

    // ADSR Envelope parameters
    var attack: Float = 0.1f  // seconds
    var decay: Float = 0.2f   // seconds
    var sustain: Float = 0.7f  // level (0.0 to 1.0)
    var release: Float = 0.3f  // seconds

    // Playback state
    var isPlaying: Boolean = false
        private set
    
    private var sampleRate: Int = 44100
    private var samplesElapsed: Long = 0
    private var samplesSinceRelease: Long = -1
    private var envelopeAtRelease: Float = 0f

    fun triggerOn(freq: Float, sampleRate: Int, attack: Float, decay: Float, sustain: Float, release: Float, waveType: Int) {
        this.frequency = freq
        this.sampleRate = sampleRate
        this.attack = maxOf(0.005f, attack)
        this.decay = maxOf(0.005f, decay)
        this.sustain = sustain
        this.release = maxOf(0.005f, release)
        this.waveformType = waveType
        
        this.samplesElapsed = 0
        this.samplesSinceRelease = -1
        this.isPlaying = true
    }

    fun triggerOff() {
        if (isPlaying && samplesSinceRelease == -1L) {
            samplesSinceRelease = 0
            envelopeAtRelease = getCurrentEnvelope()
        }
    }

    private fun getCurrentEnvelope(): Float {
        if (!isPlaying) return 0f

        val attackSamples = (attack * sampleRate).toLong()
        val decaySamples = (decay * sampleRate).toLong()
        val releaseSamples = (release * sampleRate).toLong()

        if (samplesSinceRelease != -1L) {
            // In release phase
            val progress = samplesSinceRelease.toFloat() / maxOf(1L, releaseSamples)
            if (progress >= 1.0f) {
                isPlaying = false
                return 0f
            }
            return envelopeAtRelease * (1.0f - progress)
        }

        // Active phases
        if (samplesElapsed < attackSamples) {
            // Attack phase
            return samplesElapsed.toFloat() / maxOf(1L, attackSamples)
        } else if (samplesElapsed < attackSamples + decaySamples) {
            // Decay phase
            val progress = (samplesElapsed - attackSamples).toFloat() / maxOf(1L, decaySamples)
            return 1.0f - (1.0f - sustain) * progress
        } else {
            // Sustain phase
            return sustain
        }
    }

    fun nextSample(): Float {
        if (!isPlaying) return 0f

        // 1. Calculate the raw oscillator sample
        val phaseIncrement = (2f * PI.toFloat() * frequency) / sampleRate
        phase += phaseIncrement
        if (phase >= 2f * PI.toFloat()) {
            phase -= 2f * PI.toFloat()
        }

        val rawSample = when (waveformType) {
            0 -> sin(phase) // Sine
            1 -> { // Triangle
                val p = phase / (2f * PI.toFloat())
                if (p < 0.25f) 4f * p else if (p < 0.75f) 2f - 4f * p else 4f * p - 4f
            }
            2 -> { // Sawtooth
                1f - 2f * (phase / (2f * PI.toFloat()))
            }
            3 -> { // Square
                if (phase < PI.toFloat()) 0.5f else -0.5f
            }
            else -> sin(phase)
        }

        // 2. Apply ADSR Envelope
        val env = getCurrentEnvelope()
        val envSample = rawSample * env

        // Increment time
        if (samplesSinceRelease != -1L) {
            samplesSinceRelease++
        } else {
            samplesElapsed++
        }

        // 3. Simple Low-pass sweep filter
        // Map cutoff (0.0 to 1.0) exponentially to frequency bounds
        val targetCutoffFreq = 80f + (cutoff * cutoff * 8000f)
        val alpha = (2f * PI.toFloat() * targetCutoffFreq / sampleRate).coerceIn(0.001f, 0.99f)
        
        // Simple 1st-order filter with a resonance feedback loop
        val feedback = resonance * 0.7f
        val filteredInput = envSample + feedback * lastOutput
        val output = lastOutput + alpha * (filteredInput - lastOutput)
        lastOutput = output

        return output.coerceIn(-1.0f, 1.0f)
    }
}
