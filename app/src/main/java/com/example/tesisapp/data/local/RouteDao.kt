package com.example.tesisapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Transaction
    @Query("SELECT * FROM routes WHERE date = date('now', 'localtime') ORDER BY date DESC LIMIT 1")
    fun getLatestRouteWithStops(): Flow<RouteWithStops?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<StopEntity>)

    @Query("DELETE FROM routes")
    suspend fun clearRoutes()

    @Query("DELETE FROM stops")
    suspend fun clearStops()

    @Transaction
    suspend fun updateRouteData(route: RouteEntity, stops: List<StopEntity>) {
        clearRoutes()
        clearStops()
        insertRoute(route)
        insertStops(stops)
    }

    @Query("UPDATE stops SET visitState = :newState WHERE id = :stopId")
    suspend fun updateStopStatus(stopId: Int, newState: String)

    // F4
    @Query("SELECT * FROM stops")
    suspend fun getStops(): List<StopEntity>

    @Query("SELECT * FROM stops WHERE id = :stopId")
    suspend fun getStop(stopId: Int): StopEntity?

    @Query("UPDATE stops SET visitState = 'arrived', checkinAt = :checkinAt, outOfRange = :outOfRange WHERE id = :stopId")
    suspend fun marcarLlegada(stopId: Int, checkinAt: Long, outOfRange: Boolean)

    @Query("UPDATE stops SET visitState = 'done', visitResult = :result WHERE id = :stopId")
    suspend fun marcarTerminada(stopId: Int, result: String)

    @Query("UPDATE stops SET totalSold = totalSold + :monto WHERE id = :stopId")
    suspend fun sumarVenta(stopId: Int, monto: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: StopEntity)
}