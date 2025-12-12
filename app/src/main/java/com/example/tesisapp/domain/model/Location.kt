package com.example.tesisapp.domain.model


data class Location(
    val id: Int,
    val name: String,
    val address: String,
    val status: String,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0

)