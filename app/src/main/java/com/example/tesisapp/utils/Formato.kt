package com.example.tesisapp.utils

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatos de la app (es-PE). Reemplaza el "$" fijo que tenía el pedido. */
object Formato {
    private val locale = Locale.forLanguageTag("es-PE")

    fun pen(monto: Double, decimales: Int = 2): String {
        val nf = NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = decimales; maximumFractionDigits = decimales
        }
        return "S/ " + nf.format(monto)
    }

    fun numero(valor: Double, decimales: Int = 0): String = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimales; maximumFractionDigits = decimales
    }.format(valor)

    fun pct(valor: Double, decimales: Int = 0): String = numero(valor, decimales) + " %"

    /** "2026-11-23" → "lun 23/11" */
    fun fechaCorta(iso: String): String = try {
        LocalDate.parse(iso.take(10)).format(DateTimeFormatter.ofPattern("EEE dd/MM", locale))
    } catch (_: Exception) { iso }

    /** Hora UTC "yyyy-MM-dd HH:mm:ss" (formato de Odoo) para las operaciones de la cola. */
    fun utcAhora(): String = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    fun tipoCliente(t: String): String = when (t) {
        "bodega" -> "Bodega"; "minimarket" -> "Minimarket"; "restaurante" -> "Restaurante"; "mayorista" -> "Mayorista"
        else -> ""
    }

    fun resultado(r: String): String = when (r) {
        "order" -> "Pedido tomado"; "stock" -> "Sin pedido: tiene stock"; "closed" -> "Local cerrado"
        "absent" -> "Encargado ausente"; "other" -> "Otro motivo"; else -> ""
    }
}
