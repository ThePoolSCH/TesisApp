package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.domain.repository.TaskRepository
import com.example.tesisapp.ui.screens.tasks.TaskUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val isLoading: Boolean = false,
    val activeRouteLineId: Int? = null,

    // Datos crudos
    val definitions: List<TaskDefinition> = emptyList(),
    val answersMap: Map<Int, Any> = emptyMap(), // ID -> Valor

    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isSavedSuccess: Boolean = false
) {
    // LÓGICA DE ORDENAMIENTO AQUI
    // Primero filtramos y concatenamos para asegurar que BOOLEAN salga antes que TEXT
    val taskList: List<TaskUiModel> get() {
        val mappedList = definitions.map { def ->
            TaskUiModel(def, answersMap[def.id])
        }

        // Ordenar: Primero Boolean, luego Texto. Dentro de cada grupo, por ID o secuencia original
        return mappedList.sortedWith(compareBy(
            { it.definition.type != TaskType.BOOLEAN }, // False (0) va primero (Boolean), True (1) va despues
            { it.definition.id }
        ))
    }

    val totalTasks: Int get() = definitions.size
    val completedTasks: Int get() = taskList.count { it.isAnswered }
    val progress: Float get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()

    // Igual que en ProductsViewModel
    fun setRouteLineId(id: Int) {
        if (_uiState.value.activeRouteLineId != id) {
            _uiState.update { it.copy(activeRouteLineId = id) }
            loadTasks()
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            taskRepository.getTasks()
                .onSuccess { tasks ->
                    _uiState.update { it.copy(isLoading = false, definitions = tasks) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
        }
    }

    fun onAnswerChanged(taskDef: TaskDefinition, newValue: Any) {
        val currentMap = _uiState.value.answersMap.toMutableMap()

        // Lógica específica según tipo
        when (taskDef.type) {
            TaskType.BOOLEAN -> currentMap[taskDef.id] = newValue as Boolean
            TaskType.TEXT -> currentMap[taskDef.id] = newValue as String
        }

        _uiState.update { it.copy(answersMap = currentMap) }
    }

    fun submitAnswers() {
        val visitId = _uiState.value.activeRouteLineId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            taskRepository.submitTasks(visitId, _uiState.value.answersMap)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isSavedSuccess = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
                }
        }
    }

    fun resetSuccessStatus() {
        _uiState.update { it.copy(isSavedSuccess = false) }
    }
}