package com.example.tesisapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.tesisapp.data.sync.OutboxRepository
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class TesisApp : Application(), ImageLoaderFactory, Configuration.Provider {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CoilNetworkEntryPoint {
        fun okHttpClient(): OkHttpClient
    }

    // F4: WorkManager crea el SyncWorker con sus dependencias de Hilt
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        OutboxRepository.programar(this) // envía lo que haya quedado pendiente de la sesión anterior
    }

    override fun newImageLoader(): ImageLoader {
        val entryPoint = EntryPointAccessors.fromApplication(this, CoilNetworkEntryPoint::class.java)
        return ImageLoader.Builder(this)
            .okHttpClient(entryPoint.okHttpClient())
            .build()
    }
}
