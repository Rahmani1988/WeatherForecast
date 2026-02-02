package com.currentweather.data.model.forecast

import ForecastDTO
import com.common.model.models.Current
import com.common.model.models.Location
import com.currentweather.data.model.common.toCurrentResult
import com.currentweather.data.model.common.toLocationResult

data class Forecast(
    val location: Location,
    val current: Current,
    val forecast: ForecastData
)

fun ForecastDTO.toForecastResult(): Result<Forecast> {
    return runCatching {
        val currentDtoNonNull = currentDTO
            ?: throw IllegalArgumentException("CurrentDTO is missing for Forecast and cannot be null.")
        val locationDtoNonNull = locationDTO
            ?: throw IllegalArgumentException("LocationDTO is missing for Forecast and cannot be null.")
        val forecastDtoNonNull = forecastDTO
            ?: throw IllegalArgumentException("ForecastDTO is missing for Forecast and cannot be null.")

        val current = currentDtoNonNull.toCurrentResult().getOrThrow()
        val location = locationDtoNonNull.toLocationResult().getOrThrow()
        val forecast = forecastDtoNonNull.toForecastData()

        Forecast(
            current = current,
            location = location,
            forecast = forecast
        )
    }
}