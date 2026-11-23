package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Campaign
import com.example.tesisapp.domain.model.OrderItem
import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.model.SuggestedOrder
import com.example.tesisapp.utils.Resource
import kotlinx.coroutines.flow.Flow

interface SalesRepository {

    // Sincronización (API -> Local DB)
    suspend fun syncProducts(): Resource<Unit>
    suspend fun syncCampaigns(): Resource<Unit>

    /** F4: descarga el pedido sugerido de los clientes de la ruta del día. */
    suspend fun syncSuggested(partnerIds: List<Int>): Resource<Unit>

    // Lectura (Local DB -> UI)
    fun getProducts(): Flow<List<Product>>
    fun getCampaigns(): Flow<List<Campaign>>
    suspend fun getSuggested(partnerId: Int): SuggestedOrder?

    /** F4: el pedido se guarda en la cola y se envía cuando hay conexión (sin duplicarse al reintentar). */
    suspend fun createOrder(routeLineId: Int, partnerName: String, items: List<OrderItem>, total: Double)
}
