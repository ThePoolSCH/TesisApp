package com.example.tesisapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tesisapp.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    // Usamos Flow para que la UI se actualice automáticamente
    @Query("SELECT * FROM location_table")
    fun getAllLocations(): Flow<List<LocationEntity>>

    // OnConflictStrategy.IGNORE: si intentamos insertar una ubicación que ya existe, no hace nada.
    // Útil para que los datos de ejemplo solo se inserten una vez.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(locations: List<LocationEntity>)

    @Query("SELECT COUNT(*) FROM location_table")
    suspend fun count(): Int

    @Query("UPDATE location_table SET status = :newStatus WHERE id = :locationId")
    suspend fun updateStatus(locationId: Int, newStatus: String)
}