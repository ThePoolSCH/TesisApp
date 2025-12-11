package com.example.tesisapp.di

import android.content.Context
import androidx.room.Room
import com.example.tesisapp.data.local.AppDatabase
import com.example.tesisapp.data.local.RouteDao
import com.example.tesisapp.data.local.dao.ProductDao
import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.local.dao.LocationDao
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.repository.ProductRepositoryImpl
import com.example.tesisapp.data.repository.UserRepositoryImpl
import com.example.tesisapp.data.repository.LocationRepositoryImpl
import com.example.tesisapp.data.repository.TaskRepositoryImpl
import com.example.tesisapp.data.repository.AuthRepositoryImpl // Importante
import com.example.tesisapp.domain.repository.ProductRepository
import com.example.tesisapp.domain.repository.UserRepository
import com.example.tesisapp.domain.repository.LocationRepository
import com.example.tesisapp.domain.repository.TaskRepository
import com.example.tesisapp.domain.repository.AuthRepository // Importante
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.repository.RouteRepositoryImpl
import com.example.tesisapp.domain.repository.RouteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.util.concurrent.TimeUnit

// 1. MÓDULO DE PROVEEDORES (Cosas con configuración manual: Retrofit, Room, etc.)
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- BASE DE DATOS ROOM ---
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
    fun provideUserDao(appDatabase: AppDatabase): UserDao = appDatabase.userDao()

    @Provides
    @Singleton
    fun provideProductDao(appDatabase: AppDatabase): ProductDao = appDatabase.productDao()

    @Provides
    @Singleton
    fun provideLocationDao(appDatabase: AppDatabase): LocationDao = appDatabase.locationDao()

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    // --- RED (RETROFIT & ODOO) ---
    // MOVIDO AQUÍ: Porque requiere código de configuración
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)

        val cookieHandler = CookieManager()

        return OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieHandler))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // MOVIDO AQUÍ: Porque requiere código de configuración
    @Provides
    @Singleton
    fun provideOdooApi(client: OkHttpClient): OdooApiService {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8069/") // IP Emulador. Usa tu IP real si es dispositivo físico.
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OdooApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRouteDao(appDatabase: AppDatabase): RouteDao = appDatabase.routeDao()
}

// 2. MÓDULO DE VINCULACIÓN (Interfaces -> Implementaciones)
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

    // AGREGADO AQUÍ: AuthRepository se debe vincular con @Binds igual que los demás
    // (Asegúrate de que AuthRepositoryImpl tenga @Inject constructor)
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(
        routeRepositoryImpl: RouteRepositoryImpl
    ): RouteRepository
}