package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.RouteDao
import com.example.tesisapp.data.local.RouteEntity
import com.example.tesisapp.data.local.StopEntity
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.OdooJsonRpcRequest
import com.example.tesisapp.domain.model.Route
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.repository.RouteRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val dao: RouteDao
) : RouteRepository {

    override fun getTodayRoute(): Flow<Route?> {
        return dao.getLatestRouteWithStops().map { entity ->
            entity?.let {
                // Mapeo de Entity a Domain
                Route(
                    id = it.route.id,
                    name = it.route.name,
                    date = it.route.date,
                    // Convertimos el String JSON guardado de vuelta a lista para el dominio
                    geometry = Gson().fromJson(it.route.geometryJson, List::class.java) as List<List<Double>>,
                    stops = it.stops.map { stop ->
                        RouteStop(
                            id = stop.id,
                            sequence = stop.sequence,
                            partnerName = stop.partnerName,
                            address = stop.address,
                            latitude = stop.latitude,
                            longitude = stop.longitude,
                            visitState = stop.visitState,
                            totalSold = stop.totalSold
                        )
                    }.sortedBy { s -> s.sequence }
                )
            }
        }
    }

    override suspend fun syncRoute(): Result<Unit> {
        return try {
            val response = api.getTodayRoute(OdooJsonRpcRequest())

            if (response.isSuccessful && response.body()?.result?.status == "success") {
                val data = response.body()?.result?.data

                if (data != null) {
                    val routeEntity = RouteEntity(
                        id = data.routeId,
                        name = data.routeName,
                        date = data.date,
                        geometryJson = Gson().toJson(data.geometry) // Guardar como String
                    )

                    val stopsEntities = data.stops.map { stop ->
                        StopEntity(
                            id = stop.id,
                            routeId = data.routeId,
                            sequence = stop.sequence,
                            partnerName = stop.partnerName,
                            address = stop.address,
                            latitude = stop.latitude,
                            longitude = stop.longitude,
                            visitState = stop.visitState,
                            totalSold = stop.totalSold
                        )
                    }

                    // Guardar en Room (borrando lo anterior)
                    dao.updateRouteData(routeEntity, stopsEntities)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Datos vacíos"))
                }
            } else {
                Result.failure(Exception("Error en API o No hay ruta"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}