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
import java.util.UUID
import com.example.tesisapp.data.local.dao.ColaDao
import com.example.tesisapp.data.remote.OdooRpc
import com.example.tesisapp.data.remote.RpcResult
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.utils.Formato
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.URI

class SalesRepositoryImpl(
    private val api: SalesApi,
    private val dao: SalesDao,
    private val cola: ColaDao,
    private val rpc: OdooRpc,
    private val outbox: OutboxRepository
) : SalesRepository {
    private val gson = Gson()
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
                        resolveAssetUrl(dto.imageUrl),
                        dto.category.orEmpty()
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
            entities.map { Product(it.id, it.name, it.code, it.price, it.uomName, it.imageUrl, it.category) }
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

    override suspend fun createOrder(routeLineId: Int, partnerName: String, items: List<OrderItem>, total: Double) {
        val payload = mapOf(
            "route_line_id" to routeLineId,
            "products" to items.map { mapOf("product_id" to it.productId, "qty" to it.quantity) },
            "client_uuid" to UUID.randomUUID().toString(), // el servidor no lo duplica si la cola lo reenvía
            "timestamp" to Formato.utcAhora()
        )
        outbox.encolar(PendingOpEntity.ORDER, routeLineId, "Pedido ${Formato.pen(total)} · $partnerName", payload)
    }

    override suspend fun syncSuggested(partnerIds: List<Int>): Resource<Unit> {
        if (partnerIds.isEmpty()) return Resource.Success(Unit)
        return when (val r = rpc.call("/api/sales/suggested", mapOf("partner_ids" to partnerIds))) {
            is RpcResult.Ok -> {
                val filas = r.data?.takeIf { it.isJsonArray }?.asJsonArray ?: return Resource.Success(Unit)
                cola.upsertSuggested(filas.map { e ->
                    val o = e.asJsonObject
                    SuggestedEntity(o.get("partner_id").asInt, o.get("items").toString(), o.get("avg_ticket")?.asDouble ?: 0.0)
                })
                Resource.Success(Unit)
            }
            is RpcResult.Rechazo -> Resource.Error(r.message)
            is RpcResult.SinRed -> Resource.Error("Sin conexión")
            RpcResult.SesionVencida -> Resource.Error("Sesión vencida")
        }
    }

    override suspend fun getSuggested(partnerId: Int): SuggestedOrder? {
        val e = cola.suggested(partnerId) ?: return null
        val items = JsonParser.parseString(e.itemsJson).asJsonArray.map { x ->
            val o = x.asJsonObject
            SuggestedItem(o.get("product_id").asInt, o.get("name").asString, o.get("qty").asInt,
                o.get("reason").asString, o.get("campaign")?.asBoolean ?: false)
        }
        return SuggestedOrder(items, e.avgTicket)
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