package com.example.tesisapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// 1. CLASE ENVOLTORIO GENÉRICA PARA ODOO
data class OdooResultWrapper<T>(
    @SerializedName("result") val result: T?
)

data class TaskDefinitionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<TaskDefinitionDto>
)

data class TaskOriginDto(
    @SerializedName("kind") val kind: String?,
    @SerializedName("label") val label: String?
)

data class TaskDefinitionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("question") val question: String,
    @SerializedName("type") val type: String,
    @SerializedName("required") val required: Boolean,
    // F4: tipos nuevos y alcance (la app elige por parada según el tipo de cliente)
    @SerializedName("options") val options: List<String>? = null,
    @SerializedName("unit") val unit: String? = null,
    @SerializedName("client_type") val clientType: String? = null,
    @SerializedName("origin") val origin: TaskOriginDto? = null
)

data class SubmitTaskResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)
