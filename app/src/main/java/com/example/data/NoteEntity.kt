package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "piano_notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val pitch: Int, // Midi note index or octave offset
    val step: Int,  // Step index from 0 to 15
    val length: Int = 1 // Duration in steps
)
