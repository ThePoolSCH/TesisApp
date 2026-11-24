package com.example.tesisapp.repository

import com.example.tesisapp.data.repository.parsearSugerido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SugeridoParserTest {

    /** Respuesta real de /api/sales/suggested (Odoo local, User99, 2026-11-24) con sugerencias del modelo. */
    private val respuestaMl = javaClass.classLoader!!.getResource("sugerido_ml.json")!!.readText()

    @Test
    fun lee_la_sugerencia_del_modelo() {
        val s = parsearSugerido(respuestaMl, 603.81)
        assertEquals("ml", s.method)
        assertEquals(475.28, s.expectedTicket!!, 0.001)
        assertEquals(7, s.items.size)
        assertEquals("Divas", s.items[0].name)
        assertEquals(33, s.items[0].qty)
        assertEquals(0.706, s.items[0].probability!!, 0.0001)
    }

    @Test
    fun acepta_el_formato_anterior_a_la_f5() {
        val viejo = """[{"product_id": 3, "name": "Arroz", "qty": 2, "reason": "Compra en 6 de sus últimos 8 pedidos", "campaign": true}]"""
        val s = parsearSugerido(viejo, 120.0)
        assertEquals("regla", s.method)
        assertNull(s.expectedTicket)
        assertNull(s.items[0].probability)
        assertEquals(true, s.items[0].campaign)
    }
}
