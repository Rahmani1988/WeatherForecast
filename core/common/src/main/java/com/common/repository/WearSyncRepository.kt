package com.common.repository

import kotlinx.coroutines.flow.Flow

interface WearSyncRepository {
    /**
     * Syncs the current weather data to the wearable device. Mobile -> Wear
     *
     * Returns [Result.Success] if the task was handed off to the system successfully.
     */
    suspend fun syncWeather(city: String, summary: String): Result<Unit>
}