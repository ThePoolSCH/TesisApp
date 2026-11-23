package com.example.tesisapp.ui.screens.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tesisapp.domain.model.RouteStop
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Fotos
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun TasksScreen(
    activeStop: RouteStop?,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeStop?.id) { activeStop?.let(viewModel::setStop) }
    LaunchedEffect(uiState.message) { uiState.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() } }

    // Cámara: la foto se guarda en files/fotos, se comprime y se registra con la última ubicación conocida
    var fotoPendiente by remember { mutableStateOf<Pair<TaskDefinition, File>?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val (def, archivo) = fotoPendiente ?: return@rememberLauncherForActivityResult
        fotoPendiente = null
        if (!ok) { archivo.delete(); return@rememberLauncherForActivityResult }
        scope.launch {
            val kb = withContext(Dispatchers.IO) { Fotos.comprimir(archivo) }
            try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation
                    .addOnSuccessListener { loc -> viewModel.onFotoTomada(def, archivo, kb, loc?.latitude, loc?.longitude) }
                    .addOnFailureListener { viewModel.onFotoTomada(def, archivo, kb, null, null) }
            } catch (e: SecurityException) {
                viewModel.onFotoTomada(def, archivo, kb, null, null)
            }
        }
    }
    fun tomarFoto(def: TaskDefinition) {
        val f = Fotos.nuevoArchivo(context)
        fotoPendiente = def to f
        camara.launch(Fotos.uriPara(context, f))
    }

    if (activeStop == null) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Inicia una visita en la pestaña \"Rutas\" para ver sus tareas.", style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center, color = TesisColors.Texto2)
        }
        return
    }
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TesisColors.Negro) }
        return
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, containerColor = TesisColors.Fondo) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TaskProgressCard(uiState.completedTasks, uiState.totalTasks, uiState.progress, activeStop.partnerName)
            }
            if (uiState.definitions.isEmpty()) {
                item { TesisCard { EmptyState(Icons.Outlined.CheckBox, "Sin tareas para esta visita") } }
            }
            items(uiState.taskList, key = { it.definition.id }) { t ->
                TaskItemCard(t, enabled = !uiState.enviadas,
                    onValueChange = { v -> viewModel.onAnswerChanged(t.definition, v) },
                    onTomarFoto = { tomarFoto(t.definition) })
            }
            if (uiState.definitions.isNotEmpty()) item {
                if (uiState.enviadas) {
                    TesisCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, null, tint = TesisColors.Ok)
                            Spacer(Modifier.width(8.dp))
                            Text("Respuestas guardadas: se envían a Odoo con la cola de sincronización.", color = TesisColors.Ok)
                        }
                    }
                } else {
                    PrimaryButton("Enviar", viewModel::submitAnswers, icon = Icons.Default.Send, enabled = uiState.faltan.isEmpty())
                    if (uiState.faltan.isNotEmpty()) {
                        Text("Completa: " + uiState.faltan.joinToString(", ") { it.question },
                            style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
