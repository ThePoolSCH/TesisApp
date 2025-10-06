package com.example.tesisapp.data.repository

import com.example.tesisapp.data.local.dao.ProductDao
import com.example.tesisapp.data.local.entity.ProductEntity
import com.example.tesisapp.domain.model.Product
import com.example.tesisapp.domain.repository.ProductRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.String

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productDao: ProductDao
) : ProductRepository {

    // Mapper de Entity (DB) a Model (Domain)
    private fun ProductEntity.toDomain(): Product {
        return Product(
            id = this.id,
            name = this.name,
            category = this.category,
            price = this.price,
            stock = this.stock,
            code = this.code,
            sku = this.sku,
            description = this.description,
            type = this.type,
        )
    }

    init {
        // Al crear el repositorio, comprobamos si la DB está vacía
        // y la poblamos con datos iniciales.
        CoroutineScope(Dispatchers.IO).launch {
            if (productDao.getProductCount() == 0) {
                productDao.insertAll(getHardcodedProducts())
            }
        }
    }

    override fun getProducts(): Flow<List<Product>> {
        return productDao.getProducts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // --- DATOS HARDCODEADOS ---
    private fun getHardcodedProducts(): List<ProductEntity> {
        return listOf(
            ProductEntity(name = "Laptop Gamer Pro", code = "LGP-001", category = "Electrónica", sku = "SKU12345", price = 1200.50, stock = 15, description = "Potente laptop para gaming.", type = "Hardware"),
            ProductEntity(name = "Teclado Mecánico RGB", code = "TMR-002", category = "Periféricos", sku = "SKU67890", price = 89.99, stock = 50, description = "Teclado con switches rojos.", type = "Accesorio"),
            ProductEntity(name = "Monitor Curvo 27\"", code = "MC27-003", category = "Monitores", sku = "SKU10111", price = 350.00, stock = 25, description = "Monitor 144Hz para una experiencia inmersiva.", type = "Hardware")
        )
    }
}