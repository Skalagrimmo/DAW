package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DAWDao {
    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM piano_notes WHERE projectId = :projectId")
    suspend fun getNotesForProject(projectId: Long): List<NoteEntity>

    @Query("SELECT * FROM drum_steps WHERE projectId = :projectId")
    suspend fun getDrumStepsForProject(projectId: Long): List<DrumStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrumSteps(steps: List<DrumStepEntity>)

    @Query("DELETE FROM piano_notes WHERE projectId = :projectId")
    suspend fun deleteNotesForProject(projectId: Long)

    @Query("DELETE FROM drum_steps WHERE projectId = :projectId")
    suspend fun deleteDrumStepsForProject(projectId: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: Long)

    @Transaction
    suspend fun saveFullProject(
        project: ProjectEntity,
        notes: List<NoteEntity>,
        drumSteps: List<DrumStepEntity>
    ): Long {
        val id = if (project.id == 0L) {
            insertProject(project)
        } else {
            insertProject(project)
            project.id
        }
        
        // Delete old relations and insert new ones
        deleteNotesForProject(id)
        deleteDrumStepsForProject(id)
        
        // Associate with the saved project ID
        val associatedNotes = notes.map { it.copy(projectId = id) }
        val associatedSteps = drumSteps.map { it.copy(projectId = id) }
        
        insertNotes(associatedNotes)
        insertDrumSteps(associatedSteps)
        
        return id
    }
}
