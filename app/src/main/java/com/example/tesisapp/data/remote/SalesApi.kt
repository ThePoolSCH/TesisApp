package com.example.tesisapp.data.remote

import com.example.tesisapp.data.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SalesApi {

    @POST("/api/products/catalog")
    suspend fun getCatalog(@Body body: OdooJsonRpcRequest = OdooJsonRpcRequest()): OdooRpcWrapper<List<ProductDto>>

    @POST("/api/campaigns/my_status")
    suspend fun getMyCampaigns(@Body body: OdooJsonRpcRequest = OdooJsonRpcRequest()): OdooRpcWrapper<List<CampaignDto>>

    @POST("/api/sales/create_order")
    suspend fun createOrder(@Body request: OdooJsonRpcRequest): OdooRpcWrapper<CreateOrderResultDto>
}