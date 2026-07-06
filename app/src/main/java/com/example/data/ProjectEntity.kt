package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bpm: Int = 120,
    
    // Synth ADSR parameters (0.0f - 1.0f)
    val attack: Float = 0.1f,
    val decay: Float = 0.2f,
    val sustain: Float = 0.7f,
    val release: Float = 0.3f,
    
    // Synth Filter parameters
    val cutoff: Float = 0.8f,
    val resonance: Float = 0.3f,
    val waveformType: Int = 0, // 0: Sine, 1: Triangle, 2: Sawtooth, 3: Square
    
    // Mixer volumes (0.0f - 1.0f)
    val volumeSynth: Float = 0.8f,
    val volumeKick: Float = 0.9f,
    val volumeSnare: Float = 0.8f,
    val volumeHiHat: Float = 0.7f,
    val volumeClap: Float = 0.8f,
    val volumeMaster: Float = 0.9f,
    
    // Mixer Panning (-1.0f (Left) to 1.0f (Right))
    val panSynth: Float = 0.0f,
    val panKick: Float = 0.0f,
    val panSnare: Float = -0.1f,
    val panHiHat: Float = 0.2f,
    val panClap: Float = -0.2f,
    
    // Effects Delay (Mix 0-1, Feedback 0-1)
    val delayMix: Float = 0.3f,
    val delayFeedback: Float = 0.4f,
    
    // Effects Reverb
    val reverbMix: Float = 0.2f,
    val reverbFeedback: Float = 0.5f,
    
    val timestamp: Long = System.currentTimeMillis()
)
