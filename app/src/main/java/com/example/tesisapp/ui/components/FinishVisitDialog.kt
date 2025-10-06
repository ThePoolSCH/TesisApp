package com.example.tesisapp.ui.components


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun FinishVisitDialog(
    locationName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onReportIncident: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terminar visita en '$locationName'") },
        text = { Text("¿Deseas finalizar la visita o reportar un incidente?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Finalizar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReportIncident) {
                Text("Reportar Incidente")
            }
        },
        // Añadimos un icono de 'X' para cerrar, aunque onDismissRequest ya lo maneja al tocar fuera.
        // Esto es más explícito para el usuario.
        icon = {
            IconButton(onClick = onDismiss, modifier = androidx.compose.ui.Modifier) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar diálogo")
            }
        }
    )
}