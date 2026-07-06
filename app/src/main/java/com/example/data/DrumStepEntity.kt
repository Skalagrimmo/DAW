package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drum_steps")
data class DrumStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val drumId: Int, // 0: Kick, 1: Snare, 2: HiHat, 3: Clap
    val step: Int    // Step index from 0 to 15
)
