package com.example.tesisapp.data.remote

import com.example.tesisapp.data.remote.dto.OdooAuthRequest
import com.example.tesisapp.data.remote.dto.OdooAuthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.tesisapp.data.remote.OdooJsonRpcRequest
import com.example.tesisapp.data.remote.OdooRouteResponse
import com.example.tesisapp.data.remote.dto.OdooResultWrapper
import com.example.tesisapp.data.remote.dto.SubmitTaskRequest
import com.example.tesisapp.data.remote.dto.SubmitTaskResponse
import com.example.tesisapp.data.remote.dto.TaskDefinitionsResponse

interface OdooApiService {
    @POST("/web/session/authenticate")
    suspend fun authenticate(@Body request: OdooAuthRequest): Response<OdooAuthResponse>

    @POST("/api/rutas/today")
    suspend fun getTodayRoute(@Body body: OdooJsonRpcRequest): Response<OdooRouteResponse>

    @POST("/api/rutas/checkin")
    suspend fun checkInStop(@Body body: OdooJsonRpcRequest): Response<OdooCheckInResponse>

    // NUEVO
    @POST("/api/rutas/checkout")
    suspend fun checkOutStop(@Body body: OdooJsonRpcRequest): Response<OdooCheckInResponse> // Reusamos el DTO de respuesta

    @POST("/api/tasks/definitions")
    suspend fun getTaskDefinitions(@Body body: Map<String, String> = emptyMap()): OdooResultWrapper<TaskDefinitionsResponse>

    @POST("/api/tasks/submit")
    suspend fun submitTasks(@Body request: SubmitTaskRequest): OdooResultWrapper<SubmitTaskResponse>
}