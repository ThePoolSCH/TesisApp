package com.example.tesisapp.domain.model

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val sessionId: String // Importante para futuras peticiones a Odoo
)