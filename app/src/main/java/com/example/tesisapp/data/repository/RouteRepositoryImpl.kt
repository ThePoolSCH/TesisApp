package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.local.RouteDao
import com.example.tesisapp.data.local.RouteEntity
import com.example.tesisapp.data.local.StopEntity
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.OdooJsonRpcRequest
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.domain.model.Route
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.repository.RouteRepository
import com.example.tesisapp.utils.Formato
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val dao: RouteDao,
    private val outbox: OutboxRepository
) : RouteRepository {

    private val orden = mapOf("pending" to 0, "arrived" to 1, "done" to 2)

    override fun getTodayRoute(): Flow<Route?> {
        return dao.getLatestRouteWithStops().map { entity ->
            entity?.let {
                val tipo = object : TypeToken<List<List<Double>>>() {}.type
                Route(
                    id = it.route.id,
                    name = it.route.name,
                    date = it.route.date,
                    geometry = Gson().fromJson<List<List<Double>>>(it.route.geometryJson, tipo) ?: emptyList(),
                    stops = it.stops.map { s -> s.toDomain() }.sortedBy { s -> s.sequence },
                    checkinRadiusM = it.route.checkinRadiusM
                )
            }
        }
    }

    private fun StopEntity.toDomain() = RouteStop(
        id = id, sequence = sequence, partnerName = partnerName, address = address, latitude = latitude,
        longitude = longitude, visitState = visitState, totalSold = totalSold, partnerId = partnerId, street = street,
        plannedArrival = plannedArrival, visitResult = visitResult, visitClass = visitClass, clientType = clientType,
        unplanned = unplanned, checkinAt = checkinAt, outOfRange = outOfRange
    )

    override suspend fun syncRoute(): Result<Unit> {
        return try {
            val response = api.getTodayRoute(OdooJsonRpcRequest())
            val result = response.body()?.result
            if (!response.isSuccessful || result?.status != "success" || result.data == null) {
                return Result.failure(Exception(result?.message ?: "No se pudo obtener la ruta (error ${response.code()})"))
            }
            val data = result.data
            // Lo hecho en el teléfono y aún no enviado no se pierde: si la parada local está más avanzada
            // (pendiente < en visita < visitada) que en el servidor, se conserva su estado.
            val locales = dao.getStops().associateBy { it.id }
            val stops = data.stops.map { s ->
                val servidor = StopEntity(
                    id = s.id, routeId = data.routeId, sequence = s.sequence, partnerName = s.partnerName,
                    address = s.address, latitude = s.latitude, longitude = s.longitude, visitState = s.visitState,
                    totalSold = s.totalSold, partnerId = s.partnerId ?: 0, street = s.street.orEmpty(),
                    plannedArrival = s.plannedArrival.orEmpty(), visitResult = s.visitResult.orEmpty(),
                    visitClass = s.visitClass.orEmpty(), clientType = s.clientType.orEmpty(), unplanned = s.unplanned == true
                )
                val local = locales[s.id]
                when {
                    local == null -> servidor
                    (orden[local.visitState] ?: 0) > (orden[servidor.visitState] ?: 0) -> servidor.copy(
                        visitState = local.visitState, visitResult = local.visitResult.ifBlank { servidor.visitResult },
                        checkinAt = local.checkinAt, outOfRange = local.outOfRange,
                        totalSold = maxOf(local.totalSold, servidor.totalSold))
                    else -> servidor.copy(checkinAt = local.checkinAt, outOfRange = local.outOfRange)
                }
            }
            dao.updateRouteData(
                RouteEntity(id = data.routeId, name = data.routeName, date = data.date,
                    geometryJson = Gson().toJson(data.geometry), checkinRadiusM = data.checkinRadiusM ?: 100.0),
                stops
            )
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión: se muestra la última ruta descargada."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registrarLlegada(stop: RouteStop, lat: Double, lon: Double, fueraDeRango: Boolean, motivo: String?) {
        dao.marcarLlegada(stop.id, System.currentTimeMillis(), fueraDeRango)
        outbox.encolar(PendingOpEntity.CHECKIN, stop.id, "Llegada · ${stop.partnerName}", mapOf(
            "route_line_id" to stop.id, "latitude" to lat, "longitude" to lon,
            "out_of_range_reason" to motivo?.takeIf { fueraDeRango && it.isNotBlank() },
            "timestamp" to Formato.utcAhora()
        ))
    }

    override suspend fun terminarVisita(stop: RouteStop, resultado: String, nota: String?, lat: Double?, lon: Double?) {
        dao.marcarTerminada(stop.id, resultado)
        outbox.encolar(PendingOpEntity.CHECKOUT, stop.id, "Fin de visita · ${stop.partnerName}", mapOf(
            "route_line_id" to stop.id, "result" to resultado, "note" to nota?.takeIf { it.isNotBlank() },
            "latitude" to lat, "longitude" to lon, "timestamp" to Formato.utcAhora()
        ))
    }

    override suspend fun updateLocalStatus(stopId: Int, status: String) {
        dao.updateStopStatus(stopId, status)
    }

    override suspend fun sumarVentaLocal(stopId: Int, monto: Double) = dao.sumarVenta(stopId, monto)
}
