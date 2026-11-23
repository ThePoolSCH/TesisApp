package com.example.tesisapp.data.remote

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Endpoint JSON-RPC genérico: los endpoints nuevos de la F4 se llaman por ruta y se leen como JSON. */
interface OdooRpcService {
    @POST
    suspend fun call(@Url path: String, @Body body: OdooJsonRpcRequest): retrofit2.Response<JsonObject>
}

/** Resultado de una llamada a la API propia (`{status, message, data}` dentro de `result`). */
sealed class RpcResult {
    data class Ok(val data: JsonElement?, val message: String?, val status: String = "success") : RpcResult()
    /** El servidor respondió y rechazó la operación (regla de negocio): reintentar no sirve. */
    data class Rechazo(val message: String, val status: String = "error", val data: JsonElement? = null) : RpcResult()
    /** Sin conexión, servidor caído o error 5xx: se puede reintentar más tarde. */
    data class SinRed(val message: String) : RpcResult()
    /** La sesión de Odoo venció: hay que volver a iniciar sesión. */
    object SesionVencida : RpcResult()
}

@Singleton
class OdooRpc @Inject constructor(private val service: OdooRpcService) {

    suspend fun call(path: String, params: Map<String, Any?> = emptyMap()): RpcResult {
        val limpio = params.filterValues { it != null }.mapValues { it.value!! }
        val resp = try {
            service.call(path, OdooJsonRpcRequest(params = limpio))
        } catch (e: IOException) {
            return RpcResult.SinRed(e.message ?: "Sin conexión")
        } catch (e: Exception) {
            return RpcResult.SinRed(e.message ?: "Error de red")
        }
        if (resp.code() >= 500 || resp.code() == 408 || resp.code() == 429) return RpcResult.SinRed("Servidor no disponible (${resp.code()})")
        if (!resp.isSuccessful) return RpcResult.Rechazo("Error HTTP ${resp.code()}")
        val body = resp.body() ?: return RpcResult.SinRed("Respuesta vacía")
        if (body.has("error")) {
            val err = body.getAsJsonObject("error")
            val nombre = err.getAsJsonObject("data")?.get("name")?.asString.orEmpty()
            if (err.get("code")?.asInt == 100 || nombre.contains("SessionExpired")) return RpcResult.SesionVencida
            val msg = err.getAsJsonObject("data")?.get("message")?.asString ?: err.get("message")?.asString ?: "Error del servidor"
            return RpcResult.Rechazo(msg)
        }
        val result = body.get("result")
        if (result == null || !result.isJsonObject) return RpcResult.Ok(result, null)
        val r = result.asJsonObject
        val status = r.get("status")?.takeIf { !it.isJsonNull }?.asString ?: "success"
        val message = r.get("message")?.takeIf { !it.isJsonNull }?.asString
        return if (status == "success") RpcResult.Ok(r.get("data"), message)
        else RpcResult.Rechazo(message ?: "Operación rechazada", status, r.get("data"))
    }
}
