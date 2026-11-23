package com.example.tesisapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Clase auxiliar para Room (Relación 1 a N)
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: Int, // ID de Odoo
    val name: String,
    val date: String,
    val geometryJson: String, // Guardaremos el JSON como string en la BD
    val checkinRadiusM: Double = 100.0 // F4: para validar la llegada sin conexión
)

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val id: Int, // ID de la línea de ruta de Odoo
    val routeId: Int, // FK
    val sequence: Int,
    val partnerName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val visitState: String,
    val totalSold: Double,
    // F4 (mockup A1)
    val partnerId: Int = 0,
    val street: String = "",
    val plannedArrival: String = "",
    val visitResult: String = "",
    val visitClass: String = "",
    val clientType: String = "",
    val unplanned: Boolean = false,
    val checkinAt: Long = 0L, // hora local de llegada (para el cronómetro de la visita)
    val outOfRange: Boolean = false
)



data class RouteWithStops(
    @Embedded val route: RouteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routeId"
    )
    val stops: List<StopEntity>
)

// TypeConverters para guardar la geometría
class Converters {
    @TypeConverter
    fun fromGeometryList(value: List<List<Double>>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toGeometryList(value: String): List<List<Double>> {
        val type = object : TypeToken<List<List<Double>>>() {}.type
        return Gson().fromJson(value, type) ?: emptyList()
    }
}