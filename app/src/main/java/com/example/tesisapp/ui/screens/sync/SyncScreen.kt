package com.example.tesisapp.ui.screens.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.data.local.PendingOpEntity
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.data.sync.SyncStatus
import com.example.tesisapp.ui.components.*
import com.example.tesisapp.ui.theme.TesisColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(private val outbox: OutboxRepository) : ViewModel() {
    val ops = outbox.operaciones.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val estado = outbox.estado.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun sincronizar() = outbox.sincronizarAhora()
    fun reintentar(id: Long) = viewModelScope.launch { outbox.reintentar(id) }
    fun reintentarTodas() = viewModelScope.launch { outbox.reintentarTodas() }
    fun descartar(id: Long) = viewModelScope.launch { outbox.descartar(id) }
}

/** Pantalla de sincronización (mockup A5, estados 3 y 4): qué falta enviar, qué falló y qué hacer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(onBack: () -> Unit, vm: SyncViewModel = hiltViewModel()) {
    val ops by vm.ops.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()
    var descartar by remember { mutableStateOf<PendingOpEntity?>(null) }
    val hora = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    descartar?.let { op ->
        AlertDialog(
            onDismissRequest = { descartar = null }, containerColor = TesisColors.Blanco,
            title = { Text("¿Descartar esta operación?") },
            text = { Text("\"${op.label}\" no llegará a Odoo. Úsalo solo si ya lo registraste de otra forma o si fue un error.") },
            confirmButton = { TextButton({ vm.descartar(op.id); descartar = null }) { Text("Descartar", color = TesisColors.Error) } },
            dismissButton = { TextButton({ descartar = null }) { Text("Cancelar", color = TesisColors.Negro) } }
        )
    }

    Scaffold(
        containerColor = TesisColors.Fondo,
        topBar = {
            TopAppBar(
                title = { Text("Sincronización", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TesisColors.Fondo)
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ResumenCard(estado, hora, vm::sincronizar, vm::reintentarTodas) }
            if (ops.isEmpty()) {
                item { TesisCard { EmptyState(Icons.Default.CloudDone, "Todo está en Odoo", "No hay operaciones pendientes de envío.") } }
            }
            items(ops, key = { it.id }) { op ->
                TesisCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(when (op.type) {
                            PendingOpEntity.ORDER -> Icons.Default.ShoppingCart
                            PendingOpEntity.TASKS -> Icons.Default.CheckBox
                            PendingOpEntity.CHECKIN -> Icons.Default.LocationOn
                            else -> Icons.Default.Flag
                        }, null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(op.label, fontWeight = FontWeight.SemiBold)
                            Text("Hecho ${hora.format(Date(op.createdAt))}" + if (op.attempts > 0) " · ${op.attempts} intento(s)" else "",
                                style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                        }
                        if (op.status == PendingOpEntity.STATUS_ERROR) StatusChip("Error", ChipKind.ERROR)
                        else StatusChip("Pendiente", ChipKind.PENDIENTE)
                    }
                    if (op.status == PendingOpEntity.STATUS_ERROR) {
                        Spacer(Modifier.height(8.dp))
                        Text(op.lastError ?: "Odoo rechazó la operación.", color = TesisColors.Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().background(TesisColors.ErrorBg, MaterialTheme.shapes.small).padding(8.dp))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton("Reintentar", { vm.reintentar(op.id) }, Modifier.weight(1f), Icons.Default.Refresh, height = 42)
                            SecondaryButton("Descartar", { descartar = op }, Modifier.weight(1f), Icons.Default.Delete, height = 42)
                        }
                    }
                }
            }
            item {
                Text("Sin señal puedes validar llegadas, responder tareas (con fotos), tomar pedidos y cerrar visitas: " +
                    "todo se guarda en el teléfono y se envía en orden al volver la conexión. La cartera, la ficha del " +
                    "cliente y la visita no planificada necesitan conexión.",
                    style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
            }
        }
    }
}

@Composable
private fun ResumenCard(estado: SyncStatus?, hora: SimpleDateFormat, onSync: () -> Unit, onReintentarTodas: () -> Unit) {
    TesisCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(when {
                    estado == null -> ""
                    estado.errores > 0 -> "${estado.errores} con error"
                    estado.pendientes > 0 -> "${estado.pendientes} por enviar"
                    else -> "Al día"
                }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    (if (estado?.online == false) "Sin conexión" else "Con conexión") +
                        (estado?.ultimaSync?.takeIf { it > 0 }?.let { " · última sincronización ${hora.format(Date(it))}" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2
                )
            }
            if (estado != null) SyncChip(estado) {}
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Sincronizar ahora", onSync, icon = Icons.Default.Sync, enabled = estado?.online != false)
        if ((estado?.errores ?: 0) > 1) {
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Reintentar todas", onReintentarTodas, Modifier.fillMaxWidth(), Icons.Default.Refresh)
        }
    }
}
