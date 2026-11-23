package com.example.tesisapp.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Base64
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.local.dao.ColaDao
import com.example.tesisapp.data.remote.OdooRpc
import com.example.tesisapp.data.remote.RpcResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Estado que se muestra en el chip de la barra superior y en la pantalla de sincronización. */
data class SyncStatus(val online: Boolean, val pendientes: Int, val errores: Int, val ultimaSync: Long) {
    val alDia get() = pendientes == 0 && errores == 0
}

/** Conectividad del teléfono como Flow (para el chip "Sin conexión"). */
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private fun ahora(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    val online: StateFlow<Boolean> = callbackFlow {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            // Al perderse la red por defecto, `activeNetwork` aún puede devolverla un instante: se marca sin conexión
            // y, si el sistema pasa a otra red, onAvailable vuelve a marcarla.
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(ahora())
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.distinctUntilChanged().stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Default), SharingStarted.Eagerly, ahora())
}

/**
 * Cola de operaciones (outbox). Las pantallas llaman a [encolar] y siguen trabajando; [procesar] (desde el
 * [SyncWorker]) envía lo pendiente en orden. Si el servidor rechaza una operación queda en "error" y se detienen
 * las siguientes de la misma visita (p. ej., no se cierra una visita cuya llegada fue rechazada); el vendedor
 * decide en la pantalla de sincronización si reintentar o descartar.
 */
@Singleton
class OutboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ColaDao,
    private val rpc: OdooRpc,
    network: NetworkMonitor
) {
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("sincronizacion", Context.MODE_PRIVATE)
    private val ultima = MutableStateFlow(prefs.getLong("ultima", 0L))

    val operaciones: Flow<List<PendingOpEntity>> = dao.observeAll()

    val estado: Flow<SyncStatus> = combine(dao.observeAll(), network.online, ultima) { ops, online, t ->
        SyncStatus(online, ops.count { it.status == PendingOpEntity.STATUS_PENDING },
            ops.count { it.status == PendingOpEntity.STATUS_ERROR }, t)
    }

    suspend fun encolar(type: String, routeLineId: Int, label: String, payload: Map<String, Any?>) {
        dao.insert(PendingOpEntity(type = type, routeLineId = routeLineId, label = label, payloadJson = gson.toJson(payload)))
        programar(context)
    }

    suspend fun reintentar(id: Long) { dao.reintentar(id); programar(context) }
    suspend fun reintentarTodas() { dao.reintentarTodas(); programar(context) }
    suspend fun descartar(id: Long) = dao.delete(id)
    fun sincronizarAhora() = programar(context)

    enum class Fin { LISTO, REINTENTAR }

    suspend fun procesar(): Fin {
        var hubo = true
        while (hubo) {
            hubo = false
            val bloqueadas = dao.conError().map { it.routeLineId }.toMutableSet()
            for (op in dao.pendientes()) {
                if (op.routeLineId in bloqueadas) continue
                when (val r = enviar(op)) {
                    is RpcResult.Ok -> { dao.delete(op.id); hubo = true }
                    is RpcResult.SinRed -> {
                        dao.update(op.copy(attempts = op.attempts + 1, lastError = r.message))
                        return Fin.REINTENTAR
                    }
                    RpcResult.SesionVencida -> {
                        dao.update(op.copy(status = PendingOpEntity.STATUS_ERROR, attempts = op.attempts + 1,
                            lastError = "La sesión venció: vuelve a iniciar sesión y toca Reintentar."))
                        return Fin.LISTO
                    }
                    is RpcResult.Rechazo -> {
                        dao.update(op.copy(status = PendingOpEntity.STATUS_ERROR, attempts = op.attempts + 1, lastError = r.message))
                        bloqueadas += op.routeLineId
                    }
                }
            }
        }
        val t = System.currentTimeMillis()
        prefs.edit().putLong("ultima", t).apply()
        ultima.value = t
        return Fin.LISTO
    }

    private suspend fun enviar(op: PendingOpEntity): RpcResult {
        // JsonParser conserva los enteros tal cual (Gson a Map los volvería 105.0)
        val payload = JsonParser.parseString(op.payloadJson).asJsonObject
        if (op.type == PendingOpEntity.TASKS) conFotos(payload)
        val params: Map<String, Any?> = payload.entrySet().associate { it.key to it.value }
        return when (op.type) {
            PendingOpEntity.CHECKIN -> rpc.call("/api/rutas/checkin", params)
            PendingOpEntity.CHECKOUT -> rpc.call("/api/rutas/checkout", params)
            PendingOpEntity.ORDER -> rpc.call("/api/sales/create_order", params)
            PendingOpEntity.TASKS -> rpc.call("/api/tasks/submit", params)
            else -> RpcResult.Rechazo("Operación desconocida: ${op.type}")
        }
    }

    /** Las fotos viajan en la cola como ruta de archivo; se leen y codifican recién al enviarlas. */
    private fun conFotos(payload: JsonObject) {
        payload.getAsJsonArray("answers")?.forEach { e ->
            val a = e.asJsonObject
            val ruta = a.remove("photo_path")?.asString ?: return@forEach
            val f = File(ruta)
            a.addProperty("value", if (f.exists()) Base64.encodeToString(f.readBytes(), Base64.NO_WRAP) else "")
        }
    }

    companion object {
        private const val TRABAJO = "cola_sincronizacion"

        fun programar(context: Context) {
            val req = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(TRABAJO, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outbox: OutboxRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (outbox.procesar()) {
        OutboxRepository.Fin.LISTO -> Result.success()
        OutboxRepository.Fin.REINTENTAR -> Result.retry()
    }
}
