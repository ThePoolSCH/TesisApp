package com.example.tesisapp.repository

import com.example.tesisapp.data.local.StopEntity
import com.example.tesisapp.data.repository.fusionarParadas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun parada(id: Int, estado: String, resultado: String = "", llegada: Long = 0L, vendido: Double = 0.0, fuera: Boolean = false) =
    StopEntity(id = id, routeId = 1, sequence = id, partnerName = "Cliente $id", address = "", latitude = -12.0,
        longitude = -77.0, visitState = estado, totalSold = vendido, visitResult = resultado, checkinAt = llegada, outOfRange = fuera)

class FusionParadasTest {

    @Test
    fun conserva_lo_hecho_sin_conexion() {
        // en el teléfono la visita ya se cerró con pedido; Odoo aún no recibió la cola
        val local = parada(1, "done", "order", llegada = 1000L, vendido = 9.3, fuera = true)
        val r = fusionarParadas(mapOf(1 to local), listOf(parada(1, "pending")))
        assertEquals("done", r[0].visitState)
        assertEquals("order", r[0].visitResult)
        assertEquals(1000L, r[0].checkinAt)
        assertEquals(9.3, r[0].totalSold, 0.001)
        assertTrue(r[0].outOfRange)
    }

    @Test
    fun el_servidor_gana_si_esta_igual_o_mas_avanzado() {
        val local = parada(1, "arrived", llegada = 1000L, vendido = 9.3)
        val r = fusionarParadas(mapOf(1 to local), listOf(parada(1, "done", "stock", vendido = 9.3)))
        assertEquals("done", r[0].visitState)
        assertEquals("stock", r[0].visitResult)
        assertEquals(1000L, r[0].checkinAt)                    // el cronómetro local no se pierde
    }

    @Test
    fun paradas_nuevas_y_eliminadas_siguen_al_servidor() {
        val r = fusionarParadas(mapOf(9 to parada(9, "arrived")), listOf(parada(1, "pending"), parada(2, "pending")))
        assertEquals(listOf(1, 2), r.map { it.id })            // la 9 ya no está en la ruta
        assertEquals(listOf("pending", "pending"), r.map { it.visitState })
    }
}
