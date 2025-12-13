package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.TaskDefinition

interface TaskRepository {
    suspend fun getTasks(): Result<List<TaskDefinition>>
    // Recibe el ID de la visita y un mapa: ID_Pregunta -> Valor (Bool o String)
    suspend fun submitTasks(routeLineId: Int, answers: Map<Int, Any>): Result<String>
}