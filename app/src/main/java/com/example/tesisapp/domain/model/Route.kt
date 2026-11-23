package com.example.tesisapp.domain.model

data class Route(
    val id: Int,
    val name: String,
    val date: String,
    val geometry: List<List<Double>>, // Lista de coordenadas [[lon, lat], ...]
    val stops: List<RouteStop>,
    val checkinRadiusM: Double = 100.0
)

data class RouteStop(
    val id: Int,
    val sequence: Int,
    val partnerName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val visitState: String, // 'pending', 'arrived', 'done'
    val totalSold: Double,
    // F4 (mockup A1)
    val partnerId: Int = 0,
    val street: String = "",
    val plannedArrival: String = "",
    val visitResult: String = "",
    val visitClass: String = "",
    val clientType: String = "",
    val unplanned: Boolean = false,
    val checkinAt: Long = 0L,
    val outOfRange: Boolean = false
)
