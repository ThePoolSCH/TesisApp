package com.example.tesisapp.data.repository

import com.example.tesisapp.domain.model.SuggestedItem
import com.example.tesisapp.domain.model.SuggestedOrder
import com.google.gson.JsonParser

/**
 * Lee el pedido sugerido guardado en Room. Desde la F5 se guarda el objeto completo de `/api/sales/suggested`
 * (ítems, `method` y `expected_ticket`); antes solo la lista de ítems, que también se acepta.
 */
fun parsearSugerido(json: String, avgTicket: Double): SuggestedOrder {
    val raiz = JsonParser.parseString(json)
    val obj = if (raiz.isJsonObject) raiz.asJsonObject else null
    val arr = obj?.getAsJsonArray("items") ?: raiz.asJsonArray
    val items = arr.map { x ->
        val o = x.asJsonObject
        SuggestedItem(o.get("product_id").asInt, o.get("name").asString, o.get("qty").asInt,
            o.get("reason")?.asString.orEmpty(), o.get("campaign")?.asBoolean ?: false,
            o.get("probability")?.takeIf { !it.isJsonNull }?.asDouble)
    }
    return SuggestedOrder(items, avgTicket,
        method = obj?.get("method")?.takeIf { !it.isJsonNull }?.asString ?: "regla",
        expectedTicket = obj?.get("expected_ticket")?.takeIf { !it.isJsonNull }?.asDouble)
}
