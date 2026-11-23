package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.remote.PersistentCookieJar
import com.example.tesisapp.data.local.entity.toEntity
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val cookieJar: PersistentCookieJar
) : UserRepository {

    override suspend fun saveUser(user: User) {
        userDao.insertUser(user.toEntity())
    }

    override suspend fun logout() {
        userDao.clearUser()
        cookieJar.clear() // F4: la sesión ahora se guarda en el teléfono; al salir se borra
    }

    // Aquí convertimos el Flow de Entity a Flow de Domain
    override fun getSavedUser(): Flow<User?> {
        return userDao.getUserFlow().map { entity ->
            entity?.toDomain()
        }
    }
}