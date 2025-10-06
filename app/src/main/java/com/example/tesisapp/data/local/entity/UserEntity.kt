package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val token: String
)