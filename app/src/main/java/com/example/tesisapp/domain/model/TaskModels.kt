package com.example.tesisapp.domain.model

enum class TaskType {
    BOOLEAN,
    TEXT
}

data class TaskDefinition(
    val id: Int,
    val question: String,
    val type: TaskType,
    val isRequired: Boolean
)