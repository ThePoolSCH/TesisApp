package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // Simularemos el login. En el futuro, aquí iría la llamada a la API.
    suspend fun login(email: String, password: String): Result<Unit>

    // Obtiene el usuario guardado localmente
    fun getSavedUser(): Flow<User?>

    // Cierra sesión y borra los datos locales
    suspend fun logout()
}