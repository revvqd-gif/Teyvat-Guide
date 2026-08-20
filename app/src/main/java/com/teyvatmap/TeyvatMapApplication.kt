package com.teyvatmap

import android.app.Application
import com.teyvatmap.data.db.TeyvatMapDatabase
import com.teyvatmap.data.CookieManager
import com.teyvatmap.data.MapRepository
import com.teyvatmap.data.MapRepositoryImpl
import com.teyvatmap.data.HoyoApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.logging.HttpLoggingInterceptor
import com.squareup.retrofit2.Retrofit
import com.squareup.retrofit2.converter.moshi.MoshiConverterFactory
import com.jakewharton.retrofit2.kotlin.coroutines.CoroutineCallAdapterFactory
import javax.inject.Singleton

class TeyvatMapApplication : Application() {

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        @Volatile
        private var INSTANCE: TeyvatMapApplication? = null

        fun getInstance(): TeyvatMapApplication {
            return INSTANCE ?: error("Application not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }

    val database: TeyvatMapDatabase by lazy { TeyvatMapDatabase.getInstance(this) }
    val cookieManager: CookieManager by lazy { CookieManager(this) }
    val moshi: Moshi by lazy { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        OkHttpClient.Builder().addInterceptor(logging).build()
    }
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api-takumi.mihoyo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }
    val apiService: HoyoApiService by lazy { retrofit.create(HoyoApiService::class.java) }
    val repository: MapRepository by lazy { MapRepositoryImpl(apiService, cookieManager) }
}