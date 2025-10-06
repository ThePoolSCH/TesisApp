package com.example.tesisapp.data.repository


import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.local.entity.TaskEntity
import com.example.tesisapp.domain.model.Task
import com.example.tesisapp.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    // Mapper de Entity (DB) a Model (Domain)
    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = this.id,
            locationId = this.locationId,
            name = this.name,
            description = this.description,
            priority = this.priority,
            status = this.status,
            requiresEvidence = this.requiresEvidence,
            requiresAnnotation = this.requiresAnnotation,
            evidenceUri = this.evidenceUri,
            annotationText = this.annotationText
        )
    }

    init {
        // Al crear el repositorio, comprobamos si la tabla de tareas está vacía
        // y la poblamos con datos de prueba si es necesario.
        CoroutineScope(Dispatchers.IO).launch {
            if (taskDao.count() == 0) {
                taskDao.insertAll(getHardcodedTasks())
            }
        }
    }

    override fun getTasksForLocation(locationId: Int): Flow<List<Task>> {
        return taskDao.getTasksForLocation(locationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // --- DATOS HARDCODEADOS ---
    private fun getHardcodedTasks(): List<TaskEntity> {
        return listOf(
            // Tareas para "Bodega Mi Pueblo" (locationId = 1)
            TaskEntity(locationId = 1, name = "Verificar Inventario", description = "Contar stock de bebidas gaseosas y snacks.", priority = "Alta", status = "Pendiente", requiresEvidence = false, requiresAnnotation = true),
            TaskEntity(locationId = 1, name = "Storechecking", description = "Tomar foto de la exhibición de la competencia.", priority = "Media", status = "Pendiente", requiresEvidence = true, requiresAnnotation = false),

            // Tareas para "Market Express" (locationId = 2)
            TaskEntity(locationId = 2, name = "Proponer Productos Nuevos", description = "Sugerir al menos 2 productos nuevos para el local y tomar nota de la respuesta del encargado.", priority = "Alta", status = "Pendiente", requiresEvidence = false, requiresAnnotation = true),
            TaskEntity(locationId = 2, name = "Revisar Precios", description = "Verificar que los precios de nuestros productos clave estén correctos.", priority = "Media", status = "Pendiente", requiresEvidence = false, requiresAnnotation = false),

            // Tareas para "Minimarket El Sol" (locationId = 3)
            TaskEntity(locationId = 3, name = "Actualizar Material POP", description = "Colocar los nuevos afiches promocionales en la entrada.", priority = "Baja", status = "Pendiente", requiresEvidence = true, requiresAnnotation = true)
        )
    }
}