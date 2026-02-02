package com.sync.di

import android.content.Context
import com.common.repository.WearSyncRepository
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import com.sync.repository.DefaultWearSyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearSyncModule {

    @Binds
    @Singleton
    abstract fun bindWearSyncRepository(
        impl: DefaultWearSyncRepository
    ): WearSyncRepository

    companion object {
        @Provides
        @Singleton
        fun provideDataClient(@ApplicationContext context: Context): DataClient {
            return Wearable.getDataClient(context)
        }
    }
}