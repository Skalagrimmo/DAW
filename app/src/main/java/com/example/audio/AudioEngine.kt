package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class AudioEngine {
    companion object {
        private const val TAG = "AudioEngine"
        private const val SAMPLE_RATE = 44100
        private const val NUM_CHANNELS = 2 // Stereo
        private const val POLYPHONY = 6 // Up to 6 simultaneous synth notes
    }

    // Playback state
    var bpm: Int = 120
        set(value) {
            field = value.coerceIn(40, 240)
            updateStepLength()
        }
    
    private val isPlaying = AtomicBoolean(false)
    private val currentStep = AtomicInteger(0)
    
    private val _currentStepFlow = MutableStateFlow(0)
    val currentStepFlow: StateFlow<Int> = _currentStepFlow

    // Grid data structures (16 steps)
    // Synth notes: 25 keys (C4 to C6)
    val synthSequence = Array(25) { BooleanArray(16) }
    // Drum notes: 4 instruments (0: Kick, 1: Snare, 2: HiHat, 3: Clap)
    val drumSequence = Array(4) { BooleanArray(16) }

    // Sound generators
    private val synthVoices = Array(POLYPHONY) { SynthVoice() }
    private val kickSynth = DrumSynth(0)
    private val snareSynth = DrumSynth(1)
    private val hihatSynth = DrumSynth(2)
    private val clapSynth = DrumSynth(3)

    // Mixer settings (Thread-safe floats)
    @Volatile var volSynth: Float = 0.8f
    @Volatile var volKick: Float = 0.9f
    @Volatile var volSnare: Float = 0.8f
    @Volatile var volHiHat: Float = 0.7f
    @Volatile var volClap: Float = 0.8f
    @Volatile var volMaster: Float = 0.9f

    @Volatile var panSynth: Float = 0.0f
    @Volatile var panKick: Float = 0.0f
    @Volatile var panSnare: Float = -0.1f
    @Volatile var panHiHat: Float = 0.2f
    @Volatile var panClap: Float = -0.2f

    // VST settings
    @Volatile var synthAttack: Float = 0.1f
    @Volatile var synthDecay: Float = 0.2f
    @Volatile var synthSustain: Float = 0.7f
    @Volatile var synthRelease: Float = 0.3f
    @Volatile var synthCutoff: Float = 0.8f
    @Volatile var synthResonance: Float = 0.3f
    @Volatile var synthWaveform: Int = 0 // 0: Sine, 1: Triangle, 2: Saw, 3: Square

    // Effects settings
    @Volatile var delayMix: Float = 0.25f
    @Volatile var delayFeedback: Float = 0.4f
    @Volatile var reverbMix: Float = 0.15f
    @Volatile var reverbFeedback: Float = 0.5f

    // Real-time amplitude levels for level meters
    @Volatile var currentLeftLevel: Float = 0f
    @Volatile var currentRightLevel: Float = 0f

    // Sequencer internals
    private var samplesPerStep = 0
    private var stepSampleCounter = 0
    
    // Audio render thread
    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    // Real-time VST triggered notes index tracking (midiFreq -> voice)
    private val activeRealTimeNotes = HashMap<Int, Int>()

    // Frequencies for our 25-key keyboard from C4 (midi 60) to C6 (midi 84)
    private val midiFrequencies = FloatArray(25) { index ->
        440f * 2.0f.pow((index + 60 - 69) / 12f)
    }

    // Delay buffer memory (Stereo: index * 2)
    private val delayBufferSize = SAMPLE_RATE * 2 // 1.0 second max delay
    private val delayBufferL = FloatArray(delayBufferSize)
    private val delayBufferR = FloatArray(delayBufferSize)
    private var delayWriteIndex = 0

    // Reverb buffer memory (Stereo reverb using simple feedback comb loop)
    private val reverbBufferSize = (SAMPLE_RATE * 0.08f).toInt() // 80ms room size
    private val reverbBufferL = FloatArray(reverbBufferSize)
    private val reverbBufferR = FloatArray(reverbBufferSize)
    private var reverbWriteIndex = 0

    init {
        updateStepLength()
    }

    private fun updateStepLength() {
        // Beats per min -> Beats per sec -> Seconds per 16th step -> Samples per step
        val beatDurationSec = 60.0f / bpm
        val stepDurationSec = beatDurationSec / 4.0f // 16th notes
        samplesPerStep = (stepDurationSec * SAMPLE_RATE).toInt()
    }

    fun start() {
        if (isPlaying.get()) return
        
        isPlaying.set(true)
        updateStepLength()
        
        audioThread = Thread({ renderLoop() }, "DAW_Audio_Thread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
        Log.d(TAG, "Audio Engine Started")
    }

    fun stop() {
        if (!isPlaying.get()) return
        
        isPlaying.set(false)
        audioThread?.join()
        audioThread = null
        
        // Stop all voices
        for (voice in synthVoices) {
            voice.triggerOff()
        }
        Log.d(TAG, "Audio Engine Stopped")
    }

    fun togglePlayback() {
        if (isPlaying.get()) {
            stop()
        } else {
            start()
        }
    }

    fun isEnginePlaying(): Boolean = isPlaying.get()

    fun resetSequencer() {
        currentStep.set(0)
        _currentStepFlow.value = 0
        stepSampleCounter = 0
    }

    // Trigger keyboard play (VST) from UI with ultra-low latency
    fun liveNoteOn(noteIndex: Int) {
        if (noteIndex !in 0..24) return
        val freq = midiFrequencies[noteIndex]
        
        // Find an idle voice, or steal the oldest
        var voiceToUse = -1
        for (i in synthVoices.indices) {
            if (!synthVoices[i].isPlaying) {
                voiceToUse = i
                break
            }
        }
        
        if (voiceToUse == -1) {
            // Steal voice 0 (or some index)
            voiceToUse = 0
        }

        // Apply parameters before trigger
        val voice = synthVoices[voiceToUse]
        voice.cutoff = synthCutoff
        voice.resonance = synthResonance
        
        voice.triggerOn(
            freq = freq,
            sampleRate = SAMPLE_RATE,
            attack = synthAttack,
            decay = synthDecay,
            sustain = synthSustain,
            release = synthRelease,
            waveType = synthWaveform
        )
        activeRealTimeNotes[noteIndex] = voiceToUse
    }

    fun liveNoteOff(noteIndex: Int) {
        val voiceIndex = activeRealTimeNotes[noteIndex]
        if (voiceIndex != null) {
            synthVoices[voiceIndex].triggerOff()
            activeRealTimeNotes.remove(noteIndex)
        }
    }

    private fun renderLoop() {
        // Initialize AudioTrack for low-latency floating point stereo output
        val minBufSizeInBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        
        // Choose a small render chunk (e.g. 512 frames = 1024 samples) to optimize for low latency
        val bufferFrameSize = 512
        val bufferSampleSize = bufferFrameSize * NUM_CHANNELS
        val writeBuffer = FloatArray(bufferSampleSize)
        
        val actualBufSize = maxOf(minBufSizeInBytes, bufferSampleSize * 4) // Float is 4 bytes

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build())
                .setBufferSizeInBytes(actualBufSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack: ${e.message}")
            isPlaying.set(false)
            return
        }

        while (isPlaying.get()) {
            var leftMax = 0f
            var rightMax = 0f

            for (i in 0 until bufferFrameSize) {
                // 1. Sequencer step accurate processing
                if (stepSampleCounter >= samplesPerStep) {
                    stepSampleCounter = 0
                    
                    val step = currentStep.get()
                    // Trigger elements for the current step
                    triggerStepEvents(step)
                    
                    // Increment step
                    val next = (step + 1) % 16
                    currentStep.set(next)
                    _currentStepFlow.value = next
                }
                stepSampleCounter++

                // 2. Synthesise and accumulate channels
                
                // --- SYNTH VOICE RENDER ---
                var synthMono = 0f
                for (v in 0 until POLYPHONY) {
                    val voice = synthVoices[v]
                    // Keep filter params updated dynamically
                    voice.cutoff = synthCutoff
                    voice.resonance = synthResonance
                    synthMono += voice.nextSample()
                }
                synthMono *= volSynth

                // Apply Synth panning
                val panS = panSynth.coerceIn(-1.0f, 1.0f)
                val synthL = synthMono * (1.0f - panS).coerceIn(0f, 1f)
                val synthR = synthMono * (1.0f + panS).coerceIn(0f, 1f)

                // --- DRUM VOICES RENDER ---
                val kickMono = kickSynth.nextSample() * volKick
                val snareMono = snareSynth.nextSample() * volSnare
                val hihatMono = hihatSynth.nextSample() * volHiHat
                val clapMono = clapSynth.nextSample() * volClap

                // Apply panning for drums
                val kickL = kickMono * (1.0f - panKick).coerceIn(0f, 1f)
                val kickR = kickMono * (1.0f + panKick).coerceIn(0f, 1f)

                val snareL = snareMono * (1.0f - panSnare).coerceIn(0f, 1f)
                val snareR = snareMono * (1.0f + panSnare).coerceIn(0f, 1f)

                val hihatL = hihatMono * (1.0f - panHiHat).coerceIn(0f, 1f)
                val hihatR = hihatMono * (1.0f + panHiHat).coerceIn(0f, 1f)

                val clapL = clapMono * (1.0f - panClap).coerceIn(0f, 1f)
                val clapR = clapMono * (1.0f + panClap).coerceIn(0f, 1f)

                // Sum dry mix
                var dryL = synthL + kickL + snareL + hihatL + clapL
                var dryR = synthR + kickR + snareR + hihatR + clapR

                // 3. Digital Effects Processing (Delay & Reverb sends)
                
                // --- DELAY EFFECT ---
                // Delay send is based on the synth signal
                val delayInputL = synthL * delayMix
                val delayInputR = synthR * delayMix
                
                val delayReadIdx = (delayWriteIndex - (SAMPLE_RATE * 0.35f).toInt() + delayBufferSize) % delayBufferSize
                val delayOutL = delayBufferL[delayReadIdx]
                val delayOutR = delayBufferR[delayReadIdx]

                // Feedback mixing back into delay
                delayBufferL[delayWriteIndex] = delayInputL + delayOutL * delayFeedback
                delayBufferR[delayWriteIndex] = delayInputR + delayOutR * delayFeedback
                
                delayWriteIndex = (delayWriteIndex + 1) % delayBufferSize

                // --- REVERB EFFECT ---
                // Reverb send is based on snare, hihat, clap, and synth
                val reverbInputL = (snareL + clapL + synthL * 0.4f) * reverbMix
                val reverbInputR = (snareR + clapR + synthR * 0.4f) * reverbMix
                
                val reverbReadIdx = (reverbWriteIndex - (SAMPLE_RATE * 0.065f).toInt() + reverbBufferSize) % reverbBufferSize
                val reverbOutL = reverbBufferL[reverbReadIdx]
                val reverbOutR = reverbBufferR[reverbReadIdx]

                reverbBufferL[reverbWriteIndex] = reverbInputL + reverbOutL * reverbFeedback
                reverbBufferR[reverbWriteIndex] = reverbInputR + reverbOutR * reverbFeedback
                
                reverbWriteIndex = (reverbWriteIndex + 1) % reverbBufferSize

                // Sum dry with wet effects
                var finalL = (dryL + delayOutL + reverbOutL) * volMaster
                var finalR = (dryR + delayOutR + reverbOutR) * volMaster

                // Hard limit/clipping safety protection
                finalL = finalL.coerceIn(-1.0f, 1.0f)
                finalR = finalR.coerceIn(-1.0f, 1.0f)

                // Fill buffer
                writeBuffer[i * 2] = finalL
                writeBuffer[i * 2 + 1] = finalR

                // Track peaks for UI Level Meters
                if (Math.abs(finalL) > leftMax) leftMax = Math.abs(finalL)
                if (Math.abs(finalR) > rightMax) rightMax = Math.abs(finalR)
            }

            // Expose peak levels dynamically
            currentLeftLevel = leftMax
            currentRightLevel = rightMax

            // Write full block to audio track (non-blocking if sized correctly)
            audioTrack?.write(writeBuffer, 0, bufferSampleSize, AudioTrack.WRITE_BLOCKING)
        }

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up AudioTrack: ${e.message}")
        }
        audioTrack = null
        currentLeftLevel = 0f
        currentRightLevel = 0f
    }

    private fun triggerStepEvents(step: Int) {
        // 1. Trigger Synth notes scheduled at this step
        // To handle polyphony correctly, we trigger off the previous steps notes and trigger on the new ones
        for (noteIndex in 0..24) {
            if (synthSequence[noteIndex][step]) {
                val freq = midiFrequencies[noteIndex]
                
                // Find an idle voice
                var voiceIndex = -1
                for (v in 0 until POLYPHONY) {
                    if (!synthVoices[v].isPlaying) {
                        voiceIndex = v
                        break
                    }
                }
                
                if (voiceIndex == -1) {
                    // Steal oldest voice
                    voiceIndex = step % POLYPHONY
                }

                val voice = synthVoices[voiceIndex]
                // Make sure notes triggered in step play for exactly the gate length (we'll trigger off after standard gate, or simple ADSR decay)
                voice.triggerOn(
                    freq = freq,
                    sampleRate = SAMPLE_RATE,
                    attack = synthAttack,
                    decay = synthDecay,
                    sustain = synthSustain,
                    release = synthRelease,
                    waveType = synthWaveform
                )
                
                // Set a quick release timer or let the note decays naturally based on decay settings. 
                // To keep step sequence neat, we'll release notes on step boundary if we have overlapping triggers.
            } else {
                // If there are no new notes, we can let previous notes sustain, or we can shut them down.
                // Standard DAW lets notes ring out based on ADSR release values.
            }
        }

        // 2. Trigger Drum instruments scheduled at this step
        if (drumSequence[0][step]) {
            kickSynth.trigger(1.0f)
        }
        if (drumSequence[1][step]) {
            snareSynth.trigger(1.0f)
        }
        if (drumSequence[2][step]) {
            hihatSynth.trigger(1.0f)
        }
        if (drumSequence[3][step]) {
            clapSynth.trigger(1.0f)
        }
    }
}
