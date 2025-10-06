package com.example.tesisapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products_table")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val code: String,
    val category: String,
    val sku: String,
    val price: Double,
    val stock: Int,
    val description: String,
    val type: String
)