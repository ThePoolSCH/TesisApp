package com.example.tesisapp.data.repository

import com.example.tesisapp.BuildConfig
import com.example.tesisapp.data.local.*
import com.example.tesisapp.data.remote.OdooJsonRpcRequest
import com.example.tesisapp.data.remote.SalesApi
import com.example.tesisapp.domain.model.*
import com.example.tesisapp.domain.repository.SalesRepository
import com.example.tesisapp.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.URI

class SalesRepositoryImpl(
    private val api: SalesApi,
    private val dao: SalesDao
) : SalesRepository {
    private val apiBaseUri = URI(BuildConfig.API_BASE_URL)
    private val apiHost = apiBaseUri.host?.lowercase().orEmpty()
    private val apiPort = if (apiBaseUri.port == -1) null else apiBaseUri.port
    private val apiOrigin = buildString {
        append(apiBaseUri.scheme)
        append("://")
        append(apiHost)
        if (apiPort != null) {
            append(":")
            append(apiPort)
        }
    }

    override suspend fun syncProducts(): Resource<Unit> {
        return try {
            val allProducts = mutableListOf<ProductEntity>()
            val pageSize = 200
            var offset = 0

            while (true) {
                val rpcResponse = api.getCatalog(
                    OdooJsonRpcRequest(
                        params = mapOf(
                            "limit" to pageSize,
                            "offset" to offset
                        )
                    )
                )
                val innerResponse = rpcResponse.result
                if (innerResponse == null || innerResponse.status != "success" || innerResponse.data == null) {
                    return Resource.Error(innerResponse?.message ?: "Error: Respuesta vacía de Odoo")
                }

                val batch = innerResponse.data.map { dto ->
                    ProductEntity(
                        dto.id,
                        dto.name,
                        dto.code,
                        dto.price,
                        dto.uomName,
                        resolveAssetUrl(dto.imageUrl)
                    )
                }
                allProducts.addAll(batch)
                if (batch.size < pageSize) {
                    break
                }
                offset += pageSize
            }

            dao.clearProducts()
            dao.insertProducts(allProducts)
            Resource.Success(Unit)
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
                            tDto.targetAmount,
                            tDto.currentAmount,
                            tDto.achievementPercent,
                            resolveAssetUrl(tDto.imageUrl)
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
            val payloadProducts = items.map {
                mapOf("product_id" to it.productId, "qty" to it.quantity)
            }
            val request = OdooJsonRpcRequest(
                params = mapOf(
                    "route_line_id" to routeLineId,
                    "products" to payloadProducts
                )
            )
            val rpcResponse = api.createOrder(request)
            val innerResponse = rpcResponse.result // <--- Desempaquetamos

            if (innerResponse != null && innerResponse.status == "success" && innerResponse.data != null) {
                Resource.Success(innerResponse.data.orderId)
            } else {
                Resource.Error(innerResponse?.message ?: "Error creando pedido")
            }
        } catch (e: Exception) {
            Resource.Error("Error de red: ${e.message}")
        }
    }

    private fun resolveAssetUrl(raw: String): String {
        if (raw.isBlank()) {
            return raw
        }
        if (raw.startsWith("/")) {
            return "$apiOrigin$raw"
        }
        return try {
            val uri = URI(raw)
            val host = uri.host?.lowercase().orEmpty()
            if (host.isBlank()) {
                "$apiOrigin/$raw"
            } else if (host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2" || host == "0.0.0.0") {
                val rebuilt = URI(
                    uri.scheme ?: apiBaseUri.scheme,
                    uri.userInfo,
                    apiHost,
                    if (uri.port == -1) apiPort ?: -1 else uri.port,
                    uri.path,
                    uri.query,
                    uri.fragment
                )
                rebuilt.toString()
            } else {
                raw
            }
        } catch (_: Exception) {
            "$apiOrigin/${raw.trimStart('/')}"
        }
    }
}