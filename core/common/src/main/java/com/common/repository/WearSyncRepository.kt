package com.common.repository

interface WearSyncRepository {
    /**
     * Syncs the current weather data to the wearable device.
     * Returns [Result.Success] if the task was handed off to the system successfully.
     */
    suspend fun syncWeather(city: String, summary: String): Result<Unit>
}