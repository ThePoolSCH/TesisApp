package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.*
import com.example.tesisapp.data.remote.SalesApi
import com.example.tesisapp.data.remote.dto.CreateOrderRequest
import com.example.tesisapp.data.remote.dto.ProductOrderDto
import com.example.tesisapp.domain.model.*
import com.example.tesisapp.domain.repository.SalesRepository
import com.example.tesisapp.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class SalesRepositoryImpl(
    private val api: SalesApi,
    private val dao: SalesDao
) : SalesRepository {

    override suspend fun syncProducts(): Resource<Unit> {
        return try {
            // 1. Llamada a API
            val rpcResponse = api.getCatalog()

            // 2. Desempaquetar 'result'
            val innerResponse = rpcResponse.result

            if (innerResponse != null && innerResponse.status == "success" && innerResponse.data != null) {
                val entities = innerResponse.data.map { dto ->
                    ProductEntity(dto.id, dto.name, dto.code, dto.price, dto.uomName, dto.imageUrl)
                }
                dao.clearProducts()
                dao.insertProducts(entities)
                Resource.Success(Unit)
            } else {
                Resource.Error(innerResponse?.message ?: "Error: Respuesta vacía de Odoo")
            }
        } catch (e: IOException) {
            Resource.Error("Error de conexión: ${e.message}")
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    override suspend fun syncCampaigns(): Resource<Unit> {
        return try {
            val rpcResponse = api.getMyCampaigns()
            val innerResponse = rpcResponse.result // <--- Desempaquetamos

            if (innerResponse != null && innerResponse.status == "success" && innerResponse.data != null) {
                val campaigns = mutableListOf<CampaignEntity>()
                val targets = mutableListOf<CampaignTargetEntity>()

                innerResponse.data.forEach { cDto ->
                    campaigns.add(CampaignEntity(
                        cDto.id, cDto.name, cDto.type, cDto.metric ?: "", cDto.startDate, cDto.endDate
                    ))
                    cDto.targets.forEach { tDto ->
                        targets.add(CampaignTargetEntity(
                            tDto.lineId, cDto.id, tDto.productId, tDto.productName,
                            tDto.targetAmount, tDto.currentAmount, tDto.achievementPercent, tDto.imageUrl
                        ))
                    }
                }
                dao.replaceCampaigns(campaigns, targets)
                Resource.Success(Unit)
            } else {
                Resource.Error(innerResponse?.message ?: "Error sync campañas")
            }
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }

    // ... getProducts y getCampaigns (Flows) QUEDAN IGUAL ...
    override fun getProducts(): Flow<List<Product>> {
        return dao.getAllProducts().map { entities ->
            entities.map { Product(it.id, it.name, it.code, it.price, it.uomName, it.imageUrl) }
        }
    }

    override fun getCampaigns(): Flow<List<Campaign>> {
        return dao.getCampaignsWithTargets().map { relations ->
            relations.map { rel ->
                Campaign(
                    id = rel.campaign.id,
                    name = rel.campaign.name,
                    type = rel.campaign.type,
                    metric = rel.campaign.metric,
                    startDate = rel.campaign.startDate,
                    endDate = rel.campaign.endDate,
                    targets = rel.targets.map { t ->
                        CampaignTarget(
                            t.lineId, t.productId, t.productName,
                            t.targetAmount, t.currentAmount, t.achievementPercent, t.imageUrl
                        )
                    }
                )
            }
        }
    }

    override suspend fun createOrder(routeLineId: Int, items: List<OrderItem>): Resource<Int> {
        return try {
            val dtos = items.map { ProductOrderDto(it.productId, it.quantity) }
            val request = CreateOrderRequest(routeLineId, dtos)

            val rpcResponse = api.createOrder(request)
            val innerResponse = rpcResponse.result // <--- Desempaquetamos

            if (innerResponse != null && innerResponse.status == "success") {
                Resource.Success(1)
            } else {
                Resource.Error(innerResponse?.message ?: "Error creando pedido")
            }
        } catch (e: Exception) {
            Resource.Error("Error de red: ${e.message}")
        }
    }
}