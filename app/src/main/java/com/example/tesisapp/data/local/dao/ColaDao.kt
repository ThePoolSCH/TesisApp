package com.example.tesisapp.data.local.dao

import androidx.room.*
import com.example.tesisapp.data.local.CacheEntity
import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.local.SuggestedEntity
import com.example.tesisapp.data.local.TaskDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColaDao {
    // --- Cola de operaciones ---
    @Insert
    suspend fun insert(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops ORDER BY id ASC")
    fun observeAll(): Flow<List<PendingOpEntity>>

    @Query("SELECT * FROM pending_ops WHERE status = 'pending' ORDER BY id ASC")
    suspend fun pendientes(): List<PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE status = 'error'")
    suspend fun conError(): List<PendingOpEntity>

    @Query("SELECT * FROM pending_ops WHERE routeLineId = :routeLineId")
    suspend fun deLaVisita(routeLineId: Int): List<PendingOpEntity>

    @Update
    suspend fun update(op: PendingOpEntity)

    @Query("DELETE FROM pending_ops WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_ops SET status = 'pending', lastError = NULL WHERE id = :id")
    suspend fun reintentar(id: Long)

    @Query("UPDATE pending_ops SET status = 'pending', lastError = NULL WHERE status = 'error'")
    suspend fun reintentarTodas()

    // --- Borradores de tareas ---
    @Query("SELECT * FROM task_drafts WHERE routeLineId = :routeLineId")
    fun observeDrafts(routeLineId: Int): Flow<List<TaskDraftEntity>>

    @Query("SELECT * FROM task_drafts WHERE routeLineId = :routeLineId")
    suspend fun drafts(routeLineId: Int): List<TaskDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(d: TaskDraftEntity)

    @Query("DELETE FROM task_drafts WHERE routeLineId = :routeLineId AND definitionId = :definitionId")
    suspend fun deleteDraft(routeLineId: Int, definitionId: Int)

    @Query("UPDATE task_drafts SET sent = 1 WHERE routeLineId = :routeLineId")
    suspend fun marcarEnviadas(routeLineId: Int)

    // --- Pedido sugerido ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSuggested(items: List<SuggestedEntity>)

    @Query("SELECT * FROM suggested_orders WHERE partnerId = :partnerId")
    suspend fun suggested(partnerId: Int): SuggestedEntity?

    // --- Caché ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCache(c: CacheEntity)

    @Query("SELECT * FROM api_cache WHERE `key` = :key")
    fun observeCache(key: String): Flow<CacheEntity?>
}
