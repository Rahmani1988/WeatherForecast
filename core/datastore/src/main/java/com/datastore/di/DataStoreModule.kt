package com.datastore.di

import com.datastore.user.DataStoreUserPreferenceManager
import com.datastore.user.UserPreferenceManager
import com.datastore.weather.DataStoreWeatherDataSource
import com.datastore.weather.WeatherLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferenceManager(
        manager: DataStoreUserPreferenceManager
    ): UserPreferenceManager

    @Binds
    @Singleton
    abstract fun bindWeatherLocalDataSource(
        impl: DataStoreWeatherDataSource
    ): WeatherLocalDataSource
}