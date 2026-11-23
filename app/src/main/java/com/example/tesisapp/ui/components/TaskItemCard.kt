package com.example.tesisapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.ui.screens.main.leerNumero
import com.example.tesisapp.ui.screens.tasks.TaskUiModel
import com.example.tesisapp.ui.theme.TesisColors
import com.example.tesisapp.utils.Formato
import java.io.File

/** Tarjeta de una tarea (mockup A2): origen, obligatoria y el control según su tipo. */
@Composable
fun TaskItemCard(
    taskUiModel: TaskUiModel,
    enabled: Boolean,
    onValueChange: (Any?) -> Unit,
    onTomarFoto: () -> Unit
) {
    val def = taskUiModel.definition
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TesisColors.Blanco),
        border = BorderStroke(1.5.dp, if (taskUiModel.isAnswered) TesisColors.OkFuerte else TesisColors.Negro),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(def.question, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (def.isRequired) StatusChip("Obligatorio", ChipKind.ERROR)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                val icono = when (def.originKind) { "campaign" -> Icons.Outlined.Campaign; "client_type" -> Icons.Outlined.Storefront
                    else -> Icons.Outlined.Public }
                Icon(icono, null, Modifier.size(14.dp), tint = TesisColors.Texto2)
                Spacer(Modifier.width(4.dp))
                Text(def.originLabel, style = MaterialTheme.typography.labelSmall, color = TesisColors.Texto2)
            }
            Spacer(Modifier.height(10.dp))

            when (def.type) {
                TaskType.BOOLEAN -> {
                    val v = taskUiModel.answerValue as? Boolean
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Segmento("Sí", v == true, enabled, Modifier.weight(1f)) { onValueChange(true) }
                        Segmento("No", v == false, enabled, Modifier.weight(1f)) { onValueChange(false) }
                    }
                }
                TaskType.SELECTION -> {
                    val v = taskUiModel.answerValue as? String
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        def.options.forEach { o -> Segmento(o, v == o, enabled, Modifier.weight(1f)) { onValueChange(o) } }
                    }
                }
                TaskType.NUMBER -> {
                    val inicial = (taskUiModel.answerValue as? Number)?.toDouble()?.let { Formato.numero(it, 2).trimEnd('0').trimEnd(',', '.') } ?: ""
                    var texto by remember(def.id) { mutableStateOf(inicial) }
                    // Las respuestas guardadas llegan de la base después del primer dibujo
                    LaunchedEffect(inicial) { if (texto.isEmpty() && inicial.isNotEmpty()) texto = inicial }
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { t -> texto = t; onValueChange(leerNumero(t)) },
                        enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        prefix = if (def.unit == "S/") ({ Text("S/ ") }) else null,
                        suffix = if (def.unit.isNotBlank() && def.unit != "S/") ({ Text(def.unit) }) else null,
                        isError = texto.isNotBlank() && leerNumero(texto) == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small, colors = campoColores()
                    )
                }
                TaskType.TEXT -> {
                    // Estado local: si el campo leyera directo de la base (asíncrona), el cursor saltaría al inicio
                    val guardado = (taskUiModel.answerValue as? String) ?: ""
                    var texto by remember(def.id) { mutableStateOf(guardado) }
                    LaunchedEffect(guardado) { if (texto.isEmpty() && guardado.isNotEmpty()) texto = guardado }
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it; onValueChange(it) }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Escribe aquí...") }, minLines = 2,
                        shape = MaterialTheme.shapes.small, colors = campoColores()
                    )
                }
                TaskType.PHOTO -> {
                    val ruta = taskUiModel.answerValue as? String
                    val a = taskUiModel.answer
                    if (ruta.isNullOrBlank()) {
                        Column(
                            Modifier.fillMaxWidth().height(110.dp)
                                .border(2.dp, TesisColors.Gris300, MaterialTheme.shapes.medium)
                                .clickable(enabled = enabled, onClick = onTomarFoto),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, Modifier.size(34.dp), tint = TesisColors.Texto2)
                            Text("Tomar foto", color = TesisColors.Texto2)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = File(ruta), contentDescription = "Foto", contentScale = ContentScale.Crop,
                                modifier = Modifier.size(92.dp).clip(MaterialTheme.shapes.medium))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Foto tomada", fontWeight = FontWeight.SemiBold)
                                val meta = listOfNotNull(a?.photoKb?.let { "$it KB" },
                                    if (a?.photoLat != null) "con GPS" else "sin GPS")
                                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = TesisColors.Texto2)
                                if (enabled) TextButton(onClick = onTomarFoto, contentPadding = PaddingValues(0.dp)) {
                                    Text("Tomar otra", color = TesisColors.Negro, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Segmento(texto: String, sel: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .border(1.5.dp, TesisColors.Negro, MaterialTheme.shapes.medium)
            .background(if (sel) TesisColors.Negro else TesisColors.Blanco, MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(texto, color = if (sel) TesisColors.Blanco else TesisColors.Negro, fontWeight = FontWeight.SemiBold) }
}
