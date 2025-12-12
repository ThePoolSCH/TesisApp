package com.example.tesisapp.data.remote

import com.example.tesisapp.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SalesApi {

    @POST("/api/products/catalog")
    // CAMBIO: Ahora devolvemos OdooRpcWrapper<List<ProductDto>>
    suspend fun getCatalog(@Body body: Map<String, String> = emptyMap()): OdooRpcWrapper<List<ProductDto>>

    @POST("/api/campaigns/my_status")
    // CAMBIO: Aquí también
    suspend fun getMyCampaigns(@Body body: Map<String, String> = emptyMap()): OdooRpcWrapper<List<CampaignDto>>

    @POST("/api/sales/create_order")
    // CAMBIO: Aquí también
    suspend fun createOrder(@Body request: CreateOrderRequest): OdooRpcWrapper<Any>
}