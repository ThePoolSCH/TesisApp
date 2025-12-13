package com.example.tesisapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tesisapp.domain.model.TaskType
import com.example.tesisapp.ui.screens.tasks.TaskUiModel

@Composable
fun TaskItemCard(
    taskUiModel: TaskUiModel,
    onValueChange: (Any) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Black), // Borde sutil gris muy claro
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taskUiModel.definition.question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    if (taskUiModel.definition.isRequired) {
                        Text(
                            text = "Obligatorio",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (taskUiModel.definition.type) {
                TaskType.BOOLEAN -> {
                    val isChecked = (taskUiModel.answerValue as? Boolean) ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // SWITCH PERSONALIZADO NEGRO
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onValueChange(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.Black,
                                checkedBorderColor = Color.Black,
                                uncheckedThumbColor = Color.Black,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = Color.Black
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isChecked) "Completado" else "Pendiente",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChecked) Color.Black else Color.Gray
                        )
                    }
                }

                TaskType.TEXT -> {
                    val textValue = (taskUiModel.answerValue as? String) ?: ""
                    // INPUT TEXTO PERSONALIZADO NEGRO
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { onValueChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe aquí...", color = Color.Gray) },
                        minLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFAFA),
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            cursorColor = Color.Black,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}