package com.example.tesisapp.ui.screens.tasks

import com.example.tesisapp.domain.model.TaskAnswer
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType

data class TaskUiModel(
    val definition: TaskDefinition,
    val answer: TaskAnswer? = null
) {
    val answerValue: Any? get() = answer?.value

    /** Sí/No cuenta como respondida al elegir cualquiera de las dos (antes solo contaba "Sí"). */
    val isAnswered: Boolean
        get() = when (definition.type) {
            TaskType.BOOLEAN -> answerValue is Boolean
            TaskType.NUMBER -> answerValue is Number
            TaskType.TEXT, TaskType.SELECTION, TaskType.PHOTO -> !(answerValue as? String).isNullOrBlank()
        }
}
