package com.example.tesisapp.ui.screens.tasks

import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType

data class TaskUiModel(
    val definition: TaskDefinition,
    val answerValue: Any? = null // Boolean o String
) {
    val isAnswered: Boolean
        get() = when (definition.type) {
            TaskType.BOOLEAN -> (answerValue as? Boolean) == true
            TaskType.TEXT -> ! (answerValue as? String).isNullOrBlank()
        }
}