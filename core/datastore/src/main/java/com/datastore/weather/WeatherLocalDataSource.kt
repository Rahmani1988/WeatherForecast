package com.datastore.weather

import com.common.model.models.WeatherSyncModel
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the local data source for weather data.
 */
interface WeatherLocalDataSource {
    /**
     * Flow of the weather data.
     */
    val weatherFlow: Flow<WeatherSyncModel>

    /**
     * Saves the weather data.
     */
    suspend fun saveWeather(model: WeatherSyncModel)
}