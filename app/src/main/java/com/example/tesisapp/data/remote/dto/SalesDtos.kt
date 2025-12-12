package com.example.tesisapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// Respuesta genérica de Odoo
data class OdooResponse<T>(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T?,
    @SerializedName("message") val message: String?
)

data class ProductDto(
    val id: Int,
    val name: String,
    val code: String,
    val price: Double,
    @SerializedName("uom_name") val uomName: String,
    @SerializedName("image_url") val imageUrl: String
)

data class CampaignDto(
    val id: Int,
    val name: String,
    val type: String,
    val metric: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    val targets: List<CampaignTargetDto>
)

data class CampaignTargetDto(
    @SerializedName("line_id") val lineId: Int,
    @SerializedName("product_id") val productId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("target_amount") val targetAmount: Double,
    @SerializedName("current_amount") val currentAmount: Double,
    @SerializedName("achievement_percent") val achievementPercent: Double,
    @SerializedName("image_url") val imageUrl: String
)

// DTO para enviar el pedido (Body del POST)
data class CreateOrderRequest(
    @SerializedName("route_line_id") val routeLineId: Int,
    @SerializedName("products") val products: List<ProductOrderDto>
)

data class ProductOrderDto(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("qty") val qty: Double
)

data class OdooRpcWrapper<T>(
    @SerializedName("jsonrpc") val jsonrpc: String,
    @SerializedName("result") val result: OdooResponse<T>? // Puede ser null si hay error
)