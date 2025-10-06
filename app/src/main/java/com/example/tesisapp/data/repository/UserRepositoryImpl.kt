package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.local.entity.UserEntity
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
    // private val apiService: ApiService // En el futuro, inyectarías la API aquí
) : UserRepository {

    // ---- MAPERS ----
    // Funciones para convertir entre Entity (DB) y Model (Domain)
    private fun UserEntity.toDomain(): User {
        return User(id = this.id, name = this.name, email = this.email, token = this.token)
    }

    private fun User.toEntity(): UserEntity {
        return UserEntity(id = this.id, name = this.name, email = this.email, token = this.token)
    }
    // ---- FIN MAPERS ----


    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            // SIMULACIÓN DE LLAMADA A API
            delay(1500) // Simular latencia de red

            // ¡AQUÍ ESTÁ EL HARDCODEO!
            // En un caso real, recibirías un UserDto de la API
            val hardcodedUser = User(
                id = "123",
                name = "Juan Pérez",
                email = email, // Usamos el email del input
                token = "fake-jwt-token-abcdef123456"
            )

            // Guardamos el usuario en la base de datos local
            userDao.insertUser(hardcodedUser.toEntity())

            Result.success(Unit)
        } catch (e: Exception) {
            // En un caso real, manejarías errores de red, etc.
            Result.failure(e)
        }
    }

    override fun getSavedUser(): Flow<User?> {
        // Obtenemos el UserEntity desde el DAO y lo mapeamos a nuestro modelo de dominio User
        return userDao.getUser().map { userEntity ->
            userEntity?.toDomain()
        }
    }

    override suspend fun logout() {
        userDao.deleteUser()
    }
}