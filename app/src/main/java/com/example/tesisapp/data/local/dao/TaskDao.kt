package com.example.tesisapp.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tesisapp.data.local.entity.TaskDefinitionEntity

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_definitions ORDER BY sequence ASC")
    suspend fun getAllTasks(): List<TaskDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskDefinitionEntity>)

    @Query("DELETE FROM task_definitions")
    suspend fun clearTasks()

    // Transacción simple para actualizar caché
    @androidx.room.Transaction
    suspend fun updateTasksCache(tasks: List<TaskDefinitionEntity>) {
        clearTasks()
        insertAll(tasks)
    }
}