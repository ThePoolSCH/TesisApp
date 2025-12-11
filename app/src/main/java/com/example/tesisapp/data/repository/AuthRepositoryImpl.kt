package com.example.tesisapp.data.repository

import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.dto.AuthParams
import com.example.tesisapp.data.remote.dto.OdooAuthRequest
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: OdooApiService
) : AuthRepository {

    override suspend fun login(db: String, login: String, password: String): Result<User> {
        return try {
            val request = OdooAuthRequest(params = AuthParams(db, login, password))
            val response = api.authenticate(request)

            if (response.isSuccessful && response.body()?.result != null) {
                val result = response.body()!!.result!!

                // OJO: Odoo maneja la sesión por Cookies.
                // Retrofit/OkHttp debe guardar la cookie "session_id".
                // Aquí simulamos obtenerla, pero lo ideal es un CookieJar en DI.
                val cookieHeader = response.headers()["Set-Cookie"] ?: ""

                Result.success(
                    User(
                        id = result.uid,
                        name = result.name,
                        username = result.username,
                        sessionId = cookieHeader // Simplificación
                    )
                )
            } else {
                val errorMsg = response.body()?.error?.message ?: "Error desconocido"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}