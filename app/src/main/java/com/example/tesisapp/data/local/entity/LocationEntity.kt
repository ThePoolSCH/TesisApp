package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_table")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val managerName: String,
    val managerPhone: String,
    val status: String // "Pendiente", "Visitada", etc.
)