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
    val type: String, // bool / text / number / selection / photo (como en Odoo)
    val isRequired: Boolean,
    val sequence: Int,
    // F4
    val options: String = "",       // separadas por "|"
    val unit: String = "",
    val clientType: String = "",
    val originKind: String = "all",
    val originLabel: String = ""
) {
    fun toDomain() = TaskDefinition(
        id = id, question = question, type = TaskType.from(type), isRequired = isRequired,
        options = options.split("|").filter { it.isNotBlank() }, unit = unit, clientType = clientType,
        originKind = originKind, originLabel = originLabel.ifBlank { "Todas las visitas" }
    )
}
