package com.example.tesisapp.domain.model

data class Product(
    val id: Int,
    val name: String,
    val code: String,
    val price: Double,
    val uomName: String,
    val imageUrl: String
)

data class Campaign(
    val id: Int,
    val name: String,
    val type: String, // 'gtm' o 'cross'
    val metric: String,
    val startDate: String,
    val endDate: String,
    val targets: List<CampaignTarget>
)

data class CampaignTarget(
    val lineId: Int,
    val productId: Int,
    val productName: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val achievementPercent: Double,
    val imageUrl: String
)

// Modelo para enviar el pedido
data class OrderItem(
    val productId: Int,
    val quantity: Double
)