package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
