package com.common.repository

import kotlinx.coroutines.flow.Flow

interface WearSyncRepository {
    /**
     * Syncs the current weather data to the wearable device. Mobile -> Wear
     * Returns [Result.Success] if the task was handed off to the system successfully.
     */
    suspend fun syncWeather(city: String, summary: String): Result<Unit>

    /**
     * Updates the local weather data. Wear Internal
     */
    suspend fun updateLocalWeather(city: String, summary: String)

    fun getWeatherFlow(): Flow<WeatherModel?>
}