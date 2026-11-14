package com.nasbus.app.data.repository

import android.content.Context
import com.nasbus.app.data.local.db.AppDatabase
import com.nasbus.app.data.local.entity.UserEntity
import com.nasbus.app.util.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class UserRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val existingEmail = userDao.getByEmail(email)
            if (existingEmail != null) {
                return@withContext Result.failure(IllegalArgumentException("Email is already registered"))
            }

            val existingUsername = userDao.getByUsername(username)
            if (existingUsername != null) {
                return@withContext Result.failure(IllegalArgumentException("Username is already taken"))
            }

            val passwordHash = PasswordUtils.hashPassword(password)
            val newUser = UserEntity(
                username = username,
                email = email,
                passwordHash = passwordHash
            )

            val insertedId = userDao.insert(newUser)
            val createdUser = newUser.copy(userId = insertedId)
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        emailOrUsername: String,
        password: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            // Check if input is email or username
            val user = userDao.getByEmail(emailOrUsername)
                ?: userDao.getByUsername(emailOrUsername)

            // Generic error message for all login failures to prevent account enumeration
            val genericError = IllegalArgumentException("Incorrect email or password")

            if (user == null) {
                return@withContext Result.failure(genericError)
            }

            val isPasswordValid = PasswordUtils.verifyPassword(password, user.passwordHash)
            if (!isPasswordValid) {
                return@withContext Result.failure(genericError)
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Incorrect email or password"))
        }
    }

    fun getUserById(userId: Long): Flow<UserEntity?> = flow {
        emit(userDao.getById(userId))
    }.flowOn(Dispatchers.IO)
}
