package com.example.data.repository

import com.example.data.database.UserDao
import com.example.data.model.User

class UserRepository(private val userDao: UserDao) {
    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun getActiveUser(): User? {
        return userDao.getActiveUser()
    }

    suspend fun registerUser(user: User): Long {
        return userDao.insertUser(user)
    }

    suspend fun loginUser(user: User) {
        userDao.logoutAllUsers()
        userDao.updateUser(user.copy(isLoggedIn = true))
    }

    suspend fun logout() {
        userDao.logoutAllUsers()
    }
}
