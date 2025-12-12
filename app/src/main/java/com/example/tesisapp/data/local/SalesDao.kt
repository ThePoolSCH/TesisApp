package com.example.tesisapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    // --- Productos ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    // --- Campañas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaigns(campaigns: List<CampaignEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargets(targets: List<CampaignTargetEntity>)

    @Transaction // Necesario para obtener relaciones
    @Query("SELECT * FROM campaigns")
    fun getCampaignsWithTargets(): Flow<List<CampaignWithTargets>>

    @Query("DELETE FROM campaigns")
    suspend fun clearCampaigns()

    @Query("DELETE FROM campaign_targets")
    suspend fun clearTargets()

    @Transaction
    suspend fun replaceCampaigns(campaigns: List<CampaignEntity>, targets: List<CampaignTargetEntity>) {
        clearCampaigns() // El cascade borrará los targets viejos
        insertCampaigns(campaigns)
        insertTargets(targets)
    }
}