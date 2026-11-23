package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.StopEntity

private val ORDEN_ESTADO = mapOf("pending" to 0, "arrived" to 1, "done" to 2)

/**
 * Une las paradas que llegan de Odoo con las del teléfono. Lo hecho sin conexión y aún no enviado no se pierde:
 * si la parada local está más avanzada (pendiente < en visita < visitada) que en el servidor, se conserva su estado,
 * su resultado y su hora de llegada. La hora de llegada local (cronómetro) se mantiene siempre.
 */
fun fusionarParadas(locales: Map<Int, StopEntity>, servidor: List<StopEntity>): List<StopEntity> = servidor.map { s ->
    val local = locales[s.id]
    when {
        local == null -> s
        (ORDEN_ESTADO[local.visitState] ?: 0) > (ORDEN_ESTADO[s.visitState] ?: 0) -> s.copy(
            visitState = local.visitState, visitResult = local.visitResult.ifBlank { s.visitResult },
            checkinAt = local.checkinAt, outOfRange = local.outOfRange,
            totalSold = maxOf(local.totalSold, s.totalSold))
        else -> s.copy(checkinAt = local.checkinAt, outOfRange = local.outOfRange || s.outOfRange)
    }
}
