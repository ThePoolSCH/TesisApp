package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tesisapp.domain.model.User

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val id: Int, // Odoo usa Int
    val name: String,
    val username: String,
    val sessionId: String
) {
    // Convierte de Base de Datos -> Dominio
    fun toDomain(): User {
        return User(
            id = id,
            name = name,
            username = username,
            sessionId = sessionId
        )
    }
}

// --- ¡ESTO ES LO QUE TE FALTA O ESTÁ FALLANDO! ---
// Convierte de Dominio -> Base de Datos
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        username = username,
        sessionId = sessionId
    )
}