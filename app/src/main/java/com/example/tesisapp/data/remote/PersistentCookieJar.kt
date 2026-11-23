package com.example.tesisapp.data.remote

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Guarda la cookie de sesión de Odoo en el teléfono (antes vivía solo en memoria): la cola sin conexión
 * puede enviar lo pendiente aunque Android haya cerrado la app, sin pedir de nuevo la contraseña.
 */
class PersistentCookieJar(context: Context) : CookieJar {
    private val prefs = context.getSharedPreferences("sesion_odoo", Context.MODE_PRIVATE)
    private val memoria = mutableMapOf<String, Cookie>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cargar(url)
        cookies.forEach { memoria[it.name] = it }
        prefs.edit().putStringSet(url.host, memoria.values.filter { it.persistent || it.name == "session_id" }
            .map { it.toString() }.toSet()).apply()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        cargar(url)
        val ahora = System.currentTimeMillis()
        return memoria.values.filter { it.expiresAt > ahora && it.matches(url) }
    }

    private fun cargar(url: HttpUrl) {
        if (memoria.isNotEmpty()) return
        prefs.getStringSet(url.host, emptySet())!!.mapNotNull { Cookie.parse(url, it) }.forEach { memoria[it.name] = it }
    }

    @Synchronized
    fun clear() {
        memoria.clear()
        prefs.edit().clear().apply()
    }
}
