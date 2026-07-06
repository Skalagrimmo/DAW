package com.example.data

import kotlinx.coroutines.flow.Flow

class DAWRepository(private val dawDao: DAWDao) {
    val allProjects: Flow<List<ProjectEntity>> = dawDao.getAllProjects()

    suspend fun getProjectById(projectId: Long): ProjectEntity? {
        return dawDao.getProjectById(projectId)
    }

    suspend fun getNotesForProject(projectId: Long): List<NoteEntity> {
        return dawDao.getNotesForProject(projectId)
    }

    suspend fun getDrumStepsForProject(projectId: Long): List<DrumStepEntity> {
        return dawDao.getDrumStepsForProject(projectId)
    }

    suspend fun saveProject(
        project: ProjectEntity,
        notes: List<NoteEntity>,
        drumSteps: List<DrumStepEntity>
    ): Long {
        return dawDao.saveFullProject(project, notes, drumSteps)
    }

    suspend fun deleteProject(projectId: Long) {
        dawDao.deleteProject(projectId)
        dawDao.deleteNotesForProject(projectId)
        dawDao.deleteDrumStepsForProject(projectId)
    }
}
