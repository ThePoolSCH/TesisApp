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
    val geometryJson: String // Guardaremos el JSON como string en la BD
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
    val totalSold: Double
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