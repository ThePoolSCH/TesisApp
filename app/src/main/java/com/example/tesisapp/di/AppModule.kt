package com.example.tesisapp.di

import android.content.Context
import androidx.room.Room
import com.example.tesisapp.data.local.AppDatabase
import com.example.tesisapp.data.local.RouteDao
import com.example.tesisapp.data.local.SalesDao
import com.example.tesisapp.data.local.dao.UserDao
import com.example.tesisapp.data.local.dao.LocationDao
import com.example.tesisapp.data.local.dao.TaskDao
import com.example.tesisapp.data.repository.UserRepositoryImpl
import com.example.tesisapp.data.repository.LocationRepositoryImpl
import com.example.tesisapp.data.repository.TaskRepositoryImpl
import com.example.tesisapp.data.repository.AuthRepositoryImpl
import com.example.tesisapp.domain.repository.UserRepository
import com.example.tesisapp.domain.repository.LocationRepository
import com.example.tesisapp.domain.repository.TaskRepository
import com.example.tesisapp.domain.repository.AuthRepository
import com.example.tesisapp.data.remote.OdooApiService
import com.example.tesisapp.data.remote.SalesApi
import com.example.tesisapp.data.repository.RouteRepositoryImpl
import com.example.tesisapp.data.repository.SalesRepositoryImpl
import com.example.tesisapp.domain.repository.RouteRepository
import com.example.tesisapp.domain.repository.SalesRepository
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

// 1. MÓDULO DE PROVEEDORES
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
    fun provideLocationDao(appDatabase: AppDatabase): LocationDao = appDatabase.locationDao()

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao = appDatabase.taskDao()

    @Provides
    @Singleton
    fun provideRouteDao(appDatabase: AppDatabase): RouteDao = appDatabase.routeDao()

    @Provides
    @Singleton
    fun provideSalesDao(db: AppDatabase): SalesDao = db.salesDao()

    // --- RED (RETROFIT & ODOO) ---

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

    // --- CORRECCIÓN AQUÍ ---
    // 1. Creamos Retrofit por separado para que SalesApi pueda usarlo también.
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8069/") // IP Emulador
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 2. OdooApiService ahora pide el Retrofit ya creado (antes lo creaba él mismo)
    @Provides
    @Singleton
    fun provideOdooApi(retrofit: Retrofit): OdooApiService {
        return retrofit.create(OdooApiService::class.java)
    }

    // 3. SalesApi ahora funciona porque ya existe "provideRetrofit" arriba
    @Provides
    @Singleton
    fun provideSalesApi(retrofit: Retrofit): SalesApi {
        return retrofit.create(SalesApi::class.java)
    }

    // --- REPOSITORIOS MANUALES ---

    @Provides
    @Singleton
    fun provideSalesRepository(api: SalesApi, dao: SalesDao): SalesRepository {
        return SalesRepositoryImpl(api, dao)
    }
}

// 2. MÓDULO DE VINCULACIÓN
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository
}