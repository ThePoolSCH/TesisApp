package com.example.tesisapp.domain.repository


import com.example.tesisapp.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocations(): Flow<List<Location>>
    suspend fun seedLocationsIfEmpty() // Método para insertar datos de ejemplo
    suspend fun updateLocationStatus(locationId: Int, newStatus: String)
}