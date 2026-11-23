package com.example.tesisapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * F4 · cola de operaciones hechas en el teléfono (patrón outbox). Cada acción del vendedor se guarda aquí primero
 * y el `SyncWorker` la envía a Odoo cuando hay conexión, en el orden en que ocurrió.
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // CHECKIN, CHECKOUT, TASKS, ORDER
    val routeLineId: Int,
    val label: String,         // texto para la pantalla de sincronización ("Pedido · Bodega 003929")
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val attempts: Int = 0,
    val lastError: String? = null
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ERROR = "error"
        const val CHECKIN = "CHECKIN"; const val CHECKOUT = "CHECKOUT"; const val TASKS = "TASKS"; const val ORDER = "ORDER"
    }
}

/** Respuestas de tareas en edición o ya enviadas a la cola, por visita (sobreviven a cerrar la app). */
@Entity(tableName = "task_drafts", primaryKeys = ["routeLineId", "definitionId"])
data class TaskDraftEntity(
    val routeLineId: Int,
    val definitionId: Int,
    val valueJson: String,     // true / "texto" / 2.8 / "Bajo" / ruta del archivo de la foto
    val photoTakenAt: String? = null,
    val photoLat: Double? = null,
    val photoLon: Double? = null,
    val photoKb: Int? = null,
    val sent: Boolean = false
)

/** Pedido sugerido y ticket medio por cliente (se descarga junto con la ruta del día). */
@Entity(tableName = "suggested_orders")
data class SuggestedEntity(
    @PrimaryKey val partnerId: Int,
    val itemsJson: String,
    val avgTicket: Double
)

/** Respuestas de la API guardadas para verlas sin conexión (p. ej., "Mis objetivos"). */
@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long = System.currentTimeMillis()
)
