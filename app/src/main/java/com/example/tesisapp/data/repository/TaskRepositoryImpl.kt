package com.example.tesisapp.data.repository

import android.util.Log
import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.local.TaskDraftEntity
import com.example.tesisapp.data.local.dao.ColaDao
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.local.entity.TaskDefinitionEntity
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.domain.model.TaskAnswer
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.domain.repository.TaskRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val dao: TaskDao,
    private val cola: ColaDao,
    private val outbox: OutboxRepository
) : TaskRepository {

    private val gson = Gson()

    override suspend fun refreshDefinitions(): Result<Unit> = try {
        val response = api.getTaskDefinitions().result
        if (response?.status == "success") {
            dao.updateTasksCache(response.data.mapIndexed { i, dto ->
                TaskDefinitionEntity(
                    id = dto.id, question = dto.question, type = dto.type, isRequired = dto.required, sequence = i,
                    options = dto.options.orEmpty().joinToString("|"), unit = dto.unit.orEmpty(),
                    clientType = dto.clientType.orEmpty(), originKind = dto.origin?.kind ?: "all",
                    originLabel = dto.origin?.label.orEmpty()
                )
            })
            Result.success(Unit)
        } else Result.failure(Exception("No se pudieron descargar las tareas."))
    } catch (e: Exception) {
        Log.w("TaskRepo", "Sin conexión al descargar tareas: ${e.message}")
        Result.failure(e)
    }

    override suspend fun definitionsFor(clientType: String): List<TaskDefinition> =
        dao.getAllTasks().map { it.toDomain() }.filter { it.aplicaA(clientType) }

    override fun observeAnswers(routeLineId: Int): Flow<Pair<List<TaskAnswer>, Boolean>> =
        cola.observeDrafts(routeLineId).map { drafts ->
            drafts.map { d ->
                TaskAnswer(d.definitionId, leerValor(d.valueJson), d.photoTakenAt, d.photoLat, d.photoLon, d.photoKb)
            } to (drafts.isNotEmpty() && drafts.all { it.sent })
        }

    private fun leerValor(json: String): Any? {
        val e = JsonParser.parseString(json)
        if (e.isJsonNull || !e.isJsonPrimitive) return null
        val p = e.asJsonPrimitive
        return when {
            p.isBoolean -> p.asBoolean
            p.isNumber -> p.asDouble
            else -> p.asString
        }
    }

    override suspend fun saveAnswer(routeLineId: Int, answer: TaskAnswer) {
        cola.upsertDraft(TaskDraftEntity(routeLineId, answer.definitionId, gson.toJson(answer.value),
            answer.photoTakenAt, answer.photoLat, answer.photoLon, answer.photoKb, sent = false))
    }

    override suspend fun clearAnswer(routeLineId: Int, definitionId: Int) = cola.deleteDraft(routeLineId, definitionId)

    override suspend fun submit(routeLineId: Int, partnerName: String, answers: List<TaskAnswer>, definitions: List<TaskDefinition>) {
        val porId = definitions.associateBy { it.id }
        val payload = answers.mapNotNull { a ->
            val def = porId[a.definitionId] ?: return@mapNotNull null
            when (def.type) {
                TaskType.PHOTO -> mapOf("definition_id" to a.definitionId, "photo_path" to a.value,
                    "photo" to mapOf("taken_at" to a.photoTakenAt, "latitude" to a.photoLat, "longitude" to a.photoLon))
                TaskType.NUMBER -> mapOf("definition_id" to a.definitionId, "value" to a.value)
                else -> mapOf("definition_id" to a.definitionId, "value" to a.value)
            }
        }
        cola.marcarEnviadas(routeLineId)
        outbox.encolar(PendingOpEntity.TASKS, routeLineId, "Tareas · $partnerName",
            mapOf("route_line_id" to routeLineId, "answers" to payload))
    }
}
