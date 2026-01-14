package com.example.tesisapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.local.entity.UserEntity
import com.example.tesisapp.data.local.entity.LocationEntity
import com.example.tesisapp.data.local.dao.LocationDao
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.local.entity.TaskDefinitionEntity
@Database(
    entities = [UserEntity::class, ProductEntity::class, LocationEntity::class, TaskDefinitionEntity::class, RouteEntity::class, StopEntity::class, CampaignEntity::class, CampaignTargetEntity::class],
    version = 48,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun taskDao(): TaskDao
    abstract fun routeDao(): RouteDao
    abstract fun salesDao(): SalesDao
}