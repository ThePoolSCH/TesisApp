package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.User
import kotlinx.coroutines.flow.Flow


interface UserRepository {
    // Eliminamos 'login' de aquí, esa responsabilidad es de AuthRepository
    suspend fun saveUser(user: User)
    suspend fun logout()
    fun getSavedUser(): Flow<User?>
}