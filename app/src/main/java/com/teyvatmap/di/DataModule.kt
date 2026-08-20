package com.teyvatmap.di

import android.content.Context
import com.google.dagger.hilt.InstallIn
import com.google.dagger.hilt.components.SingletonComponent
import com.teyvatmap.data.CookieManager
import com.teyvatmap.data.MapRepository
import com.teyvatmap.data.MapRepositoryImpl
import com.teyvatmap.data.TeyvatMapDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideTeyvatMapDatabase(@ApplicationContext context: Context): TeyvatMapDatabase {
        return TeyvatMapDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCookieManager(@ApplicationContext context: Context): CookieManager {
        return CookieManager(context)
    }

    @Provides
    @Singleton
    fun provideMapRepository(
        apiService: com.teyvatmap.data.HoyoApiService,
        cookieManager: CookieManager
    ): MapRepository {
        return MapRepositoryImpl(apiService, cookieManager)
    }
}