package com.example.tesisapp.sync

import com.example.tesisapp.data.remote.OdooJsonRpcRequest
import com.example.tesisapp.data.remote.OdooRpc
import com.example.tesisapp.data.remote.OdooRpcService
import com.example.tesisapp.data.remote.RpcResult
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** Servicio falso: devuelve una respuesta fija o lanza una excepción. */
private class ServicioFijo(private val r: () -> Response<JsonObject>) : OdooRpcService {
    var ultimo: OdooJsonRpcRequest? = null
    override suspend fun call(path: String, body: OdooJsonRpcRequest): Response<JsonObject> { ultimo = body; return r() }
}

private fun json(s: String) = JsonParser.parseString(s).asJsonObject

class OdooRpcTest {

    @Test
    fun exito_devuelve_data_y_mensaje() = runTest {
        val r = OdooRpc(ServicioFijo { Response.success(json("""{"result":{"status":"success","message":"ok","data":{"id":5}}}""")) })
            .call("/api/x")
        assertTrue(r is RpcResult.Ok)
        assertEquals(5, (r as RpcResult.Ok).data!!.asJsonObject.get("id").asInt)
        assertEquals("ok", r.message)
    }

    @Test
    fun rechazo_de_negocio_no_se_reintenta() = runTest {
        val r = OdooRpc(ServicioFijo { Response.success(json("""{"result":{"status":"error","message":"Visita no encontrada"}}""")) })
            .call("/api/x")
        assertEquals(RpcResult.Rechazo("Visita no encontrada", "error", null), r)
    }

    @Test
    fun out_of_range_llega_como_rechazo_con_su_estado() = runTest {
        val r = OdooRpc(ServicioFijo {
            Response.success(json("""{"result":{"status":"out_of_range","message":"Estás a 293 m","data":{"distance_m":293}}}"""))
        }).call("/api/rutas/checkin")
        assertEquals("out_of_range", (r as RpcResult.Rechazo).status)
    }

    @Test
    fun sesion_vencida_se_detecta() = runTest {
        val r = OdooRpc(ServicioFijo {
            Response.success(json("""{"error":{"code":100,"message":"Odoo Session Expired","data":{"name":"odoo.http.SessionExpiredException"}}}"""))
        }).call("/api/x")
        assertEquals(RpcResult.SesionVencida, r)
    }

    @Test
    fun sin_red_y_errores_5xx_se_reintentan() = runTest {
        val caido = OdooRpc(ServicioFijo { throw IOException("Unable to resolve host") }).call("/api/x")
        assertTrue(caido is RpcResult.SinRed)
        val error502 = OdooRpc(ServicioFijo { Response.error(502, "".toResponseBody()) }).call("/api/x")
        assertTrue(error502 is RpcResult.SinRed)
    }

    @Test
    fun parametros_nulos_no_se_envian() = runTest {
        val servicio = ServicioFijo { Response.success(json("""{"result":{"status":"success"}}""")) }
        OdooRpc(servicio).call("/api/x", mapOf("route_line_id" to 7, "note" to null))
        assertEquals(mapOf("route_line_id" to 7), servicio.ultimo!!.params)
    }
}
