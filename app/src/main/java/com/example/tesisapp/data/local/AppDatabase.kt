package com.example.tesisapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.tesisapp.data.local.dao.ProductDao
import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.local.entity.ProductEntity
import com.example.tesisapp.data.local.entity.UserEntity
import com.example.tesisapp.data.local.entity.LocationEntity
import com.example.tesisapp.data.local.dao.LocationDao

@Database(
    entities = [UserEntity::class, ProductEntity::class, LocationEntity::class], // <-- AÑADIR ProductEntity
    version = 4, // <-- INCREMENTAR VERSIÓN A 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao // <-- AÑADIR METODO
    abstract fun locationDao(): LocationDao
}