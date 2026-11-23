package com.example.tesisapp.data.repository

import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.dto.AuthParams
import com.example.tesisapp.data.remote.dto.OdooAuthRequest
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.repository.AuthRepository
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: OdooApiService
) : AuthRepository {

    override suspend fun login(db: String, login: String, password: String): Result<User> {
        if (login.isBlank() || password.isBlank()) {
            return Result.failure(Exception("Ingresa tu usuario y tu contraseña."))
        }
        return try {
            val request = OdooAuthRequest(params = AuthParams(db, login.trim(), password))
            val response = api.authenticate(request)
            val body = response.body()

            if (response.isSuccessful && body?.result != null) {
                val result = body.result
                // La sesión de Odoo viaja en la cookie session_id (la guarda PersistentCookieJar)
                Result.success(User(id = result.uid, name = result.name, username = result.username,
                    sessionId = response.headers()["Set-Cookie"] ?: ""))
            } else {
                Result.failure(Exception(mensajeError(response.code(), body?.error?.message, body?.error?.data?.toString(), db)))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("El servidor tardó demasiado en responder. Intenta de nuevo."))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar con el servidor. Revisa tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo iniciar sesión (${e.javaClass.simpleName})."))
        }
    }

    /** Traduce los errores de Odoo a mensajes que el vendedor entienda (antes se mostraba "Error desconocido"). */
    private fun mensajeError(http: Int, mensaje: String?, detalle: String?, db: String): String {
        val texto = "${mensaje.orEmpty()} ${detalle.orEmpty()}"
        return when {
            http >= 500 -> "El servidor tiene un problema (error $http). Avisa a tu supervisor."
            texto.contains("AccessDenied", true) || texto.contains("Access Denied", true) ->
                "Usuario o contraseña incorrectos."
            texto.contains("database", true) && (texto.contains("not exist", true) || texto.contains("KeyError")) ->
                "La base de datos \"$db\" no existe en el servidor."
            mensaje.isNullOrBlank() -> "No se pudo iniciar sesión (error $http)."
            else -> mensaje
        }
    }
}
