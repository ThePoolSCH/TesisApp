package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tesisapp.domain.model.TaskDefinition
import com.example.tesisapp.domain.model.TaskType

@Entity(tableName = "task_definitions")
data class TaskDefinitionEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val question: String,
    val type: String, // Guardamos como String: "bool" o "text"
    val isRequired: Boolean,
    val sequence: Int // Útil para ordenar
) {
    // Mapper de Entidad a Dominio
    fun toDomain(): TaskDefinition {
        return TaskDefinition(
            id = id,
            question = question,
            type = if (type == "bool") TaskType.BOOLEAN else TaskType.TEXT,
            isRequired = isRequired
        )
    }
}