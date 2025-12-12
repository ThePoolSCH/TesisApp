package com.example.tesisapp.data.remote

import com.google.gson.annotations.SerializedName

// Petición genérica JSON-RPC
data class OdooJsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String = "call",
    val params: Map<String, Any> = emptyMap(),
    val id: Long = System.currentTimeMillis()
)

// Respuesta Wrapper de Odoo
data class OdooRouteResponse(
    val result: RouteResultDto?
)

data class RouteResultDto(
    val status: String,
    val data: RouteDataDto?
)

data class RouteDataDto(
    @SerializedName("route_id") val routeId: Int,
    @SerializedName("route_name") val routeName: String,
    val date: String,
    val geometry: List<List<Double>>, // OSRM devuelve arrays
    val stops: List<StopDto>
)

data class StopDto(
    val id: Int,
    val sequence: Int,
    @SerializedName("partner_name") val partnerName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("visit_state") val visitState: String,
    @SerializedName("total_sold") val totalSold: Double
)

data class OdooCheckInResponse(val result: CheckInResult?)
data class CheckInResult(val status: String, val message: String)