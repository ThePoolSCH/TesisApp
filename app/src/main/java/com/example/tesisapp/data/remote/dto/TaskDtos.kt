package com.example.tesisapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// 1. CLASE ENVOLTORIO GENÉRICA PARA ODOO
data class OdooResultWrapper<T>(
    @SerializedName("result") val result: T
)

// 2. LA RESPUESTA INTERNA (Lo que tú definiste)
data class TaskDefinitionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: List<TaskDefinitionDto>
)

data class TaskDefinitionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("question") val question: String,
    @SerializedName("type") val type: String,
    @SerializedName("required") val required: Boolean
)

// --- Para el Submit ---
data class SubmitTaskRequest(
    @SerializedName("route_line_id") val routeLineId: Int,
    @SerializedName("answers") val answers: List<TaskAnswerDto>
)

data class TaskAnswerDto(
    @SerializedName("definition_id") val definitionId: Int,
    @SerializedName("value") val value: Any
)

data class SubmitTaskResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)