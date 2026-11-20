package com.example.tesisapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient

@HiltAndroidApp
class TesisApp : Application(), ImageLoaderFactory {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CoilNetworkEntryPoint {
        fun okHttpClient(): OkHttpClient
    }

    override fun newImageLoader(): ImageLoader {
        val entryPoint = EntryPointAccessors.fromApplication(this, CoilNetworkEntryPoint::class.java)
        return ImageLoader.Builder(this)
            .okHttpClient(entryPoint.okHttpClient())
            .build()
    }
}