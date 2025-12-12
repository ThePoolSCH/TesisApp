package com.example.tesisapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val code: String,
    val price: Double,
    val uomName: String,
    val imageUrl: String
)

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String,
    val metric: String,
    val startDate: String,
    val endDate: String
)

@Entity(
    tableName = "campaign_targets",
    foreignKeys = [
        ForeignKey(
            entity = CampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["campaignId"])]
)
data class CampaignTargetEntity(
    @PrimaryKey val lineId: Int,
    val campaignId: Int, // FK
    val productId: Int,
    val productName: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val achievementPercent: Double,
    val imageUrl: String
)

data class CampaignWithTargets(
    @Embedded val campaign: CampaignEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "campaignId"
    )
    val targets: List<CampaignTargetEntity>
)