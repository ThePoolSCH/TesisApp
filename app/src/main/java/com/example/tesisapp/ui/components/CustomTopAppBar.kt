package com.example.tesisapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tesisapp.data.sync.SyncStatus
import com.example.tesisapp.domain.model.User

/** Chip de sincronización (mockups A1 y A5): al tocarlo se abre la pantalla de sincronización. */
@Composable
fun SyncChip(sync: SyncStatus, onClick: () -> Unit) {
    when {
        sync.errores > 0 -> StatusChip("Error · ${sync.errores}", ChipKind.ERROR, Icons.Default.ErrorOutline, onClick = onClick)
        !sync.online -> StatusChip(if (sync.pendientes > 0) "Sin conexión · ${sync.pendientes}" else "Sin conexión",
            ChipKind.LINEA, Icons.Default.CloudOff, onClick = onClick)
        sync.pendientes > 0 -> StatusChip("${sync.pendientes} pendiente${if (sync.pendientes > 1) "s" else ""}",
            ChipKind.PENDIENTE, Icons.Default.CloudUpload, onClick = onClick)
        else -> StatusChip("Al día", ChipKind.OK, Icons.Default.CloudDone, onClick = onClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    user: User?,
    sync: SyncStatus?,
    onSyncClick: () -> Unit,
    onViewProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val firstName = remember(user) { user?.name?.substringBefore(" ") ?: "" }

    TopAppBar(
        title = { Text(text = "TesisApp", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        actions = {
            if (sync != null) {
                SyncChip(sync, onSyncClick)
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (firstName.isNotEmpty()) {
                Text(text = firstName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Opciones de perfil")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Sincronización") }, onClick = { menuExpanded = false; onSyncClick() })
                    DropdownMenuItem(text = { Text("Ver perfil") }, onClick = { menuExpanded = false; onViewProfileClick() })
                    DropdownMenuItem(text = { Text("Cerrar sesión") }, onClick = { menuExpanded = false; onLogoutClick() })
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
