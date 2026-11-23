package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Route
import com.example.tesisapp.domain.model.RouteStop
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    // Observar la ruta guardada en local
    fun getTodayRoute(): Flow<Route?>

    // Llamar a la API y guardar en local (sin perder lo hecho sin conexión)
    suspend fun syncRoute(): Result<Unit>

    /** Llegada al cliente: se guarda en el teléfono y se encola (F4, sin conexión). */
    suspend fun registrarLlegada(stop: RouteStop, lat: Double, lon: Double, fueraDeRango: Boolean, motivo: String?)

    /** Fin de la visita con su resultado obligatorio (RF-04). */
    suspend fun terminarVisita(stop: RouteStop, resultado: String, nota: String?, lat: Double?, lon: Double?)

    suspend fun updateLocalStatus(stopId: Int, status: String)

    /** Suma el pedido encolado al total vendido de la parada (para verlo sin esperar a Odoo). */
    suspend fun sumarVentaLocal(stopId: Int, monto: Double)
}
