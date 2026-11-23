package com.example.tesisapp.util

import com.example.tesisapp.domain.model.TaskAnswer
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.ui.screens.main.leerNumero
import com.example.tesisapp.ui.screens.tasks.TaskUiModel
import com.example.tesisapp.utils.Formato
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatoTest {
    @Test
    fun montos_en_soles() {
        assertEquals("S/ 9.30", Formato.pen(9.3))
        assertEquals("S/ 1,234.50", Formato.pen(1234.5))
        assertEquals("S/ 200", Formato.pen(200.0, 0))
    }

    @Test
    fun hora_utc_en_formato_de_odoo() {
        assertTrue(Formato.utcAhora().matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")))
    }

    @Test
    fun textos_de_resultado_y_tipo() {
        assertEquals("Pedido tomado", Formato.resultado("order"))
        assertEquals("Bodega", Formato.tipoCliente("bodega"))
        assertEquals("", Formato.tipoCliente("desconocido"))
        assertTrue(Formato.fechaCorta("2026-11-23").endsWith("23/11"))
    }
}

class TareasTest {
    private fun def(tipo: TaskType, clientType: String = "") = TaskDefinition(1, "¿?", tipo, true, clientType = clientType)

    @Test
    fun tipos_de_odoo() {
        assertEquals(TaskType.PHOTO, TaskType.from("photo"))
        assertEquals(TaskType.SELECTION, TaskType.from("selection"))
        assertEquals(TaskType.TEXT, TaskType.from("desconocido"))
    }

    @Test
    fun alcance_por_tipo_de_cliente() {
        assertTrue(def(TaskType.TEXT).aplicaA("restaurante"))                 // sin tipo = todas las visitas
        assertTrue(def(TaskType.NUMBER, "bodega").aplicaA("bodega"))
        assertFalse(def(TaskType.NUMBER, "bodega").aplicaA("restaurante"))
    }

    @Test
    fun cuando_una_tarea_esta_respondida() {
        assertTrue(TaskUiModel(def(TaskType.BOOLEAN), TaskAnswer(1, false)).isAnswered)  // "No" también es respuesta
        assertFalse(TaskUiModel(def(TaskType.BOOLEAN)).isAnswered)
        assertTrue(TaskUiModel(def(TaskType.NUMBER), TaskAnswer(1, 2.8)).isAnswered)
        assertFalse(TaskUiModel(def(TaskType.TEXT), TaskAnswer(1, "  ")).isAnswered)
        assertTrue(TaskUiModel(def(TaskType.PHOTO), TaskAnswer(1, "/data/fotos/a.jpg")).isAnswered)
    }

    @Test
    fun numeros_con_coma_o_punto() {
        assertEquals(2.8, leerNumero("2,80")!!, 0.0001)
        assertEquals(2.8, leerNumero("2.8")!!, 0.0001)
        assertNull(leerNumero("dos"))
    }
}
