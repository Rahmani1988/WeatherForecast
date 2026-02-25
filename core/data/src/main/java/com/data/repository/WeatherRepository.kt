package com.data.repository

import com.data.model.current.CurrentWeather
import com.data.model.forecast.Forecast

/**
 * Repository interface for fetching forecast data.
 */
interface WeatherRepository {
    /**
     * Fetches the current weather for a given location.
     */
    suspend fun fetchCurrentWeather(location: String): Result<CurrentWeather>

    /**
     * Fetches the forecast for a given location and number of days.
     */
    suspend fun fetchForecast(location: String, days: Int): Result<Forecast>
}