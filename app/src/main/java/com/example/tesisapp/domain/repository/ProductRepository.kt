package com.example.tesisapp.domain.repository

import com.example.tesisapp.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(): Flow<List<Product>>
}