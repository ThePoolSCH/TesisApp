package com.example.tesisapp.domain.model

enum class TaskType {
    BOOLEAN, TEXT, NUMBER, SELECTION, PHOTO;

    companion object {
        fun from(odoo: String) = when (odoo) {
            "bool" -> BOOLEAN; "number" -> NUMBER; "selection" -> SELECTION; "photo" -> PHOTO; else -> TEXT
        }
    }
}

data class TaskDefinition(
    val id: Int,
    val question: String,
    val type: TaskType,
    val isRequired: Boolean,
    // F4 (mockup A2)
    val options: List<String> = emptyList(),
    val unit: String = "",
    val clientType: String = "",   // vacío = todos los tipos de cliente
    val originKind: String = "all", // all / campaign / client_type
    val originLabel: String = "Todas las visitas"
) {
    fun aplicaA(tipoCliente: String) = clientType.isBlank() || clientType == tipoCliente
}

/** Respuesta en edición. Para la foto, `value` es la ruta del archivo en el teléfono. */
data class TaskAnswer(
    val definitionId: Int,
    val value: Any?,
    val photoTakenAt: String? = null,
    val photoLat: Double? = null,
    val photoLon: Double? = null,
    val photoKb: Int? = null
)
