package com.example.tesisapp.domain.use_case

import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return repository.getProducts()
    }
}