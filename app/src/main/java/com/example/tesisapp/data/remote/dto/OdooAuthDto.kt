package com.example.tesisapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// Estructura que pide Odoo
data class OdooAuthRequest(
    val jsonrpc: String = "2.0",
    val method: String = "call",
    val params: AuthParams
)

data class AuthParams(
    val db: String,
    val login: String,
    val password: String
)

// Estructura que devuelve Odoo
data class OdooAuthResponse(
    val jsonrpc: String,
    val result: UserContextDto?,
    val error: OdooError?
)

data class UserContextDto(
    val uid: Int,
    val name: String,
    val username: String,
    @SerializedName("session_id") val sessionId: String? // A veces viene en cookies, a veces en body
)

data class OdooError(
    val message: String,
    val data: Any?
)