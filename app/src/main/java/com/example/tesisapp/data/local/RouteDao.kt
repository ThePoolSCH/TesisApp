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
    @Query("SELECT * FROM routes ORDER BY date DESC LIMIT 1")
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
}