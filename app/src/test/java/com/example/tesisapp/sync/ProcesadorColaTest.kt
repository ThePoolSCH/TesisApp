package com.example.tesisapp.sync

import com.example.tesisapp.data.local.CacheEntity
import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.local.PendingOpEntity.Companion.CHECKIN
import com.example.tesisapp.data.local.PendingOpEntity.Companion.CHECKOUT
import com.example.tesisapp.data.local.PendingOpEntity.Companion.ORDER
import com.example.tesisapp.data.local.SuggestedEntity
import com.example.tesisapp.data.local.TaskDraftEntity
import com.example.tesisapp.data.local.dao.ColaDao
import com.example.tesisapp.data.remote.RpcResult
import com.example.tesisapp.data.sync.Fin
import com.example.tesisapp.data.sync.ProcesadorCola
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** DAO en memoria: solo lo que usa el procesador de la cola. */
private class ColaEnMemoria(ops: List<PendingOpEntity>) : ColaDao {
    val ops = ops.toMutableList()
    override suspend fun insert(op: PendingOpEntity): Long { ops += op; return op.id }
    override fun observeAll(): Flow<List<PendingOpEntity>> = flowOf(ops)
    override suspend fun pendientes() = ops.filter { it.status == PendingOpEntity.STATUS_PENDING }.sortedBy { it.id }
    override suspend fun conError() = ops.filter { it.status == PendingOpEntity.STATUS_ERROR }
    override suspend fun deLaVisita(routeLineId: Int) = ops.filter { it.routeLineId == routeLineId }
    override suspend fun update(op: PendingOpEntity) { ops[ops.indexOfFirst { it.id == op.id }] = op }
    override suspend fun delete(id: Long) { ops.removeAll { it.id == id } }
    override suspend fun reintentar(id: Long) = TODO()
    override suspend fun reintentarTodas() = TODO()
    override fun observeDrafts(routeLineId: Int): Flow<List<TaskDraftEntity>> = TODO()
    override suspend fun drafts(routeLineId: Int): List<TaskDraftEntity> = TODO()
    override suspend fun upsertDraft(d: TaskDraftEntity) = TODO()
    override suspend fun deleteDraft(routeLineId: Int, definitionId: Int) = TODO()
    override suspend fun marcarEnviadas(routeLineId: Int) = TODO()
    override suspend fun upsertSuggested(items: List<SuggestedEntity>) = TODO()
    override suspend fun suggested(partnerId: Int): SuggestedEntity? = TODO()
    override suspend fun putCache(c: CacheEntity) = TODO()
    override fun observeCache(key: String): Flow<CacheEntity?> = TODO()
}

private fun op(id: Long, tipo: String, visita: Int) =
    PendingOpEntity(id = id, type = tipo, routeLineId = visita, label = "$tipo $visita", payloadJson = "{}")

class ProcesadorColaTest {

    @Test
    fun envia_en_orden_y_vacia_la_cola() = runTest {
        val dao = ColaEnMemoria(listOf(op(1, CHECKIN, 10), op(2, ORDER, 10), op(3, CHECKOUT, 10)))
        val enviadas = mutableListOf<Long>()
        val fin = ProcesadorCola(dao) { enviadas += it.id; RpcResult.Ok(null, null) }.procesar()
        assertEquals(Fin.LISTO, fin)
        assertEquals(listOf(1L, 2L, 3L), enviadas)
        assertEquals(0, dao.ops.size)
    }

    @Test
    fun sin_red_se_detiene_y_cuenta_el_intento() = runTest {
        val dao = ColaEnMemoria(listOf(op(1, CHECKIN, 10), op(2, CHECKOUT, 10)))
        val fin = ProcesadorCola(dao) { RpcResult.SinRed("timeout") }.procesar()
        assertEquals(Fin.REINTENTAR, fin)                       // WorkManager reintenta más tarde
        assertEquals(1, dao.ops.first { it.id == 1L }.attempts)
        assertEquals(0, dao.ops.first { it.id == 2L }.attempts) // no se intentó la siguiente
        assertEquals(PendingOpEntity.STATUS_PENDING, dao.ops.first { it.id == 1L }.status)
    }

    @Test
    fun rechazo_bloquea_solo_la_misma_visita() = runTest {
        // la llegada a la visita 10 es rechazada: su cierre no se envía; la visita 20 sigue
        val dao = ColaEnMemoria(listOf(op(1, CHECKIN, 10), op(2, CHECKIN, 20), op(3, CHECKOUT, 10), op(4, CHECKOUT, 20)))
        val enviadas = mutableListOf<Long>()
        val fin = ProcesadorCola(dao) {
            enviadas += it.id
            if (it.id == 1L) RpcResult.Rechazo("La visita ya fue cerrada por el supervisor") else RpcResult.Ok(null, null)
        }.procesar()
        assertEquals(Fin.LISTO, fin)
        assertEquals(listOf(1L, 2L, 4L), enviadas)
        assertEquals(listOf(1L, 3L), dao.ops.map { it.id })    // quedan la rechazada y la bloqueada
        assertEquals(PendingOpEntity.STATUS_ERROR, dao.ops[0].status)
        assertEquals("La visita ya fue cerrada por el supervisor", dao.ops[0].lastError)
        assertEquals(PendingOpEntity.STATUS_PENDING, dao.ops[1].status)
    }

    @Test
    fun sesion_vencida_marca_error_y_se_detiene() = runTest {
        val dao = ColaEnMemoria(listOf(op(1, ORDER, 10), op(2, ORDER, 20)))
        val fin = ProcesadorCola(dao) { RpcResult.SesionVencida }.procesar()
        assertEquals(Fin.LISTO, fin)                            // reintentar no sirve: hay que iniciar sesión
        assertEquals(PendingOpEntity.STATUS_ERROR, dao.ops[0].status)
        assertEquals(PendingOpEntity.STATUS_PENDING, dao.ops[1].status)
    }
}
