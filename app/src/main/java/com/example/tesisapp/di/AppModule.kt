package com.example.tesisapp.di

import android.content.Context
import androidx.room.Room
import com.example.tesisapp.data.local.AppDatabase
import com.example.tesisapp.data.local.dao.ProductDao
import com.example.tesisapp.domain.repository.ProductRepository
import com.example.tesisapp.data.repository.ProductRepositoryImpl
import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.repository.UserRepositoryImpl
import com.example.tesisapp.domain.repository.UserRepository
import com.example.tesisapp.data.local.dao.LocationDao
import com.example.tesisapp.data.repository.LocationRepositoryImpl
import com.example.tesisapp.domain.repository.LocationRepository
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.repository.TaskRepositoryImpl
import com.example.tesisapp.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "tesis_app_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideProductDao(appDatabase: AppDatabase): ProductDao {
        return appDatabase.productDao()
    }

    @Provides
    @Singleton
    fun provideLocationDao(appDatabase: AppDatabase): LocationDao {
        return appDatabase.locationDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }
}

// Un módulo aparte para los bindings de las interfaces
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository
}