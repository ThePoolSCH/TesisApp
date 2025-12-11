package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /**
     * Obtiene un Flow con la lista de tareas para una ubicación específica.
     */
    fun getTasksForLocation(locationId: Int): Flow<List<Task>>
    suspend fun updateTaskStatus(taskId: Int, newStatus: String)
}