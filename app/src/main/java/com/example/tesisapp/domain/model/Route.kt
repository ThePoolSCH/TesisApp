package com.example.tesisapp.domain.model

data class Route(
    val id: Int,
    val name: String,
    val date: String,
    val geometry: List<List<Double>>, // Lista de coordenadas [[lon, lat], ...]
    val stops: List<RouteStop>
)

data class RouteStop(
    val id: Int,
    val sequence: Int,
    val partnerName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val visitState: String, // 'pending', 'arrived', etc.
    val totalSold: Double
)