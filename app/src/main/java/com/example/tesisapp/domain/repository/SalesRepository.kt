package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Campaign
import com.example.tesisapp.domain.model.OrderItem
import com.example.tesisapp.domain.model.Product  // <--- ¡ESTE IMPORT ES EL QUE FALTA!
import kotlinx.coroutines.flow.Flow
import com.example.tesisapp.utils.Resource // Asumo que tienes una clase Resource/Result

interface SalesRepository {

    // Sincronización (API -> Local DB)
    suspend fun syncProducts(): Resource<Unit>
    suspend fun syncCampaigns(): Resource<Unit>

    // Lectura (Local DB -> UI)
    fun getProducts(): Flow<List<Product>>
    fun getCampaigns(): Flow<List<Campaign>>

    // Acción (UI -> API)
    suspend fun createOrder(routeLineId: Int, items: List<OrderItem>): Resource<Int> // Retorna OrderID
}