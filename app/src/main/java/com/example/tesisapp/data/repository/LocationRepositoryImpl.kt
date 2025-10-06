package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.dao.LocationDao
import com.example.tesisapp.data.local.entity.LocationEntity
import com.example.tesisapp.domain.model.Location
import com.example.tesisapp.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao
) : LocationRepository {

    // Mapper para convertir de Entity (DB) a Model (Domain)
    private fun LocationEntity.toDomain(): Location {
        return Location(
            id = this.id,
            name = this.name,
            address = this.address,
            status = this.status,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }

    override fun getLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun seedLocationsIfEmpty() {
        // Solo insertamos los datos si la tabla está vacía
        if (locationDao.count() == 0) {
            val hardcodedLocations = listOf(
                LocationEntity(name = "Bodega Mi Pueblo", latitude = -12.086, longitude = -77.05, address = "Calle Lima 321, Pueblo Libre", managerName = "Carlos Mendoza", managerPhone = "987654321", status = "Pendiente"),
                LocationEntity(name = "Market Express", latitude = -12.088, longitude = -77.052, address = "Av. Sucre 550, Magdalena", managerName = "Ana Torres", managerPhone = "912345678", status = "Pendiente"),
                LocationEntity(name = "Minimarket El Sol", latitude = -12.09, longitude = -77.048, address = "Jr. Libertad 102, San Miguel", managerName = "Sofia Ramirez", managerPhone = "998877665", status = "Visitada")
            )
            locationDao.insertAll(hardcodedLocations)
        }
    }

    override suspend fun updateLocationStatus(locationId: Int, newStatus: String) { // <-- NUEVO
        locationDao.updateStatus(locationId, newStatus)
    }
}