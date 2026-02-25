package com.currentweather.di

import com.currentweather.data.repository.DefaultLocationRepository
import com.currentweather.data.repository.DefaultSearchLocationLocationRepository
import com.currentweather.data.repository.LocationRepository
import com.currentweather.data.repository.SearchLocationRepository
import dagger.Binds
import dagger.Module
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class CurrentWeatherModule {

    @Binds
    @Reusable
    abstract fun bindLocationRepository(
        defaultLocationRepository: DefaultLocationRepository
    ): LocationRepository

    @Binds
    @Reusable
    abstract fun bindSearchLocationRepository(
        defaultLocationRepository: DefaultSearchLocationLocationRepository
    ): SearchLocationRepository
}