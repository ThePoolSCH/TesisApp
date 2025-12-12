package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    // Observar la ruta guardada en local
    fun getTodayRoute(): Flow<Route?>

    // Llamar a la API y guardar en local
    suspend fun syncRoute(): Result<Unit>

    suspend fun startVisit(stopId: Int, lat: Double, lon: Double): Result<Unit>

    suspend fun updateLocalStatus(stopId: Int, status: String)

    suspend fun finishVisit(stopId: Int): Result<Unit>
}