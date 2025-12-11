package com.example.tesisapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tesisapp.domain.model.User
import android.util.Log // <--- Importa esto


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar(
    user: User?,
    onViewProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    SideEffect {
        Log.d("DEBUG_USER", "Usuario recibido: $user")
        Log.d("DEBUG_USER", "Nombre extraído: ${user?.name}")
    }
    var menuExpanded by remember { mutableStateOf(false) }

    // Lógica para obtener solo el primer nombre
    // Si user es null, devuelve cadena vacía.
    // substringBefore(" ") toma todo antes del primer espacio.
    val firstName = remember(user) {
        user?.name?.substringBefore(" ") ?: ""
    }

    TopAppBar(
        title = {
            // He simplificado el título ya que el nombre ahora estará a la derecha
            Text(
                text = "TesisApp",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            // El bloque 'actions' es un RowScope por defecto, los elementos se ponen en fila

            // 1. Mostramos el nombre si existe
            if (firstName.isNotEmpty()) {
                Text(
                    text = "Hola, $firstName", // O solo firstName
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Un pequeño espacio entre el texto y el ícono
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 2. El ícono y su menú
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Opciones de perfil"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver perfil") },
                        onClick = {
                            menuExpanded = false
                            onViewProfileClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar Sesión") },
                        onClick = {
                            menuExpanded = false
                            onLogoutClick()
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}