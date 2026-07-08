package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "resumes")
data class Resume(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 0L,
    val title: String = "Untitled Resume",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val location: String = "",
    val summary: String = "",
    val workExperiences: List<WorkExperience> = emptyList(),
    val educations: List<Education> = emptyList(),
    val skills: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val templateId: String = "modern", // "modern", "serif", "minimalist", "creative"
    val timestamp: Long = System.currentTimeMillis()
)

data class WorkExperience(
    val company: String = "",
    val role: String = "",
    val duration: String = "", // e.g. "2022 - Present"
    val description: String = "" // Bullet points or descriptions
)

data class Education(
    val school: String = "",
    val degree: String = "",
    val duration: String = "", // e.g. "2018 - 2022"
    val description: String = ""
)

class ResumeTypeConverters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    private val workListType = Types.newParameterizedType(List::class.java, WorkExperience::class.java)
    private val workListAdapter = moshi.adapter<List<WorkExperience>>(workListType)

    private val eduListType = Types.newParameterizedType(List::class.java, Education::class.java)
    private val eduListAdapter = moshi.adapter<List<Education>>(eduListType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = stringListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        if (json.isNullOrEmpty()) return emptyList()
        return stringListAdapter.fromJson(json)
    }

    @TypeConverter
    fun fromWorkList(list: List<WorkExperience>?): String? = workListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toWorkList(json: String?): List<WorkExperience>? {
        if (json.isNullOrEmpty()) return emptyList()
        return workListAdapter.fromJson(json)
    }

    @TypeConverter
    fun fromEduList(list: List<Education>?): String? = eduListAdapter.toJson(list ?: emptyList())

    @TypeConverter
    fun toEduList(json: String?): List<Education>? {
        if (json.isNullOrEmpty()) return emptyList()
        return eduListAdapter.fromJson(json)
    }
}
