package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.TaskAnswer
import com.example.tesisapp.domain.model.TaskDefinition
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    /** Descarga las tareas del vendedor para hoy (generales, de sus campañas y por tipo de cliente). */
    suspend fun refreshDefinitions(): Result<Unit>

    /** Tareas que aplican a una parada, desde la copia local (funciona sin conexión). */
    suspend fun definitionsFor(clientType: String): List<TaskDefinition>

    /** Respuestas guardadas de la visita y si ya se enviaron a la cola. */
    fun observeAnswers(routeLineId: Int): Flow<Pair<List<TaskAnswer>, Boolean>>

    suspend fun saveAnswer(routeLineId: Int, answer: TaskAnswer)

    suspend fun clearAnswer(routeLineId: Int, definitionId: Int)

    /** Encola el envío de las respuestas (con sus fotos) de la visita. */
    suspend fun submit(routeLineId: Int, partnerName: String, answers: List<TaskAnswer>, definitions: List<TaskDefinition>)
}
