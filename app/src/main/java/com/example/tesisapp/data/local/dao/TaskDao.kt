package com.example.tesisapp.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tesisapp.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    /**
     * Inserta una lista de tareas. Ignora las inserciones si ya existen.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    /**
     * Obtiene todas las tareas asociadas a un ID de ubicación específico.
     * Devuelve un Flow para que la UI se actualice automáticamente.
     */
    @Query("SELECT * FROM tasks_table WHERE locationId = :locationId ORDER BY priority DESC")
    fun getTasksForLocation(locationId: Int): Flow<List<TaskEntity>>

    /**
     * Cuenta el número total de tareas en la tabla.
     * Se usará para la lógica de inserción de datos de prueba.
     */
    @Query("SELECT COUNT(*) FROM tasks_table")
    suspend fun count(): Int
}