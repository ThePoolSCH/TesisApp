package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.CacheEntity
import com.example.tesisapp.data.local.RouteDao
import com.example.tesisapp.data.local.StopEntity
import com.example.tesisapp.data.local.dao.ColaDao
import com.example.tesisapp.data.remote.OdooRpc
import com.example.tesisapp.data.remote.RpcResult
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ---------- Modelos (vienen tal cual de la API; Gson los llena) ----------
data class ClienteResumen(
    val id: Int, val name: String, val street: String = "",
    @SerializedName("client_type") val clientType: String = "", @SerializedName("visit_class") val visitClass: String = "",
    val latitude: Double = 0.0, val longitude: Double = 0.0,
    @SerializedName("last_visit") val lastVisit: String = "", @SerializedName("next_visit") val nextVisit: String = "",
    @SerializedName("in_today_route") val inTodayRoute: Boolean = false
)

data class VentaMes(val month: String, val amount: Double)
data class ProductoHabitual(@SerializedName("product_id") val productId: Int, val name: String, val qty: Int, val times: Int,
                            val orders: Int, val reason: String)
data class VisitaPasada(val date: String, val result: String, val total: Double, val unplanned: Boolean)

data class FichaCliente(
    @SerializedName("partner_id") val partnerId: Int, val name: String, val street: String = "", val phone: String = "",
    @SerializedName("client_type") val clientType: String = "", @SerializedName("visit_class") val visitClass: String = "",
    @SerializedName("visit_frequency") val visitFrequency: Int = 0, @SerializedName("last_visit") val lastVisit: String = "",
    @SerializedName("orders_count") val ordersCount: Int = 0, @SerializedName("avg_ticket") val avgTicket: Double = 0.0,
    @SerializedName("last_order") val lastOrder: String = "",
    val months: List<VentaMes> = emptyList(),
    @SerializedName("usual_products") val usualProducts: List<ProductoHabitual> = emptyList(),
    val visits: List<VisitaPasada> = emptyList()
)

data class Tramo(val min: Double, val pay: Double)
data class ReglaComision(val name: String, val type: String, val tiers: List<Tramo> = emptyList(),
                         @SerializedName("cap_pct") val capPct: Double = 0.0, @SerializedName("floor_pct") val floorPct: Double = 0.0)
data class CampanaComision(
    @SerializedName("campaign_id") val campaignId: Int, val name: String,
    @SerializedName("start_date") val startDate: String, @SerializedName("end_date") val endDate: String,
    @SerializedName("days_left") val daysLeft: Int, val target: Double, val achieved: Double,
    @SerializedName("achievement_percent") val achievementPercent: Double,
    @SerializedName("projection_percent") val projectionPercent: Double,
    @SerializedName("base_bonus") val baseBonus: Double, val rule: ReglaComision?,
    @SerializedName("estimated_payout_pct") val estimatedPayoutPct: Double,
    @SerializedName("estimated_amount") val estimatedAmount: Double
)
data class Liquidacion(val settlement: String, val date: String, val state: String, val amount: Double, val details: List<String>)
data class MisComisiones(val campaigns: List<CampanaComision> = emptyList(), val settlements: List<Liquidacion> = emptyList(),
                         val currency: String = "PEN")

data class MesPerfil(
    val start: String = "", @SerializedName("visits_planned") val visitsPlanned: Int = 0,
    @SerializedName("visits_done") val visitsDone: Int = 0, @SerializedName("visits_with_order") val visitsWithOrder: Int = 0,
    val orders: Int = 0, val sales: Double = 0.0
)
data class Perfil(
    val name: String = "", val login: String = "", val email: String = "", val phone: String = "",
    val company: String = "", val currency: String = "PEN", val district: String = "", val region: String = "",
    val supervisor: String = "", @SerializedName("supervisor_phone") val supervisorPhone: String = "",
    @SerializedName("daily_capacity") val dailyCapacity: Int = 0, val clients: Int = 0, val month: MesPerfil = MesPerfil()
)

/** Resultado de las llamadas que necesitan conexión. */
sealed class Online<out T> {
    data class Ok<T>(val value: T, val message: String? = null) : Online<T>()
    data class Error(val message: String) : Online<Nothing>()
}

/**
 * F4 · mockups A3 y A5: cartera, ficha del cliente, visita no planificada y "Mis objetivos".
 * La cartera y la ficha piden conexión; "Mis objetivos" queda guardado para verlo sin señal.
 */
@Singleton
class ClientesRepository @Inject constructor(
    private val rpc: OdooRpc,
    private val routeDao: RouteDao,
    private val cola: ColaDao
) {
    private val gson = Gson()

    private fun mensaje(r: RpcResult): String = when (r) {
        is RpcResult.Rechazo -> r.message
        is RpcResult.SinRed -> "Necesitas conexión para esto."
        RpcResult.SesionVencida -> "Tu sesión venció: vuelve a iniciar sesión."
        is RpcResult.Ok -> ""
    }

    private inline fun <reified T> leer(data: JsonElement?): T = gson.fromJson(data, object : TypeToken<T>() {}.type)

    suspend fun cartera(): Online<List<ClienteResumen>> = when (val r = rpc.call("/api/clients/portfolio")) {
        is RpcResult.Ok -> Online.Ok(leer(r.data))
        else -> Online.Error(mensaje(r))
    }

    suspend fun ficha(partnerId: Int): Online<FichaCliente> =
        when (val r = rpc.call("/api/clients/history", mapOf("partner_id" to partnerId))) {
            is RpcResult.Ok -> Online.Ok(leer(r.data))
            else -> Online.Error(mensaje(r))
        }

    /** Agrega el cliente a la ruta de hoy (requiere conexión: el servidor crea la parada). */
    suspend fun visitaNoPlanificada(partnerId: Int, motivo: String): Online<Int> {
        val r = rpc.call("/api/rutas/add_stop", mapOf("partner_id" to partnerId, "reason" to motivo))
        if (r !is RpcResult.Ok) return Online.Error(mensaje(r))
        val o = r.data!!.asJsonObject
        val ruta = routeDao.getStops().firstOrNull()?.routeId ?: 0
        routeDao.insertStop(StopEntity(
            id = o.get("id").asInt, routeId = ruta, sequence = o.get("sequence").asInt,
            partnerName = o.get("partner_name").asString, address = o.get("address").asString,
            latitude = o.get("latitude").asDouble, longitude = o.get("longitude").asDouble,
            visitState = o.get("visit_state").asString, totalSold = o.get("total_sold").asDouble,
            partnerId = o.get("partner_id").asInt, street = o.get("street").asString,
            plannedArrival = o.get("planned_arrival").asString, visitResult = o.get("visit_result").asString,
            visitClass = o.get("visit_class").asString, clientType = o.get("client_type").asString, unplanned = true
        ))
        return Online.Ok(o.get("id").asInt, r.message)
    }

    // ---------- Mis objetivos y comisión (A5) ----------
    val comisiones: Flow<Pair<MisComisiones?, Long>> = cola.observeCache(CLAVE_COMISIONES).map { c ->
        (c?.let { gson.fromJson(it.json, MisComisiones::class.java) }) to (c?.updatedAt ?: 0L)
    }

    suspend fun actualizarComisiones(): String? = when (val r = rpc.call("/api/commissions/my_status")) {
        is RpcResult.Ok -> { cola.putCache(CacheEntity(CLAVE_COMISIONES, r.data.toString())); null }
        else -> mensaje(r)
    }

    // ---------- Perfil (D4) ----------
    val perfil: Flow<Pair<Perfil?, Long>> = cola.observeCache(CLAVE_PERFIL).map { c ->
        (c?.let { gson.fromJson(it.json, Perfil::class.java) }) to (c?.updatedAt ?: 0L)
    }

    suspend fun actualizarPerfil(): String? = when (val r = rpc.call("/api/me")) {
        is RpcResult.Ok -> { cola.putCache(CacheEntity(CLAVE_PERFIL, r.data.toString())); null }
        else -> mensaje(r)
    }

    companion object {
        private const val CLAVE_COMISIONES = "mis_comisiones"
        private const val CLAVE_PERFIL = "mi_perfil"
    }
}
