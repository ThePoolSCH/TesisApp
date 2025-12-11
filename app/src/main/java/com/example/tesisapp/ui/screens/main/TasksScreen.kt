package com.example.tesisapp.ui.screens.main


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.ui.components.TaskItemCard
import com.example.tesisapp.ui.components.TaskProgressCard

@Composable
fun TasksScreen(viewModel: TasksViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.activeVisitLocationName == null) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Inicie una visita en la pestaña 'Rutas' para ver las tareas asignadas.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TaskProgressCard(
                    completed = uiState.completedTasks,
                    total = uiState.totalTasks,
                    progress = uiState.progress,
                    locationName = uiState.activeVisitLocationName
                )
            }

            items(uiState.tasks, key = { it.id }) { task ->
                TaskItemCard(
                    task = task,
                    onCheckedChange = { isChecked ->
                        viewModel.onTaskCheckedChanged(task, isChecked)
                    }
                )
            }
        }
    }
}