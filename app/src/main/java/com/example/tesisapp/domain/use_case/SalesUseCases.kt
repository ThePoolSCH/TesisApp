package com.example.tesisapp.domain.use_case

import com.example.tesisapp.domain.model.OrderItem
import com.example.tesisapp.domain.repository.SalesRepository
import javax.inject.Inject

class SyncCatalogUseCase @Inject constructor(private val repo: SalesRepository) {
    suspend operator fun invoke() = repo.syncProducts()
}

class GetCatalogUseCase @Inject constructor(private val repo: SalesRepository) {
    operator fun invoke() = repo.getProducts()
}

class SyncCampaignsUseCase @Inject constructor(private val repo: SalesRepository) {
    suspend operator fun invoke() = repo.syncCampaigns()
}

class GetCampaignsUseCase @Inject constructor(private val repo: SalesRepository) {
    operator fun invoke() = repo.getCampaigns()
}

class SubmitOrderUseCase @Inject constructor(private val repo: SalesRepository) {
    suspend operator fun invoke(routeLineId: Int, items: List<OrderItem>) =
        repo.createOrder(routeLineId, items)
}