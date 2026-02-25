package com.di

import com.data.repository.DefaultWeatherRepository
import com.data.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class DataModule {

    @Binds
    @Reusable
    abstract fun bindCurrentWeatherRepository(
        defaultCurrentWeatherRepository: DefaultWeatherRepository
    ): WeatherRepository
}