package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, NoteEntity::class, DrumStepEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DAWDatabase : RoomDatabase() {
    abstract fun dawDao(): DAWDao

    companion object {
        @Volatile
        private var INSTANCE: DAWDatabase? = null

        fun getDatabase(context: Context): DAWDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DAWDatabase::class.java,
                    "daw_studio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
