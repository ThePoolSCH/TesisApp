package com.example.tesisapp.domain.model

data class Product(
    val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val stock: Int,
    // Puedes añadir los otros campos si los vas a mostrar en la UI
     val code: String,
     val sku: String,
     val description: String,
     val type: String
)