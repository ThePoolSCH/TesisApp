package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.model.TaskAnswer
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.domain.repository.TaskRepository
import com.example.tesisapp.ui.screens.tasks.TaskUiModel
import com.example.tesisapp.utils.Formato
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TasksUiState(
    val isLoading: Boolean = false,
    val stop: RouteStop? = null,
    val definitions: List<TaskDefinition> = emptyList(),
    val answers: Map<Int, TaskAnswer> = emptyMap(),
    val enviadas: Boolean = false,
    val message: String? = null
) {
    val taskList: List<TaskUiModel> get() = definitions.map { TaskUiModel(it, answers[it.id]) }
    val totalTasks: Int get() = definitions.size
    val completedTasks: Int get() = taskList.count { it.isAnswered }
    val progress: Float get() = if (totalTasks > 0) completedTasks / totalTasks.toFloat() else 0f
    val faltan: List<TaskDefinition> get() = taskList.filter { it.definition.isRequired && !it.isAnswered }.map { it.definition }
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()
    private var job: Job? = null

    /** Tareas de la parada en curso: las generales, las de sus campañas y las de su tipo de cliente (sin conexión). */
    fun setStop(stop: RouteStop) {
        if (_uiState.value.stop?.id == stop.id) return
        job?.cancel()
        _uiState.update { TasksUiState(isLoading = true, stop = stop) }
        job = viewModelScope.launch {
            var defs = taskRepository.definitionsFor(stop.clientType)
            if (defs.isEmpty()) { taskRepository.refreshDefinitions(); defs = taskRepository.definitionsFor(stop.clientType) }
            _uiState.update { it.copy(isLoading = false, definitions = defs) }
            taskRepository.observeAnswers(stop.id).collect { (answers, enviadas) ->
                _uiState.update { it.copy(answers = answers.associateBy { a -> a.definitionId }, enviadas = enviadas) }
            }
        }
    }

    fun onAnswerChanged(def: TaskDefinition, value: Any?) {
        val stop = _uiState.value.stop ?: return
        viewModelScope.launch {
            val vacio = value == null || (value is String && value.isEmpty())
            if (vacio) taskRepository.clearAnswer(stop.id, def.id)
            else taskRepository.saveAnswer(stop.id, TaskAnswer(def.id, value))
        }
    }

    /** Foto recién tomada: se comprime (en la pantalla) y se guarda con su hora y posición. */
    fun onFotoTomada(def: TaskDefinition, archivo: File, kb: Int, lat: Double?, lon: Double?) {
        val stop = _uiState.value.stop ?: return
        viewModelScope.launch {
            _uiState.value.answers[def.id]?.value?.let { anterior -> if (anterior is String && anterior != archivo.path) File(anterior).delete() }
            taskRepository.saveAnswer(stop.id, TaskAnswer(def.id, archivo.path, Formato.utcAhora(), lat, lon, kb))
        }
    }

    fun submitAnswers() {
        val s = _uiState.value
        val stop = s.stop ?: return
        if (s.faltan.isNotEmpty()) {
            _uiState.update { it.copy(message = "Completa: " + s.faltan.joinToString(", ") { d -> d.question }) }
            return
        }
        viewModelScope.launch {
            taskRepository.submit(stop.id, stop.partnerName, s.answers.values.toList(), s.definitions)
            _uiState.update { it.copy(message = "Respuestas guardadas. Se enviarán a Odoo en cuanto haya conexión.") }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}

/** Valor de una respuesta numérica tal como se escribe ("2,80" → 2.8). */
fun leerNumero(texto: String): Double? = texto.replace(',', '.').toDoubleOrNull()

fun TaskType.etiqueta() = when (this) {
    TaskType.BOOLEAN -> "Sí/No"; TaskType.TEXT -> "Texto"; TaskType.NUMBER -> "Número"
    TaskType.SELECTION -> "Opciones"; TaskType.PHOTO -> "Foto"
}
