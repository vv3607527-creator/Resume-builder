package com.example.data.repository

import com.example.data.database.ResumeDao
import com.example.data.model.Resume
import kotlinx.coroutines.flow.Flow

class ResumeRepository(private val resumeDao: ResumeDao) {
    val allResumes: Flow<List<Resume>> = resumeDao.getAllResumes()

    fun getResumesForUser(userId: Long): Flow<List<Resume>> {
        return resumeDao.getResumesForUser(userId)
    }

    suspend fun getResumeById(id: Long): Resume? {
        return resumeDao.getResumeById(id)
    }

    suspend fun insertResume(resume: Resume): Long {
        return resumeDao.insertResume(resume)
    }

    suspend fun updateResume(resume: Resume) {
        resumeDao.updateResume(resume)
    }

    suspend fun deleteResume(resume: Resume) {
        resumeDao.deleteResume(resume)
    }
}
