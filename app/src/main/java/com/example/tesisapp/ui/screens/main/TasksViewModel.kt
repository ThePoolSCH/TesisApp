package com.example.tesisapp.ui.screens.main


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.model.Task
import com.example.tesisapp.domain.repository.LocationRepository
import com.example.tesisapp.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val isLoading: Boolean = true,
    val activeVisitLocationName: String? = null,
    val tasks: List<Task> = emptyList()
) {
    val totalTasks: Int get() = tasks.size
    val completedTasks: Int get() = tasks.count { it.status == "Completada" }
    val pendingTasks: Int get() = totalTasks - completedTasks
    val progress: Float get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
}

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    locationRepository: LocationRepository // Para saber la ubicación activa
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 1. Observamos la ubicación activa.
            locationRepository.getLocations()
                .map { locations -> locations.firstOrNull { it.status == "En visita" } }
                .flatMapLatest { activeLocation ->
                    // 2. Si hay una ubicación activa, obtenemos sus tareas.
                    // Si no, devolvemos un flow vacío.
                    if (activeLocation != null) {
                        _uiState.update { it.copy(activeVisitLocationName = activeLocation.name) }
                        taskRepository.getTasksForLocation(activeLocation.id)
                    } else {
                        _uiState.update { it.copy(activeVisitLocationName = null) }
                        flowOf(emptyList()) // Devuelve una lista vacía si no hay visita
                    }
                }
                .collect { tasks ->
                    // 3. Actualizamos el estado de la UI con la lista de tareas.
                    _uiState.update {
                        it.copy(isLoading = false, tasks = tasks)
                    }
                }
        }
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) {
        viewModelScope.launch {
            val newStatus = if (isChecked) "Completada" else "Pendiente"
            taskRepository.updateTaskStatus(task.id, newStatus)
        }
    }
}