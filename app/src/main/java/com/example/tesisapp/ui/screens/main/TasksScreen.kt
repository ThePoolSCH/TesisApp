package com.example.tesisapp.ui.screens.main


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.ui.components.TaskItemCard
import com.example.tesisapp.ui.components.TaskProgressCard

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    activeRouteLineId: Int? // Recibimos el ID desde MainScreen
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Sincronizar ID con ViewModel
    LaunchedEffect(activeRouteLineId) {
        if (activeRouteLineId != null) {
            viewModel.setRouteLineId(activeRouteLineId)
        }
    }

    // Manejo de Feedback (Toasts)
    LaunchedEffect(uiState.isSavedSuccess, uiState.errorMessage) {
        if (uiState.isSavedSuccess) {
            Toast.makeText(context, "Respuestas guardadas en Odoo", Toast.LENGTH_SHORT).show()
            viewModel.resetSuccessStatus()
        }
        if (uiState.errorMessage != null) {
            Toast.makeText(context, uiState.errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    // 1. Caso: Cargando
    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 2. Caso: No hay visita activa (activeRouteLineId es null)
    if (activeRouteLineId == null) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Inicie una visita en la pestaña 'Rutas' para ver las tareas asignadas.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

    // 3. Caso: Contenido Principal
    Scaffold(
        floatingActionButton = {
            if (uiState.taskList.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.submitAnswers() },
                    containerColor = Color.Black,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(" Enviar ", color = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tarjeta de Progreso
            item {
                TaskProgressCard(
                    completed = uiState.completedTasks,
                    total = uiState.totalTasks,
                    progress = uiState.progress,
                    locationName = "Visita en Curso" // Puedes pasar el nombre si lo traes en el repo
                )
            }

            // Lista de Tareas
            items(uiState.taskList, key = { it.definition.id }) { taskUiModel ->
                TaskItemCard(
                    taskUiModel = taskUiModel,
                    onValueChange = { newValue ->
                        viewModel.onAnswerChanged(taskUiModel.definition, newValue)
                    }
                )
            }

            // Espacio final para el FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}