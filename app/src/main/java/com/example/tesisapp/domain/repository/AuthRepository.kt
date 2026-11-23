package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.User
import com.example.tesisapp.utils.Resource // Asumimos que tienes una clase sellada para manejar estados (Success, Error, Loading)

interface AuthRepository {
    suspend fun login(db: String, login: String, password: String): Result<User>
}