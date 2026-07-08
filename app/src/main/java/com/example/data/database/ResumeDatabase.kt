package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.Resume
import com.example.data.model.User
import com.example.data.model.ResumeTypeConverters

@Database(entities = [Resume::class, User::class], version = 2, exportSchema = false)
@TypeConverters(ResumeTypeConverters::class)
abstract class ResumeDatabase : RoomDatabase() {
    abstract fun resumeDao(): ResumeDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ResumeDatabase? = null

        fun getDatabase(context: Context): ResumeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ResumeDatabase::class.java,
                    "resume_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
