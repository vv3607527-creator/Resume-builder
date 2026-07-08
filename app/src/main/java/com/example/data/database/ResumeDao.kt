package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Resume
import kotlinx.coroutines.flow.Flow

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resumes ORDER BY timestamp DESC")
    fun getAllResumes(): Flow<List<Resume>>

    @Query("SELECT * FROM resumes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getResumesForUser(userId: Long): Flow<List<Resume>>

    @Query("SELECT * FROM resumes WHERE id = :id LIMIT 1")
    suspend fun getResumeById(id: Long): Resume?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResume(resume: Resume): Long

    @Update
    suspend fun updateResume(resume: Resume)

    @Delete
    suspend fun deleteResume(resume: Resume)
}
