package com.example.tesisapp.data.repository

import android.util.Log
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.local.entity.TaskDefinitionEntity
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.dto.SubmitTaskRequest
import com.example.tesisapp.data.remote.dto.TaskAnswerDto
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.repository.TaskRepository
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val dao: TaskDao
) : TaskRepository {

    override suspend fun getTasks(): Result<List<TaskDefinition>> {
        return try {
            // Llamada a la API
            val wrapper = api.getTaskDefinitions()

            // Accedemos a .result (Quitamos el envoltorio de Odoo)
            val response = wrapper.result

            // NOTA: Si Odoo da error 500 interno, wrapper.result podría ser nulo dependiendo de tu Gson.
            // Es buena práctica validar:
            if (response == null) {
                Log.e("TaskRepo", "Odoo devolvió result nulo (posible error de servidor)")
                return loadFromLocal()
            }

            if (response.status == "success") {
                val entities = response.data.map { dto ->
                    TaskDefinitionEntity(
                        id = dto.id,
                        question = dto.question,
                        type = dto.type,
                        isRequired = dto.required,
                        sequence = dto.id
                    )
                }

                // Guardar en local
                dao.updateTasksCache(entities)

                // Retornar éxito
                Result.success(entities.map { it.toDomain() })
            } else {
                Log.e("TaskRepo", "Status no fue success: ${response.status}")
                loadFromLocal()
            }
        } catch (e: Exception) {
            Log.e("TaskRepo", "Error en getTasks: ${e.message}", e)
            loadFromLocal()
        }
    }

    override suspend fun submitTasks(routeLineId: Int, answers: Map<Int, Any>): Result<String> {
        return try {
            val answerDtos = answers.map { (id, value) ->
                TaskAnswerDto(definitionId = id, value = value)
            }

            val request = SubmitTaskRequest(routeLineId = routeLineId, answers = answerDtos)

            // También envolvemos la respuesta del submit
            val wrapper = api.submitTasks(request)
            val response = wrapper.result

            if (response != null && response.status == "success") {
                Result.success(response.message ?: "Guardado correctamente")
            } else {
                Result.failure(Exception(response?.message ?: "Error desconocido"))
            }
        } catch (e: Exception) {
            Log.e("TaskRepo", "Error submitTasks", e)
            Result.failure(e)
        }
    }

    private suspend fun loadFromLocal(): Result<List<TaskDefinition>> {
        val localData = dao.getAllTasks()
        return if (localData.isNotEmpty()) {
            Result.success(localData.map { it.toDomain() })
        } else {
            Result.failure(Exception("No hay conexión y no hay tareas guardadas localmente."))
        }
    }
}