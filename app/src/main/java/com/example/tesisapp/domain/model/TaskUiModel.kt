package com.example.tesisapp.domain.model

// Este modelo es solo para la UI, combina definición + respuesta
data class TaskUiModel(
    val definition: TaskDefinition, // La que viene de Odoo/Room
    val answerValue: Any?,          // La respuesta actual (Boolean o String)
    val isSynced: Boolean = false   // Para saber si ya se envió (opcional)
) {
    val isCompleted: Boolean
        get() = when (definition.type) {
            TaskType.BOOLEAN -> answerValue != null // Asumimos completado si se interactuó
            TaskType.TEXT -> ! (answerValue as? String).isNullOrBlank()
        }
}